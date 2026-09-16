import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import { page } from 'vitest/browser';
import AppShell from '../src/components/AppShell';
import ErrorBoundary from '../src/components/ErrorBoundary';
import PublicShell from '../src/components/PublicShell';
import DocumentsPage from '../src/pages/DocumentsPage';
import { installFetchMock, jsonResponse } from './mockFetch';
import { settleBeforeCapture, settleLayout } from './visualHelpers';

/**
 * Docker-only snapshots of the documents diff viewer.
 *
 * The viewer is the one part of this app the visual suites never covered: the admin pages and the two
 * Document Properties panels have references, the diff/merge pages had none, and e2e/ asserts their
 * behavior rather than their look. So a change to the merge ticker, the pair toggle or the
 * configuration pane moved pixels that nothing compared.
 *
 * The fixtures are the ones e2e/ already drives the same page with, loaded through Vite rather than
 * `fs`, so one set of sample data serves both suites.
 */

const FIXTURES = import.meta.glob('../e2e/fixtures/*.json', { eager: true, import: 'default' }) as Record<
  string,
  unknown
>;

const fixture = (name: string): unknown => FIXTURES[`../e2e/fixtures/${name}`];

/** The per-pair diff endpoint answers from `{leftId}_{rightId}.json`, exactly as e2e/test-utils.js does. */
function workItemsDiff(_url: string, init?: RequestInit): Response {
  const body = JSON.parse((init?.body as string) ?? '{}');
  const left = body.leftWorkItem ? body.leftWorkItem.id : 'NONE';
  const right = body.rightWorkItem ? body.rightWorkItem.id : 'NONE';
  const found = fixture(`${left}_${right}.json`);
  return jsonResponse(found ?? { fieldDiffs: [] });
}

const DOCUMENTS_URL =
  '/documents?sourceProjectId=elibrary&sourceSpaceId=Specification&sourceDocument=Catalog%20Specification' +
  '&targetProjectId=drivepilot&targetSpaceId=Design&targetDocument=Catalog%20Design&linkRole=relates_to';

const origUrl = window.location.pathname + window.location.search;

function mockViewer() {
  installFetchMock([
    { method: 'GET', match: /\/extension\/info$/, json: fixture('version-info.json') ?? {} },
    { method: 'GET', match: /\/communication\/settings/, json: fixture('communication-settings.json') ?? {} },
    { method: 'GET', match: /\/settings\/diff\/names/, json: fixture('configs.json') ?? [] },
    { method: 'POST', match: /\/diff\/documents$/, json: fixture('documents-diff.json') ?? {} },
    { method: 'POST', match: /\/diff\/document-workitems$/, respond: workItemsDiff },
  ]);
}

/**
 * The progress bar hides itself a second after the load completes (ProgressBar.jsx), so a capture taken
 * before that lands photographs a 4em band that a capture taken after does not. Waiting for the settled
 * state removes the race rather than outrunning it, and it is also the state a user looks at.
 */
async function settledPage(): Promise<HTMLElement> {
  await vi.waitFor(
    () => expect(getComputedStyle(document.querySelector('.progress') as HTMLElement).display).toBe('none'),
    { timeout: 5000 },
  );
  return document.querySelector('.diff-app') as HTMLElement;
}

async function shoot(name: string) {
  const app = await settledPage();
  await settleLayout();
  // No cap on the height: assertNotResampled in visualHelpers fails the capture if the page outgrows
  // the window, which names the fix instead of silently clipping or downscaling the reference.
  await page.viewport(1280, Math.ceil(app.scrollHeight) + 40);
  await settleBeforeCapture();
  await expect(page.elementLocator(app)).toMatchScreenshot(name);
}

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  window.history.replaceState({}, '', origUrl);
});

function renderViewer() {
  mockViewer();
  window.history.replaceState({}, '', DOCUMENTS_URL);
  render(
    <AppShell>
      <PublicShell>
        <ErrorBoundary>
          <DocumentsPage />
        </ErrorBoundary>
      </PublicShell>
    </AppShell>,
  );
}

async function loaded() {
  await vi.waitFor(() => expect(document.querySelector('.header .merge-pane')).not.toBeNull(), { timeout: 10000 });
  await vi.waitFor(() => expect(document.querySelectorAll('.wi-diff').length).toBeGreaterThan(0));
}

describe.skipIf(!__PIXEL_REFERENCES__)('Documents diff viewer visual', () => {
  it('loaded, with the merge pane and the work item pairs', async () => {
    renderViewer();
    await loaded();
    await shoot('documents-diff');
  });

  it('configuration pane expanded, over the diff', async () => {
    renderViewer();
    await loaded();

    // The pane toggle is the control #684 turned from an svg with an onClick into a button. Opening it
    // here puts both states of that control, and the whole pane it reveals, under the comparison.
    (document.querySelector('.control-pane .expand-button') as HTMLButtonElement).click();
    await vi.waitFor(() => expect(document.querySelectorAll('.control-pane.expanded')).toHaveLength(1));

    await shoot('documents-diff-control-pane');
  });
});
