import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup } from 'vitest-browser-react';
import DocumentsPage from '../src/pages/DocumentsPage';
import WorkItemsPage from '../src/pages/WorkItemsPage';
import { setFieldValue } from './formextHelpers';
import { fixture, openControlPane, pairDiff, renderViewer, restoreUrl } from './viewerHarness';

// The "Diff view" dropdown of the configuration pane. The view lives in the app context, not in the query
// string, and switching it only restyles the page: no diff is fetched again and no selection is lost.

const DOCUMENTS_URL =
  '/documents?sourceProjectId=elibrary&sourceSpaceId=Specification&sourceDocument=Catalog%20Specification' +
  '&targetProjectId=drivepilot&targetSpaceId=Design&targetDocument=Catalog%20Design&linkRole=relates_to';

const WORKITEMS_URL =
  '/workitems?sourceProjectId=elibrary&targetProjectId=elibrary&linkRole=relates_to&configuration=Default' +
  '&sourceRecordsPerPage=20&sourcePage=1&config=Default&ids=1';

const PAIR_TICKER = '[data-testid="EL-4977_DP-11559"] .merge-ticker input[type="checkbox"]';

function renderDocuments() {
  renderViewer(
    DOCUMENTS_URL,
    [
      { method: 'POST', match: /\/diff\/documents$/, json: fixture('documents-diff.json') ?? {} },
      { method: 'POST', match: /\/diff\/document-workitems$/, respond: pairDiff },
      { method: 'POST', match: /\/diff\/documents-fields$/, json: fixture('documents-fields.json') ?? {} },
    ],
    <DocumentsPage />,
  );
}

async function documentsLoaded() {
  await vi.waitFor(() => expect(document.querySelector('.header .merge-pane')).not.toBeNull(), { timeout: 10000 });
  await vi.waitFor(() => expect(document.querySelector(PAIR_TICKER)).not.toBeNull());
}

const diffBody = () => document.querySelector('#diff-body');

const viewSelect = () => document.querySelector<HTMLSelectElement>('#diff-view')!;

const diffRequests = () =>
  vi.mocked(fetch).mock.calls.filter(([input]) => /\/diff\//.test(typeof input === 'string' ? input : input.toString()))
    .length;

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  restoreUrl();
});

describe('Diff view switch', () => {
  it('opens side by side', async () => {
    renderDocuments();
    await documentsLoaded();
    await openControlPane();

    expect(viewSelect().value).toBe('side-by-side');
    expect(diffBody()).toHaveClass('diff-view-side-by-side');
    expect(diffBody()).not.toHaveClass('diff-view-unified');
  });

  it('switches to unified without reloading the diff or losing the selection', async () => {
    renderDocuments();
    await documentsLoaded();
    document.querySelector<HTMLInputElement>(PAIR_TICKER)!.click();
    await vi.waitFor(() => expect(document.querySelector<HTMLInputElement>(PAIR_TICKER)!.checked).toBe(true));
    await openControlPane();
    const requestsBefore = diffRequests();

    setFieldValue(viewSelect(), 'unified');

    await vi.waitFor(() => expect(diffBody()).toHaveClass('diff-view-unified'));
    expect(diffBody()).not.toHaveClass('diff-view-side-by-side');
    expect(document.querySelector<HTMLInputElement>(PAIR_TICKER)!.checked).toBe(true);
    expect(diffRequests()).toBe(requestsBefore);
  });

  it('keeps the view when the documents are compared as something else', async () => {
    renderDocuments();
    await documentsLoaded();
    await openControlPane();
    setFieldValue(viewSelect(), 'unified');
    await vi.waitFor(() => expect(diffBody()).toHaveClass('diff-view-unified'));

    setFieldValue(document.querySelector<HTMLSelectElement>('#target-type')!, 'Fields');

    await vi.waitFor(() => expect(document.querySelector('[data-testid="version-field-diff"]')).not.toBeNull(), {
      timeout: 10000,
    });
    expect(diffBody()).toHaveClass('diff-view-unified');
    expect(viewSelect().value).toBe('unified');
  });

  it('is offered by the work items viewer too', async () => {
    renderViewer(
      WORKITEMS_URL,
      [
        { method: 'POST', match: /\/diff\/workitems-pairs$/, json: fixture('workItems-pairs.json') ?? {} },
        { method: 'POST', match: /\/diff\/detached-workitems$/, json: fixture('detached-workitems.json') ?? {} },
      ],
      <WorkItemsPage />,
    );
    await vi.waitFor(() => expect(document.querySelector('.header .merge-pane')).not.toBeNull(), { timeout: 10000 });
    await openControlPane();

    setFieldValue(viewSelect(), 'unified');

    await vi.waitFor(() => expect(diffBody()).toHaveClass('diff-view-unified'));
  });

  // axe accepts a label that names only the hidden native <select>, see the link role direction case in
  // DocumentsDiff.visual.test.tsx.
  it('names the diff view combobox after its label', async () => {
    renderDocuments();
    await documentsLoaded();
    await openControlPane();
    const trigger = await vi.waitFor(() => {
      const found = document.querySelector('#diff-view + .searchable-dropdown .sd-trigger');
      expect(found).not.toBeNull();
      return found!;
    });
    expect(trigger).toHaveAccessibleName('Diff view:');
  });
});
