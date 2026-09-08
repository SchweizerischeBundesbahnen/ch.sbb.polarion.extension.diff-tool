import { useEffect, useMemo, useState } from 'react';
import { SearchableSelect } from '@sbb-polarion/react-sbb-polarion';
import NumericSpinner from './NumericSpinner';
import PanelShell from './PanelShell';
import compareIcon from './compare.svg';
import { FieldCell, FieldRow, RadioPair, SubRow, SwitchRow } from './formRows';
import { openDocumentsDiff } from './openDocumentsDiff';
import type { PanelProps } from './panelProps';
import { rememberedIfOffered, useAdoptRemembered, useRemembering } from './rememberedSelection';
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

// The ids of the legacy `<select>`s, which are also the cookie names the remembered selections are kept
// under (see rememberedSelection.ts) - so they must keep matching the ids rendered below.
const PROJECT_SELECT = 'comparison-project-selector';
const SPACE_SELECT = 'comparison-space-selector';
const DOCUMENT_SELECT = 'document-selector';
const REVISION_SELECT = 'revision-selector';
const LINK_ROLE_SELECT = 'comparison-link-role-selector';
const CONFIG_SELECT = 'comparison-config-selector';

const encode = (segment: string) => encodeURIComponent(segment);

/**
 * React port of the "Documents Comparison" Document Properties panel (the legacy
 * webapp/diff-tool/html/diff-tool.html + js/modules/DiffTool.js + GenericMixin.js).
 *
 * The ids and classes of the legacy fragment are kept, so the panel CSS (diff-tool.css, injected into
 * the shadow root) applies unchanged and anyone who knew the old DOM still recognises this one.
 *
 * Dropdown choices are remembered across document opens, as they were before - see
 * rememberedSelection.ts for why that needs code here now.
 *
 * Two deliberate departures from the legacy behaviour, both noted at their site below: the Compare button
 * now requires a target document, and the configuration select falls back to the first configuration
 * rather than to empty.
 */
