import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup } from 'vitest-browser-react';
import DocumentsPage from '../src/pages/DocumentsPage';
import { fixture, openControlPane, openDialog, pairDiff, renderViewer, restoreUrl, shoot } from './viewerHarness';

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

  // The auxiliary dialogs, each over the page that raises it rather than on its own. What a dialog looks
  // like is only half of it: the other half is the backdrop over the page underneath, which is where a
  // change to the shared Modal shows up.

  it('the merge confirmation dialog, raised over a selected pair', async () => {
    renderDocuments();
    await loaded();

    // The merge button stays disabled until something is selected. The pair is named rather than taken
    // first: only a pair that actually has diffs renders a ticker that selects anything, and this is the
    // one the merge specs in e2e/ drive for the same reason.
    // Each pair fetches its own diff, and the ticker only appears once that pair has one, so the
    // checkbox is waited for rather than assumed present when the rows first render.
    const ticker = await vi.waitFor(() => {
      const box = document.querySelector<HTMLInputElement>(
        '[data-testid="EL-4977_DP-11559"] .merge-ticker input[type="checkbox"]',
      );
      expect(box).not.toBeNull();
      return box!;
    });
    ticker.click();
    const merge = await vi.waitFor(() => {
      const button = document.querySelector<HTMLButtonElement>('.merge-pane .merge-button .btn')!;
      expect(button.disabled).toBe(false);
      return button;
    });
    merge.click();

    await openDialog('Merge confirmation');
    await shoot('documents-diff-merge-confirmation');
  });

  // The "Swap documents" dialog is deliberately not covered. DocumentsDiff only raises it above
  // PAIRS_COUNT_TO_ASK_SWAP_CONFIRMATION, which is 200, and documents-diff.json holds 19 pairs, so the
  // swap button goes straight through. Reaching it would mean a 200-pair fixture invented for one
  // screenshot, which is a worse trade than leaving that dialog to a behavioural test.
});
