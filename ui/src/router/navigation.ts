import { useMemo, useSyncExternalStore } from 'react';

/**
 * Drop-in replacements for the three `next/navigation` hooks this app used.
 *
 * The app does no path routing: the pathname is fixed per HTML entry (documents.html /
 * collections.html / workitems.html) and every navigation is a query-string mutation. A router
 * library would add a matcher and a `basename` for no benefit, and its `useSearchParams()` returns a
 * `[params, setParams]` tuple - a different API that would force a rewrite of all call sites anyway.
 *
 * `push` mirrors Next's App Router soft navigation: history changes, the tree is not remounted, and
 * components subscribed to the search params re-render. Components that read a param once into
 * `useState` keep their value, exactly as before.
 */

const listeners = new Set<() => void>();

// useSyncExternalStore requires a reference-stable snapshot: returning a fresh URLSearchParams on
// every call would re-render forever, and would also thrash the `useMemo(..., [searchParams])`
// dependencies at the call sites. Rebuild only when the query string actually changed.
let cached: { search: string; params: URLSearchParams } = {
  search: '',
  params: new URLSearchParams(''),
};

function getSnapshot(): URLSearchParams {
  if (cached.search !== window.location.search) {
    cached = {
      search: window.location.search,
      params: new URLSearchParams(window.location.search),
    };
  }
  return cached.params;
}

function notify(): void {
  listeners.forEach((listener) => listener());
}

function subscribe(listener: () => void): () => void {
  if (listeners.size === 0) {
    window.addEventListener('popstate', notify);
  }
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
    if (listeners.size === 0) {
      window.removeEventListener('popstate', notify);
    }
  };
}

export interface Router {
  push: (url: string) => void;
  replace: (url: string) => void;
}

// A single stable instance, like Next's useRouter(), so it is safe in dependency arrays.
const router: Router = {
  push(url: string): void {
    window.history.pushState(null, '', url);
    notify();
  },
  replace(url: string): void {
    window.history.replaceState(null, '', url);
    notify();
  },
};

/** Reactive, read-only view of the current query string. */
export function useSearchParams(): URLSearchParams {
  return useSyncExternalStore(subscribe, getSnapshot, getSnapshot);
}

/** Query params which change only the look of the page, never the data it loads. */
const VIEW_ONLY_PARAMS = ['swapColors'];

/**
 * The current query string without view-only params. The data-loading effects depend on it, so
 * toggling a view-only param does not reload the diff. Reference-stable while only those params change.
 */
export function useDataSearchParams(): URLSearchParams {
  const searchParams = useSearchParams();
  const dataParams = new URLSearchParams(searchParams);
  VIEW_ONLY_PARAMS.forEach((param) => dataParams.delete(param));
  const key = dataParams.toString();
  // eslint-disable-next-line react-hooks/exhaustive-deps -- keyed by content, not by the fresh object
  return useMemo(() => dataParams, [key]);
}

/** Fixed for the lifetime of the page, so no subscription is needed. */
export function usePathname(): string {
  return window.location.pathname;
}

export function useRouter(): Router {
  return router;
}
