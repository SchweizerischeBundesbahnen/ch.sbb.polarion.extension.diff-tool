import { Toaster } from '@grigoriev/react-sbb-polarion';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import ExecutionQueuePage from '../src/admin/pages/ExecutionQueuePage';
import { answerConfirm } from './confirmDialog';
import { type FetchMock, type Route, installFetchMock, jsonResponse } from './mockFetch';

// Behaviour of the port of execution.js: which charts exist, the two configuration tables, and
// save/cancel/default. The chart rendering itself is Chart.js on a canvas and is covered by the visual
// test; here the concern is the data and the settings round-trip.

const origUrl = window.location.pathname + window.location.search;

const META = {
  features: [
    { id: 'DIFF_DOCUMENTS', label: 'Diff docs', description: '/diff/documents request' },
    { id: 'DIFF_HTML', label: 'Diff HTML', description: '/diff/html request' },
    { id: 'DIFF_TEXT', label: 'Diff text', description: '/diff/text request' },
  ],
  cpuLoad: { id: 'CPU_LOAD', label: 'CPU Load', description: 'Overall CPU load' },
  workerCount: 3,
  maxRecommendedThreads: 4,
  queueCapacity: 1000,
};

// DIFF_DOCUMENTS on worker 1, DIFF_HTML on worker 2, DIFF_TEXT skipping the queue.
const SAVED = {
  workers: { DIFF_DOCUMENTS: 1, DIFF_HTML: 2, DIFF_TEXT: 0 },
  threads: { '1': 1, '2': 2, '3': 1 },
  bundleTimestamp: '2026-07-01 10:00',
};
const INFO = { version: { bundleBuildTimestamp: '2026-07-01 10:00' } };

function routes(overrides: Route[] = []): Route[] {
  return [
    ...overrides,
    { method: 'GET', match: /\/queue\/configuration-meta$/, json: META },
    { method: 'GET', match: /\/extension\/info$/, json: INFO },
    { method: 'GET', match: /\/names\/Default\/content/, json: SAVED },
    { method: 'GET', match: /\/default-content$/, json: { workers: { DIFF_DOCUMENTS: 0 }, threads: { '1': 1 } } },
    { method: 'GET', match: /\/revisions\?/, json: [{ name: '4711', date: '2026-06-30', author: 'jane' }] },
    { method: 'PUT', match: /\/names\/Default\/content/, json: {} },
    { method: 'POST', match: /queueStatistics/, json: {} },
  ];
}

function Page() {
  return (
    <>
      <ExecutionQueuePage />
      <Toaster />
    </>
  );
}

async function renderPage(fetchMock: FetchMock = installFetchMock(routes())) {
  render(<Page />);
  await vi.waitFor(() => expect(document.querySelector('#features-workers')).not.toBeNull());
  return fetchMock;
}

const toolbarButton = (index: number) =>
  Array.from(document.querySelectorAll<HTMLButtonElement>('.actions-pane button'))[index];

const chartTitles = () =>
  Array.from(document.querySelectorAll('.chart-container .chart-header h3')).map((h) => h.textContent);

/**
 * Sets a controlled field's value the way a user would. Assigning `.value` directly is not enough: React
 * caches the last value it saw on the node and dedupes an event whose value matches that cache, so
 * onChange never fires. The prototype setter updates the node without touching React's cache.
 */
function setFieldValue(element: HTMLInputElement | HTMLSelectElement, value: string): void {
  const prototype = element instanceof HTMLSelectElement ? HTMLSelectElement.prototype : HTMLInputElement.prototype;
  Object.getOwnPropertyDescriptor(prototype, 'value')!.set!.call(element, value);
  element.dispatchEvent(new Event(element instanceof HTMLSelectElement ? 'change' : 'input', { bubbles: true }));
}

/** React listens for focusout, not blur - blur does not bubble, so it never reaches React's root. */
function blur(element: HTMLElement): void {
  element.dispatchEvent(new FocusEvent('focusout', { bubbles: true }));
}

beforeEach(() => {
  window.history.replaceState({}, '', '?feature=execution-queue&embedded=true');
});

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
  window.history.replaceState({}, '', origUrl);
});

