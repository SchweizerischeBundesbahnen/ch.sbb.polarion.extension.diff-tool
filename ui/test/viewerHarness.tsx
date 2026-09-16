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
  await settleLayout();
  // No cap on the height: assertNotResampled in visualHelpers fails the capture if the page outgrows the
  // window, which names the fix instead of silently clipping or downscaling the reference.
  await page.viewport(1280, Math.ceil(app.scrollHeight) + 40);
  await settleBeforeCapture();
  await expect(page.elementLocator(app)).toMatchScreenshot(name);
}

/** Opens the configuration pane, the control #684 turned from an svg with an onClick into a button. */
export async function openControlPane(): Promise<void> {
  (document.querySelector('.control-pane .expand-button') as HTMLButtonElement).click();
  await vi.waitFor(() => expect(document.querySelectorAll('.control-pane.expanded')).toHaveLength(1));
}
