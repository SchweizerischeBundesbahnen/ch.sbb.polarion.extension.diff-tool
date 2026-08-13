import { useEffect, useState } from 'react';
import { sendRequest } from '../services/useRemote';
import type { SearchResult } from './types';

export interface ItemsSearch<T> {
  result: SearchResult<T> | null;
  loading: boolean;
  /** Message of a failed search, e.g. what Polarion says about a malformed Lucene query. */
  error: string | null;
}

/**
 * Runs one page of a picker search. `url: null` means "nothing to search yet" - which is how a topic opened
 * outside a project scope stays empty instead of querying the whole repository.
 *
 * Same cancellation guard as formext/useRemoteList: the effect cleanup flips `cancelled`, so typing a second
 * query before the first answers cannot leave the earlier page on screen.
 */
export default function useItemsSearch<T>(url: string | null): ItemsSearch<T> {
  const [result, setResult] = useState<SearchResult<T> | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!url) {
      setResult(null);
      setLoading(false);
      setError(null);
      return;
    }

    let cancelled = false;
    setLoading(true);
    setError(null);

    (async () => {
      try {
        const response = await sendRequest({ method: 'GET', url: url, contentType: 'application/json' });
        if (!response.ok) {
          const body = (await response.json().catch(() => null)) as { errorMessage?: string; message?: string } | null;
          throw new Error(body?.errorMessage || body?.message || `Search failed (HTTP ${response.status})`);
        }
        const loaded = (await response.json()) as SearchResult<T>;
        if (!cancelled) {
          setResult(loaded);
          setLoading(false);
        }
      } catch (searchError) {
        if (!cancelled) {
          setResult(null);
          setLoading(false);
          setError(searchError instanceof Error ? searchError.message : 'Search failed');
        }
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [url]);

  return { result: result, loading: loading, error: error };
}

/** Builds a search URL, leaving out the parameters the backend defaults anyway. */
export function searchUrl(
  projectId: string,
  what: 'workitems' | 'collections',
  query: string,
  page: number,
  recordsPerPage: number,
): string {
  const params = new URLSearchParams();
  if (query) {
    params.set('query', query);
  }
  params.set('page', String(page));
  params.set('recordsPerPage', String(recordsPerPage));
  return `/projects/${encodeURIComponent(projectId)}/${what}/search?${params.toString()}`;
}
