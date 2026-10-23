import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup } from 'vitest-browser-react';
import DocumentsPage from '../src/pages/DocumentsPage';
import { fixture, openControlPane, openDialog, pairDiff, renderViewer, restoreUrl } from './viewerHarness';

// A merge reloads the diff of each merged pair, and the pair stays selected. Its fields are rendered anew
// and must show the selection of the new load, not the one made before the merge.

const DOCUMENTS_URL =
  '/documents?sourceProjectId=elibrary&sourceSpaceId=Specification&sourceDocument=Catalog%20Specification' +
  '&targetProjectId=drivepilot&targetSpaceId=Design&targetDocument=Catalog%20Design&linkRole=relates_to';

const PAIR = '[data-testid="EL-4977_DP-11559"]';

const fieldTicker = (name: string) =>
  document.querySelector<HTMLInputElement>(`${PAIR} [aria-label="Select field ${name} for merge"]`);

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  restoreUrl();
});

describe('Field selection after a merge', () => {
  it('shows the fields of a reloaded pair as selected with the pair, not as picked before the merge', async () => {
    renderViewer(
      DOCUMENTS_URL,
      [
        { method: 'POST', match: /\/diff\/documents$/, json: fixture('documents-diff.json') ?? {} },
        { method: 'POST', match: /\/diff\/document-workitems$/, respond: pairDiff },
        { method: 'POST', match: /\/merge\/documents$/, json: fixture('EL-4977_DP-11559_merge.json') ?? {} },
      ],
      <DocumentsPage />,
    );
    // The toolbar shows once every pair has loaded. A pair still loading resets its selection when it is done.
    await vi.waitFor(
      () => expect(getComputedStyle(document.querySelector('.header .merge-pane') as HTMLElement).display).toBe('flex'),
      { timeout: 10000 },
    );
    await openControlPane();
    const perFieldSelection = document.querySelector<HTMLInputElement>('#per-field-merge-selection')!;
    if (!perFieldSelection.checked) {
      perFieldSelection.click();
    }
    document.querySelector<HTMLButtonElement>('.control-pane .expand-button')!.click();

    document.querySelector<HTMLInputElement>(`${PAIR} .header .merge-ticker input`)!.click();
    await vi.waitFor(() => expect(fieldTicker('Description')!.checked).toBe(true));
    fieldTicker('Description')!.click();
    await vi.waitFor(() => expect(fieldTicker('Description')!.checked).toBe(false));
    expect(fieldTicker('Status')!.checked).toBe(true);

    // From now on the diff answers a moment later, as a server does. A mock answering at once lets React
    // render the reload's "loading" and "loaded" as one, and the fields would never be rendered anew.
    const answerAtOnce = globalThis.fetch;
    vi.stubGlobal('fetch', (input: RequestInfo | URL, init?: RequestInit) =>
      /\/diff\/document-workitems$/.test(String(input))
        ? new Promise<Response>((resolve) => setTimeout(() => resolve(answerAtOnce(input, init)), 300))
        : answerAtOnce(input, init),
    );
    const descriptionBeforeMerge = fieldTicker('Description');
    document.querySelector<HTMLButtonElement>('.merge-pane .merge-button .btn')!.click();
    await openDialog('Merge confirmation');
    document.querySelector<HTMLButtonElement>('[data-testid="merge-confirmation-modal-action-button"]')!.click();
    await openDialog('Merge Report');

    // The pair reloads: its fields go away while it loads, and come back with the new load.
    await vi.waitFor(
      () => {
        expect(fieldTicker('Description')).not.toBeNull();
        expect(fieldTicker('Description')).not.toBe(descriptionBeforeMerge);
      },
      { timeout: 5000 },
    );
    expect(document.querySelector<HTMLInputElement>(`${PAIR} .header .merge-ticker input`)!.checked).toBe(true);
    expect(fieldTicker('Description')!.checked).toBe(true);
    expect(fieldTicker('Status')!.checked).toBe(true);
  });
});
