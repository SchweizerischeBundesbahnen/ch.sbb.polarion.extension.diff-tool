import { flushSync } from 'react-dom';
import { createRoot } from 'react-dom/client';
import { afterEach, describe, expect, it, vi } from 'vitest';
import useDiffService from '../src/services/useDiffService';
import { installFetchMock } from './mockFetch';

// The viewer's *reading* half of the two localStorage handoffs, both written by code outside the viewer:
//
//   ?ids=<sha1>              -> localStorage["<sha1>_ids"], written by the work items picker topic
//                               (src/topics/openWorkItemsDiff.ts, whose own half is pinned by
//                               openItemsDiff.test.ts; it was webapp/diff-tool/js/diff-tool-widget-utils.js
//                               while that table was rendered by Java).
//   ?additionalParams=<uuid> -> localStorage["<uuid>_additionalParams"], written by the Document
//                               Properties panel (src/formext/openDocumentsDiff.ts).
//
// Nothing else pins these key names and shapes together across the two sides, so a rename on either half
// would surface only as "the comparison opens empty". These tests fail loudly instead.

const disposers: (() => void)[] = [];

/** Renders a throwaway component purely to get at the hook outside a component tree. */
function renderProbedService(): ReturnType<typeof useDiffService> {
  let service: ReturnType<typeof useDiffService> | null = null;
  function Probe() {
    service = useDiffService();
    return null;
  }
  const container = document.createElement('div');
  document.body.appendChild(container);
  const root = createRoot(container);
  // flushSync so the hook has run by the time this returns.
  flushSync(() => root.render(<Probe />));
  disposers.push(() => {
    root.unmount();
    container.remove();
  });
  return service!;
}

interface Pair {
  leftWorkItem?: { id: string };
}

/** Collects what the viewer was told to render, which is where the filtered pairs are delivered. */
function capturingLoadingContext() {
  const rendered: Pair[][] = [];
  return {
    rendered: rendered,
    context: {
      pairsLoadingStarted: () => {},
      pairsLoadingFinished: (pairs: Pair[]) => rendered.push(pairs),
      pairsLoadingFinishedWithError: () => {},
    },
  };
}

afterEach(() => {
  disposers.splice(0).forEach((dispose) => dispose());
  localStorage.clear();
  vi.unstubAllGlobals();
});

describe('work item ids handed over by the table widget', () => {
  it('reads the selected ids out of localStorage under the "<hash>_ids" key', async () => {
    // Exactly what DiffToolWidgetUtils.openWorkItemsDiffApplication() writes.
    localStorage.setItem('abc123_ids', 'EL-1,EL-2,EL-3');
    const fetchMock = installFetchMock([
      { method: 'POST', match: /\/diff\/workitems-pairs$/, json: { pairedWorkItems: [] } },
    ]);
    const service = renderProbedService();

    await service.sendFindWorkItemsPairsRequest(
      new URLSearchParams('sourceProjectId=a&targetProjectId=b&linkRole=relates_to&ids=abc123'),
      capturingLoadingContext().context,
    );

    expect(JSON.parse(String(fetchMock.mock.calls[0][1]!.body)).leftWorkItemIds).toEqual(['EL-1', 'EL-2', 'EL-3']);
  });

  it('asks for nothing when the entry is missing, rather than sending the hash as an id', async () => {
    const fetchMock = installFetchMock([
      { method: 'POST', match: /\/diff\/workitems-pairs$/, json: { pairedWorkItems: [] } },
    ]);
    const service = renderProbedService();

    await service.sendFindWorkItemsPairsRequest(new URLSearchParams('ids=gone'), capturingLoadingContext().context);

    expect(JSON.parse(String(fetchMock.mock.calls[0][1]!.body)).leftWorkItemIds).toEqual([]);
  });
});

describe('work items filter handed over by the Document Properties panel', () => {
  const PAIRS: Pair[] = [
    { leftWorkItem: { id: 'EL-1' } },
    { leftWorkItem: { id: 'EL-2' } },
    { leftWorkItem: { id: 'EL-3' } },
  ];

  /** Runs a documents diff with the given entry stored, and returns the ids the viewer got to render. */
  async function renderedIds(entry: string): Promise<string[]> {
    localStorage.setItem('uuid-1_additionalParams', entry);
    installFetchMock([{ method: 'POST', match: /\/diff\/documents$/, json: { pairedWorkItems: PAIRS } }]);
    const service = renderProbedService();
    const loading = capturingLoadingContext();

    await service.sendDocumentsDiffRequest(
      new URLSearchParams('additionalParams=uuid-1'),
      undefined,
      loading.context,
      false,
    );

    return loading.rendered[0].map((pair) => pair.leftWorkItem!.id);
  }

  it('keeps only the listed work items for an "include" filter', async () => {
    // The exact shape openDocumentsDiff.ts writes.
    const entry = JSON.stringify({
      individualFieldsSelection: true,
      ts: 1,
      filter: { value: 'EL-1,EL-3', type: 'include' },
    });

    expect(await renderedIds(entry)).toEqual(['EL-1', 'EL-3']);
  });

  it('drops the listed work items for an "exclude" filter', async () => {
    const entry = JSON.stringify({
      individualFieldsSelection: true,
      ts: 1,
      filter: { value: 'EL-2', type: 'exclude' },
    });

    expect(await renderedIds(entry)).toEqual(['EL-1', 'EL-3']);
  });

  it('accepts a space separated list, which is the other form the panel allows', async () => {
    const entry = JSON.stringify({ ts: 1, filter: { value: 'EL-1 EL-2', type: 'include' } });

    expect(await renderedIds(entry)).toEqual(['EL-1', 'EL-2']);
  });

  it('leaves every pair alone when the entry carries no filter', async () => {
    expect(await renderedIds(JSON.stringify({ individualFieldsSelection: true, ts: 1 }))).toEqual([
      'EL-1',
      'EL-2',
      'EL-3',
    ]);
  });

  it('leaves every pair alone when the entry is not readable', async () => {
    expect(await renderedIds('not json')).toEqual(['EL-1', 'EL-2', 'EL-3']);
  });
});
