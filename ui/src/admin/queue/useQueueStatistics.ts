import { useCallback, useEffect, useRef, useState } from 'react';
import { sendRequest } from '../../services/useRemote';

/** Worker key in the statistics response: "1".."N" for a worker, "COMMON" for the CPU load series. */
export const COMMON_WORKER = 'COMMON';
export const CPU_LOAD_CHART = 'CPU_LOAD';

export const POLL_INTERVAL_MS = 3000;

/** ch.sbb.polarion.extension.diff_tool.rest.model.queue.EndpointCallEntry / CpuLoadEntry */
export interface StatisticsEntry {
  timestamp: string;
  queued?: number;
  executing?: number;
  value?: number;
}

/** worker key -> feature id -> the entries sampled for it. */
export type Statistics = Record<string, Record<string, StatisticsEntry[]>>;

interface UseQueueStatisticsOptions {
  /** Chart keys currently on screen: worker numbers plus, optionally, CPU_LOAD. */
  charts: string[];
  /** Chart key -> how many minutes of history to keep. */
  intervals: Record<string, number>;
  /** Paused until the settings have loaded, so the first poll knows which workers exist. */
  enabled: boolean;
  /** Overridable so tests do not have to wait whole seconds for a second poll. */
  pollIntervalMs?: number;
}

interface UseQueueStatisticsResult {
  /** Accumulated, interval-trimmed entries, replaced on every poll. */
  statistics: Statistics;
  error: string | null;
  /**
   * Drop a chart's history and re-request its whole window. The interval is passed explicitly because
   * the caller changes it in the same event: reading it back from props here would see the previous
   * value, and the cursor would be seeded with the old (possibly shorter) window.
   */
  reset: (chart: string, intervalMinutes: number) => void;
  /** Drop everything, e.g. after saving a new worker assignment. */
  resetAll: () => void;
}

/** The server's timestamp format: yyyy-MM-dd'T'HH:mm:ss.SSS'Z' (see TimeframeStatisticsEntry). */
function formatCursor(date: Date): string {
  return `${date.toISOString().slice(0, 23)}Z`;
}

function cursorFor(intervalMinutes: number): string {
  return formatCursor(new Date(Date.now() - intervalMinutes * 60_000));
}

/** The response key a chart's entries live under. */
function responseKey(chart: string): string {
  return chart === CPU_LOAD_CHART ? COMMON_WORKER : chart;
}

/**
 * Polls POST /queueStatistics every 3s, exactly as the legacy page did.
 *
 * Two things make this more than a plain fetch loop:
 *  - Each chart sends its own `from` cursor - the timestamp of the newest entry it already holds - so the
 *    server returns only what is new. The cursor is per chart because each has its own interval.
 *  - The result is accumulated client-side and trimmed to the chart's interval on every poll, since the
 *    server only ever returns the delta.
 */
export default function useQueueStatistics({
  charts,
  intervals,
  enabled,
  pollIntervalMs = POLL_INTERVAL_MS,
}: UseQueueStatisticsOptions): UseQueueStatisticsResult {
  const [statistics, setStatistics] = useState<Statistics>({});
  const [error, setError] = useState<string | null>(null);

  // Kept in refs so changing an interval (or the chart list) does not restart the polling timer.
  const cursors = useRef<Record<string, string>>({});
  const accumulated = useRef<Statistics>({});
  const chartsRef = useRef(charts);
  const intervalsRef = useRef(intervals);
  chartsRef.current = charts;
  intervalsRef.current = intervals;

  const reset = useCallback((chart: string, intervalMinutes: number) => {
    delete accumulated.current[responseKey(chart)];
    cursors.current[chart] = cursorFor(intervalMinutes);
    setStatistics({ ...accumulated.current });
  }, []);

  const resetAll = useCallback(() => {
    accumulated.current = {};
    cursors.current = {};
    setStatistics({});
  }, []);

  useEffect(() => {
    if (!enabled) {
      return;
    }
    let cancelled = false;

    const poll = async () => {
      // Seed a cursor for any chart that does not have one yet, so the first poll asks for its whole
      // window rather than all 30 minutes the server retains.
      for (const chart of chartsRef.current) {
        cursors.current[chart] ??= cursorFor(intervalsRef.current[chart] ?? 1);
      }
      try {
        const response = await sendRequest({
          method: 'POST',
          url: '/queueStatistics',
          contentType: 'application/json',
          body: JSON.stringify({ from: cursors.current }),
        });
        if (cancelled) {
          return;
        }
        if (!response.ok) {
          throw new Error(`Loading queue statistics failed (HTTP ${response.status})`);
        }
        const delta = (await response.json()) as Statistics;
        if (cancelled) {
          return;
        }

        for (const chart of chartsRef.current) {
          const key = responseKey(chart);
          const incoming = delta[key];
          if (!incoming) {
            continue;
          }
          const cutoff = Date.now() - (intervalsRef.current[chart] ?? 1) * 60_000;
          const existing = accumulated.current[key] ?? {};
          const merged: Record<string, StatisticsEntry[]> = { ...existing };
          let newest = '';
          for (const [feature, entries] of Object.entries(incoming)) {
            merged[feature] = [...(existing[feature] ?? []), ...entries].filter(
              (entry) => new Date(entry.timestamp).getTime() >= cutoff,
            );
            const last = entries.at(-1);
            if (last && last.timestamp > newest) {
              newest = last.timestamp;
            }
          }
          accumulated.current[key] = merged;
          // Advance the cursor to the newest entry actually received. Taking the maximum across the
          // worker's features (rather than the last one iterated, as the legacy page did) means a feature
          // that reported nothing this round cannot drag the cursor backwards.
          if (newest) {
            cursors.current[chart] = newest;
          }
        }
        setStatistics({ ...accumulated.current });
        setError(null);
      } catch (caught) {
        if (!cancelled) {
          setError((caught as Error).message);
        }
      }
    };

    void poll();
    const timer = setInterval(() => void poll(), pollIntervalMs);
    return () => {
      cancelled = true;
      clearInterval(timer);
    };
  }, [enabled, pollIntervalMs]);

  return { statistics: statistics, error: error, reset: reset, resetAll: resetAll };
}
