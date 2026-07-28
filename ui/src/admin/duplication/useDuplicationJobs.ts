import { useCallback, useEffect, useRef, useState } from 'react';
import { sendRequest } from '../../services/useRemote';
import type { DuplicationJobInfo } from '../types';

export const POLL_ACTIVE_MS = 3000;
export const POLL_IDLE_MS = 30000;

/** A job in one of these states will not change again, so nothing needs to be polled for it. */
const TERMINAL_STATES = ['FINISHED', 'ABORTED'];

export function isRunning(job: DuplicationJobInfo): boolean {
  return !TERMINAL_STATES.includes(job.state ?? '');
}

interface UseDuplicationJobsOptions {
  activeIntervalMs?: number;
  idleIntervalMs?: number;
}

interface UseDuplicationJobsResult {
  jobs: DuplicationJobInfo[];
  error: string | null;
  /** How long until the next automatic refresh, so the page can say so. */
  nextRefreshMs: number;
  /** Whether anything is still running, which is what sets the cadence. */
  anyRunning: boolean;
  /** Fetch immediately, e.g. right after scheduling a new job. */
  refresh: () => void;
  /** Increments on every completed poll; used to re-load the open log frames. */
  pollCount: number;
}

/**
 * Polls the duplication jobs, quickly while something is running and slowly when nothing is - the same
 * adaptive cadence the legacy page used, so an idle admin page is not hitting Polarion every 3 seconds.
 *
 * Scheduling is a chained timeout rather than an interval: the next delay is only known once a response
 * has said whether anything is still running, and this also guarantees no overlapping requests.
 */
export default function useDuplicationJobs({
  activeIntervalMs = POLL_ACTIVE_MS,
  idleIntervalMs = POLL_IDLE_MS,
}: UseDuplicationJobsOptions = {}): UseDuplicationJobsResult {
  const [jobs, setJobs] = useState<DuplicationJobInfo[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [nextRefreshMs, setNextRefreshMs] = useState(idleIntervalMs);
  const [pollCount, setPollCount] = useState(0);

  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const cancelled = useRef(false);
  const inFlight = useRef(false);
  const refreshRef = useRef<() => void>(() => {});

  const load = useCallback(async () => {
    // A slow response must not let a second request pile up behind it.
    if (inFlight.current) {
      return;
    }
    inFlight.current = true;
    try {
      const response = await sendRequest({ method: 'GET', url: '/projects/duplicate/jobs' });
      if (cancelled.current) {
        return;
      }
      if (!response.ok) {
        throw new Error(`Failed to load jobs (HTTP ${response.status})`);
      }
      const loaded = (await response.json()) as DuplicationJobInfo[];
      if (cancelled.current) {
        return;
      }
      setJobs(loaded);
      setError(null);
      setPollCount((count) => count + 1);
      scheduleNext(loaded.some(isRunning) ? activeIntervalMs : idleIntervalMs);
    } catch (caught) {
      if (!cancelled.current) {
        setError((caught as Error).message);
        scheduleNext(idleIntervalMs);
      }
    } finally {
      inFlight.current = false;
    }

    function scheduleNext(delay: number) {
      setNextRefreshMs(delay);
      if (timer.current) {
        clearTimeout(timer.current);
      }
      timer.current = setTimeout(() => void refreshRef.current(), delay);
    }
  }, [activeIntervalMs, idleIntervalMs]);

  refreshRef.current = load;

  const refresh = useCallback(() => {
    if (timer.current) {
      clearTimeout(timer.current);
      timer.current = null;
    }
    void load();
  }, [load]);

  useEffect(() => {
    cancelled.current = false;
    void load();
    return () => {
      cancelled.current = true;
      if (timer.current) {
        clearTimeout(timer.current);
        timer.current = null;
      }
    };
  }, [load]);

  return {
    jobs: jobs,
    error: error,
    nextRefreshMs: nextRefreshMs,
    anyRunning: jobs.some(isRunning),
    refresh: refresh,
    pollCount: pollCount,
  };
}
