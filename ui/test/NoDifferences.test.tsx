import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup } from 'vitest-browser-react';
import CollectionsPage from '../src/pages/CollectionsPage';
import DocumentsPage from '../src/pages/DocumentsPage';
import WorkItemsPage from '../src/pages/WorkItemsPage';
import { jsonResponse } from './mockFetch';
import { fixture, pairDiff, renderViewer, restoreUrl } from './viewerHarness';

// The "No differences" notice of the documents viewer, of the collections viewer which embeds it, and of the
// work items viewer: shown below the comparison header once a diff has
// loaded and found nothing, and never while the diff loads or when it found differences.

const DOCUMENTS =
  '/documents?sourceProjectId=elibrary&sourceSpaceId=Specification&sourceDocument=Catalog%20Specification' +
  '&targetProjectId=drivepilot&targetSpaceId=Design&targetDocument=Catalog%20Design&linkRole=relates_to&config=Default';

const noDiff = () => jsonResponse({ fieldDiffs: [] });

/** The fields fixture with no field differing. */
function fieldsWithoutDiffs(): unknown {
  const fields = structuredClone(fixture('documents-fields.json')) as {
    pairedFields: { leftField: { htmlDiff?: string | null }; rightField: { htmlDiff?: string | null } }[];
  };
  fields.pairedFields.forEach((pair) => {
    pair.leftField.htmlDiff = null;
    pair.rightField.htmlDiff = null;
  });
  return fields;
}

/** The content fixture with no text differing between work items. */
function contentWithoutDiffs(): unknown {
  const content = structuredClone(fixture('documents-content.json')) as {
    pairedContentAnchors: Record<string, { diffAbove?: unknown; diffBelow?: unknown } | null>[];
  };
  content.pairedContentAnchors.forEach((pair) =>
    [pair.leftAnchor, pair.rightAnchor].filter(Boolean).forEach((anchor) => {
      anchor!.diffAbove = null;
      anchor!.diffBelow = null;
    }),
  );
  return content;
}

function renderDocuments(
  url: string,
  routes: { pairDiff?: typeof pairDiff; fields?: unknown; content?: unknown } = {},
) {
  renderViewer(
    url,
    [
      { method: 'POST', match: /\/diff\/documents$/, json: fixture('documents-diff.json') ?? {} },
      { method: 'POST', match: /\/diff\/document-workitems$/, respond: routes.pairDiff ?? pairDiff },
      {
        method: 'POST',
        match: /\/diff\/documents-fields$/,
        json: routes.fields ?? fixture('documents-fields.json') ?? {},
      },
      {
        method: 'POST',
        match: /\/diff\/documents-content$/,
        json: routes.content ?? fixture('documents-content.json') ?? {},
      },
    ],
    <DocumentsPage />,
  );
}

const COLLECTIONS =
  '/collections?sourceProjectId=elibrary&sourceCollectionId=1&targetProjectId=drivepilot&targetCollectionId=1' +
  '&linkRole=relates_to&config=Default&compareAs=Workitems&sourceSpaceId=Specification' +
  '&sourceDocument=Administration+Specification&targetSpaceId=Specification&targetDocument=Administration+Specification';

/** A document of the source collection without a counterpart in the target collection. */
const COLLECTIONS_UNPAIRED =
  '/collections?sourceProjectId=elibrary&sourceCollectionId=1&targetProjectId=Project2&targetCollectionId=1' +
  '&linkRole=relates_to&config=Default&compareAs=Workitems&sourceSpaceId=Testing&sourceDocument=Test+Specification' +
  '&targetSpaceId=Testing&targetDocument=Test+Specification';

function renderCollections(url: string, collections: unknown) {
  renderViewer(
    url,
    [
      { method: 'POST', match: /\/diff\/collections$/, json: collections },
      { method: 'POST', match: /\/diff\/documents$/, json: fixture('documents-from-collection-diff.json') ?? {} },
      { method: 'POST', match: /\/diff\/document-workitems$/, respond: noDiff },
    ],
    <CollectionsPage />,
  );
}

const WORKITEMS =
  '/workitems?sourceProjectId=elibrary&targetProjectId=elibrary&linkRole=relates_to&configuration=Default' +
  '&sourceRecordsPerPage=20&sourcePage=1&config=Default&ids=1';

/**
 * The work items viewer, its pair answered with the given field differences. `redundant` are the selected work
 * items left out of the comparison for having several counterparts; `paired` false leaves no pair at all.
 */
