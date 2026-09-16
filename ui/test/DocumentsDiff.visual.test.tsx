import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup } from 'vitest-browser-react';
import DocumentsPage from '../src/pages/DocumentsPage';
import { fixture, openControlPane, pairDiff, renderViewer, restoreUrl, shoot } from './viewerHarness';

// Docker-only snapshots of the documents diff viewer: the work item pairs with their merge tickers and
// pair toggles, and the same page with the configuration pane open over it.

const DOCUMENTS_URL =
  '/documents?sourceProjectId=elibrary&sourceSpaceId=Specification&sourceDocument=Catalog%20Specification' +
  '&targetProjectId=drivepilot&targetSpaceId=Design&targetDocument=Catalog%20Design&linkRole=relates_to';

function renderDocuments() {
  renderViewer(
    DOCUMENTS_URL,
    [
      { method: 'POST', match: /\/diff\/documents$/, json: fixture('documents-diff.json') ?? {} },
      { method: 'POST', match: /\/diff\/document-workitems$/, respond: pairDiff },
    ],
    <DocumentsPage />,
  );
}

async function loaded() {
  await vi.waitFor(() => expect(document.querySelector('.header .merge-pane')).not.toBeNull(), { timeout: 10000 });
  await vi.waitFor(() => expect(document.querySelectorAll('.wi-diff').length).toBeGreaterThan(0));
}

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  restoreUrl();
});

describe.skipIf(!__PIXEL_REFERENCES__)('Documents diff viewer visual', () => {
  it('loaded, with the merge pane and the work item pairs', async () => {
    renderDocuments();
    await loaded();
    await shoot('documents-diff');
  });

  it('configuration pane expanded, over the diff', async () => {
    renderDocuments();
    await loaded();
    await openControlPane();
    await shoot('documents-diff-control-pane');
  });
});
