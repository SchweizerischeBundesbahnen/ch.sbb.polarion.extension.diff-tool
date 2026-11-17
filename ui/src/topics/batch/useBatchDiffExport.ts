import { useCallback, useEffect, useRef, useState } from 'react';
import { convertHtmlToPdf, downloadBlob } from '../../services/pdfExport';
import type { PdfOptions } from '../../services/pdfExport';
import { sendRequest } from '../../services/useRemote';
import { uniqueFileNames, viewerUrl } from './batchExport';
import type { BatchSettings, DocumentPair } from './batchExport';
import { abortError, isAbortError, renderDiffInFrame } from './hiddenDiffFrame';
import type { RenderedDiff } from './hiddenDiffFrame';

export type RowState = 'waiting' | 'diffing' | 'converting' | 'finished' | 'error' | 'interrupted';

export interface BatchRow {
  pair: DocumentPair;
  fileName: string;
  state: RowState;
  /** Work items diffed so far, while a work items diff loads. */
  progress?: { loaded: number; total: number };
  error?: string;
}

export interface BatchState {
  status: 'closed' | 'running' | 'finished' | 'interrupted';
  rows: BatchRow[];
  /** Pairs done with, whatever their outcome. */
  processed: number;
  errors: boolean;
}

/** What a run does with a pair, replaceable in a test. */
export interface BatchDependencies {
  renderDiff: (
    url: string,
    pdfOptions: PdfOptions,
    options: { onProgress: (loaded: number, total: number) => void; signal: AbortSignal },
  ) => Promise<RenderedDiff>;
  convert: (html: string, pdfOptions: PdfOptions) => Promise<Blob>;
  download: (blob: Blob, fileName: string) => void;
  viewerUrl: (pair: DocumentPair, settings: BatchSettings) => string;
}

export const DEFAULT_DEPENDENCIES: BatchDependencies = {
  renderDiff: renderDiffInFrame,
  convert: (html, pdfOptions) => convertHtmlToPdf(sendRequest, html, pdfOptions),
  download: downloadBlob,
  viewerUrl: viewerUrl,
};

const CLOSED: BatchState = { status: 'closed', rows: [], processed: 0, errors: false };

const failureOf = (error: unknown) => (error instanceof Error && error.message ? error.message : 'Export failed');

export interface BatchDiffExport {
  state: BatchState;
  start: (pairs: DocumentPair[], settings: BatchSettings) => void;
  stop: () => void;
  close: () => void;
}

/**
 * Runs a batch PDF export of document diffs: one pair at a time, its diff rendered by the viewer in a hidden
 * frame, converted to PDF and downloaded as soon as it is ready. One pair at a time loads the server as a user
 * opening the viewer does. A pair which fails is reported in its row, and the run goes on with the next one.
 * Stop ends the pair in progress and starts no other.
 */
export default function useBatchDiffExport(dependencies: BatchDependencies = DEFAULT_DEPENDENCIES): BatchDiffExport {
  const [state, setState] = useState<BatchState>(CLOSED);
  const run = useRef<AbortController | null>(null);

  const updateRow = useCallback((index: number, update: Partial<BatchRow>) => {
    setState((previous) => ({
      ...previous,
      rows: previous.rows.map((row, rowIndex) => (rowIndex === index ? { ...row, ...update } : row)),
    }));
  }, []);

  const finishRow = useCallback((index: number, update: Partial<BatchRow>, failed: boolean) => {
    setState((previous) => ({
      ...previous,
      processed: previous.processed + 1,
      errors: previous.errors || failed,
      rows: previous.rows.map((row, rowIndex) => (rowIndex === index ? { ...row, ...update } : row)),
    }));
  }, []);

  const exportPair = useCallback(
    async (row: BatchRow, index: number, settings: BatchSettings, signal: AbortSignal) => {
      updateRow(index, { state: 'diffing' });
      const rendered = await dependencies.renderDiff(dependencies.viewerUrl(row.pair, settings), settings.pdf, {
        onProgress: (loaded, total) => updateRow(index, { progress: { loaded: loaded, total: total } }),
        signal: signal,
      });
      updateRow(index, { state: 'converting' });
      const pdf = await dependencies.convert(rendered.html, settings.pdf);
      // A conversion cannot be recalled once the server has it, but a stopped run downloads nothing more
      if (signal.aborted) {
        throw abortError();
      }
      dependencies.download(pdf, row.fileName);
    },
    [dependencies, updateRow],
  );

  const start = useCallback(
    (pairs: DocumentPair[], settings: BatchSettings) => {
      if (run.current || pairs.length === 0) {
        return;
      }
      const controller = new AbortController();
      run.current = controller;
      const fileNames = uniqueFileNames(pairs);
      const rows: BatchRow[] = pairs.map((pair, index) => ({
        pair: pair,
        fileName: fileNames[index],
        state: 'waiting',
      }));
      setState({ status: 'running', rows: rows, processed: 0, errors: false });

      (async () => {
        for (let index = 0; index < rows.length; index++) {
          if (controller.signal.aborted) {
            break;
          }
          try {
            await exportPair(rows[index], index, settings, controller.signal);
            finishRow(index, { state: 'finished' }, false);
          } catch (error) {
            if (isAbortError(error)) {
              updateRow(index, { state: 'interrupted' });
              break;
            }
            finishRow(index, { state: 'error', error: failureOf(error) }, true);
          }
        }
        run.current = null;
        setState((previous) =>
          previous.status === 'running'
            ? { ...previous, status: controller.signal.aborted ? 'interrupted' : 'finished' }
            : previous,
        );
      })();
    },
    [exportPair, finishRow, updateRow],
  );

  const stop = useCallback(() => {
    if (!run.current) {
      return;
    }
    run.current.abort();
    setState((previous) => ({
      ...previous,
      status: 'interrupted',
      rows: previous.rows.map((row) =>
        row.state === 'waiting' || row.state === 'diffing' || row.state === 'converting'
          ? { ...row, state: 'interrupted' }
          : row,
      ),
    }));
  }, []);

  const close = useCallback(() => {
    run.current?.abort();
    run.current = null;
    setState(CLOSED);
  }, []);

  // A page left while a run goes on stops it, so no frame keeps loading behind it
  useEffect(() => () => run.current?.abort(), []);

  // Leaving the page would end the run, so the browser asks first
  useEffect(() => {
    if (state.status !== 'running') {
      return undefined;
    }
    const confirmLeave = (event: BeforeUnloadEvent) => {
      event.preventDefault();
    };
    window.addEventListener('beforeunload', confirmLeave);
    return () => window.removeEventListener('beforeunload', confirmLeave);
  }, [state.status]);

  return { state: state, start: start, stop: stop, close: close };
}