describe('ExecutionQueuePage', () => {
  it('charts only the workers something is assigned to, plus CPU load', async () => {
    await renderPage();

    // Worker 3 has no feature and DIFF_TEXT skips the queue, so neither gets a chart.
    expect(chartTitles()).toEqual(['Worker-1', 'Worker-2', 'CPU Load']);
  });

  it('lists every feature with the label and tooltip from the server, not a hardcoded map', async () => {
    await renderPage();

    expect(document.querySelector('#feature-DIFF_HTML')!.textContent).toBe('Diff HTML');
    expect(document.querySelector('#feature-more-info-DIFF_HTML')!.getAttribute('title')).toBe('/diff/html request');
  });

  it('shows the assigned worker, and a dash for a feature that skips the queue', async () => {
    await renderPage();

    expect(document.querySelector('#current-worker-DIFF_DOCUMENTS')!.textContent).toBe('1');
    expect(document.querySelector('#current-worker-DIFF_TEXT')!.textContent).toBe('-');
  });

  it('does not offer the worker a feature already uses', async () => {
    await renderPage();

    const options = Array.from(document.querySelectorAll<HTMLOptionElement>('#new-worker-DIFF_DOCUMENTS option')).map(
      (option) => option.value,
    );
    expect(options).not.toContain('1');
    expect(options).toEqual(expect.arrayContaining(['0', '2', '3']));
  });

  it('renders a threads row per worker, bounded by the machine maximum', async () => {
    await renderPage();

    expect(document.querySelectorAll('#workers-threads tbody tr')).toHaveLength(META.workerCount);
    expect(document.querySelector('#current-threads-2')!.textContent).toBe('2');
    const input = document.querySelector<HTMLInputElement>('#new-threads-1')!;
    expect(input.max).toBe('4');
    expect(input.title).toContain('Max threads count: 4');
  });

  it('clamps a thread count above the maximum on blur', async () => {
    await renderPage();
    const input = document.querySelector<HTMLInputElement>('#new-threads-1')!;

    setFieldValue(input, '99');
    blur(input);

    await vi.waitFor(() => expect(document.querySelector<HTMLInputElement>('#new-threads-1')!.value).toBe('4'));
  });

  it('clears a non-positive thread count on blur', async () => {
    await renderPage();
    const input = document.querySelector<HTMLInputElement>('#new-threads-1')!;

    setFieldValue(input, '0');
    blur(input);

    await vi.waitFor(() => expect(document.querySelector<HTMLInputElement>('#new-threads-1')!.value).toBe(''));
  });

  it('saves the persisted settings merged with the pending edits', async () => {
    const fetchMock = await renderPage();
    setFieldValue(document.querySelector<HTMLInputElement>('#new-threads-3')!, '3');

    toolbarButton(0).click();

    await vi.waitFor(() => {
      const put = fetchMock.mock.calls.find(([, init]) => init?.method === 'PUT');
      expect(put).toBeDefined();
      const body = JSON.parse(String(put![1]!.body));
      // Untouched entries keep their persisted values; only worker 3's threads changed.
      expect(body.workers).toEqual(SAVED.workers);
      expect(body.threads).toEqual({ '1': 1, '2': 2, '3': 3 });
    });
  });

  it('reports a failed save', async () => {
    const fetchMock = installFetchMock(
      routes([{ method: 'PUT', match: /\/content/, respond: () => jsonResponse({ errorMessage: 'read only' }, 403) }]),
    );
    await renderPage(fetchMock);

    toolbarButton(0).click();

    await vi.waitFor(() => expect(document.body.textContent).toContain('read only'));
  });

  it('loads the default values on Default without persisting them', async () => {
    const fetchMock = await renderPage();

    toolbarButton(2).click();
    await answerConfirm('OK');

    await vi.waitFor(() =>
      expect(fetchMock.mock.calls.some(([url]) => String(url).includes('/default-content'))).toBe(true),
    );
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === 'PUT')).toBe(false);
    // The defaults put everything on the queue-skipping worker, so only the CPU chart is left.
    await vi.waitFor(() => expect(chartTitles()).toEqual(['CPU Load']));
  });

  it('re-reads the settings on Cancel, but only after confirmation', async () => {
    const fetchMock = await renderPage();
    const reads = () =>
      fetchMock.mock.calls.filter(([url, init]) => String(url).includes('/content') && init?.method !== 'PUT').length;
    const before = reads();

    toolbarButton(1).click();
    await answerConfirm('Cancel');
    expect(reads()).toBe(before);

    toolbarButton(1).click();
    await answerConfirm('OK');
    await vi.waitFor(() => expect(reads()).toBeGreaterThan(before));
  });

  it('toggles the revisions table', async () => {
    await renderPage();

    toolbarButton(3).click();
    await vi.waitFor(() => expect(document.body.textContent).toContain('4 711'));
  });

  it('actually plots the polled series onto the canvas', async () => {
    // Stands in for a pixel snapshot of the charts, which cannot be stable: Chart.js labels the x-axis
    // with the data's own timestamps and the 3s poll keeps adding to it. Reading the canvas back proves
    // what the snapshot was really there to prove - that the registration in chartSetup.ts is complete
    // and Chart.js draws (an unregistered controller or scale throws instead, leaving it blank).
    const now = Date.now();
    const series = Array.from({ length: 30 }, (_, index) => ({
      timestamp: `${new Date(now - (30 - index) * 1000).toISOString().slice(0, 23)}Z`,
      queued: index % 4,
      executing: index % 2,
    }));
    await renderPage(
      installFetchMock(
        routes([{ method: 'POST', match: /queueStatistics/, json: { '1': { DIFF_DOCUMENTS: series } } }]),
      ),
    );

    await vi.waitFor(() => {
      const canvas = document.querySelector<HTMLCanvasElement>('#chart-container-1 canvas')!;
      expect(canvas.width).toBeGreaterThan(0);
      const pixels = canvas.getContext('2d')!.getImageData(0, 0, canvas.width, canvas.height).data;
      let painted = 0;
      for (let index = 0; index < pixels.length; index += 4) {
        if (pixels[index + 3] > 0 && pixels[index] < 250) {
          painted += 1;
        }
      }
      expect(painted).toBeGreaterThan(500);
    });
  });

  it('collapses a chart panel, unmounting its canvas', async () => {
    await renderPage();
    await vi.waitFor(() => expect(document.querySelector('#chart-container-1 canvas')).not.toBeNull());

    document.querySelector<HTMLButtonElement>('#chart-expand-button-1')!.click();

    await vi.waitFor(() => expect(document.querySelector('#chart-container-1 canvas')).toBeNull());
    // The other charts are unaffected.
    expect(document.querySelector('#chart-container-2 canvas')).not.toBeNull();
  });

  it('re-requests a chart window when its interval changes', async () => {
    const fetchMock = await renderPage();
    setFieldValue(document.querySelector<HTMLSelectElement>('#select-interval-1')!, '15');

    // The widened window is requested on the next poll, up to one 3s interval away - the legacy page
    // behaved the same way, so the wait is the behaviour rather than a flake.
    await vi.waitFor(
      () => {
        const last = fetchMock.mock.calls.filter(([url]) => String(url).includes('queueStatistics')).at(-1)!;
        const cursor = JSON.parse(String(last[1]!.body)).from['1'];
        expect(Math.round((Date.now() - new Date(cursor).getTime()) / 60_000)).toBe(15);
      },
      { timeout: 6000 },
    );
  });

  it('saves a reassignment made through the worker dropdown', async () => {
    const fetchMock = await renderPage();

    setFieldValue(document.querySelector<HTMLSelectElement>('#new-worker-DIFF_TEXT')!, '3');
    await vi.waitFor(() => expect(document.querySelector<HTMLSelectElement>('#new-worker-DIFF_TEXT')!.value).toBe('3'));
    toolbarButton(0).click();

    await vi.waitFor(() => {
      const put = fetchMock.mock.calls.find(([, init]) => init?.method === 'PUT');
      expect(JSON.parse(String(put![1]!.body)).workers).toEqual({ ...SAVED.workers, DIFF_TEXT: 3 });
    });
  });

  it('drops a pending thread edit when the field is cleared again', async () => {
    const fetchMock = await renderPage();
    const input = document.querySelector<HTMLInputElement>('#new-threads-2')!;

    setFieldValue(input, '4');
    await vi.waitFor(() => expect(document.querySelector<HTMLInputElement>('#new-threads-2')!.value).toBe('4'));
    setFieldValue(document.querySelector<HTMLInputElement>('#new-threads-2')!, '');
    await vi.waitFor(() => expect(document.querySelector<HTMLInputElement>('#new-threads-2')!.value).toBe(''));

    toolbarButton(0).click();

    await vi.waitFor(() => {
      const put = fetchMock.mock.calls.find(([, init]) => init?.method === 'PUT');
      // Back to the persisted value, not the abandoned edit.
      expect(JSON.parse(String(put![1]!.body)).threads).toEqual(SAVED.threads);
    });
  });

  it('resets a chart position without disturbing its data', async () => {
    await renderPage();
    await vi.waitFor(() => expect(document.querySelector('#chart-container-1 canvas')).not.toBeNull());

    document.querySelector<HTMLButtonElement>('#reset-chart-1')!.click();

    // The canvas survives: resetting the view must not tear the chart down and lose the history.
    expect(document.querySelector('#chart-container-1 canvas')).not.toBeNull();
  });

  it('warns when the stored settings came from a different build', async () => {
    installFetchMock(
      routes([
        { method: 'GET', match: /\/names\/Default\/content/, json: { ...SAVED, bundleTimestamp: '2020-01-01 00:00' } },
      ]),
    );
    render(<Page />);

    await vi.waitFor(() => expect(document.querySelector('.alert-warning')).not.toBeNull());
  });

  it('surfaces a failure to load the queue configuration', async () => {
    installFetchMock(
      routes([
        {
          method: 'GET',
          match: /\/queue\/configuration-meta$/,
          respond: () => jsonResponse({ errorMessage: 'nope' }, 500),
        },
      ]),
    );
    render(<Page />);

    await vi.waitFor(() => expect(document.querySelector('.alert-error')).not.toBeNull());
  });

  it('states the real queue capacity rather than a hardcoded number', async () => {
    await renderPage();

    expect(document.querySelector('.quick-help')!.textContent).toContain('1000');
  });
});
