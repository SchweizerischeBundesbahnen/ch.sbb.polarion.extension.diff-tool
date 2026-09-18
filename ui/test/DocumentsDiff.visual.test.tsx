import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup } from 'vitest-browser-react';
import DocumentsPage from '../src/pages/DocumentsPage';
import { jsonResponse } from './mockFetch';
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

/**
 * One pair answered with field issues on its Description.
 *
 * No fixture in e2e/fixtures carries an issue on any field, so the whole diff-issues state, the red
 * header and the popup listing the reasons, has never been rendered by any suite. The server does
 * return them, so the state is real; the sample data simply has none. Synthesized here rather than
 * added to e2e/fixtures, so the Playwright suite keeps asserting exactly what it asserts today.
 */
function pairDiffWithFieldIssues(url: string, init?: RequestInit): Response {
  const body = JSON.parse((init?.body as string) ?? '{}');
  if (body.leftWorkItem?.id !== 'EL-4977') {
    return pairDiff(url, init);
  }
  const data = structuredClone(fixture('EL-4977_DP-11559.json')) as {
    fieldDiffs: { id: string; issues: string[] }[];
  };
  data.fieldDiffs.find((field) => field.id === 'description')!.issues = [
    'Referenced work item EL-90001 has no counterpart in the target project.',
    'An image in the rich text is not attached to the target document.',
  ];
  return jsonResponse(data);
}

function renderDocumentsWithFieldIssues() {
  renderViewer(
    DOCUMENTS_URL,
    [
      { method: 'POST', match: /\/diff\/documents$/, json: fixture('documents-diff.json') ?? {} },
      { method: 'POST', match: /\/diff\/document-workitems$/, respond: pairDiffWithFieldIssues },
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

  it('a field with issues, and the popup a keyboard user opens on it', async () => {
    renderDocumentsWithFieldIssues();
    await loaded();

    // useFocus opens the same popup the pointer does, which is the half of #684 that had no reference.
    const header = await vi.waitFor(() => {
      const found = document.querySelector<HTMLElement>('[data-testid="EL-4977_DP-11559"] .diff-header[tabindex]');
      expect(found).not.toBeNull();
      return found!;
    });
    header.focus();
    await vi.waitFor(() => expect(document.querySelector('.tooltip-container')).not.toBeNull());
    // Both issues are listed, not just the first: the popup is the only place they are readable.
    expect(document.querySelectorAll('.tooltip-container li')).toHaveLength(2);

    await shoot('documents-diff-field-issues');
  });

  // The "Swap documents" dialog is deliberately not covered. DocumentsDiff only raises it above
  // PAIRS_COUNT_TO_ASK_SWAP_CONFIRMATION, which is 200, and documents-diff.json holds 19 pairs, so the
  // swap button goes straight through. Reaching it would mean a 200-pair fixture invented for one
  // screenshot, which is a worse trade than leaving that dialog to a behavioural test.
});
