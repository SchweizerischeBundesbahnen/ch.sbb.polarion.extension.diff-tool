import { useEffect, useMemo, useState } from 'react';
import { faFilePdf } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import type { SettingName } from '@sbb-polarion/react-sbb-polarion';
import useRemoteList, { firstError } from '../formext/useRemoteList';
import ItemsTable, { type Column, EnumCell } from './ItemsTable';
import Paginator from './Paginator';
import PickerControls from './PickerControls';
import QueryPanel from './QueryPanel';
import TableFooter from './TableFooter';
import BatchExportProgressModal from './batch/BatchExportProgressModal';
import ExportOptionsPanel, { DEFAULT_EXPORT_OPTIONS } from './batch/ExportOptionsPanel';
import type { ExportOptions } from './batch/ExportOptionsPanel';
import RevisionSelect from './batch/RevisionSelect';
import TargetDocumentCell, { documentKey } from './batch/TargetDocumentCell';
import type { TargetResolution } from './batch/TargetDocumentCell';
import TitleWithId from './batch/TitleWithId';
import './batch/batch.css';
import { isSameDocument } from './batch/batchExport';
import type { DocumentPair, DocumentRef } from './batch/batchExport';
import useBatchDiffExport from './batch/useBatchDiffExport';
import type { BatchDiffExport } from './batch/useBatchDiffExport';
import useResolvedTargets from './batch/useResolvedTargets';
import useTopicParams, { DEFAULT_RECORDS_PER_PAGE } from './topicParams';
import type { LinkRoleOption, ProjectInfo, SearchDocument } from './types';
import useItemsSearch, { searchUrl } from './useItemsSearch';

const encode = (segment: string) => encodeURIComponent(segment);

/** All documents of a project, to pick a target document from. A project holds hundreds, not thousands. */
const ALL_DOCUMENTS = 100000;

const keyOf = (document: SearchDocument) => documentKey(document.spaceId ?? '', document.id);

const sourceRef = (document: SearchDocument): DocumentRef => ({
  projectId: document.projectId,
  spaceId: document.spaceId ?? '',
  spaceTitle: document.spaceTitle,
  name: document.id,
  title: document.title,
});

interface Revisions {
  source: string;
  target: string;
}

const HEAD_REVISIONS: Revisions = { source: '', target: '' };

interface Props {
  /** The runner of the export, replaceable in a test. */
  batch?: BatchDiffExport;
}

/**
 * The "Documents diff to PDF" navigation topic: pick documents of this project, pair each with its counterpart in a target
 * project, and export the diff of every pair to PDF, one file per pair.
 *
 * A document is paired with the document of the same space and name in the target project, which a user can
 * replace by another one. Each side of a pair can be compared at a revision of its own, so a document can also
 * be compared with an earlier state of itself in the same project.
 */
