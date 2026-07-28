export interface WorkItemsFilter {
  value: string;
  /** `include` compares only the listed work items, `exclude` compares everything but them. */
  type: 'include' | 'exclude';
}

export interface DiffRequest {
  sourceProjectId: string;
  sourceSpaceId: string;
  sourceDocument: string;
  sourceRevision: string;
  targetProjectId: string;
  targetSpaceId: string;
  targetDocument: string;
  targetRevision: string;
  linkRole: string;
  config: string;
  branched: boolean;
  filter?: WorkItemsFilter;
}

const VIEWER_PATH = '/polarion/diff-tool-app/ui/app/documents.html';
const ADDITIONAL_PARAMS_SUFFIX = '_additionalParams';
const TTL_MS = 24 * 60 * 60 * 1000;

/**
 * Drops `<uuid>_additionalParams` entries older than 24 h, or unparseable, plus the `_filter` / `_type`
 * keys an older version of this handoff wrote. Without it every comparison would leak one localStorage
 * entry forever.
 */
function collectGarbage(now: number): void {
  Object.keys(localStorage)
    .filter((key) => key.endsWith(ADDITIONAL_PARAMS_SUFFIX))
    .forEach((key) => {
      try {
        const parsed = JSON.parse(localStorage.getItem(key) ?? 'null') as { ts?: unknown } | null;
        if (!parsed || typeof parsed.ts !== 'number' || now - parsed.ts > TTL_MS) {
          localStorage.removeItem(key);
        }
      } catch {
        localStorage.removeItem(key);
      }
    });

  Object.keys(localStorage)
    .filter((key) => key.endsWith('_filter') || key.endsWith('_type'))
    .forEach((key) => localStorage.removeItem(key));
}

/**
 * Builds the documents-comparison URL and stashes the parameters that do not belong in a query string.
 *
 * This is the handoff contract between the Document Properties panel and the viewer page
 * (src/pages/DocumentsPage.jsx reads both halves), and it MUST NOT drift: the query parameter names and
 * order, `compareAs=Workitems`, the `<uuid>_additionalParams` localStorage key and the shape written
 * under it are all a verbatim port of the legacy `DiffTool.showDiffResult()`. `linkRole`,
 * `sourceRevision` and `targetRevision` are appended only when non-empty, because the viewer treats a
 * present-but-empty parameter differently from an absent one.
 *
 * Returned (rather than only opened) so the contract can be asserted in a test.
 */
export function buildDocumentsDiffUrl(request: DiffRequest, now: number = Date.now()): string {
  let path =
    `${VIEWER_PATH}` +
    `?sourceProjectId=${request.sourceProjectId}&sourceSpaceId=${request.sourceSpaceId}&sourceDocument=${request.sourceDocument}` +
    `&targetProjectId=${request.targetProjectId}&targetSpaceId=${request.targetSpaceId}&targetDocument=${request.targetDocument}` +
    `&config=${request.config}&compareAs=Workitems&branched=${request.branched}`;
  if (request.linkRole) {
    path += `&linkRole=${request.linkRole}`;
  }
  if (request.sourceRevision) {
    path += `&sourceRevision=${request.sourceRevision}`;
  }
  if (request.targetRevision) {
    path += `&targetRevision=${request.targetRevision}`;
  }

  collectGarbage(now);

  const additionalParamsHash = crypto.randomUUID();
  const additionalParams: { individualFieldsSelection: boolean; ts: number; filter?: WorkItemsFilter } = {
    individualFieldsSelection: true,
    ts: now,
  };
  if (request.filter) {
    additionalParams.filter = request.filter;
  }
  localStorage.setItem(additionalParamsHash + ADDITIONAL_PARAMS_SUFFIX, JSON.stringify(additionalParams));

  return `${path}&additionalParams=${additionalParamsHash}`;
}

/** Opens the comparison in a new tab, as the legacy panel's Compare button did. */
export function openDocumentsDiff(request: DiffRequest): void {
  window.open(buildDocumentsDiffUrl(request), '_blank');
}
