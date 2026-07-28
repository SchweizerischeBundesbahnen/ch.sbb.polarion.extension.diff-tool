import { useEffect, useMemo, useState } from 'react';
import { SearchableSelect } from '@grigoriev/react-sbb-polarion';
import NumericSpinner from './NumericSpinner';
import PanelShell from './PanelShell';
import compareIcon from './compare.svg';
import { openDocumentsDiff } from './openDocumentsDiff';
import type { PanelProps } from './panelProps';
import useRemoteList, { firstError, firstLoading } from './useRemoteList';

interface SpaceInfo {
  id: string;
  name: string;
}

interface DocumentInfo {
  id: string;
  title: string;
}

interface RevisionInfo {
  name: string;
  baselineName?: string | null;
}

/** The HEAD entry the revision list always starts with; `""` means "latest" to the viewer. */
const HEAD: RevisionInfo = { name: '', baselineName: null };

const encode = (segment: string) => encodeURIComponent(segment);

/**
 * React port of the "Documents Comparison" Document Properties panel (the legacy
 * webapp/diff-tool/html/diff-tool.html + js/modules/DiffTool.js + GenericMixin.js).
 *
 * The ids and classes of the legacy fragment are kept, so the panel CSS (diff-tool.css, injected into
 * the shadow root) applies unchanged and anyone who knew the old DOM still recognises this one.
 *
 * Three deliberate departures from the legacy behaviour, all noted at their site below: the Compare
 * button now requires a target document, the configuration select starts on the first configuration
 * instead of empty, and selections are no longer persisted across document opens.
 */
