import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, renderHook } from 'vitest-browser-react';
import useResolvedTargets from '../src/topics/batch/useResolvedTargets';
import { jsonResponse } from './mockFetch';

// The lookup of target documents for the batch export: whether each source document has a counterpart of the same
// space and name in the target project. The responses are held back here, so a test decides when they arrive.

interface Pending {
  url: string;
  asked: { spaceId: string; name: string }[];
  answer: () => void;
}

const DESIGN = { spaceId: 'Design', name: 'Catalog' };
const SPECIFICATION = { spaceId: 'Specification', name: 'Catalog' };

/** Holds every lookup until the test answers it. A document of the Design space is found, any other is not. */
function holdLookups(): Pending[] {
  const pending: Pending[] = [];
  vi.stubGlobal(
    'fetch',
    vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
      const asked = JSON.parse((init?.body as string) ?? '[]') as { spaceId: string; name: string }[];
      return new Promise<Response>((resolve) => {
        const answer = () =>
          resolve(
            jsonResponse(
              asked.map((document) => ({
                ...document,
                found: document.spaceId === 'Design',
                spaceTitle: document.spaceId,
                title: document.name,
              })),
            ),
          );
        pending.push({ url: input.toString(), asked: asked, answer: answer });
      });
    }),
  );
  return pending;
}

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

describe('useResolvedTargets', () => {
  it('keeps a lookup which is still pending when the table renders again', async () => {
    const pending = holdLookups();
    const { result, rerender } = await renderHook(
      (props?: { documents: { spaceId: string; name: string }[] }) =>
        useResolvedTargets('drivepilot', props?.documents ?? []),
      { initialProps: { documents: [DESIGN] } },
    );
    await vi.waitFor(() => expect(pending).toHaveLength(1));

    // A row is selected while the lookup runs: the same document, in a new array
    await rerender({ documents: [{ ...DESIGN }] });
    pending[0].answer();

    await vi.waitFor(() => expect(result.current.resolved.get('Design/Catalog')?.found).toBe(true));
    expect(pending).toHaveLength(1);
  });

  it('asks only for the documents it does not know yet', async () => {
    const pending = holdLookups();
    const { result, rerender } = await renderHook(
      (props?: { documents: { spaceId: string; name: string }[] }) =>
        useResolvedTargets('drivepilot', props?.documents ?? []),
      { initialProps: { documents: [DESIGN] } },
    );
    await vi.waitFor(() => expect(pending).toHaveLength(1));

    await rerender({ documents: [DESIGN, SPECIFICATION] });
    await vi.waitFor(() => expect(pending).toHaveLength(2));
    expect(pending[1].asked).toEqual([SPECIFICATION]);

    pending[1].answer();
    pending[0].answer();
    await vi.waitFor(() => expect(result.current.resolved.size).toBe(2));
    expect(result.current.resolved.get('Specification/Catalog')?.found).toBe(false);
  });

  it('drops the answer of a lookup in a project which is no longer the target', async () => {
    const pending = holdLookups();
    const { result, rerender } = await renderHook(
      (props?: { project: string }) => useResolvedTargets(props?.project ?? '', [DESIGN]),
      { initialProps: { project: 'drivepilot' } },
    );
    await vi.waitFor(() => expect(pending).toHaveLength(1));

    await rerender({ project: 'elibrary' });
    await vi.waitFor(() => expect(pending).toHaveLength(2));
    expect(pending[1].url).toContain('/projects/elibrary/');

    pending[0].answer();
    // Give the stale answer the chance to land, then check it did not
    await new Promise((resolve) => setTimeout(resolve, 50));
    expect(result.current.resolved.size).toBe(0);

    pending[1].answer();
    await vi.waitFor(() => expect(result.current.resolved.get('Design/Catalog')?.found).toBe(true));
  });
});
