import { useEffect, useMemo, useState } from 'react';
import { SearchableSelect } from '@sbb-polarion/react-sbb-polarion';
import { sendAbsoluteRequest, sendRequest } from '../services/useRemote';
import ChapterMergeDialog, { type MergeOutcome, type MergeStage } from './ChapterMergeDialog';
import PanelShell from './PanelShell';
import { reloadDocument } from './documentReload';
import { FieldCell, FieldRow, SwitchRow } from './formRows';
import type { PanelProps } from './panelProps';
import { rememberedIfOffered, useAdoptRemembered, useRemembering } from './rememberedSelection';
import { clearReports } from './reporting';
import useRemoteList, { firstError, firstLoading } from './useRemoteList';

interface SpaceInfo {
  id: string;
  name: string;
}

interface DocumentInfo {
  id: string;
  title: string;
}

/** What polling a merge answers with while it is still running, and what a failed one answers with. */
interface ChapterMergeJobDetails {
  status?: string | null;
  progressMessage?: string | null;
  errorMessage?: string | null;
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

/**
 * How often the panel asks whether its merge has finished, and how long it keeps asking. The budget outlasts
 * the server's own limit on a merge (`chapter.merge.timeout`, 60 minutes by default), which answers a merge
 * that runs longer with a failure of its own.
 */
const POLL_INTERVAL_MS = 2000;
const POLL_ATTEMPTS = 2000;

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
 * A merge can take long, so the server carries it out in the background: this panel starts it and then asks
 * the job it was handed for the result until it is there.
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
  // On by default: a work item is rendered by the layout its type has in the document it lands in, so a copy
  // whose type the target document has no layout for is not shown the way it was in the source document.
  const [copyWorkItemLayouts, setCopyWorkItemLayouts] = useState(true);

  // Where the merge stands, from the question it asks to the result it reports. The dialog it is carried
  // out in reads it; `null` means there is no merge and no dialog.
  const [stage, setStage] = useState<MergeStage | null>(null);

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

  // The merge has a progress of its own, inside its dialog: this is the overlay of the lists the form offers.
  const busy = firstLoading(spaces, documents);
  const loadError = firstError(spaces, documents);

  // Both chapters are mandatory, and both are outline numbers of the documents they belong to.
  const chaptersValid =
    OUTLINE_NUMBER_PATTERN.test(sourceChapter.trim()) && OUTLINE_NUMBER_PATTERN.test(targetChapter.trim());
  const canMerge = Boolean(projectId && spaceId && documentId && mode && insertMode) && chaptersValid;

  /** Runs the merge in the dialog which asked for it: its stages are what that dialog shows. */
  const merge = async () => {
    setStage({ kind: 'running', message: 'Merging chapter' });
    clearReports();
    try {
      const jobUrl = await startMerge();
      setStage({ kind: 'running', message: `Merging chapter ${sourceChapter.trim()}, this can take a while` });
      const mergeResult = await awaitResult(jobUrl, (progress) => setStage({ kind: 'running', message: progress }));
      setStage({ kind: 'result', outcome: outcomeOf(mergeResult) });
    } catch (caught) {
      // The merge never got as far as a result of its own. The dialog says so where it was spinning a moment
      // ago: a merge which vanishes without a word leaves the user to work out for themselves whether their
      // document was touched.
      setStage({ kind: 'result', outcome: failureOutcome((caught as Error).message || MERGE_ERROR) });
    }
  };

  /** Closing the result is the user's acknowledgement, so the document is reloaded then - if it changed. */
  const closeDialog = () => {
    const merged = stage?.kind === 'result' && stage.outcome.merged;
    setStage(null);
    if (merged) {
      reloadDocument();
    }
  };

  /** Starts the merge and returns the URL of the job which delivers its result, as the server named it. */
  const startMerge = async (): Promise<string> => {
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
    if (!response.ok) {
      throw new Error(messageFrom(await response.text()));
    }
    const jobUrl = response.headers.get('Location');
    if (!jobUrl) {
      throw new Error('The merge was started without a job to ask for its result');
    }
    return jobUrl;
  };

  /**
   * Asks the merge whether it has finished until it has, and reports what it is doing in the meantime.
   *
   * A merge which is still running is answered with 202; a finished one redirects to its result, which the
   * browser follows, so the answer this sees is the result itself. A merge which is still running when the
   * budget is used up is left to finish on the server.
   */
  const awaitResult = async (jobUrl: string, onProgress: (message: string) => void): Promise<MergeResult> => {
    for (let attempt = 0; attempt < POLL_ATTEMPTS; attempt++) {
      if (attempt > 0) {
        await sleep(POLL_INTERVAL_MS);
      }
      const response = await sendAbsoluteRequest({ method: 'GET', url: jobUrl });
      if (response.status === 202) {
        const jobDetails = (await response.json()) as ChapterMergeJobDetails;
        if (jobDetails.progressMessage) {
          onProgress(jobDetails.progressMessage);
        }
        continue;
      }
      if (!response.ok) {
        throw new Error(messageFrom(await response.text()));
      }
      return (await response.json()) as MergeResult;
    }
    throw new Error('The merge is still running, reload the document later to see what it did');
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
          disabled={!canMerge || busy !== null || stage !== null}
          onClick={() => setStage({ kind: 'confirm' })}
        >
          <span className="sbb-icon-table-plus" role="img" aria-label="Merge" />
          Merge Chapter
        </button>
      </div>

      {/* The whole merge, in one dialog: what it is about to do - a merge changes two documents and cannot be
          undone from here - then its progress, then what it did. Closing the result reloads the document when
          the merge changed it, see closeDialog. */}
      <ChapterMergeDialog
        stage={stage}
        summary={
          <>
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
          </>
        }
        onConfirm={() => void merge()}
        onCancel={() => setStage(null)}
        onClose={closeDialog}
      />
    </PanelShell>
  );
}

/** What the merge did, or why it did not happen, as the dialog states it. */
function outcomeOf(mergeResult: MergeResult): MergeOutcome {
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
  };
}

/** A merge which produced no result of its own: what went wrong, in the words the server used. */
function failureOutcome(message: string): MergeOutcome {
  return {
    merged: false,
    title: 'Chapter not merged',
    lines: [message],
  };
}

/**
 * Why the merge did not happen, in the terms the user can act on: a merge reports what stopped it in its
 * own result.
 */
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
 * The server's error text. A failed merge answers with `{ "errorMessage": "..." }` and the endpoints answer
 * with `{ "message": "..." }`, but a failure upstream of the resource (a proxy, a session timeout) can answer
 * with anything, so a non-JSON body falls back to a generic message rather than showing the user raw HTML.
 */
function messageFrom(body: string): string {
  try {
    const parsed = JSON.parse(body) as { errorMessage?: unknown; message?: unknown };
    const message =
      typeof parsed.errorMessage === 'string' && parsed.errorMessage ? parsed.errorMessage : parsed.message;
    return typeof message === 'string' && message ? message : MERGE_ERROR;
  } catch {
    return MERGE_ERROR;
  }
}