export default function DiffToolPanel({ props }: { props: PanelProps }) {
  const [compareWithSame, setCompareWithSame] = useState(false);
  const [compareAsBranched, setCompareAsBranched] = useState(false);

  const [projectId, setProjectId] = useState('');
  const [spaceId, setSpaceId] = useState('');
  const [documentId, setDocumentId] = useState('');

  const [revisionMode, setRevisionMode] = useState<'manual' | 'list'>('manual');
  const [manualRevision, setManualRevision] = useState('');
  const [listRevision, setListRevision] = useState('');
  const [onlyBaselines, setOnlyBaselines] = useState(false);

  const [linkRole, setLinkRole] = useState('');
  // DEPARTURE: the legacy fragment marked the first configuration `selected`, then SearchableDropdown's
  // restoreSelection() cleared it again, so the panel opened with none chosen and could build a
  // `&config=` URL with an empty value. Honour what the server meant and preselect the first.
  const [config, setConfig] = useState(props.configurations[0] ?? '');

  const [useFilter, setUseFilter] = useState(false);
  const [filterType, setFilterType] = useState<'include' | 'exclude'>('exclude');
  const [filterValue, setFilterValue] = useState('');

  // "Compare with another revision of the same document" replaces the whole target selection with the
  // source document, which is exactly what the legacy `sameDoc ? this.sourceX : ...` reads did.
  const targetProjectId = compareWithSame ? props.sourceProjectId : projectId;
  const targetSpaceId = compareWithSame ? props.sourceSpaceId : spaceId;
  const targetDocument = compareWithSame ? props.sourceDocument : documentId;

  const spaces = useRemoteList<SpaceInfo>({
    url: projectId && !compareWithSame ? `/projects/${encode(projectId)}/spaces` : null,
    progressMessage: 'Loading spaces',
    errorMessage: 'Error occurred loading spaces',
  });

  const documents = useRemoteList<DocumentInfo>({
    url:
      projectId && spaceId && !compareWithSame
        ? `/projects/${encode(projectId)}/spaces/${encode(spaceId)}/documents`
        : null,
    progressMessage: 'Loading documents',
    errorMessage: 'Error occurred loading documents',
  });

  const revisions = useRemoteList<RevisionInfo>({
    url:
      revisionMode === 'list' && targetProjectId && targetSpaceId && targetDocument
        ? `/projects/${encode(targetProjectId)}/spaces/${encode(targetSpaceId)}/documents/${encode(targetDocument)}/revisions`
        : null,
    progressMessage: 'Loading revisions',
    errorMessage: 'Error occurred loading revisions',
  });

  // Selecting a project invalidates the space below it, and a space invalidates the document, exactly as
  // the legacy projectChanged()/spaceChanged() cleared the selects further down.
  useEffect(() => setSpaceId(''), [projectId]);
  useEffect(() => setDocumentId(''), [projectId, spaceId]);

  const revisionOptions = useMemo(() => [HEAD, ...revisions.items], [revisions.items]);
  // "show only baselines" hid the non-baseline options (HEAD included) and re-selected the first one
  // still visible. Recomputing on either input reproduces both legacy call sites - after a load and on
  // the checkbox click - without the DOM walk.
  const visibleRevisions = useMemo(
    () => (onlyBaselines ? revisionOptions.filter((revision) => revision.baselineName) : revisionOptions),
    [revisionOptions, onlyBaselines],
  );
  useEffect(() => setListRevision(visibleRevisions[0]?.name ?? ''), [visibleRevisions]);

  const busy = firstLoading(spaces, documents, revisions);
  const loadError = firstError(spaces, documents, revisions);

  // DEPARTURE: the legacy panel also enabled Compare as soon as "Enter manually" was clicked, even with
  // no target document chosen - which produced a URL with an empty targetDocument. A target is required
  // now; in list mode the revisions must have loaded, as before.
  const canCompare = Boolean(targetDocument) && (revisionMode === 'manual' || visibleRevisions.length > 0);

  const compare = () =>
    openDocumentsDiff({
      sourceProjectId: props.sourceProjectId,
      sourceSpaceId: props.sourceSpaceId,
      sourceDocument: props.sourceDocument,
      sourceRevision: props.sourceRevision,
      targetProjectId: targetProjectId,
      targetSpaceId: targetSpaceId,
      targetDocument: targetDocument,
      targetRevision: revisionMode === 'manual' ? manualRevision : listRevision,
      // Branched documents are paired through the `branched_from` role, and comparing a document with
      // itself needs no pairing at all, so in both cases the chosen link role is irrelevant.
      linkRole: compareWithSame || compareAsBranched ? '' : linkRole,
      config: config,
      branched: compareAsBranched,
      filter: useFilter ? { value: filterValue, type: filterType } : undefined,
    });

  const hiddenWhenComparingSame = compareWithSame ? 'property-wrapper hide' : 'property-wrapper';

  return (
    <PanelShell prefix="comparison" busy={busy} error={loadError}>
      <p>
        Please select <strong>target</strong> document and its revision below preliminary selecting its project and
        space, to compare current document in selected revision with it. Additionally select which link role should be
        taken into account when determining counterpart work items as well as diffing configuration.
      </p>

      {/* The two checkboxes are mutually exclusive: each hides the other while it is ticked. */}
      <div className={compareAsBranched ? 'property-wrapper hide' : 'property-wrapper'} id="compare-with-same-wrapper">
        <input
          type="checkbox"
          id="compare-with-same-checkbox"
          checked={compareWithSame}
          onChange={(event) => setCompareWithSame(event.target.checked)}
        />
        <label htmlFor="compare-with-same-checkbox">Compare with another revision of the same document</label>
      </div>
      <div className={compareWithSame ? 'property-wrapper hide' : 'property-wrapper'} id="compare-as-branched-wrapper">
        <input
          type="checkbox"
          id="compare-as-branched-checkbox"
          checked={compareAsBranched}
          onChange={(event) => setCompareAsBranched(event.target.checked)}
        />
        <label htmlFor="compare-as-branched-checkbox">Compare as branched documents</label>
      </div>

      <div className={hiddenWhenComparingSame}>
        <label htmlFor="comparison-project-selector" className="fixed-width w-1">
          Project:
        </label>
        <SearchableSelect
          id="comparison-project-selector"
          value={projectId}
          onChange={setProjectId}
          options={props.projects}
          placeholder="Select Project..."
          allowEmpty
        />
      </div>
      <div className={hiddenWhenComparingSame}>
        <label htmlFor="comparison-space-selector" className="fixed-width w-1">
          Space:
        </label>
        <SearchableSelect
          id="comparison-space-selector"
          value={spaceId}
          onChange={setSpaceId}
          options={spaces.items}
          placeholder="Select Space..."
          allowEmpty
        />
      </div>
      <div className={hiddenWhenComparingSame}>
        <label htmlFor="document-selector" className="fixed-width w-1">
          Document:
        </label>
        <SearchableSelect
          id="document-selector"
          value={documentId}
          onChange={setDocumentId}
          options={documents.items.map((document) => ({ id: document.id, name: document.title }))}
          placeholder="Select Document..."
          allowEmpty
        />
      </div>

      <div className="property-wrapper">
        <label htmlFor="select-revision-panel" className="fixed-width w-1">
          Revision:
        </label>
        <div id="select-revision-panel">
          <div id="select-revision-radios">
            <input
              type="radio"
              id="revision-enter-manually"
              name="select-revision-type"
              value="manually"
              checked={revisionMode === 'manual'}
              onChange={() => setRevisionMode('manual')}
            />
            <label htmlFor="revision-enter-manually">Enter manually</label>
            <input
              type="radio"
              id="revision-select-from-list"
              name="select-revision-type"
              value="list"
              checked={revisionMode === 'list'}
              onChange={() => setRevisionMode('list')}
            />
            <label htmlFor="revision-select-from-list">Select from list</label>
          </div>

          {revisionMode === 'manual' ? (
            <div id="select-revision-manual-container">
              <NumericSpinner
                id="select-revision-manual-input"
                value={manualRevision}
                onChange={setManualRevision}
                placeholder="leave empty to use latest revision"
              />
            </div>
          ) : (
            <div id="select-revision-list-container">
              <SearchableSelect
                id="revision-selector"
                value={listRevision}
                onChange={setListRevision}
                options={visibleRevisions.map((revision) => ({
                  id: revision.name,
                  name: revision.baselineName ? `${revision.name} | ${revision.baselineName}` : revision.name || 'HEAD',
                }))}
                placeholder="Select Revision..."
              />
              <div>
                <input
                  type="checkbox"
                  id="baseline-checkbox"
                  checked={onlyBaselines}
                  onChange={(event) => setOnlyBaselines(event.target.checked)}
                />
                <label htmlFor="baseline-checkbox">show only baselines</label>
              </div>
            </div>
          )}
        </div>
      </div>

      <div
        className={compareWithSame || compareAsBranched ? 'property-wrapper hide' : 'property-wrapper'}
        id="comparison-link-role-wrapper"
      >
        <label htmlFor="comparison-link-role-selector" className="fixed-width w-1">
          Link role:
        </label>
        <SearchableSelect
          id="comparison-link-role-selector"
          value={linkRole}
          onChange={setLinkRole}
          options={props.linkRoles}
          placeholder="Select Link Role..."
          allowEmpty
        />
      </div>

      <div className="property-wrapper">
        <label htmlFor="comparison-config-selector" className="fixed-width w-1">
          Configuration:
        </label>
        <SearchableSelect
          id="comparison-config-selector"
          value={config}
          onChange={setConfig}
          options={props.configurations.map((name) => ({ id: name, name: name }))}
          placeholder="Select Configuration..."
        />
      </div>

      <div className="property-wrapper">
        <input
          type="checkbox"
          id="use-work-items-filter"
          checked={useFilter}
          onChange={(event) => setUseFilter(event.target.checked)}
        />
        <label htmlFor="use-work-items-filter">Use work items filter</label>
      </div>
      {useFilter ? (
        <div className="property-wrapper">
          <div id="work-items-filter-pane" style={{ width: '100%' }}>
            <div id="work-items-filter-radios">
              <input
                type="radio"
                id="include-work-items"
                name="work-items-filter-type"
                value="including"
                checked={filterType === 'include'}
                onChange={() => setFilterType('include')}
              />
              <label htmlFor="include-work-items">Only work items</label>
              <input
                type="radio"
                id="exclude-work-items"
                name="work-items-filter-type"
                value="excluding"
                checked={filterType === 'exclude'}
                onChange={() => setFilterType('exclude')}
              />
              <label htmlFor="exclude-work-items">Excluding work items</label>
            </div>
            <div id="work-items-filter" style={{ margin: '5px 0 0 5px' }}>
              <input
                id="work-items-filter-input"
                placeholder="comma/space separated list of IDs"
                type="text"
                style={{ width: '100%' }}
                value={filterValue}
                onChange={(event) => setFilterValue(event.target.value)}
              />
            </div>
          </div>
        </div>
      ) : null}

      <div className="buttons-wrapper">
        <button type="button" id="compare-documents" disabled={!canCompare || busy !== null} onClick={compare}>
          <img src={compareIcon} alt="" />
          Compare
        </button>
      </div>
    </PanelShell>
  );
}
