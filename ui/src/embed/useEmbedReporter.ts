import { useContext, useEffect, useRef } from 'react';
import AppContext from '@/components/AppContext';
import { isPdfExportEmbed } from '@/embed/exportMode';
import { errorMessage, postToParent, progressMessage, readyMessage } from '@/embed/messages';

/**
 * How long the rendered diff keeps changing once its data is loaded: `DiffContent` takes its height after 1 s,
 * the progress bar hides after 1 s, and the app shell applies "hide chapters" after 200 ms. The page is
 * reported ready only after this, so it is exported as the user would see it.
 */
export const SETTLE_DELAY_MS = 1500;

/** The part of the loading context of a diff which tells how its loading goes. */
export interface ReportedLoading {
  pairsLoading?: boolean;
  pairsCount?: number;
  loadedDiffs?: number;
  pairsLoadingError?: string | null;
  diffsLoadingErrors?: boolean;
  fieldsDiffLoadingError?: string | null;
  contentDiffLoadingError?: string | null;
}

const DIFFS_LOADING_FAILED = 'Failed to load the diff of some work items.';

/** The error of the loading, if it failed. */
export function loadingError(loading: ReportedLoading): string | null {
  return (
    loading.pairsLoadingError ||
    loading.fieldsDiffLoadingError ||
    loading.contentDiffLoadingError ||
    (loading.diffsLoadingErrors ? DIFFS_LOADING_FAILED : null)
  );
}

interface AppState {
  dataLoaded: boolean;
  diffsExist: boolean;
}

/**
 * Reports the loading of a diff to the page which embeds the viewer for a PDF export: the progress of a work
 * items diff, an error, and when the diff is rendered completely. Does nothing in the ordinary viewer.
 */
export default function useEmbedReporter(loading: ReportedLoading, embedded: boolean = isPdfExportEmbed()): void {
  const state = (useContext(AppContext) as unknown as { state: AppState }).state;
  const error = loadingError(loading);
  const reported = useRef<'ready' | 'error' | null>(null);
  const diffsExist = useRef(state.diffsExist);
  diffsExist.current = state.diffsExist;

  const loaded = loading.loadedDiffs ?? 0;
  const total = loading.pairsCount ?? 0;
  const pairsKnown = loading.pairsLoading === false;

  useEffect(() => {
    if (embedded && pairsKnown && !reported.current) {
      postToParent(progressMessage(loaded, total));
    }
  }, [embedded, pairsKnown, loaded, total]);

  useEffect(() => {
    if (embedded && error && !reported.current) {
      reported.current = 'error';
      postToParent(errorMessage(error));
    }
  }, [embedded, error]);

  useEffect(() => {
    if (!embedded || !state.dataLoaded || reported.current) {
      return undefined;
    }
    const timer = setTimeout(() => {
      reported.current = 'ready';
      postToParent(readyMessage(diffsExist.current));
    }, SETTLE_DELAY_MS);
    return () => clearTimeout(timer);
  }, [embedded, state.dataLoaded]);
}
