import { useEffect, useMemo, useState } from 'react';
import type { SettingName } from '@grigoriev/react-sbb-polarion';
import useRemoteList, { firstError } from '../formext/useRemoteList';
import ItemsTable, { type Column, EnumCell } from './ItemsTable';
import Paginator from './Paginator';
import PickerControls from './PickerControls';
import QueryPanel from './QueryPanel';
import TableFooter from './TableFooter';
import { openWorkItemsDiff } from './openWorkItemsDiff';
import useTopicParams, { DEFAULT_RECORDS_PER_PAGE } from './topicParams';
import type { LinkRoleOption, ProjectInfo, SearchWorkItem } from './types';
import useItemsSearch, { searchUrl } from './useItemsSearch';

const encode = (segment: string) => encodeURIComponent(segment);

const COLUMNS: Column<SearchWorkItem>[] = [
  {
    key: 'id',
    label: 'ID',
    render: (item) => (
      <a href={`/polarion/#/project/${encode(item.projectId)}/workitem?id=${encode(item.id)}`} target="_top">
        {item.id}
      </a>
    ),
  },
  { key: 'title', label: 'Title', render: (item) => item.title },
  { key: 'type', label: 'Type', render: (item) => <EnumCell option={item.type} /> },
  { key: 'status', label: 'Status', render: (item) => <EnumCell option={item.status} /> },
  { key: 'severity', label: 'Severity', render: (item) => <EnumCell option={item.severity} /> },
];

/**
 * The "Multiple Work Items" navigation topic: pick WorkItems of this project, pick the counterpart project,
 * link role and diff configuration, then compare.
 *
 * React port of `webapp/diff-tool/pages/multiple-work-items.jsp` plus its
 * `WorkItemsDiffWidgetRenderer`. The wording and the selection rules are the widget's.
 */
export default function WorkItemsPickerPage() {
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

  // The widget preselected the first entry of each list when the URL carried no choice - for the target
  // project it even wrote that default back into its own parameters.
  const targetProjectId = params.get('targetProjectId') || projects.items[0]?.id || '';
  const linkRole = params.get('linkRole') || linkRoles.items[0]?.id || '';
  const configuration = params.get('configuration') || configurations.items[0]?.name || '';

  const search = useItemsSearch<SearchWorkItem>(
    sourceProjectId ? searchUrl(sourceProjectId, 'workitems', query, page, recordsPerPage) : null,
  );
  // Memoized because selectableIds derives from it: a fresh array per render would recompute that on every
  // keystroke in the query field.
  const items = useMemo(() => search.result?.items ?? [], [search.result]);

  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  // The widget reloaded the whole frame on Apply, Reset and every page step, which dropped the selection.
  // Clearing it here keeps that rule: what you compare is always what is currently on screen.
  useEffect(() => setSelectedIds([]), [sourceProjectId, query, page, recordsPerPage]);

  const selectableIds = useMemo(() => items.filter((item) => item.readable).map((item) => item.id), [items]);
  const allSelected = selectableIds.length > 0 && selectableIds.every((id) => selectedIds.includes(id));

  const toggle = (id: string, selected: boolean) =>
    setSelectedIds((current) => (selected ? [...current, id] : current.filter((selectedId) => selectedId !== id)));

  const openInTableUrl = sourceProjectId
    ? `/polarion/#/project/${encode(sourceProjectId)}/workitems${query ? `?query=${encode(query)}` : ''}`
    : null;

  const compare = () =>
    openWorkItemsDiff({
      sourceProjectId: sourceProjectId,
      targetProjectId: targetProjectId,
      linkRole: linkRole,
      config: configuration,
      ids: selectedIds,
    });

  return (
    <>
      <div className="header">
        <h3>Compare work items</h3>
      </div>

      <div className="main-pane">
        <PickerControls
          compareDisabled={selectedIds.length === 0}
          compareTitle="Please, select at least one item to be compared"
          onCompare={compare}
          hint="Please select diff configuration, link role and work items below which you want to compare and click button above"
          projects={projects.items}
          targetProjectId={targetProjectId}
          onTargetProjectChange={(value) => params.update({ targetProjectId: value })}
          linkRoles={linkRoles.items}
          linkRole={linkRole}
          onLinkRoleChange={(value) => params.update({ linkRole: value })}
          configurations={configurations.items}
          configuration={configuration}
          onConfigurationChange={(value) => params.update({ configuration: value })}
          error={firstError(projects, linkRoles, configurations) ?? search.error}
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

          <ItemsTable<SearchWorkItem>
            columns={COLUMNS}
            items={items}
            rowKey={(item, index) => item.id || `row-${index}`}
            unavailableMessage={(item) => item.unavailableMessage}
            renderSelection={(item) => (
              <input
                type="checkbox"
                className="select-item"
                aria-label={`Select ${item.id}`}
                checked={selectedIds.includes(item.id)}
                onChange={(event) => toggle(item.id, event.target.checked)}
              />
            )}
            selectAll={{
              checked: allSelected,
              onChange: (checked) => setSelectedIds(checked ? selectableIds : []),
            }}
            loading={search.loading}
            emptyMessage={sourceProjectId ? 'No work items found' : 'Open this page in a project to select work items'}
            footer={
              search.result ? (
                <>
                  {/* The query as the backend executed it, which is what the widget's footer showed. */}
                  <TableFooter
                    totalCount={search.result.totalCount}
                    shownCount={items.length}
                    query={search.result.query}
                    openInTableUrl={openInTableUrl}
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
    </>
  );
}
