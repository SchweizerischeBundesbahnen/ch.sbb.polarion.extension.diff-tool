import { useEffect, useMemo, useState } from 'react';
import { Modal, SearchableSelect } from '@sbb-polarion/react-sbb-polarion';
import { sendRequest } from '../services/useRemote';
import MergeResultDialog, { type MergeOutcome } from './MergeResultDialog';
import PanelShell from './PanelShell';
import { reloadDocument } from './documentReload';
import { FieldCell, FieldRow, SwitchRow } from './formRows';
import type { PanelProps } from './panelProps';
import { rememberedIfOffered, useAdoptRemembered, useRemembering } from './rememberedSelection';
import { clearReports, reportFailure } from './reporting';
import useRemoteList, { firstError, firstLoading } from './useRemoteList';

interface SpaceInfo {
  id: string;
  name: string;
}

interface DocumentInfo {
  id: string;
  title: string;
}

/** What `POST /merge/chapter` returns, and what polling the job answers with. */
interface ChapterMergeJobInfo {
  jobId: string;
  state?: string | null;
  statusMessage?: string | null;
  logUrl?: string | null;
  mergeResult?: MergeResult | null;
}

interface ChapterMergeInfo {
  insertedOutlineNumber?: string | null;
  createdWorkItemIds?: string[];
  movedWorkItemIds?: string[];
  referencedWorkItemIds?: string[];
  copiedLayoutTypeIds?: string[];
}

interface MergeReport {
  warnings?: unknown[];
  prohibited?: unknown[];
  creationFailed?: unknown[];
  logs?: string;
}

interface MergeResult {
  success: boolean;
  targetModuleHasStructuralChanges?: boolean;
  mergeNotAuthorized?: boolean;
  mergeReport?: MergeReport | null;
  chapterMergeInfo?: ChapterMergeInfo | null;
}

const encode = (segment: string) => encodeURIComponent(segment);

// The ids of the selects, which double as the cookie names the remembered selections are kept under (see
// rememberedSelection.ts). They carry the `merge-` prefix of their own, so this panel does not share its
// memory with the comparison and the copy panel.
const PROJECT_SELECT = 'merge-project-selector';
const SPACE_SELECT = 'merge-space-selector';
const DOCUMENT_SELECT = 'merge-document-selector';
const MODE_SELECT = 'merge-mode-selector';
const INSERT_MODE_SELECT = 'merge-insert-mode-selector';
const REFERENCED_ITEMS_SELECT = 'merge-referenced-items-selector';

const MERGE_ERROR = 'Error merging chapter';

/** An outline number as the document shows it, eg. `2` or `2.1.1`. */
const OUTLINE_NUMBER_PATTERN = /^\d+(\.\d+)*$/;

/** How often the panel asks whether its merge job has finished, and for how long it keeps asking. */
const POLL_INTERVAL_MS = 2000;
const POLL_ATTEMPTS = 450;

const MODES = [
  { id: 'COPY', name: 'copy - create new workitems' },
  { id: 'MOVE', name: 'move - move workitems, copy headings' },
];

const INSERT_MODES = [
  { id: 'UNDER', name: 'under - directly under the target chapter' },
  { id: 'AFTER', name: 'after - as a new chapter of the same level' },
];

const REFERENCED_ITEMS = [
  { id: 'KEEP_REFERENCE', name: 'reference the same workitem' },
  { id: 'COPY_AS_NEW', name: 'create a new workitem' },
  { id: 'SKIP', name: 'skip and report' },
];

const sleep = (milliseconds: number) => new Promise((resolve) => setTimeout(resolve, milliseconds));

/**
 * The "Documents Merge" Document Properties panel: it copies or moves a chapter of another document,
 * with everything below it, into the document which is currently open.
 *
 * Note the direction. The open document, which the server injects as the `source*` properties every panel
 * gets, is here the **target** of the operation, and the document the user picks is the source.
 *
 * A merge can take long, so the server carries it out as a Polarion job: this panel schedules the job and
 * then asks for its result until it is there.
 */
