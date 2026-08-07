import { useCallback } from 'react';
import { useRouter, useSearchParams } from '../router/navigation';

export const DEFAULT_RECORDS_PER_PAGE = 20;

export interface TopicParams {
  get: (name: string, fallback?: string) => string;
  getNumber: (name: string, fallback: number) => number;
  /** Writes several parameters at once; `''` and `null` remove one. */
  update: (updates: Record<string, string | number | null>) => void;
}

/**
 * The pickers' view state (query, page, page size, target project, link role, configuration) lives in the
 * page's own query string, under the parameter names the JSP widgets used.
 *
 * The widgets kept the same state in the *top frame's* URL and reloaded the whole frame on every Apply and
 * every page step (DiffToolWidgetUtils.replaceUrlParam + top.location.reload()). Here it is a pushState on
 * this page only, so reload and back/forward still restore the view but nothing else on the screen blinks.
 */
export default function useTopicParams(): TopicParams {
  const searchParams = useSearchParams();
  const router = useRouter();

  const update = useCallback(
    (updates: Record<string, string | number | null>) => {
      const params = new URLSearchParams(window.location.search);
      Object.entries(updates).forEach(([name, value]) => {
        if (value === null || value === '') {
          params.delete(name);
        } else {
          params.set(name, String(value));
        }
      });
      router.push(`${window.location.pathname}?${params.toString()}`);
    },
    [router],
  );

  const get = useCallback((name: string, fallback = '') => searchParams.get(name) ?? fallback, [searchParams]);

  const getNumber = useCallback(
    (name: string, fallback: number) => {
      const parsed = Number.parseInt(searchParams.get(name) ?? '', 10);
      return Number.isNaN(parsed) || parsed < 1 ? fallback : parsed;
    },
    [searchParams],
  );

  return { get: get, getNumber: getNumber, update: update };
}
