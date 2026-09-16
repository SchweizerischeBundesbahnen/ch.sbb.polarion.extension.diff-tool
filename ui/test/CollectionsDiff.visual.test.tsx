import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup } from 'vitest-browser-react';
import CollectionsPage from '../src/pages/CollectionsPage';
import { fixture, openControlPane, openDialog, pairDiff, renderViewer, restoreUrl, shoot } from './viewerHarness';

// Docker-only snapshots of the collections diff viewer, the collections.html entry.
//
// It pairs the documents of two collections and then hands the chosen pair to the documents viewer, so
// which of its two faces you see depends on whether that pair has a counterpart. Both are covered here,
// each from the sample data e2e/ already drives that face with:
//
//   collection-merge.json + documents-from-collection-diff.json  -> a paired document, diffed
//   collections.json                                             -> no counterpart, offering to create one
//
// Unlike the other two viewers this one renders no progress bar, which is why shoot() guards that wait
// rather than making it unconditional.

const PAIRED_URL =
  '/collections?sourceProjectId=elibrary&sourceCollectionId=1&targetProjectId=drivepilot&targetCollectionId=1' +
  '&linkRole=relates_to&config=Default&compareAs=Workitems&sourceSpaceId=Specification' +
  '&sourceDocument=Administration+Specification&targetSpaceId=Specification&targetDocument=Administration+Specification';

const UNPAIRED_URL =
  '/collections?sourceProjectId=elibrary&sourceCollectionId=1&targetProjectId=Project2&targetCollectionId=1' +
  '&linkRole=relates_to&config=Default&compareAs=Workitems&sourceSpaceId=Testing&sourceDocument=Test+Specification' +
  '&targetSpaceId=Testing&targetDocument=Test+Specification';

function renderCollections(url: string, collections: unknown, documents: unknown) {
  renderViewer(
    url,
    [
      { method: 'POST', match: /\/diff\/collections$/, json: collections },
      { method: 'POST', match: /\/diff\/documents$/, json: documents },
      { method: 'POST', match: /\/diff\/document-workitems$/, respond: pairDiff },
      { method: 'POST', match: /\/duplicate$/, json: fixture('duplicate.json') ?? {} },
    ],
    <CollectionsPage />,
  );
}

function renderPaired() {
  renderCollections(PAIRED_URL, fixture('collection-merge.json'), fixture('documents-from-collection-diff.json'));
}

async function headerLoaded() {
  await vi.waitFor(() => expect(document.querySelector('[data-testid="LEFT-collection"]')).not.toBeNull(), {
    timeout: 10000,
  });
}

/** The pair rows only render once their own diff has arrived, one request per pair. */
async function pairsLoaded() {
  await vi.waitFor(() => expect(document.querySelectorAll('.wi-diff .content').length).toBeGreaterThan(0), {
    timeout: 10000,
  });
}

async function createOffered() {
  await vi.waitFor(() => expect(document.querySelector('[data-testid="create-document-button"]')).not.toBeNull());
}

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  restoreUrl();
});

describe.skipIf(!__PIXEL_REFERENCES__)('Collections diff viewer visual', () => {
  it('a paired document, handed to the documents viewer', async () => {
    renderPaired();
    await headerLoaded();
    await pairsLoaded();
    await shoot('collections-diff');
  });

  it('configuration pane expanded, over the diff', async () => {
    renderPaired();
    await headerLoaded();
    await pairsLoaded();
    await openControlPane();
    await shoot('collections-diff-control-pane');
  });

  it('a document with no counterpart, offering to create one', async () => {
    renderCollections(UNPAIRED_URL, fixture('collections.json'), fixture('documents-from-collection.json'));
    await headerLoaded();
    await createOffered();
    await shoot('collections-diff-no-counterpart');
  });

  it('the document configuration dialog, raised over that offer', async () => {
    renderCollections(UNPAIRED_URL, fixture('collections.json'), fixture('documents-from-collection.json'));
    await headerLoaded();
    await createOffered();
    document.querySelector<HTMLButtonElement>('[data-testid="create-document-button"]')!.click();

    await openDialog('Choose document configuration');
    await shoot('collections-diff-target-configuration');
  });
});