export default function MergeToolPanel({ props }: { props: PanelProps }) {
  const projectIds = useMemo(() => props.projects.map((project) => project.id), [props.projects]);

  const [projectId, setProjectId] = useState(() => rememberedIfOffered(PROJECT_SELECT, projectIds));
  const [spaceId, setSpaceId] = useState('');
  const [documentId, setDocumentId] = useState('');
  const [mode, setMode] = useState('COPY');
  const [insertMode, setInsertMode] = useState('UNDER');
  const [referencedItems, setReferencedItems] = useState('KEEP_REFERENCE');
  const [sourceChapter, setSourceChapter] = useState('');
  const [targetChapter, setTargetChapter] = useState('');
  // Off by default: it is the one option which changes the configuration of the target document rather than
  // its content, and a document whose layout cannot be resolved is one Polarion refuses to open.
  const [copyWorkItemLayouts, setCopyWorkItemLayouts] = useState(false);

  const [confirming, setConfirming] = useState(false);
  const [merging, setMerging] = useState<string | null>(null);
  const [outcome, setOutcome] = useState<MergeOutcome | null>(null);

  const spaces = useRemoteList<SpaceInfo>({
    url: projectId ? `/projects/${encode(projectId)}/spaces` : null,
    progressMessage: 'Loading spaces',
    errorMessage: 'Error occurred loading spaces',
  });

  const documents = useRemoteList<DocumentInfo>({
    url: projectId && spaceId ? `/projects/${encode(projectId)}/spaces/${encode(spaceId)}/documents` : null,
    progressMessage: 'Loading documents',
    errorMessage: 'Error occurred loading documents',
  });

  const chooseProject = useRemembering(PROJECT_SELECT, setProjectId);
  const chooseSpace = useRemembering(SPACE_SELECT, setSpaceId);
  const chooseDocument = useRemembering(DOCUMENT_SELECT, setDocumentId);
  const chooseMode = useRemembering(MODE_SELECT, setMode);
  const chooseInsertMode = useRemembering(INSERT_MODE_SELECT, setInsertMode);
  const chooseReferencedItems = useRemembering(REFERENCED_ITEMS_SELECT, setReferencedItems);

  useEffect(() => setSpaceId(''), [projectId]);
  useEffect(() => setDocumentId(''), [projectId, spaceId]);
  const spaceIds = useMemo(() => spaces.items.map((space) => space.id), [spaces.items]);
  const documentIds = useMemo(() => documents.items.map((document) => document.id), [documents.items]);
  useAdoptRemembered(SPACE_SELECT, spaceIds, setSpaceId);
  useAdoptRemembered(DOCUMENT_SELECT, documentIds, setDocumentId);

  const sourceDocumentTitle = documents.items.find((document) => document.id === documentId)?.title ?? documentId;

  const busy = merging ?? firstLoading(spaces, documents);
  const loadError = firstError(spaces, documents);

  // Both chapters are mandatory, and both are outline numbers of the documents they belong to.
  const chaptersValid =
    OUTLINE_NUMBER_PATTERN.test(sourceChapter.trim()) && OUTLINE_NUMBER_PATTERN.test(targetChapter.trim());
  const canMerge = Boolean(projectId && spaceId && documentId && mode && insertMode) && chaptersValid;

  const confirm = () => {
    setConfirming(false);
    void merge();
  };

  const merge = async () => {
    setMerging('Merging chapter');
    clearReports();
    try {
      const jobInfo = await scheduleMerge();
      setMerging(`Merging chapter ${sourceChapter.trim()}, this can take a while`);
      const finishedJob = await awaitResult(jobInfo.jobId);
      setOutcome(outcomeOf(finishedJob));
    } catch (caught) {
      // The merge never got as far as a result of its own, so there is nothing to show but what went wrong
      reportFailure((caught as Error).message || MERGE_ERROR);
    } finally {
      setMerging(null);
    }
  };

  /** The dialog is the user's acknowledgement, so the document is reloaded once it is closed - if it changed. */
  const closeOutcome = () => {
    const merged = outcome?.merged ?? false;
    setOutcome(null);
    if (merged) {
      reloadDocument();
    }
  };

  const scheduleMerge = async (): Promise<ChapterMergeJobInfo> => {
    const response = await sendRequest({
      method: 'POST',
      url: '/merge/chapter',
      contentType: 'application/json',
      body: JSON.stringify({
        sourceDocument: { projectId: projectId, spaceId: spaceId, name: documentId },
        targetDocument: {
          projectId: props.sourceProjectId,
          spaceId: props.sourceSpaceId,
          name: props.sourceDocument,
        },
        mode: mode,
        insertMode: insertMode,
        sourceChapterOutlineNumber: sourceChapter.trim(),
        targetChapterOutlineNumber: targetChapter.trim(),
        referencedItems: referencedItems,
        copyWorkItemLayouts: copyWorkItemLayouts,
      }),
    });
    const text = await response.text();
    if (!response.ok) {
      throw new Error(messageFrom(text));
    }
    return JSON.parse(text) as ChapterMergeJobInfo;
  };

  /** Asks the merge job whether it has finished until it has. A job which never answers is given up on. */
  const awaitResult = async (jobId: string): Promise<ChapterMergeJobInfo> => {
    for (let attempt = 0; attempt < POLL_ATTEMPTS; attempt++) {
      if (attempt > 0) {
        await sleep(POLL_INTERVAL_MS);
      }
      const response = await sendRequest({ method: 'GET', url: `/merge/chapter/jobs/${encode(jobId)}` });
      if (!response.ok) {
        throw new Error(messageFrom(await response.text()));
      }
      const jobInfo = (await response.json()) as ChapterMergeJobInfo;
      if (jobInfo.mergeResult) {
        return jobInfo;
      }
      if (jobInfo.state === 'FINISHED') {
        throw new Error(jobInfo.statusMessage || 'The merge finished without a result, see the job log');
      }
    }
    throw new Error(`The merge is still running, see the job log at /polarion/job-report?jobId=${jobId}`);
  };

  return (
    <PanelShell prefix="merge" busy={busy} error={loadError}>
      <p>
        Please select the <strong>source</strong> document and the chapter to be merged into the current document, as
        well as the chapter of the current document the content is placed at.
      </p>

      {/* Which document content is taken from. */}
      <div className="diff-section">
        <FieldRow label="Project:" labelFor={PROJECT_SELECT}>
          <FieldCell>
            <SearchableSelect
              id={PROJECT_SELECT}
              value={projectId}
              onChange={chooseProject}
              options={props.projects}
              placeholder="Select Project..."
              allowEmpty
            />
          </FieldCell>
        </FieldRow>
        <FieldRow label="Space:" labelFor={SPACE_SELECT}>
          <FieldCell>
            <SearchableSelect
              id={SPACE_SELECT}
              value={spaceId}
              onChange={chooseSpace}
              options={spaces.items}
              placeholder="Select Space..."
              allowEmpty
            />
          </FieldCell>
        </FieldRow>
        <FieldRow label="Document:" labelFor={DOCUMENT_SELECT}>
          <FieldCell>
            <SearchableSelect
              id={DOCUMENT_SELECT}
              value={documentId}
              onChange={chooseDocument}
              options={documents.items.map((document) => ({ id: document.id, name: document.title }))}
              placeholder="Select Document..."
              allowEmpty
            />
          </FieldCell>
        </FieldRow>
        <FieldRow label="Source chapter:" labelFor="merge-source-chapter-input">
          <FieldCell>
            <input
              id="merge-source-chapter-input"
              type="text"
              placeholder="eg. 2.1.1"
              value={sourceChapter}
              onChange={(event) => setSourceChapter(event.target.value)}
            />
          </FieldCell>
        </FieldRow>
        <FieldRow label="Copy mode:" labelFor={MODE_SELECT}>
          <FieldCell>
            <SearchableSelect
              id={MODE_SELECT}
              value={mode}
              onChange={chooseMode}
              options={MODES}
              placeholder="Select Mode..."
            />
          </FieldCell>
        </FieldRow>
      </div>

      {/* Where the content lands in the document which is currently open. */}
      <div className="diff-section group-start">
        <FieldRow label="Target chapter:" labelFor="merge-target-chapter-input">
          <FieldCell>
            <input
              id="merge-target-chapter-input"
              type="text"
              placeholder="eg. 3.1"
              value={targetChapter}
              onChange={(event) => setTargetChapter(event.target.value)}
            />
          </FieldCell>
        </FieldRow>
        <FieldRow label="Insert mode:" labelFor={INSERT_MODE_SELECT}>
          <FieldCell>
            <SearchableSelect
              id={INSERT_MODE_SELECT}
              value={insertMode}
              onChange={chooseInsertMode}
              options={INSERT_MODES}
              placeholder="Select Insert Mode..."
            />
          </FieldCell>
        </FieldRow>
      </div>

      {/* What the merge carries over besides the work items themselves. */}
      <div className="diff-section group-start">
        <FieldRow label="Referenced workitems:" labelFor={REFERENCED_ITEMS_SELECT}>
          <FieldCell>
            <SearchableSelect
              id={REFERENCED_ITEMS_SELECT}
              value={referencedItems}
              onChange={chooseReferencedItems}
              options={REFERENCED_ITEMS}
              placeholder="Select Behaviour..."
            />
          </FieldCell>
        </FieldRow>
        <SwitchRow
          id="merge-copy-layouts-checkbox"
          label="Copy missing workitem layouts into the document"
          checked={copyWorkItemLayouts}
          onChange={setCopyWorkItemLayouts}
        />
      </div>

      <div className="buttons-wrapper">
        <button
          type="button"
          id="merge-chapter"
          disabled={!canMerge || busy !== null}
          onClick={() => setConfirming(true)}
        >
          <span className="sbb-icon-table-plus" role="img" aria-label="Merge" />
          Merge Chapter
        </button>
      </div>

      {/* What the merge is about to do. A merge changes two documents and cannot be undone from here, so it is
          stated in the user's own terms and waits to be confirmed. */}
      <Modal
        open={confirming}
        title="Merge chapter"
        okText="Confirm"
        cancelText="Cancel"
        onOk={confirm}
        onCancel={() => setConfirming(false)}
      >
        <div id="merge-confirmation">
          <p>
            Chapter <strong>{sourceChapter.trim()}</strong> of <strong>{sourceDocumentTitle}</strong> ({projectId} /{' '}
            {spaceId}) will be <strong>{mode === 'MOVE' ? 'moved' : 'copied'}</strong>{' '}
            <strong>{insertMode === 'AFTER' ? 'after' : 'under'}</strong> chapter{' '}
            <strong>{targetChapter.trim()}</strong> of <strong>{props.sourceDocumentTitle}</strong>.
          </p>
          {mode === 'MOVE' ? (
            <p>The workitems leave the source document. Chapter headings are copied, not moved.</p>
          ) : null}
          <p>Do you want to proceed?</p>
        </div>
      </Modal>

      {/* What the merge did. Closing it reloads the document when the merge changed it - see closeOutcome. */}
      <MergeResultDialog outcome={outcome} onClose={closeOutcome} />
    </PanelShell>
  );
}

