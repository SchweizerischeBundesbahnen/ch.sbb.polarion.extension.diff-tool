import { useEffect, useMemo, useState } from 'react';
import { SearchableSelect } from '@grigoriev/react-sbb-polarion';
import { sendRequest } from '../services/useRemote';
import PanelShell from './PanelShell';
import type { PanelProps } from './panelProps';
import { rememberedIfOffered, useAdoptRemembered, useRemembering } from './rememberedSelection';
import useRemoteList, { firstError, firstLoading } from './useRemoteList';

interface SpaceInfo {
  id: string;
  name: string;
}

interface SettingNameInfo {
  name: string;
}

/** What `POST .../duplicate` returns: where the new document was created. */
interface CreatedDocument {
  projectId: string;
  spaceId?: string | null;
  name: string;
}

interface CreatedLink {
  text: string;
  href: string;
}

const encode = (segment: string) => encodeURIComponent(segment);

// The ids of the legacy `<select>`s, which double as the cookie names the remembered selections are kept
// under (see rememberedSelection.ts) - so they must keep matching the ids rendered below.
const PROJECT_SELECT = 'copy-project-selector';
const SPACE_SELECT = 'copy-space-selector';
const LINK_ROLE_SELECT = 'copy-link-role-selector';
const CONFIG_SELECT = 'copy-config-selector';
const HANDLE_REFS_SELECT = 'handle-refs-selector';

/**
 * Builds the two halves of the "Document created" link: a readable label and the editor URL. Ported
 * verbatim from `CopyTool.createNewDocument()`, including the `_default` space being omitted from both
 * (Polarion's default space has no path segment) and the label being left unencoded while the href is
 * encoded.
 */
export function createdDocumentLink(created: CreatedDocument, basePath: string): CreatedLink {
  const showSpace = Boolean(created.spaceId) && created.spaceId !== '_default';
  const spaceInnerText = showSpace ? `${created.spaceId}/` : '';
  const spacePathPart = showSpace ? `${encode(created.spaceId!)}/` : '';
  return {
    text: `${basePath}#/project/${created.projectId}/${spaceInnerText}${created.name}`,
    href: `${basePath}#/project/${encode(created.projectId)}/wiki/${spacePathPart}${encode(created.name)}`,
  };
}

/**
 * React port of the "Documents Copy" Document Properties panel (the legacy
 * webapp/diff-tool/html/copy-tool.html + js/modules/CopyTool.js + GenericMixin.js).
 *
 * Unlike the comparison panel, the configuration list here follows the **target** project: picking a
 * project reloads the diff configuration names from that project's scope, which is what the legacy
 * `projectChanged()` -> `reloadSettings(targetProject)` chain did.
 */
