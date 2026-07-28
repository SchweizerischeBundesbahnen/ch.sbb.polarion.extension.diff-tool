import { useEffect, useState } from 'react';
import { SearchableSelect } from '@grigoriev/react-sbb-polarion';
import { sendRequest } from '../services/useRemote';
import PanelShell from './PanelShell';
import type { PanelProps } from './panelProps';
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
  const [projectId, setProjectId] = useState('');
  const [spaceId, setSpaceId] = useState('');
  const [linkRole, setLinkRole] = useState('');
  const [config, setConfig] = useState(props.configurations[0] ?? '');
  const [handleReferences, setHandleReferences] = useState('');
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

  useEffect(() => setSpaceId(''), [projectId]);
  // Keep the selection only if the new project still offers it; otherwise fall back to its first
  // configuration, matching what the server preselects for the source project.
  useEffect(
    () => setConfig((current) => (configurationNames.includes(current) ? current : (configurationNames[0] ?? ''))),
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
          id="copy-project-selector"
          value={projectId}
          onChange={setProjectId}
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
          id="copy-space-selector"
          value={spaceId}
          onChange={setSpaceId}
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
          id="copy-link-role-selector"
          value={linkRole}
          onChange={setLinkRole}
          options={props.linkRoles}
          placeholder="Select Link Role..."
        />
      </div>
      <div className="property-wrapper">
        <label htmlFor="copy-config-selector" className="fixed-width w-1">
          Configuration:
        </label>
        <SearchableSelect
          id="copy-config-selector"
          value={config}
          onChange={setConfig}
          options={configurationNames.map((name) => ({ id: name, name: name }))}
          placeholder="Select Configuration..."
        />
      </div>

      <div className="property-wrapper">
        <label htmlFor="handle-refs-selector" className="fixed-width w-1">
          Referenced workitems:
        </label>
        <SearchableSelect
          id="handle-refs-selector"
          value={handleReferences}
          onChange={setHandleReferences}
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