/** What the merge did, or why it did not happen, as the dialog states it. */
function outcomeOf(jobInfo: ChapterMergeJobInfo): MergeOutcome {
  const mergeResult = jobInfo.mergeResult!;
  const info = mergeResult.chapterMergeInfo;
  const created = info?.createdWorkItemIds?.length ?? 0;
  const moved = info?.movedWorkItemIds?.length ?? 0;
  const referenced = info?.referencedWorkItemIds?.length ?? 0;
  const layouts = info?.copiedLayoutTypeIds?.length ?? 0;
  const warnings = mergeResult.mergeReport?.warnings?.length ?? 0;
  const failed = mergeResult.mergeReport?.creationFailed?.length ?? 0;

  const lines: string[] = [];
  if (mergeResult.success && info?.insertedOutlineNumber) {
    lines.push(`Merged as chapter ${info.insertedOutlineNumber}`);
  }
  if (created > 0) {
    lines.push(`${created} workitem(s) created`);
  }
  if (moved > 0) {
    lines.push(`${moved} workitem(s) moved`);
  }
  if (referenced > 0) {
    lines.push(`${referenced} workitem(s) referenced`);
  }
  if (layouts > 0) {
    lines.push(`${layouts} workitem layout(s) copied into the document`);
  }
  if (failed > 0) {
    lines.push(`${failed} workitem(s) could not be merged`);
  }
  if (warnings > 0) {
    lines.push(`${warnings} warning(s)`);
  }
  if (!mergeResult.success) {
    lines.unshift(failureMessage(mergeResult));
  } else if (lines.length === 0) {
    lines.push('Nothing was merged');
  }

  return {
    merged: created + moved + referenced > 0,
    title: mergeResult.success ? 'Chapter merged' : 'Chapter not merged',
    lines: lines,
    logs: mergeResult.mergeReport?.logs,
    logUrl: jobInfo.logUrl,
  };
}

/** Why the merge did not happen, in the words the server used where it had any. */
function failureMessage(mergeResult: MergeResult): string {
  if (mergeResult.mergeNotAuthorized) {
    return 'You are not authorized to merge into this document';
  }
  if (mergeResult.targetModuleHasStructuralChanges) {
    return 'The document has been changed meanwhile, please reload it and try again';
  }
  return MERGE_ERROR;
}

/**
 * The server's error text. The endpoints answer with `{ "message": "..." }`, but a failure upstream of
 * the resource (a proxy, a session timeout) can answer with anything, so a non-JSON body falls back to a
 * generic message rather than showing the user raw HTML.
 */
function messageFrom(body: string): string {
  try {
    const parsed = JSON.parse(body) as { message?: unknown };
    return typeof parsed.message === 'string' && parsed.message ? parsed.message : MERGE_ERROR;
  } catch {
    return MERGE_ERROR;
  }
}
