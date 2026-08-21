import { type ReactNode, useEffect, useState } from 'react';
import type { SettingName } from '@sbb-polarion/react-sbb-polarion';
import { format } from 'date-fns';
import useRemoteList, { firstError } from '../formext/useRemoteList';
import ItemsTable, { type Column } from './ItemsTable';
import Paginator from './Paginator';
import PickerControls, { TargetProjectSelect } from './PickerControls';
import QueryPanel from './QueryPanel';
import TableFooter from './TableFooter';
import { openCollectionsDiff } from './openCollectionsDiff';
import useTopicParams, { DEFAULT_RECORDS_PER_PAGE } from './topicParams';
import type { LinkRoleOption, ProjectInfo, SearchCollection } from './types';
import useItemsSearch, { type ItemsSearch, searchUrl } from './useItemsSearch';

const encode = (segment: string) => encodeURIComponent(segment);

/**
 * The two date columns. The REST model carries epoch milliseconds; this is Polarion's own display format,
 * which is also short enough for the narrow columns of the two-table layout.
 */
const formatDate = (millis?: number | null) => (millis ? format(new Date(millis), 'yyyy-MM-dd HH:mm') : '');

const COLUMNS: Column<SearchCollection>[] = [
  { key: 'name', label: 'Name', render: (item) => item.name },
  { key: 'author', label: 'Author', render: (item) => item.authorName },
  { key: 'created', label: 'Created', render: (item) => formatDate(item.created) },
  { key: 'updated', label: 'Updated', render: (item) => formatDate(item.updated) },
];

/**
 * The "Collections" navigation topic: pick one baseline collection on each side and compare them.
 *
 * React port of `webapp/diff-tool/pages/collections.jsp` plus its `CollectionsDiffWidgetRenderer`, including
 * its two-column layout, its radio-per-side selection and its wording.
 */
export default function CollectionsPickerPage() {
  const params = useTopicParams();
  const sourceProjectId = params.get('sourceProjectId');

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

  const targetProjectId = params.get('targetProjectId') || projects.items[0]?.id || '';
  const linkRole = params.get('linkRole') || linkRoles.items[0]?.id || '';
  const configuration = params.get('configuration') || configurations.items[0]?.name || '';

  const sourceQuery = params.get('sourceQuery');
  const sourcePage = params.getNumber('sourcePage', 1);
  const sourceRecordsPerPage = params.getNumber('sourceRecordsPerPage', DEFAULT_RECORDS_PER_PAGE);
  const targetQuery = params.get('targetQuery');
  const targetPage = params.getNumber('targetPage', 1);
  const targetRecordsPerPage = params.getNumber('targetRecordsPerPage', DEFAULT_RECORDS_PER_PAGE);

  const sourceSearch = useItemsSearch<SearchCollection>(
    sourceProjectId ? searchUrl(sourceProjectId, 'collections', sourceQuery, sourcePage, sourceRecordsPerPage) : null,
  );
  const targetSearch = useItemsSearch<SearchCollection>(
    targetProjectId ? searchUrl(targetProjectId, 'collections', targetQuery, targetPage, targetRecordsPerPage) : null,
  );

  const [sourceCollectionId, setSourceCollectionId] = useState('');
  const [targetCollectionId, setTargetCollectionId] = useState('');
  // As in the WorkItems picker: the widget reloaded the frame on Apply, Reset and paging, so a selection never
  // outlived the table it was made in.
  useEffect(() => setSourceCollectionId(''), [sourceProjectId, sourceQuery, sourcePage, sourceRecordsPerPage]);
  useEffect(() => setTargetCollectionId(''), [targetProjectId, targetQuery, targetPage, targetRecordsPerPage]);

  const compare = () =>
    openCollectionsDiff({
      sourceProjectId: sourceProjectId,
      sourceCollectionId: sourceCollectionId,
      targetProjectId: targetProjectId,
      targetCollectionId: targetCollectionId,
      linkRole: linkRole,
      config: configuration,
    });

  const table = (
    side: 'source' | 'target',
    search: ItemsSearch<SearchCollection>,
    selectedId: string,
    onSelect: (id: string) => void,
  ) => {
    const items = search.result?.items ?? [];
    return (
      <ItemsTable<SearchCollection>
        columns={COLUMNS}
        items={items}
        rowKey={(item, index) => item.id || `row-${index}`}
        unavailableMessage={(item) => item.unavailableMessage}
        renderSelection={(item) => (
          <input
            type="radio"
            className="select-item"
            name={`${side}-collection`}
            aria-label={`Select ${item.name ?? item.id}`}
            checked={selectedId === item.id}
            onChange={() => onSelect(item.id)}
          />
        )}
        loading={search.loading}
        emptyMessage={
          side === 'source' && !sourceProjectId
            ? 'Open this page in a project to select a collection'
            : 'No collections found'
        }
        footer={
          search.result ? (
            <>
              {/* Collections have no Polarion table view, which is why the widget's footer omitted the
                  "open in table" link in collection scope. */}
              <TableFooter
                totalCount={search.result.totalCount}
                shownCount={items.length}
                query={search.result.query}
              />
              <Paginator
                page={search.result.page}
                lastPage={search.result.lastPage}
                onPage={(nextPage) => params.update({ [`${side}Page`]: nextPage })}
              />
            </>
          ) : null
        }
      />
    );
  };

  const queryPanel = (side: 'source' | 'target', query: string, recordsPerPage: number, children?: ReactNode) => (
    <QueryPanel
      side={side}
      query={query}
      recordsPerPage={recordsPerPage}
      onApply={(nextQuery, nextRecordsPerPage) =>
        params.update({
          [`${side}Query`]: nextQuery,
          [`${side}RecordsPerPage`]: nextRecordsPerPage,
          [`${side}Page`]: 1,
        })
      }
      onReset={() =>
        params.update({
          [`${side}Query`]: null,
          [`${side}RecordsPerPage`]: DEFAULT_RECORDS_PER_PAGE,
          [`${side}Page`]: 1,
        })
      }
    >
      {children}
    </QueryPanel>
  );

  return (
    <>
      <div className="header">
        <h3>Compare Collections</h3>
      </div>

      <div className="main-pane">
        <PickerControls
          compareDisabled={!sourceCollectionId || !targetCollectionId}
          compareTitle="Please, select one item in left table and one item in right table to be compared"
          onCompare={compare}
          hint="Please select one collection in left table and then one collection in right table below which you want to compare and click button above"
          linkRoles={linkRoles.items}
          linkRole={linkRole}
          onLinkRoleChange={(value) => params.update({ linkRole: value })}
          configurations={configurations.items}
          configuration={configuration}
          onConfigurationChange={(value) => params.update({ configuration: value })}
          error={firstError(projects, linkRoles, configurations) ?? sourceSearch.error ?? targetSearch.error}
        />

        <div className="columns">
          <div className="items-for-diff column">
            {queryPanel('source', sourceQuery, sourceRecordsPerPage)}
            {table('source', sourceSearch, sourceCollectionId, setSourceCollectionId)}
          </div>
          <div className="items-for-diff column">
            {queryPanel(
              'target',
              targetQuery,
              targetRecordsPerPage,
              <TargetProjectSelect
                projects={projects.items}
                value={targetProjectId}
                onChange={(value) => params.update({ targetProjectId: value })}
              />,
            )}
            {table('target', targetSearch, targetCollectionId, setTargetCollectionId)}
          </div>
        </div>
      </div>
    </>
  );
}
