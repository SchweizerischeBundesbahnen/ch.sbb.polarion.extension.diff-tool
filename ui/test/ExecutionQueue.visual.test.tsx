import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import { page } from 'vitest/browser';
import App from '../src/App';
import { installFetchMock } from './mockFetch';

/**
 * Docker-only snapshot of the Execution Queue page with its charts collapsed.
 *
 * The chart canvases are deliberately out of frame. Their content is time-varying by nature: Chart.js
 * labels the x-axis with the timestamps in the data, and how much data is present depends on how many of
 * the 3s polls landed before the screenshot. Freezing the clock and honouring the `from` cursors in the
 * mock each narrowed it, but the comparison stayed intermittent - and a reference that fails one run in
 * three is worse than no reference at all.
 *
 * So this covers what a screenshot is good at here: the page chrome - panel headers and their collapse
 * state, both configuration tables, the action toolbar and the quick help. That Chart.js is registered
 * correctly and actually draws is asserted in ExecutionQueuePage.test.tsx instead, by checking that the
 * canvas has painted pixels.
 */

const origUrl = window.location.pathname + window.location.search;

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  window.history.replaceState({}, '', origUrl);
});

const META = {
  features: [
    { id: 'DIFF_DOCUMENTS', label: 'Diff docs', description: '/diff/documents request' },
    { id: 'DIFF_HTML', label: 'Diff HTML', description: '/diff/html request' },
    { id: 'DIFF_TEXT', label: 'Diff text', description: '/diff/text request' },
  ],
  cpuLoad: { id: 'CPU_LOAD', label: 'CPU Load', description: 'Overall CPU load of the machine' },
  workerCount: 3,
  maxRecommendedThreads: 8,
  queueCapacity: 1000,
};

describe.skipIf(!__PIXEL_REFERENCES__)('Execution Queue page visual', () => {
  it('loaded, charts collapsed (panel headers, workers and threads tables, quick help)', async () => {
    installFetchMock([
      { method: 'GET', match: /\/queue\/configuration-meta$/, json: META },
      { method: 'GET', match: /\/extension\/info$/, json: { version: { bundleBuildTimestamp: '2026-07-01 10:00' } } },
      {
        method: 'GET',
        match: /\/names\/Default\/content/,
        json: {
          workers: { DIFF_DOCUMENTS: 1, DIFF_HTML: 1, DIFF_TEXT: 0 },
          threads: { '1': 2, '2': 1, '3': 1 },
          bundleTimestamp: '2026-07-01 10:00',
        },
      },
      { method: 'POST', match: /queueStatistics/, json: {} },
    ]);
    window.history.replaceState({}, '', '?feature=execution-queue&embedded=true');
    render(<App />);

    await vi.waitFor(() => expect(document.querySelector('#current-threads-1')!.textContent).toBe('2'));
    await vi.waitFor(() => expect(document.querySelectorAll('.chart-container')).toHaveLength(2));

    // Collapse both panels so nothing canvas-drawn is in frame.
    document.querySelectorAll<HTMLButtonElement>('.chart-expand-button').forEach((button) => button.click());
    await vi.waitFor(() => expect(document.querySelectorAll('.chart-container canvas')).toHaveLength(0));

    const app = document.querySelector('.app') as HTMLElement;
    await page.viewport(1280, Math.ceil(app.scrollHeight) + 40);
    await expect(page.elementLocator(app)).toMatchScreenshot('execution-queue-collapsed');
  });
});