export default function DocumentsBatchPage({ batch: givenBatch }: Readonly<Props>) {
  const params = useTopicParams();
  const sourceProjectId = params.get('sourceProjectId');
  const query = params.get('sourceQuery');
  const page = params.getNumber('sourcePage', 1);
  const recordsPerPage = params.getNumber('sourceRecordsPerPage', DEFAULT_RECORDS_PER_PAGE);

  const projects = useRemoteList<ProjectInfo>({
    url: '/projects',
    progressMessage: 'Loading projects',
    errorMessage: 'Error occurred loading projects',
  });
  const linkRoles = useRemoteList<LinkRoleOption>({
    url: sourceProjectId ? `/projects/${encode(sourceProjectId)}/link-roles` : null,
    progressMessage: 'Loading link roles',
    errorMessage: 'Error occurred loading link roles',
  });
  const configurations = useRemoteList<SettingName>({
    url: sourceProjectId ? `/settings/diff/names?scope=project/${encode(sourceProjectId)}/` : null,
    progressMessage: 'Loading configurations',
    errorMessage: 'Error occurred loading configurations',
  });

  // The target project defaults to the source one: a document compared with an earlier revision of itself
  // needs nothing else, and a document of another project needs the target project picked anyway.
  const targetProjectId = params.get('targetProjectId') || sourceProjectId || projects.items[0]?.id || '';
  const linkRole = params.get('linkRole');
  const configuration = params.get('configuration') || configurations.items[0]?.name || '';

  const search = useItemsSearch<SearchDocument>(
    sourceProjectId ? searchUrl(sourceProjectId, 'documents', query, page, recordsPerPage) : null,
  );
  const items = useMemo(() => search.result?.items ?? [], [search.result]);

  // The selection is kept across pages and queries: a batch is usually put together from several of them
  const [selected, setSelected] = useState<Map<string, SearchDocument>>(new Map());
  const [overrides, setOverrides] = useState<Map<string, DocumentRef>>(new Map());
  const [revisions, setRevisions] = useState<Map<string, Revisions>>(new Map());
  const [options, setOptions] = useState<ExportOptions>(DEFAULT_EXPORT_OPTIONS);
  const [candidatesWanted, setCandidatesWanted] = useState(false);

  useEffect(() => {
    setSelected(new Map());
    setRevisions(new Map());
  }, [sourceProjectId]);

  // A target picked or a target revision chosen belongs to the target project it was chosen in
  useEffect(() => {
    setOverrides(new Map());
    setRevisions((previous) => new Map([...previous].map(([key, value]) => [key, { ...value, target: '' }])));
    setCandidatesWanted(false);
  }, [targetProjectId]);

  const toLookUp = useMemo(() => {
    const documents = new Map<string, { spaceId: string; name: string }>();
    [...items, ...selected.values()]
      .filter((document) => document.readable && document.spaceId)
      .forEach((document) => documents.set(keyOf(document), { spaceId: document.spaceId ?? '', name: document.id }));
    return [...documents.values()];
  }, [items, selected]);
  const targets = useResolvedTargets(targetProjectId, toLookUp);

  const candidates = useItemsSearch<SearchDocument>(
    candidatesWanted && targetProjectId ? searchUrl(targetProjectId, 'documents', '', 1, ALL_DOCUMENTS) : null,
  );

  const targetOf = (document: SearchDocument): TargetResolution => {
    const key = keyOf(document);
    const override = overrides.get(key);
    if (override) {
      return { state: 'found', document: override, overridden: true };
    }
    const resolved = targets.resolved.get(key);
    if (!resolved) {
      return { state: 'pending' };
    }
    if (!resolved.found) {
      return { state: 'missing', message: resolved.unavailableMessage || 'Not found' };
    }
    return {
      state: 'found',
      overridden: false,
      document: {
        projectId: targetProjectId,
        spaceId: resolved.spaceId,
        spaceTitle: resolved.spaceTitle,
        name: resolved.name,
        title: resolved.title,
      },
    };
  };

  const pairs: DocumentPair[] = [...selected.entries()]
    .sort(([left], [right]) => left.localeCompare(right))
    .flatMap(([key, document]) => {
      const target = targetOf(document);
      if (target.state !== 'found') {
        return [];
      }
      const revision = revisions.get(key) ?? HEAD_REVISIONS;
      return [
        {
          source: sourceRef(document),
          target: target.document,
          sourceRevision: revision.source,
          targetRevision: revision.target,
        },
      ];
    });
  const unpaired = selected.size - pairs.length;
  // Only the work items comparison pairs work items, the fields and content comparisons need no link role
  const linkRoleMissing = options.compareAs === 'Workitems' && !linkRole && pairs.some((pair) => !isSameDocument(pair));

  const exportDisabledReason = (() => {
    if (selected.size === 0) {
      return 'Please, select at least one document to be exported';
    }
    if (unpaired > 0) {
      return 'Please, pick a target document for each selected document';
    }
    if (linkRoleMissing) {
      return 'Please, select the link role which pairs the work items of different documents';
    }
    if (!configuration) {
      return 'Please, select a diff configuration';
    }
    return null;
  })();

  const ownBatch = useBatchDiffExport();
  const batch = givenBatch ?? ownBatch;

  const startExport = () => batch.start(pairs, { ...options, linkRole: linkRole, config: configuration });

  const setRevision = (key: string, side: keyof Revisions, value: string) =>
    setRevisions((previous) => new Map(previous).set(key, { ...(previous.get(key) ?? HEAD_REVISIONS), [side]: value }));

  const toggle = (document: SearchDocument, checked: boolean) =>
    setSelected((previous) => {
      const next = new Map(previous);
      if (checked) {
        next.set(keyOf(document), document);
      } else {
        next.delete(keyOf(document));
      }
      return next;
    });

  const selectablePage = items.filter((item) => item.readable);
  const pageSelected = selectablePage.length > 0 && selectablePage.every((item) => selected.has(keyOf(item)));

  const pickTarget = (key: string, picked: SearchDocument | null) => {
    setOverrides((previous) => {
      const next = new Map(previous);
      if (picked) {
        next.set(key, {
          projectId: targetProjectId,
          spaceId: picked.spaceId ?? '',
          spaceTitle: picked.spaceTitle,
          name: picked.id,
          title: picked.title,
        });
      } else {
        next.delete(key);
      }
      return next;
    });
    setRevision(key, 'target', '');
  };

  // The project's name, or its ID until the list of projects has loaded
  const targetProjectName = projects.items.find((project) => project.id === targetProjectId)?.name || targetProjectId;

  const columns: Column<SearchDocument>[] = [
    {
      key: 'space',
      label: 'Space',
      render: (item) => (item.spaceId ? <TitleWithId title={item.spaceTitle} id={item.spaceId} /> : null),
    },
    {
      key: 'document',
      label: 'Document',
      render: (item) => (
        <TitleWithId title={item.title} id={item.id}>
          <a
            href={`/polarion/#/project/${encode(item.projectId)}/wiki/${encode(item.spaceId ?? '')}/${encode(item.id)}`}
            target="_top"
          >
            {item.title || item.id}
          </a>
        </TitleWithId>
      ),
    },
    { key: 'type', label: 'Type', render: (item) => <EnumCell option={item.type} /> },
    { key: 'status', label: 'Status', render: (item) => <EnumCell option={item.status} /> },
    {
      key: 'revision',
      label: 'Revision',
      render: (item) =>
        selected.has(keyOf(item)) ? (
          <span className="revision-cell">
            <RevisionSelect
              document={sourceRef(item)}
              value={revisions.get(keyOf(item))?.source ?? ''}
              onChange={(value) => setRevision(keyOf(item), 'source', value)}
              ariaLabel={`Revision of ${item.title || item.id}`}
            />
          </span>
        ) : (
          <span className="revision-cell">
            <span className="unselected">HEAD</span>
          </span>
        ),
    },
    {
      key: 'target',
      label: targetProjectName ? `Target document in "${targetProjectName}" project` : 'Target document',
      render: (item) => (
        <TargetDocumentCell
          sourceName={item.title || item.id}
          target={targetOf(item)}
          candidates={candidates.result?.items ?? null}
          candidatesLoading={candidates.loading}
          onEdit={() => setCandidatesWanted(true)}
          onPick={(picked) => pickTarget(keyOf(item), picked)}
        />
      ),
    },
    {
      key: 'targetRevision',
      label: 'Target revision',
      render: (item) => {
        const target = targetOf(item);
        if (!selected.has(keyOf(item)) || target.state !== 'found') {
          return (
            <span className="revision-cell">
              <span className="unselected">HEAD</span>
            </span>
          );
        }
        return (
          <span className="revision-cell">
            <RevisionSelect
              key={documentKey(target.document.spaceId, target.document.name)}
              document={target.document}
              value={revisions.get(keyOf(item))?.target ?? ''}
              onChange={(value) => setRevision(keyOf(item), 'target', value)}
              ariaLabel={`Target revision of ${item.title || item.id}`}
            />
          </span>
        );
      },
    },
  ];

  const exportLabel = pairs.length > 1 ? `Export ${pairs.length} diffs to PDF` : 'Export diff to PDF';

  return (
    <>
      <div className="header">
        <h3>Export diffs of documents to PDF</h3>
      </div>

      <div className="main-pane">
        <PickerControls
          compareDisabled={exportDisabledReason !== null || batch.state.status === 'running'}
          compareTitle={exportDisabledReason ?? 'Export the diff of each selected document to PDF'}
          compareLabel={exportLabel}
          compareIconElement={<FontAwesomeIcon icon={faFilePdf} />}
          onCompare={startExport}
          hint="Please select the target project, the documents below and the export options, and click the button above. Each diff is exported to a PDF file of its own."
          projects={projects.items}
          targetProjectId={targetProjectId}
          onTargetProjectChange={(value) => params.update({ targetProjectId: value })}
          error={firstError(projects, linkRoles, configurations) ?? search.error ?? targets.error ?? candidates.error}
        />

        <ExportOptionsPanel
          value={options}
          onChange={setOptions}
          linkRoles={linkRoles.items}
          linkRole={linkRole}
          onLinkRoleChange={(value) => params.update({ linkRole: value })}
          configurations={configurations.items}
          configuration={configuration}
          onConfigurationChange={(value) => params.update({ configuration: value })}
        />

        <div className="items-for-diff">
          <QueryPanel
            side="source"
            query={query}
            recordsPerPage={recordsPerPage}
            onApply={(nextQuery, nextRecordsPerPage) =>
              params.update({ sourceQuery: nextQuery, sourceRecordsPerPage: nextRecordsPerPage, sourcePage: 1 })
            }
            onReset={() =>
              params.update({ sourceQuery: null, sourceRecordsPerPage: DEFAULT_RECORDS_PER_PAGE, sourcePage: 1 })
            }
          />

          {selected.size > 0 ? (
            <div className="selection-summary" role="status">
              <span className="count">{selected.size}</span>
              <span>
                {selected.size === 1 ? 'document selected' : 'documents selected'}
                {unpaired > 0 ? `, ${unpaired} without a target document` : ''}
              </span>
              <button type="button" className="sbb-btn sbb-btn--link" onClick={() => setSelected(new Map())}>
                Clear selection
              </button>
            </div>
          ) : null}

          <ItemsTable<SearchDocument>
            columns={columns}
            items={items}
            rowKey={(item, index) => (item.id ? keyOf(item) : `row-${index}`)}
            unavailableMessage={(item) => item.unavailableMessage}
            renderSelection={(item) => (
              <input
                type="checkbox"
                className="select-item"
                aria-label={`Select ${item.title || item.id}`}
                checked={selected.has(keyOf(item))}
                onChange={(event) => toggle(item, event.target.checked)}
              />
            )}
            selectAll={{
              checked: pageSelected,
              onChange: (checked) => selectablePage.forEach((item) => toggle(item, checked)),
            }}
            loading={search.loading}
            emptyMessage={sourceProjectId ? 'No documents found' : 'Open this page in a project to select documents'}
            footer={
              search.result ? (
                <>
                  <TableFooter
                    totalCount={search.result.totalCount}
                    shownCount={items.length}
                    query={search.result.query}
                  />
                  <Paginator
                    page={search.result.page}
                    lastPage={search.result.lastPage}
                    onPage={(nextPage) => params.update({ sourcePage: nextPage })}
                  />
                </>
              ) : null
            }
          />
        </div>
      </div>

      <BatchExportProgressModal state={batch.state} onStop={batch.stop} onClose={batch.close} />
    </>
  );
}
