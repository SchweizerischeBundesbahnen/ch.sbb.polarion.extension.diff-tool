const VIEWER_PATH = '/polarion/diff-tool-app/ui/app/workitems.html';
const IDS_SUFFIX = '_ids';

export interface WorkItemsDiffRequest {
  sourceProjectId: string;
  targetProjectId: string;
  linkRole: string;
  config: string;
  /** IDs of the selected WorkItems, in the order the table offered them. */
  ids: string[];
}

/**
 * SHA-1 of a string as lowercase hex, as the legacy `DiffToolWidgetUtils.digestMessage` computed it. The hash
 * is only a localStorage key, never a security measure.
 */
export async function digestMessage(message: string): Promise<string> {
  const hashBuffer = await crypto.subtle.digest('SHA-1', new TextEncoder().encode(message));
  return Array.from(new Uint8Array(hashBuffer))
    .map((byte) => byte.toString(16).padStart(2, '0'))
    .join('');
}

/**
 * Builds the WorkItems-comparison URL and stashes the selected IDs under `<sha1>_ids` in localStorage.
 *
 * This is the handoff contract to the viewer page, ported verbatim from
 * `webapp/diff-tool/js/diff-tool-widget-utils.js` (`openWorkItemsDiffApplication`), and it MUST NOT drift:
 * the ID list travels through localStorage rather than the query string because it can be far longer than a
 * URL allows, and `services/useDiffService.js` looks it up by exactly that key - see the reading half in
 * `test/widgetHandoff.test.tsx`. The parameter names and their order are the legacy ones as well.
 *
 * The one deliberate correction, the same one `formext/openDocumentsDiff.ts` made: the values are
 * percent-encoded. The legacy code concatenated them raw, so a configuration or role name containing `&`
 * produced a different URL than intended. The viewer reads them through `URLSearchParams.get()`, which
 * decodes.
 *
 * Returned rather than only opened so the contract can be asserted in a test.
 */
export async function buildWorkItemsDiffUrl(request: WorkItemsDiffRequest): Promise<string> {
  const joinedIds = request.ids.join(',');
  const idsHash = await digestMessage(joinedIds);
  localStorage.setItem(idsHash + IDS_SUFFIX, joinedIds);

  const params = new URLSearchParams();
  params.set('sourceProjectId', request.sourceProjectId);
  params.set('targetProjectId', request.targetProjectId);
  params.set('linkRole', request.linkRole);
  params.set('config', request.config);
  params.set('ids', idsHash);

  return `${VIEWER_PATH}?${params.toString()}`;
}

/** Opens the comparison in a new tab, as the widget's Compare button did. */
export async function openWorkItemsDiff(request: WorkItemsDiffRequest): Promise<void> {
  window.open(await buildWorkItemsDiffUrl(request), '_blank');
}