export default function CopyToolPanel({ props }: { props: PanelProps }) {
  // Seeded from the cookie the legacy panel wrote, where the options are already known at mount.
  const projectIds = useMemo(() => props.projects.map((project) => project.id), [props.projects]);
  const linkRoleIds = useMemo(() => props.linkRoles.map((role) => role.id), [props.linkRoles]);
  const handleRefsIds = useMemo(
    () => props.handleReferencesTypes.map((type) => type.id),
    [props.handleReferencesTypes],
  );

  const [projectId, setProjectId] = useState(() => rememberedIfOffered(PROJECT_SELECT, projectIds));
  const [spaceId, setSpaceId] = useState('');
  const [linkRole, setLinkRole] = useState(() => rememberedIfOffered(LINK_ROLE_SELECT, linkRoleIds));
  const [config, setConfig] = useState(
    () => rememberedIfOffered(CONFIG_SELECT, props.configurations) || (props.configurations[0] ?? ''),
  );
  const [handleReferences, setHandleReferences] = useState(() =>
    rememberedIfOffered(HANDLE_REFS_SELECT, handleRefsIds),
  );
  const [copyComments, setCopyComments] = useState(false);

  const [creating, setCreating] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [created, setCreated] = useState<CreatedLink | null>(null);

  const spaces = useRemoteList<SpaceInfo>({
    url: projectId ? `/projects/${encode(projectId)}/spaces` : null,
    progressMessage: 'Loading spaces',
    errorMessage: 'Error occurred loading spaces',
  });

  const configurations = useRemoteList<SettingNameInfo>({
    url: projectId ? `/settings/diff/names?scope=project/${encode(projectId)}/` : null,
    progressMessage: 'Loading configurations',
    errorMessage: `Error occurred loading project [${projectId}] diff configuration`,
  });

  // Until a target project is chosen the server-injected list (the source project's scope) is what the
  // legacy fragment showed too.
  const configurationNames = projectId ? configurations.items.map((setting) => setting.name) : props.configurations;

  // A user's choice is remembered; the cascade reset below deliberately is not (see useRemembering).
  const chooseProject = useRemembering(PROJECT_SELECT, setProjectId);
  const chooseSpace = useRemembering(SPACE_SELECT, setSpaceId);
  const chooseLinkRole = useRemembering(LINK_ROLE_SELECT, setLinkRole);
  const chooseConfig = useRemembering(CONFIG_SELECT, setConfig);
  const chooseHandleReferences = useRemembering(HANDLE_REFS_SELECT, setHandleReferences);

  useEffect(() => setSpaceId(''), [projectId]);
  // ...and once the target project's spaces have loaded, the remembered one is re-applied, as the legacy
  // spaceDropdown.refresh() did. `_default` is offered by almost every project, so this usually hits.
  const spaceIds = useMemo(() => spaces.items.map((space) => space.id), [spaces.items]);
  useAdoptRemembered(SPACE_SELECT, spaceIds, setSpaceId);

  // Keep the selection if the new project still offers it, then prefer the remembered one, then its
  // first configuration - which is what the server preselects for the source project.
  useEffect(
    () =>
      setConfig((current) =>
        configurationNames.includes(current)
          ? current
          : rememberedIfOffered(CONFIG_SELECT, configurationNames) || (configurationNames[0] ?? ''),
      ),
    [configurationNames],
  );

  const busy = creating ? 'Creating a document' : firstLoading(spaces, configurations);
  const loadError = error ?? firstError(spaces, configurations);

  // Every field is required, exactly as in the legacy updateCreateButtonState(). Note that this is also
  // what makes copy-tool's leading "none" link role (id "") unusable - see linkRoles in panelProps.ts.
  const canCreate = Boolean(projectId && spaceId && linkRole && config && handleReferences);

  const create = async () => {
    setCreating(true);
    setError(null);
    setCreated(null);
    try {
      const revisionUrlPart = props.sourceRevision ? `?revision=${encode(props.sourceRevision)}` : '';
      const response = await sendRequest({
        method: 'POST',
        url:
          `/projects/${encode(props.sourceProjectId)}/spaces/${encode(props.sourceSpaceId)}` +
          `/documents/${encode(props.sourceDocument)}/duplicate${revisionUrlPart}`,
        contentType: 'application/json',
        body: JSON.stringify({
          targetDocumentIdentifier: {
            projectId: projectId,
            spaceId: spaceId,
            name: props.sourceDocument,
          },
          targetDocumentTitle: props.sourceDocumentTitle,
          linkRoleId: linkRole,
          configName: config,
          handleReferences: handleReferences,
          copyDocumentComments: copyComments,
        }),
      });
      const text = await response.text();
      if (!response.ok) {
        throw new Error(messageFrom(text));
      }
      setCreated(createdDocumentLink(JSON.parse(text) as CreatedDocument, `//${location.host}${location.pathname}`));
    } catch (caught) {
      setError((caught as Error).message || 'Error creating document');
    } finally {
      setCreating(false);
    }
  };

  return (
    <PanelShell prefix="copy" busy={busy} error={loadError}>
      <p>
        Please select <strong>target</strong> project and space. Additionally select which link role should be taken
        into account when copying WorkItems, as well as fields configuration.
      </p>

      <div className="property-wrapper">
        <label htmlFor="copy-project-selector" className="fixed-width w-1">
          Project:
        </label>
        <SearchableSelect
          id={PROJECT_SELECT}
          value={projectId}
          onChange={chooseProject}
          options={props.projects}
          placeholder="Select Project..."
          allowEmpty
        />
      </div>
      <div className="property-wrapper">
        <label htmlFor="copy-space-selector" className="fixed-width w-1">
          Space:
        </label>
        <SearchableSelect
          id={SPACE_SELECT}
          value={spaceId}
          onChange={chooseSpace}
          options={spaces.items}
          placeholder="Select Space..."
          allowEmpty
        />
      </div>

      <div className="property-wrapper">
        <label htmlFor="copy-link-role-selector" className="fixed-width w-1">
          Link role:
        </label>
        {/* No allowEmpty: the server puts a real "none" entry (id "") at the head of this list, and a
            second empty-valued option would shadow it. */}
        <SearchableSelect
          id={LINK_ROLE_SELECT}
          value={linkRole}
          onChange={chooseLinkRole}
          options={props.linkRoles}
          placeholder="Select Link Role..."
        />
      </div>
      <div className="property-wrapper">
        <label htmlFor="copy-config-selector" className="fixed-width w-1">
          Configuration:
        </label>
        <SearchableSelect
          id={CONFIG_SELECT}
          value={config}
          onChange={chooseConfig}
          options={configurationNames.map((name) => ({ id: name, name: name }))}
          placeholder="Select Configuration..."
        />
      </div>

      <div className="property-wrapper">
        <label htmlFor="handle-refs-selector" className="fixed-width w-1">
          Referenced workitems:
        </label>
        <SearchableSelect
          id={HANDLE_REFS_SELECT}
          value={handleReferences}
          onChange={chooseHandleReferences}
          options={props.handleReferencesTypes.map((type) => ({ id: type.id, name: type.title }))}
          placeholder="Select Behaviour..."
          allowEmpty
        />
      </div>

      <div className="property-wrapper">
        <input
          type="checkbox"
          id="copy-comments-checkbox"
          checked={copyComments}
          onChange={(event) => setCopyComments(event.target.checked)}
        />
        <label htmlFor="copy-comments-checkbox">Copy document comments</label>
      </div>

      <div className="buttons-wrapper">
        <button type="button" id="create-document" disabled={!canCreate || busy !== null} onClick={() => void create()}>
          <span className="sbb-icon-table-plus" role="img" aria-label="Add" style={{ marginRight: 6 }} />
          Create Document
        </button>
      </div>

      {created ? (
        <div id="creation-success" className="alert">
          <span className="alert-success">Document created:</span>{' '}
          <a href={created.href} target="_blank" rel="noreferrer" style={{ fontWeight: 'normal' }}>
            {created.text}
          </a>
        </div>
      ) : null}
    </PanelShell>
  );
}

/**
 * The server's error text. The duplicate endpoint answers with `{ "message": "..." }`, but a failure
 * upstream of the resource (a proxy, a session timeout) can answer with anything, so a non-JSON body
 * falls back to the legacy generic message rather than showing the user raw HTML.
 */
function messageFrom(body: string): string {
  try {
    const parsed = JSON.parse(body) as { message?: unknown };
    return typeof parsed.message === 'string' && parsed.message ? parsed.message : 'Error creating document';
  } catch {
    return 'Error creating document';
  }
}
