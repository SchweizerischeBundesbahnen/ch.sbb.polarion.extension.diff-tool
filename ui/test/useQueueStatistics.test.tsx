import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import useQueueStatistics, { type Statistics } from '../src/admin/queue/useQueueStatistics';
import { type FetchMock, installFetchMock, jsonResponse } from './mockFetch';

// The 3s statistics poll. Worth isolating: it owns the per-chart `from` cursors and the client-side
// trimming, which is the only place the server's delta-only responses are turned into a window of history.

interface Sent {
  from: Record<string, string>;
}

function Probe({
  charts,
  intervals,
  pollIntervalMs,
}: {
  charts: string[];
  intervals: Record<string, number>;
  /** Kept short in the tests that need to observe a second poll. */
  pollIntervalMs?: number;
}) {
  const { statistics, error } = useQueueStatistics({
    charts: charts,
    intervals: intervals,
    enabled: true,
    pollIntervalMs: pollIntervalMs,
  });
  return (
    <>
      <output data-testid="stats">{JSON.stringify(statistics)}</output>
      <output data-testid="error">{error ?? ''}</output>
    </>
  );
}

const stats = (): Statistics => JSON.parse(document.querySelector('[data-testid="stats"]')!.textContent!);
const errorText = () => document.querySelector('[data-testid="error"]')!.textContent;

const bodyOf = (fetchMock: FetchMock, call: number): Sent => JSON.parse(String(fetchMock.mock.calls[call][1]!.body));

/** An entry `secondsAgo` before now, in the server's format. */
function entry(secondsAgo: number, extra: Record<string, number>) {
  return { timestamp: `${new Date(Date.now() - secondsAgo * 1000).toISOString().slice(0, 23)}Z`, ...extra };
}

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

