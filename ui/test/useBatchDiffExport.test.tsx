import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, renderHook } from 'vitest-browser-react';
import { DEFAULT_EXPORT_OPTIONS } from '../src/topics/batch/ExportOptionsPanel';
import type { BatchSettings, DocumentPair } from '../src/topics/batch/batchExport';
import { abortError } from '../src/topics/batch/hiddenDiffFrame';
import useBatchDiffExport from '../src/topics/batch/useBatchDiffExport';
import type { BatchDependencies } from '../src/topics/batch/useBatchDiffExport';

// The runner of a batch PDF export, with its frame, conversion and download replaced: the order it works in,
// what each row reports, and how stopping and failures end a pair or the run.

const pair = (name: string): DocumentPair => ({
  source: { projectId: 'elibrary', spaceId: 'Specification', name: name },
  target: { projectId: 'drivepilot', spaceId: 'Design', name: name },
  sourceRevision: '',
  targetRevision: '',
});

const SETTINGS: BatchSettings = { ...DEFAULT_EXPORT_OPTIONS, linkRole: 'relates_to', config: 'Default' };

const PDF = new Blob(['%PDF']);

/** A promise settled from outside, to hold a step of the run where a test wants it. */
function deferred<T>() {
  let resolve!: (value: T) => void;
  let reject!: (reason: unknown) => void;
  const promise = new Promise<T>((res, rej) => {
    resolve = res;
    reject = rej;
  });
  return { promise: promise, resolve: resolve, reject: reject };
}

function dependencies(overrides: Partial<BatchDependencies> = {}): BatchDependencies {
  return {
    renderDiff: vi.fn<BatchDependencies['renderDiff']>().mockResolvedValue({ html: '<html></html>', diffsExist: true }),
    convert: vi.fn<BatchDependencies['convert']>().mockResolvedValue(PDF),
    download: vi.fn<BatchDependencies['download']>(),
    viewerUrl: vi.fn<BatchDependencies['viewerUrl']>((documentPair) => `viewer?doc=${documentPair.source.name}`),
    ...overrides,
  };
}

async function renderRunner(deps: BatchDependencies) {
  const { result } = await renderHook(() => useBatchDiffExport(deps));
  return result;
}

afterEach(async () => {
  await cleanup();
  vi.restoreAllMocks();
});