function renderWorkItems(fieldDiffs?: unknown[], { redundant = [] as string[], paired = true } = {}) {
  const pairDiff = fixture('detached-workitems.json') as object;
  const pairs = structuredClone(fixture('workItems-pairs.json')) as {
    pairedWorkItems: unknown[];
    leftWorkItemIdsWithRedundancy: string[];
  };
  pairs.leftWorkItemIdsWithRedundancy = redundant;
  if (!paired) {
    pairs.pairedWorkItems = [];
  }
  renderViewer(
    WORKITEMS,
    [
      { method: 'POST', match: /\/diff\/workitems-pairs$/, json: pairs },
      {
        method: 'POST',
        match: /\/diff\/detached-workitems$/,
        json: fieldDiffs ? { ...pairDiff, fieldDiffs: fieldDiffs } : pairDiff,
      },
    ],
    <WorkItemsPage />,
  );
}

const notice = () => document.querySelector('#diff-body > .no-differences');

/** Waits for the diff to load: the viewer shows its merge pane only then. */
async function loaded(): Promise<void> {
  await vi.waitFor(() => expect(document.querySelector('#diff-body')).not.toBeNull(), { timeout: 10000 });
  await vi.waitFor(() => expect(document.querySelector('.merge-pane')).not.toBeNull(), { timeout: 10000 });
}

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
  restoreUrl();
});

describe('No differences notice', () => {
  it('shows when no work item pair differs', async () => {
    renderDocuments(DOCUMENTS, { pairDiff: noDiff });

    await vi.waitFor(() => expect(notice()?.textContent).toBe('No differences'), { timeout: 10000 });
    expect(notice()!.getAttribute('role')).toBe('status');
  });

  it('does not show when work items differ', async () => {
    renderDocuments(DOCUMENTS);

    await vi.waitFor(() => expect(document.querySelector('.wi-diff')).not.toBeNull(), { timeout: 10000 });
    await loaded();
    expect(notice()).toBeNull();
  });

  it('shows when no document field differs', async () => {
    renderDocuments(`${DOCUMENTS}&compareAs=Fields`, { fields: fieldsWithoutDiffs() });

    await vi.waitFor(() => expect(notice()?.textContent).toBe('No differences'), { timeout: 10000 });
  });

  it('does not show when document fields differ', async () => {
    renderDocuments(`${DOCUMENTS}&compareAs=Fields`);

    await loaded();
    await vi.waitFor(() => expect(document.querySelector('.wi-diff')).not.toBeNull(), { timeout: 10000 });
    expect(notice()).toBeNull();
  });

  it('shows when no content between work items differs', async () => {
    renderDocuments(`${DOCUMENTS}&compareAs=Content`, { content: contentWithoutDiffs() });

    await vi.waitFor(() => expect(notice()?.textContent).toBe('No differences'), { timeout: 10000 });
  });

  it('does not show when content between work items differs', async () => {
    renderDocuments(`${DOCUMENTS}&compareAs=Content`);

    await loaded();
    expect(notice()).toBeNull();
  });

  it('shows in the collections viewer for a paired document without differences', async () => {
    renderCollections(COLLECTIONS, fixture('collection-merge.json'));

    await vi.waitFor(() => expect(notice()?.textContent).toBe('No differences'), { timeout: 10000 });
  });

  it('does not show in the collections viewer for a document without a counterpart', async () => {
    renderCollections(COLLECTIONS_UNPAIRED, fixture('collections.json'));

    await vi.waitFor(() => expect(document.querySelector('[data-testid="create-document-button"]')).not.toBeNull(), {
      timeout: 10000,
    });
    expect(document.querySelector('.no-differences')).toBeNull();
  });

  it('shows in the work items viewer when no pair differs', async () => {
    renderWorkItems([]);

    await vi.waitFor(() => expect(notice()?.textContent).toBe('No differences'), { timeout: 10000 });
  });

  it('does not show in the work items viewer when every selected work item was left out', async () => {
    renderWorkItems([], { redundant: ['EL-16'], paired: false });

    await vi.waitFor(() => expect(document.querySelector('[data-testid="redundancy-modal"]')).not.toBeNull(), {
      timeout: 10000,
    });
    await loaded();
    expect(notice()).toBeNull();
  });

  it('does not show in the work items viewer when some selected work items were left out', async () => {
    renderWorkItems([], { redundant: ['EL-17'] });

    await vi.waitFor(() => expect(document.querySelector('.wi-diff')).not.toBeNull(), { timeout: 10000 });
    await loaded();
    expect(notice()).toBeNull();
  });

  it('does not show in the work items viewer when a pair differs', async () => {
    renderWorkItems();

    await vi.waitFor(() => expect(document.querySelector('.wi-diff')).not.toBeNull(), { timeout: 10000 });
    await loaded();
    expect(notice()).toBeNull();
  });
});
