const VIEWER_PATH = '/polarion/diff-tool-app/ui/app/collections.html';

export interface CollectionsDiffRequest {
  sourceProjectId: string;
  sourceCollectionId: string;
  targetProjectId: string;
  targetCollectionId: string;
  linkRole: string;
  config: string;
}

/**
 * Builds the collections-comparison URL, ported verbatim from
 * `webapp/diff-tool/js/diff-tool-widget-utils.js` (`openCollectionsDiffApplication`). The parameter names,
 * their order and `compareAs=Workitems` are the contract `src/pages/CollectionsPage.jsx` reads; only the
 * percent-encoding is new, as in openWorkItemsDiff.
 *
 * Returned rather than only opened so the contract can be asserted in a test.
 */
export function buildCollectionsDiffUrl(request: CollectionsDiffRequest): string {
  const params = new URLSearchParams();
  params.set('sourceProjectId', request.sourceProjectId);
  params.set('sourceCollectionId', request.sourceCollectionId);
  params.set('targetProjectId', request.targetProjectId);
  params.set('targetCollectionId', request.targetCollectionId);
  params.set('linkRole', request.linkRole);
  params.set('config', request.config);
  params.set('compareAs', 'Workitems');

  return `${VIEWER_PATH}?${params.toString()}`;
}

/** Opens the comparison in a new tab, as the widget's Compare button did. */
export function openCollectionsDiff(request: CollectionsDiffRequest): void {
  window.open(buildCollectionsDiffUrl(request), '_blank');
}