describe('useBatchDiffExport', () => {
  it('exports the pairs one after another and downloads each PDF', async () => {
    const deps = dependencies();
    const result = await renderRunner(deps);

    result.current.start([pair('A'), pair('B')], SETTINGS);

    await vi.waitFor(() => expect(result.current.state.status).toBe('finished'));
    expect(result.current.state.rows.map((row) => row.state)).toEqual(['finished', 'finished']);
    expect(result.current.state.processed).toBe(2);
    expect(result.current.state.errors).toBe(false);
    expect(deps.renderDiff).toHaveBeenNthCalledWith(1, 'viewer?doc=A', SETTINGS.pdf, expect.anything());
    expect(deps.convert).toHaveBeenCalledWith('<html></html>', SETTINGS.pdf);
    expect(deps.download).toHaveBeenNthCalledWith(1, PDF, 'Specification_A__drivepilot_Design_A.pdf');
    expect(deps.download).toHaveBeenNthCalledWith(2, PDF, 'Specification_B__drivepilot_Design_B.pdf');
  });

  it('starts a pair only once the previous one is done', async () => {
    const first = deferred<{ html: string; diffsExist: boolean }>();
    const renderDiff = vi
      .fn<BatchDependencies['renderDiff']>()
      .mockReturnValueOnce(first.promise)
      .mockResolvedValue({ html: '<html></html>', diffsExist: true });
    const deps = dependencies({ renderDiff: renderDiff });
    const result = await renderRunner(deps);

    result.current.start([pair('A'), pair('B')], SETTINGS);

    await vi.waitFor(() => expect(result.current.state.rows[0].state).toBe('diffing'));
    expect(result.current.state.rows[1].state).toBe('waiting');
    expect(renderDiff).toHaveBeenCalledTimes(1);

    first.resolve({ html: '<html></html>', diffsExist: true });
    await vi.waitFor(() => expect(result.current.state.status).toBe('finished'));
    expect(renderDiff).toHaveBeenCalledTimes(2);
  });

  it('shows the progress of the diff of a pair', async () => {
    const first = deferred<{ html: string; diffsExist: boolean }>();
    const renderDiff = vi.fn<BatchDependencies['renderDiff']>((_url, _pdf, options) => {
      options.onProgress(3, 10);
      return first.promise;
    });
    const result = await renderRunner(dependencies({ renderDiff: renderDiff }));

    result.current.start([pair('A')], SETTINGS);

    await vi.waitFor(() => expect(result.current.state.rows[0].progress).toEqual({ loaded: 3, total: 10 }));
    first.resolve({ html: '<html></html>', diffsExist: true });
    await vi.waitFor(() => expect(result.current.state.status).toBe('finished'));
  });

  it('reports a failed pair in its row and goes on with the next one', async () => {
    const convert = vi
      .fn<BatchDependencies['convert']>()
      .mockRejectedValueOnce(new Error('No ch.sbb.polarion.extension.pdf-exporter extension found.'))
      .mockResolvedValue(PDF);
    const deps = dependencies({ convert: convert });
    const result = await renderRunner(deps);

    result.current.start([pair('A'), pair('B')], SETTINGS);

    await vi.waitFor(() => expect(result.current.state.status).toBe('finished'));
    expect(result.current.state.rows[0]).toMatchObject({
      state: 'error',
      error: 'No ch.sbb.polarion.extension.pdf-exporter extension found.',
    });
    expect(result.current.state.rows[1].state).toBe('finished');
    expect(result.current.state.errors).toBe(true);
    expect(deps.download).toHaveBeenCalledTimes(1);
  });

  it('exports a pair without differences too', async () => {
    const renderDiff = vi
      .fn<BatchDependencies['renderDiff']>()
      .mockResolvedValue({ html: '<html></html>', diffsExist: false });
    const deps = dependencies({ renderDiff: renderDiff });
    const result = await renderRunner(deps);

    result.current.start([pair('A')], SETTINGS);

    await vi.waitFor(() => expect(result.current.state.status).toBe('finished'));
    expect(result.current.state.rows[0].state).toBe('finished');
    expect(deps.download).toHaveBeenCalledTimes(1);
  });

  it('stops the pair in progress and starts no other', async () => {
    const renderDiff = vi.fn<BatchDependencies['renderDiff']>(
      (_url, _pdf, options) =>
        new Promise((_resolve, reject) => options.signal.addEventListener('abort', () => reject(abortError()))),
    );
    const deps = dependencies({ renderDiff: renderDiff });
    const result = await renderRunner(deps);

    result.current.start([pair('A'), pair('B')], SETTINGS);
    await vi.waitFor(() => expect(result.current.state.rows[0].state).toBe('diffing'));

    result.current.stop();

    await vi.waitFor(() => expect(result.current.state.status).toBe('interrupted'));
    expect(result.current.state.rows.map((row) => row.state)).toEqual(['interrupted', 'interrupted']);
    expect(renderDiff).toHaveBeenCalledTimes(1);
    expect(deps.download).not.toHaveBeenCalled();
  });

  it('downloads nothing of a conversion which ends after the run was stopped', async () => {
    const conversion = deferred<Blob>();
    const deps = dependencies({ convert: vi.fn<BatchDependencies['convert']>().mockReturnValue(conversion.promise) });
    const result = await renderRunner(deps);

    result.current.start([pair('A')], SETTINGS);
    await vi.waitFor(() => expect(result.current.state.rows[0].state).toBe('converting'));
    result.current.stop();
    conversion.resolve(PDF);

    await vi.waitFor(() => expect(result.current.state.rows[0].state).toBe('interrupted'));
    await new Promise((resolve) => setTimeout(resolve, 20));
    expect(deps.download).not.toHaveBeenCalled();
  });

  it('closes the dialog and forgets the run', async () => {
    const result = await renderRunner(dependencies());

    result.current.start([pair('A')], SETTINGS);
    await vi.waitFor(() => expect(result.current.state.status).toBe('finished'));
    result.current.close();

    await vi.waitFor(() =>
      expect(result.current.state).toEqual({ status: 'closed', rows: [], processed: 0, errors: false }),
    );
  });

  it('starts nothing without pairs', async () => {
    const deps = dependencies();
    const result = await renderRunner(deps);

    result.current.start([], SETTINGS);

    expect(result.current.state.status).toBe('closed');
    expect(deps.renderDiff).not.toHaveBeenCalled();
  });
});
