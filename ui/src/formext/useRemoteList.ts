import { useEffect, useState } from 'react';
import { sendRequest } from '../services/useRemote';

export interface RemoteList<T> {
  items: T[];
  /** The progress message while the request is in flight, otherwise `null`. */
  loading: string | null;
  error: string | null;
}

interface RemoteListOptions {
  /** REST path relative to the extension's `/internal` (or `/api`) base; `null` disables the load. */
  url: string | null;
  /** Shown in the panel's progress overlay while loading, e.g. `"Loading spaces"`. */
  progressMessage: string;
  /** Shown in the panel's error alert if the request fails. */
  errorMessage: string;
}

/**
 * Loads one of the panel's dependent lists (spaces, documents, revisions, configuration names).
 *
 * Replaces the legacy `GenericMixin.callAsync` + manual `<option>` building: the list is state, and the
 * three chained selects are just three of these keyed on the selection above them. `url: null` clears
 * the list without a request, which is how "no project picked yet" is expressed.
 *
 * A stale response is discarded (the effect's cleanup flips `cancelled`), so quickly switching project
 * twice cannot leave the documents of the first one on screen.
 */
export default function useRemoteList<T>({ url, progressMessage, errorMessage }: RemoteListOptions): RemoteList<T> {
  const [items, setItems] = useState<T[]>([]);
  const [loading, setLoading] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!url) {
      setItems([]);
      setLoading(null);
      setError(null);
      return;
    }

    let cancelled = false;
    setLoading(progressMessage);
    setError(null);

    (async () => {
      try {
        const response = await sendRequest({ method: 'GET', url: url, contentType: 'application/json' });
        if (!response.ok) {
          throw new Error(`HTTP ${response.status}`);
        }
        const loaded = (await response.json()) as T[];
        if (!cancelled) {
          setItems(loaded);
          setLoading(null);
        }
      } catch {
        if (!cancelled) {
          setItems([]);
          setLoading(null);
          setError(errorMessage);
        }
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [url, progressMessage, errorMessage]);

  return { items: items, loading: loading, error: error };
}

/** The first progress message among several lists, so the panel shows one overlay at a time. */
export function firstLoading(...lists: { loading: string | null }[]): string | null {
  return lists.find((list) => list.loading !== null)?.loading ?? null;
}

/** The first error among several lists. */
export function firstError(...lists: { error: string | null }[]): string | null {
  return lists.find((list) => list.error !== null)?.error ?? null;
}
