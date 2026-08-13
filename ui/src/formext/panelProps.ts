export interface IdName {
  id: string;
  name: string;
}

export interface HandleReferencesOption {
  id: string;
  title: string;
  description: string;
}

/**
 * Everything the server already knows when it renders the Document Properties fragment, handed to the
 * panel in one go as `data-props` (JSON, HTML-attribute-escaped by BaseFormExtension).
 *
 * Server-injected rather than fetched: this fragment is rendered on *every* document open, so having
 * React fetch the projects, link roles and configuration names would add three REST round-trips to a
 * pane the user may never look at - and two of those lists have no endpoint today. The five separate
 * placeholders the legacy fragment used (`{PROJECT_OPTIONS}` and friends, raw `<option>` HTML) cannot
 * survive the move to React, which owns the `<select>` children.
 */
export interface PanelProps {
  /** The document the panel is attached to: `IModule` project / space / name / title / revision. */
  sourceProjectId: string;
  sourceSpaceId: string;
  sourceDocument: string;
  sourceDocumentTitle: string;
  /** `""` for a HEAD document; a revision string when the editor shows a specific revision. */
  sourceRevision: string;
  projects: IdName[];
  /** Link roles of the source project. For copy-tool the list starts with `{ id: "", name: "none" }`. */
  linkRoles: IdName[];
  /** Diff configuration names in the source project's scope; never empty (falls back to `["Default"]`). */
  configurations: string[];
  handleReferencesTypes: HandleReferencesOption[];
}

const EMPTY: PanelProps = {
  sourceProjectId: '',
  sourceSpaceId: '',
  sourceDocument: '',
  sourceDocumentTitle: '',
  sourceRevision: '',
  projects: [],
  linkRoles: [],
  configurations: [],
  handleReferencesTypes: [],
};

/**
 * Reads the server-injected props off the fragment div. Anything missing or malformed degrades to the
 * empty set rather than throwing: a panel that renders with no projects to pick is recoverable (the user
 * sees empty dropdowns), a panel that throws during mount leaves the Document Properties pane blank.
 */
export function readPanelProps(host: HTMLElement): PanelProps {
  const raw = host.dataset.props;
  if (!raw) {
    return EMPTY;
  }
  try {
    return { ...EMPTY, ...(JSON.parse(raw) as Partial<PanelProps>) };
  } catch (error) {
    console.error('diff-tool: could not parse the panel props injected by the server.', error);
    return EMPTY;
  }
}
