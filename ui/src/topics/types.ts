/**
 * Mirrors of the REST models the two picker pages read. Kept field-for-field with the Java side:
 * `rest/model/search/*` for the search results and `rest/model/diff/LinkRoleOption` for the link roles.
 */

export interface EnumOption {
  id: string;
  name: string;
  iconUrl?: string | null;
}

/**
 * One row of the WorkItem table (ch...rest.model.search.SearchWorkItem).
 *
 * Everything below `readable` is filled only for a readable row, which is the only kind whose columns are
 * rendered - an unreadable one shows `unavailableMessage` across the whole width instead. Even `id` can be
 * missing, for an item the index returned and the repository could not resolve.
 */
export interface SearchWorkItem {
  id: string;
  projectId: string;
  title?: string | null;
  type?: EnumOption | null;
  status?: EnumOption | null;
  severity?: EnumOption | null;
  readable: boolean;
  unavailableMessage?: string | null;
}

/** One row of the baseline collection table (ch...rest.model.search.SearchCollection). */
export interface SearchCollection {
  id: string;
  projectId: string;
  name?: string | null;
  authorName?: string | null;
  /** Epoch milliseconds, formatted for display by the page. */
  created?: number | null;
  updated?: number | null;
  readable: boolean;
  unavailableMessage?: string | null;
}

/** One page of a search (ch...rest.model.search.SearchResult). */
export interface SearchResult<T> {
  totalCount: number;
  page: number;
  lastPage: number;
  query: string;
  items: T[];
}

/** ch...rest.model.duplication.ProjectInfo, as returned by GET /projects. */
export interface ProjectInfo {
  id: string;
  name: string;
}

/** ch...rest.model.diff.LinkRoleOption, as returned by GET /projects/{id}/link-roles. */
export interface LinkRoleOption {
  id: string;
  name: string;
  oppositeName?: string | null;
}