export default function DiffToolPanel({ props }: { props: PanelProps }) {
  const [compareWithSame, setCompareWithSame] = useState(false);
  const [compareAsBranched, setCompareAsBranched] = useState(false);

  // Seeded from the cookie the legacy panel wrote, where the options are already known at mount.
  const projectIds = useMemo(() => props.projects.map((project) => project.id), [props.projects]);
  const [projectId, setProjectId] = useState(() => rememberedIfOffered(PROJECT_SELECT, projectIds));
  const [spaceId, setSpaceId] = useState('');
  const [documentId, setDocumentId] = useState('');

  const [revisionMode, setRevisionMode] = useState<'manual' | 'list'>('manual');
  const [manualRevision, setManualRevision] = useState('');
  const [listRevision, setListRevision] = useState('');
  const [onlyBaselines, setOnlyBaselines] = useState(false);

  const linkRoleIds = useMemo(() => props.linkRoles.map((role) => role.id), [props.linkRoles]);
  const [linkRole, setLinkRole] = useState(() => rememberedIfOffered(LINK_ROLE_SELECT, linkRoleIds));
  // The remembered configuration wins; otherwise the first one, which is what the server marks
  // `selected`. (The legacy restoreSelection() cleared that mark when nothing was remembered, so the
  // panel could open with none chosen and build a `&config=` URL with an empty value.)
  const [config, setConfig] = useState(
    () => rememberedIfOffered(CONFIG_SELECT, props.configurations) || (props.configurations[0] ?? ''),
  );

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

  // A user's choice is remembered; the cascade resets below deliberately are not (see useRemembering).
  const chooseProject = useRemembering(PROJECT_SELECT, setProjectId);
  const chooseSpace = useRemembering(SPACE_SELECT, setSpaceId);
  const chooseDocument = useRemembering(DOCUMENT_SELECT, setDocumentId);
  const chooseRevision = useRemembering(REVISION_SELECT, setListRevision);
  const chooseLinkRole = useRemembering(LINK_ROLE_SELECT, setLinkRole);
  const chooseConfig = useRemembering(CONFIG_SELECT, setConfig);

  // Selecting a project invalidates the space below it, and a space invalidates the document, exactly as
  // the legacy projectChanged()/spaceChanged() cleared the selects further down.
  useEffect(() => setSpaceId(''), [projectId]);
  useEffect(() => setDocumentId(''), [projectId, spaceId]);

  // ...and once the dependent list has loaded, the remembered choice is re-applied - the legacy
  // spaceDropdown.refresh() / documentDropdown.refresh() did this, since refresh() restores too.
  const spaceIds = useMemo(() => spaces.items.map((space) => space.id), [spaces.items]);
  const documentIds = useMemo(() => documents.items.map((document) => document.id), [documents.items]);
  useAdoptRemembered(SPACE_SELECT, spaceIds, setSpaceId);
  useAdoptRemembered(DOCUMENT_SELECT, documentIds, setDocumentId);

  const revisionOptions = useMemo(() => [HEAD, ...revisions.items], [revisions.items]);
  // "show only baselines" hid the non-baseline options (HEAD included) and re-selected the first one
  // still visible. Recomputing on either input reproduces both legacy call sites - after a load and on
  // the checkbox click - without the DOM walk.
  const visibleRevisions = useMemo(
    () => (onlyBaselines ? revisionOptions.filter((revision) => revision.baselineName) : revisionOptions),
    [revisionOptions, onlyBaselines],
  );
  const visibleRevisionIds = useMemo(() => visibleRevisions.map((revision) => revision.name), [visibleRevisions]);
  useEffect(() => {
    // The remembered revision wins when it is still on the (possibly baseline-filtered) list, matching
    // the legacy order: baselineSelected() picked the first visible one, then refresh() restored.
    setListRevision(rememberedIfOffered(REVISION_SELECT, visibleRevisionIds) || (visibleRevisions[0]?.name ?? ''));
  }, [visibleRevisions, visibleRevisionIds]);

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

  /** The whole target selection goes when the panel is comparing one document with itself. */
  const targetSectionClass = compareWithSame ? 'diff-section group-start hide' : 'diff-section group-start';

  return (
    <PanelShell prefix="comparison" busy={busy} error={loadError}>
      <p>
        Please select <strong>target</strong> document and its revision below preliminary selecting its project and
        space, to compare current document in selected revision with it. Additionally select which link role should be
        taken into account when determining counterpart work items as well as diffing configuration.
      </p>

      {/* How the two documents are paired. The two checkboxes are mutually exclusive: each hides the
          other while it is ticked. */}
      <div className="diff-section">
        <SwitchRow
          rowId="compare-with-same-wrapper"
          className={compareAsBranched ? 'hide' : undefined}
          id="compare-with-same-checkbox"
          label="Compare with another revision of the same document"
          checked={compareWithSame}
          onChange={setCompareWithSame}
        />
        <SwitchRow
          rowId="compare-as-branched-wrapper"
          className={compareWithSame ? 'hide' : undefined}
          id="compare-as-branched-checkbox"
          label="Compare as branched documents"
          checked={compareAsBranched}
          onChange={setCompareAsBranched}
        />
      </div>

      {/* Which document to compare against, narrowed down one dropdown at a time. */}
      <div className={targetSectionClass} id="comparison-target-wrapper">
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
      </div>

      {/* Which revision of it, entered or picked - a section of its own, the value belonging under the
          choice rather than beside it.

          Its sub-rows keep the control column rather than taking the label one, unlike the work items
          filter's below: there the switch is the whole row and what it reveals indents to the label, here
          the radios already sit in the control column and the value has to line up under them. */}
      <div className="diff-section group-start">
        {/* No `labelFor`: the row's value is a radio group, and a label pointing at one of the radios
            would make a click on `Revision:` pick that mode - discarding a revision chosen from the
            list. The group is named through the label's id instead. */}
        <FieldRow label="Revision:" labelId="revision-label">
          <FieldCell>
            <RadioPair
              ariaLabelledBy="revision-label"
              name="select-revision-type"
              value={revisionMode}
              onChange={setRevisionMode}
              options={[
                { id: 'revision-enter-manually', label: 'Enter manually', value: 'manual' },
                { id: 'revision-select-from-list', label: 'Select from list', value: 'list' },
              ]}
            />
          </FieldCell>
        </FieldRow>
        {revisionMode === 'manual' ? (
          <SubRow rowId="select-revision-manual-container">
            <NumericSpinner
              id="select-revision-manual-input"
              value={manualRevision}
              onChange={setManualRevision}
              placeholder="leave empty to use latest revision"
            />
          </SubRow>
        ) : (
          <>
            <SubRow rowId="select-revision-list-container">
              <SearchableSelect
                id={REVISION_SELECT}
                value={listRevision}
                onChange={chooseRevision}
                options={visibleRevisions.map((revision) => ({
                  id: revision.name,
                  name: revision.baselineName ? `${revision.name} | ${revision.baselineName}` : revision.name || 'HEAD',
                }))}
                placeholder="Select Revision..."
              />
            </SubRow>
            <SubRow rowId="baseline-wrapper">
              {/* `option-pair` for a lone checkbox because that is what centers a label-wrapped
                  control against its text; see diff-tool.css. */}
              <div className="option-pair">
                <label htmlFor="baseline-checkbox">
                  <input
                    id="baseline-checkbox"
                    type="checkbox"
                    checked={onlyBaselines}
                    onChange={(event) => setOnlyBaselines(event.target.checked)}
                  />
                  show only baselines
                </label>
              </div>
            </SubRow>
          </>
        )}
      </div>

      {/* How the work items inside the two documents are paired up, and which fields are compared. */}
      <div className="diff-section group-start">
        <FieldRow
          rowId="comparison-link-role-wrapper"
          className={compareWithSame || compareAsBranched ? 'hide' : undefined}
          label="Link role:"
          labelFor={LINK_ROLE_SELECT}
        >
          <FieldCell>
            <SearchableSelect
              id={LINK_ROLE_SELECT}
              value={linkRole}
              onChange={chooseLinkRole}
              options={props.linkRoles}
              placeholder="Select Link Role..."
              allowEmpty
            />
          </FieldCell>
        </FieldRow>
        <FieldRow label="Configuration:" labelFor={CONFIG_SELECT}>
          <FieldCell>
            <SearchableSelect
              id={CONFIG_SELECT}
              value={config}
              onChange={chooseConfig}
              options={props.configurations.map((name) => ({ id: name, name: name }))}
              placeholder="Select Configuration..."
            />
          </FieldCell>
        </FieldRow>
      </div>

      {/* Which work items to leave out of the comparison, the switch keeping its own line and the two
          rows it reveals belonging under it. */}
      <div className="diff-section group-start">
        <SwitchRow
          id="use-work-items-filter"
          label="Use work items filter"
          checked={useFilter}
          onChange={setUseFilter}
        />
        {useFilter ? (
          <>
            <SubRow rowId="work-items-filter-radios" wide>
              <RadioPair
                name="work-items-filter-type"
                value={filterType}
                onChange={setFilterType}
                options={[
                  { id: 'include-work-items', label: 'Only work items', value: 'include' },
                  { id: 'exclude-work-items', label: 'Excluding work items', value: 'exclude' },
                ]}
              />
            </SubRow>
            <SubRow rowId="work-items-filter" wide>
              <input
                id="work-items-filter-input"
                type="text"
                placeholder="comma/space separated list of IDs"
                value={filterValue}
                onChange={(event) => setFilterValue(event.target.value)}
              />
            </SubRow>
          </>
        ) : null}
      </div>

      <div className="buttons-wrapper">
        <button type="button" id="compare-documents" disabled={!canCompare || busy !== null} onClick={compare}>
          <img src={compareIcon} alt="" />
          Compare
        </button>
      </div>
    </PanelShell>
  );
}
