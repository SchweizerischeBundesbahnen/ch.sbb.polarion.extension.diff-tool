import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup } from 'vitest-browser-react';
import WorkItemsPage from '../src/pages/WorkItemsPage';
import { fixture, openControlPane, renderViewer, restoreUrl, shoot } from './viewerHarness';

// Docker-only snapshots of the work items diff viewer, the workitems.html entry. It pairs work items
// selected by a query rather than by their place in a document, and renders its own copy of the pair
// row: components/workitems/WorkItemsPairDiff.jsx, a sibling of the documents one.

const WORKITEMS_URL =
  '/workitems?sourceProjectId=elibrary&targetProjectId=elibrary&linkRole=relates_to&configuration=Default' +
  '&sourceRecordsPerPage=20&sourcePage=1&config=Default&ids=1';

function renderWorkItems() {
  renderViewer(
    WORKITEMS_URL,
    [
      { method: 'POST', match: /\/diff\/workitems-pairs$/, json: fixture('workItems-pairs.json') ?? {} },
      { method: 'POST', match: /\/diff\/detached-workitems$/, json: fixture('detached-workitems.json') ?? {} },
    ],
    <WorkItemsPage />,
  );
}

async function loaded() {
  await vi.waitFor(() => expect(document.querySelector('.header .merge-pane')).not.toBeNull(), { timeout: 10000 });
  await vi.waitFor(() => expect(document.querySelectorAll('.wi-diff').length).toBeGreaterThan(0));

  // The sample pairs include a source work item with two counterparts, so the page opens over its
  // redundancy notice. That state is worth a reference of its own; these two are about the diff
  // underneath it, which the dialog's backdrop otherwise greys out.
  await vi.waitFor(() => expect(document.querySelector('[data-testid="redundancy-modal"]')).not.toBeNull());
  (document.querySelector('[data-testid="redundancy-modal-cancel-button"]') as HTMLButtonElement).click();
  await vi.waitFor(() =>
    expect(getComputedStyle(document.querySelector('[data-testid="redundancy-modal"]') as HTMLElement).display).toBe(
      'none',
    ),
  );
}

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  restoreUrl();
});

describe.skipIf(!__PIXEL_REFERENCES__)('Work items diff viewer visual', () => {
  it('the redundancy notice the page opens over', async () => {
    renderWorkItems();
    await vi.waitFor(() => expect(document.querySelector('[data-testid="redundancy-modal"]')).not.toBeNull(), {
      timeout: 10000,
    });
    await vi.waitFor(() => expect(document.querySelectorAll('.wi-diff').length).toBeGreaterThan(0));
    await shoot('workitems-redundancy-notice');
  });

  it('loaded, with the merge pane and the paired work items', async () => {
    renderWorkItems();
    await loaded();
    await shoot('workitems-diff');
  });

  it('configuration pane expanded, over the diff', async () => {
    renderWorkItems();
    await loaded();
    await openControlPane();
    await shoot('workitems-diff-control-pane');
  });
});