describe('useQueueStatistics', () => {
  it('asks only for the configured window on the first poll', async () => {
    const fetchMock = installFetchMock([{ method: 'POST', match: /queueStatistics/, json: {} }]);
    render(<Probe charts={['1', 'CPU_LOAD']} intervals={{ '1': 5, CPU_LOAD: 1 }} />);

    await vi.waitFor(() => expect(fetchMock).toHaveBeenCalled());
    const { from } = bodyOf(fetchMock, 0);
    // A cursor per chart, each reaching back exactly its own interval - not the 30 minutes the server
    // retains.
    const minutesBack = (cursor: string) => Math.round((Date.now() - new Date(cursor).getTime()) / 60_000);
    expect(minutesBack(from['1'])).toBe(5);
    expect(minutesBack(from.CPU_LOAD)).toBe(1);
  });

  it('exposes the returned entries under their worker and feature', async () => {
    installFetchMock([
      {
        method: 'POST',
        match: /queueStatistics/,
        json: { '1': { DIFF_HTML: [entry(1, { queued: 2, executing: 1 })] } },
      },
    ]);
    render(<Probe charts={['1']} intervals={{ '1': 1 }} />);

    await vi.waitFor(() => expect(stats()['1']?.DIFF_HTML).toHaveLength(1));
    expect(stats()['1'].DIFF_HTML[0]).toMatchObject({ queued: 2, executing: 1 });
  });

  it('maps the CPU_LOAD chart onto the COMMON response key', async () => {
    const fetchMock = installFetchMock([
      { method: 'POST', match: /queueStatistics/, json: { COMMON: { CPU_LOAD: [entry(1, { value: 0.42 })] } } },
    ]);
    render(<Probe charts={['CPU_LOAD']} intervals={{ CPU_LOAD: 1 }} />);

    await vi.waitFor(() => expect(stats().COMMON?.CPU_LOAD).toHaveLength(1));
    // The cursor is still keyed by the chart name, which is what the server echoes back per worker.
    expect(bodyOf(fetchMock, 0).from).toHaveProperty('CPU_LOAD');
  });

  it('advances the cursor to the newest entry received, so nothing is re-fetched', async () => {
    const newest = entry(1, { queued: 1, executing: 0 });
    const fetchMock = installFetchMock([
      {
        method: 'POST',
        match: /queueStatistics/,
        json: { '1': { DIFF_HTML: [entry(3, { queued: 0, executing: 0 }), newest] } },
      },
    ]);
    render(<Probe charts={['1']} intervals={{ '1': 30 }} pollIntervalMs={30} />);

    await vi.waitFor(() => expect(fetchMock.mock.calls.length).toBeGreaterThan(1));
    expect(bodyOf(fetchMock, 1).from['1']).toBe(newest.timestamp);
  });

  it('takes the newest timestamp across the features of a worker', async () => {
    // A feature that reported nothing this round must not drag the cursor back, which is what taking the
    // last-iterated feature's timestamp (as the legacy page did) could do.
    const older = entry(5, { queued: 0, executing: 0 });
    const newer = entry(1, { queued: 1, executing: 1 });
    const fetchMock = installFetchMock([
      { method: 'POST', match: /queueStatistics/, json: { '1': { DIFF_HTML: [newer], DIFF_TEXT: [older] } } },
    ]);
    render(<Probe charts={['1']} intervals={{ '1': 30 }} pollIntervalMs={30} />);

    await vi.waitFor(() => expect(fetchMock.mock.calls.length).toBeGreaterThan(1));
    expect(bodyOf(fetchMock, 1).from['1']).toBe(newer.timestamp);
  });

  it('drops entries that have fallen outside the chart interval', async () => {
    installFetchMock([
      {
        method: 'POST',
        match: /queueStatistics/,
        // 120s old against a 1 minute window, plus one inside it.
        json: { '1': { DIFF_HTML: [entry(120, { queued: 9, executing: 9 }), entry(1, { queued: 1, executing: 1 })] } },
      },
    ]);
    render(<Probe charts={['1']} intervals={{ '1': 1 }} />);

    await vi.waitFor(() => expect(stats()['1']?.DIFF_HTML).toHaveLength(1));
    expect(stats()['1'].DIFF_HTML[0]).toMatchObject({ queued: 1 });
  });

  it('accumulates across polls rather than replacing', async () => {
    let call = 0;
    installFetchMock([
      {
        method: 'POST',
        match: /queueStatistics/,
        respond: () => {
          call += 1;
          return jsonResponse({ '1': { DIFF_HTML: [entry(1, { queued: call, executing: 0 })] } });
        },
      },
    ]);
    render(<Probe charts={['1']} intervals={{ '1': 30 }} pollIntervalMs={30} />);

    await vi.waitFor(() => expect(stats()['1']?.DIFF_HTML.length).toBeGreaterThan(1));
  });

  it('surfaces a failed poll', async () => {
    installFetchMock([
      { method: 'POST', match: /queueStatistics/, respond: () => jsonResponse({ errorMessage: 'nope' }, 500) },
    ]);
    render(<Probe charts={['1']} intervals={{ '1': 1 }} />);

    await vi.waitFor(() => expect(errorText()).toContain('HTTP 500'));
  });

  it('backs off while the statistics endpoint keeps failing', async () => {
    // Without a backoff a service that is down is still asked every 3s for as long as the tab is open.
    // At a 20ms cadence an unthrottled loop would fire ~13 times in 260ms; backing off (20, 20, 40, 80,
    // 160) fires about 5. The bounds are deliberately wide apart so this is not a timing race.
    const fetchMock = installFetchMock([
      { method: 'POST', match: /\/queueStatistics$/, respond: () => jsonResponse({}, 503) },
    ]);
    render(<Probe charts={['1']} intervals={{ '1': 30 }} pollIntervalMs={20} />);
    await vi.waitFor(() => expect(errorText()).toContain('failed'));

    await new Promise((resolve) => setTimeout(resolve, 260));

    expect(fetchMock.mock.calls.length).toBeLessThan(9);
  });

  // Guards the backoff *reset* rather than the backoff itself: if a success failed to clear the failure
  // count, polling would stay at the capped delay for as long as the page was open.
  it('returns to the normal cadence once a poll succeeds', async () => {
    let fail = true;
    const fetchMock = installFetchMock([
      {
        method: 'POST',
        match: /\/queueStatistics$/,
        respond: () => (fail ? jsonResponse({}, 503) : jsonResponse({ '1': { DIFF: [entry(1, { queued: 1 })] } })),
      },
    ]);
    render(<Probe charts={['1']} intervals={{ '1': 30 }} pollIntervalMs={20} />);
    await vi.waitFor(() => expect(errorText()).toContain('failed'));
    // Let it back off a few steps, then let the service recover.
    await new Promise((resolve) => setTimeout(resolve, 200));
    fail = false;
    await vi.waitFor(() => expect(errorText()).toBe(''));

    const afterRecovery = fetchMock.mock.calls.length;
    await new Promise((resolve) => setTimeout(resolve, 120));

    // Back at the short cadence, so several more polls landed rather than one.
    expect(fetchMock.mock.calls.length - afterRecovery).toBeGreaterThan(2);
  });

  it('never has two polls in flight at once', async () => {
    // The old setInterval fired regardless of whether the previous request had returned, so a slow
    // endpoint left several overlapping - each advancing the shared cursors as it came back.
    let inFlight = 0;
    let maxInFlight = 0;
    let release: (() => void) | undefined;
    installFetchMock([
      {
        method: 'POST',
        match: /\/queueStatistics$/,
        respond: () => {
          inFlight += 1;
          maxInFlight = Math.max(maxInFlight, inFlight);
          return jsonResponse({});
        },
      },
    ]);
    // Hold the first response open for many cadences.
    const held = new Promise<void>((resolve) => {
      release = resolve;
    });
    const realFetch = globalThis.fetch;
    vi.stubGlobal('fetch', async (...args: Parameters<typeof fetch>) => {
      const response = await realFetch(...args);
      await held;
      inFlight -= 1;
      return response;
    });

    render(<Probe charts={['1']} intervals={{ '1': 30 }} pollIntervalMs={10} />);
    await new Promise((resolve) => setTimeout(resolve, 120));

    expect(maxInFlight).toBe(1);
    release?.();
  });

  it('does not poll while disabled', async () => {
    const fetchMock = installFetchMock([{ method: 'POST', match: /queueStatistics/, json: {} }]);

    function Disabled() {
      useQueueStatistics({ charts: ['1'], intervals: { '1': 1 }, enabled: false });
      return <span data-testid="ready">ready</span>;
    }
    render(<Disabled />);

    await vi.waitFor(() => expect(document.querySelector('[data-testid="ready"]')).not.toBeNull());
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('ignores a worker the response does not mention', async () => {
    installFetchMock([{ method: 'POST', match: /queueStatistics/, json: { '1': { DIFF_HTML: [entry(1, {})] } } }]);
    render(<Probe charts={['1', '2']} intervals={{ '1': 1, '2': 1 }} />);

    await vi.waitFor(() => expect(stats()['1']).toBeDefined());
    expect(stats()['2']).toBeUndefined();
  });
});
