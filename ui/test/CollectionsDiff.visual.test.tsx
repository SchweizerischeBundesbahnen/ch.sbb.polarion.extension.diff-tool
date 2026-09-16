import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup } from 'vitest-browser-react';
import CollectionsPage from '../src/pages/CollectionsPage';
import { fixture, openControlPane, pairDiff, renderViewer, restoreUrl, shoot } from './viewerHarness';

// Docker-only snapshots of the collections diff viewer, the collections.html entry. It pairs the
// documents of two collections first and diffs the chosen pair below, so the capture holds both the
// collection header and the document diff under it.
//
// Unlike the other two viewers this one renders no progress bar, which is why shoot() guards that wait
// rather than making it unconditional.

const COLLECTIONS_URL =
  '/collections?sourceProjectId=elibrary&sourceCollectionId=1&targetProjectId=Project2&targetCollectionId=1' +
  '&linkRole=relates_to&config=Default&compareAs=Workitems&sourceSpaceId=Testing&sourceDocument=Test+Specification' +
  '&targetSpaceId=Testing&targetDocument=Test+Specification';

function renderCollections() {
  renderViewer(
    COLLECTIONS_URL,
    [
      { method: 'POST', match: /\/diff\/collections$/, json: fixture('collections.json') ?? {} },
      { method: 'POST', match: /\/diff\/documents$/, json: fixture('documents-from-collection.json') ?? {} },
      { method: 'POST', match: /\/diff\/document-workitems$/, respond: pairDiff },
      { method: 'POST', match: /\/duplicate$/, json: fixture('duplicate.json') ?? {} },
    ],
    <CollectionsPage />,
  );
}

async function loaded() {
  await vi.waitFor(() => expect(document.querySelector('[data-testid="LEFT-collection"]')).not.toBeNull(), {
    timeout: 10000,
  });
  await vi.waitFor(() =>
    expect(document.querySelector('[data-testid="LEFT-collection"] .path-value')?.textContent).toBeTruthy(),
  );
}

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  restoreUrl();
});

describe.skipIf(!__PIXEL_REFERENCES__)('Collections diff viewer visual', () => {
  it('loaded, with the paired collections and their documents', async () => {
    renderCollections();
    await loaded();
    await shoot('collections-diff');
  });

  it('configuration pane expanded, over the diff', async () => {
    renderCollections();
    await loaded();
    await openControlPane();
    await shoot('collections-diff-control-pane');
  });
});
