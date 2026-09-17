import type { ReactNode } from 'react';
import { expect, vi } from 'vitest';
import { render } from 'vitest-browser-react';
import { page } from 'vitest/browser';
import AppShell from '../src/components/AppShell';
import ErrorBoundary from '../src/components/ErrorBoundary';
import PublicShell from '../src/components/PublicShell';
import { type Route, installFetchMock, jsonResponse } from './mockFetch';
import { settleBeforeCapture, settleLayout } from './visualHelpers';

/**
 * What the three diff/merge viewers need to be photographed: the sample data, the shell they mount in,
 * and a capture that waits for the page to settle.
 *
 * The viewers are the part of this app the visual suites long left out. The admin pages, the two
 * Document Properties panels and the topic pickers have references; documents.html, workitems.html and
 * collections.html had none, because e2e/ asserts their behavior rather than their look.
 *
 * The fixtures are the ones e2e/ already drives these pages with, reached through `import.meta.glob`
 * rather than `fs`, so one set of sample data serves both suites and neither can drift from the other.
 */

const FIXTURES = import.meta.glob('../e2e/fixtures/*.json', { eager: true, import: 'default' }) as Record<
  string,
  unknown
>;

export const fixture = (name: string): unknown => FIXTURES[`../e2e/fixtures/${name}`];

/** The per-pair diff endpoint answers from `{leftId}_{rightId}.json`, exactly as e2e/test-utils.js does. */
export function pairDiff(_url: string, init?: RequestInit): Response {
  const body = JSON.parse((init?.body as string) ?? '{}');
  const left = body.leftWorkItem ? body.leftWorkItem.id : 'NONE';
  const right = body.rightWorkItem ? body.rightWorkItem.id : 'NONE';
  return jsonResponse(fixture(`${left}_${right}.json`) ?? { fieldDiffs: [] });
}

/**
 * The calls every viewer makes on the way up. The settings route matches any scope on purpose: the
 * collections viewer asks for the target project's configurations as well as the source project's.
 */
const SHARED_ROUTES: Route[] = [
  { method: 'GET', match: /\/extension\/info$/, json: fixture('version-info.json') ?? {} },
  { method: 'GET', match: /\/communication\/settings/, json: fixture('communication-settings.json') ?? {} },
  { method: 'GET', match: /\/settings\/diff\/names/, json: fixture('configs.json') ?? [] },
];

/** The viewport every capture is measured from, so the measurement cannot inherit the last one. */
const MEASURE_WIDTH = 1280;
const MEASURE_HEIGHT = 400;

const origUrl = window.location.pathname + window.location.search;

/** Put back whatever URL the file started on, so one viewer's query string cannot leak into the next. */
export function restoreUrl(): void {
  window.history.replaceState({}, '', origUrl);
}

export function renderViewer(url: string, routes: Route[], viewer: ReactNode): void {
  installFetchMock([...SHARED_ROUTES, ...routes]);
  window.history.replaceState({}, '', url);
  render(
    <AppShell>
      <PublicShell>
        <ErrorBoundary>{viewer}</ErrorBoundary>
      </PublicShell>
    </AppShell>,
  );
}

/**
 * Photographs the page shell once it has stopped moving.
 *
 * The documents and work items viewers show a progress bar that hides itself a second after the load
 * completes (ProgressBar.jsx), so a capture taken before that lands photographs a 4em band that a
 * capture taken after does not. Waiting for the settled state removes the race rather than outrunning
 * it, and it is also the state a user looks at. The collections viewer renders no progress bar at all,
 * hence the guard rather than an unconditional wait.
 */
export async function shoot(name: string): Promise<void> {
  if (document.querySelector('.progress')) {
    await vi.waitFor(
      () => expect(getComputedStyle(document.querySelector('.progress') as HTMLElement).display).toBe('none'),
      { timeout: 5000 },
    );
  }

  const app = document.querySelector('.diff-app') as HTMLElement;

  // Measure from a fixed viewport, never from whatever the previous capture left behind.
  //
  // `.diff-app` is `height: 100vh` (globals.css), so its scrollHeight is max(content, viewport). Sizing
  // the viewport from that and then leaving it set made every capture inherit the one before it: the
  // collections references came out 720, 760, 800, 840, a +40 staircase that had nothing to do with
  // their content, and adding a single test shifted every later reference by 66px. Resetting first
  // makes each capture depend on its own content alone, whatever order the file runs in.
  await page.viewport(MEASURE_WIDTH, MEASURE_HEIGHT);
  await settleLayout();
  const height = Math.max(Math.ceil(app.scrollHeight) + 40, MEASURE_HEIGHT);

  // No cap on the height: assertNotResampled in visualHelpers fails the capture if the page outgrows the
  // window, which names the fix instead of silently clipping or downscaling the reference.
  await page.viewport(MEASURE_WIDTH, height);
  await settleLayout();
  await settleBeforeCapture();
  await expect(page.elementLocator(app)).toMatchScreenshot(name);
}

/**
 * Waits for one of the viewer's dialogs to be on screen, named by its title.
 *
 * Modal.jsx keeps every dialog mounted and toggles an inline `display`, so "is it open" is a computed
 * style rather than a presence check, and not every one of them carries a testId.
 */
export async function openDialog(title: string): Promise<void> {
  await vi.waitFor(() => {
    const shown = [...document.querySelectorAll<HTMLElement>('.modal')].filter(
      (modal) => getComputedStyle(modal).display !== 'none',
    );
    expect(shown).toHaveLength(1);
    expect(shown[0].querySelector('.modal-title')?.textContent).toBe(title);
  });
}

/** Opens the configuration pane, the control #684 turned from an svg with an onClick into a button. */
export async function openControlPane(): Promise<void> {
  // Waited for, not assumed: the toggle renders only once `controlPaneAccessible` has arrived, which is
  // a second round trip after the pairs. Taking it straight worked only because this was never the
  // first capture in its file.
  const toggle = await vi.waitFor(() => {
    const found = document.querySelector<HTMLButtonElement>('.control-pane .expand-button');
    expect(found).not.toBeNull();
    return found!;
  });
  toggle.click();
  await vi.waitFor(() => expect(document.querySelectorAll('.control-pane.expanded')).toHaveLength(1));
}
