import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { render } from 'vitest-browser-react';
import ExtensionInfo from '../src/components/ExtensionInfo';
import { responseError } from '../src/services/responseError';
import useDiffService from '../src/services/useDiffService';
import { installFetchMock } from './mockFetch';

// A non-ok response whose body is not JSON used to reach the UI as `throw response.json()`, i.e. an
// unsettled Promise. The catch re-adopted it with `Promise.resolve(x).then(onFulfilled)`, so the parse
// failure had no rejection handler and the browser reported an unhandled rejection.

let rejections: unknown[];
let onRejection: (event: PromiseRejectionEvent) => void;

beforeEach(() => {
  rejections = [];
  onRejection = (event) => {
    rejections.push(event.reason);
    event.preventDefault();
  };
  window.addEventListener('unhandledrejection', onRejection);
});

afterEach(() => {
  window.removeEventListener('unhandledrejection', onRejection);
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

/** Lets the request chain settle and any unhandled rejection be reported. */
const settle = () => new Promise((resolve) => setTimeout(resolve, 200));

describe('responseError', () => {
  it('rejects with an Error, never with a Promise', async () => {
    const error = await responseError(new Response('', { status: 401 }));

    expect(error).toBeInstanceOf(Error);
    expect(error.message).toMatch(/401/);
  });

  it('prefers the server message when the body is JSON', async () => {
    const fromMessage = await responseError(new Response(JSON.stringify({ message: 'boom' }), { status: 500 }));
    const fromErrorMessage = await responseError(
      new Response(JSON.stringify({ errorMessage: 'bang' }), { status: 500 }),
    );

    expect(fromMessage.message).toBe('boom');
    expect(fromErrorMessage.message).toBe('bang');
  });

  it('falls back to the status when the body carries no message', async () => {
    const empty = await responseError(new Response('', { status: 503 }));
    const blank = await responseError(new Response('  \n ', { status: 503 }));
    const noMessage = await responseError(new Response(JSON.stringify({ other: 1 }), { status: 503 }));
    const blankMessage = await responseError(new Response(JSON.stringify({ message: ' ' }), { status: 503 }));
    const jsonNumber = await responseError(new Response('42', { status: 503 }));
    const jsonNull = await responseError(new Response('null', { status: 503 }));

    for (const error of [empty, blank, noMessage, blankMessage, jsonNumber, jsonNull]) {
      expect(error.message).toMatch(/^HTTP 503/);
    }
  });

  it('keeps a short non-JSON body and drops a long one', async () => {
    const short = await responseError(new Response('Gateway timeout', { status: 504 }));
    const long = await responseError(new Response('x'.repeat(501), { status: 504 }));

    expect(short.message).toBe('Gateway timeout');
    expect(long.message).toMatch(/^HTTP 504/);
  });

  it('takes a JSON string body as the message', async () => {
    expect((await responseError(new Response('"plain"', { status: 500 }))).message).toBe('plain');
  });

  it('falls back to the status when the body cannot be read', async () => {
    const unreadable = {
      status: 500,
      statusText: '',
      text: () => Promise.reject(new TypeError('stream error')),
    } as unknown as Response;

    expect((await responseError(unreadable)).message).toBe('HTTP 500');
  });
});

describe('a component handling a failed request', () => {
  const bodies: [label: string, status: number, body: string, contentType?: string][] = [
    ['an empty body', 401, ''],
    ['an HTML error page', 403, '<html lang="en"><body>Forbidden</body></html>', 'text/html'],
    ['a whitespace-only body', 500, '   \n  '],
    ['a JSON error body', 500, JSON.stringify({ message: 'diff failed' }), 'application/json'],
  ];

  it.for(bodies)('produces no unhandled rejection for %s', async ([, status, body, contentType]) => {
    installFetchMock([
      {
        match: /\/extension\/info/,
        respond: () => new Response(body, { status, headers: contentType ? { 'Content-Type': contentType } : {} }),
      },
    ]);
    const logged: string[] = [];
    vi.spyOn(console, 'log').mockImplementation((...args: unknown[]) => logged.push(args.join(' ')));

    render(<ExtensionInfo />);
    await settle();

    expect(rejections).toEqual([]);
    expect(logged).toHaveLength(1);
    expect(logged[0]).toContain('Error occurred loading extension info');
  });

  it('reports the server message rather than a parse failure', async () => {
    installFetchMock([
      {
        match: /\/extension\/info/,
        respond: () => new Response(JSON.stringify({ message: 'no such bundle' }), { status: 404 }),
      },
    ]);
    const logged: string[] = [];
    vi.spyOn(console, 'log').mockImplementation((...args: unknown[]) => logged.push(args.join(' ')));

    render(<ExtensionInfo />);
    await settle();

    expect(rejections).toEqual([]);
    expect(logged[0]).toBe('Error occurred loading extension info: no such bundle');
  });
});

describe('useDiffService on a failed request', () => {
  // The viewer reports the failure through loadingContext and attaches no .catch of its own, so the
  // returned promise must not leak its rejection either.
  const loadingContextStub = () => ({
    pairsLoadingStarted: vi.fn(),
    pairsLoadingFinished: vi.fn(),
    pairsLoadingFinishedWithError: vi.fn(),
  });

  it('reports the error once and leaks no rejection', async () => {
    installFetchMock([
      { method: 'POST', match: /\/diff\/documents/, respond: () => new Response('', { status: 401 }) },
    ]);
    const loadingContext = loadingContextStub();

    // Mirrors DocumentsDiff: .then() plus the deliberate .catch that swallows the redundant rejection.
    useDiffService()
      .sendDocumentsDiffRequest(
        new URLSearchParams('sourceProjectId=a&targetProjectId=b'),
        'cache',
        loadingContext,
        false,
      )
      .then(() => {})
      .catch(() => {});
    await settle();

    expect(rejections).toEqual([]);
    expect(loadingContext.pairsLoadingFinishedWithError).toHaveBeenCalledWith('HTTP 401');
  });

  it('passes the server message through', async () => {
    installFetchMock([
      {
        method: 'POST',
        match: /\/diff\/documents/,
        respond: () => new Response(JSON.stringify({ message: 'left document not found' }), { status: 400 }),
      },
    ]);
    const loadingContext = loadingContextStub();

    useDiffService()
      .sendDocumentsDiffRequest(
        new URLSearchParams('sourceProjectId=a&targetProjectId=b'),
        'cache',
        loadingContext,
        false,
      )
      .then(() => {})
      .catch(() => {});
    await settle();

    expect(rejections).toEqual([]);
    expect(loadingContext.pairsLoadingFinishedWithError).toHaveBeenCalledWith('left document not found');
  });

  // Pins the promise contract the 4 consumers rely on: swallowing the request rejection with the
  // second argument of .then() leaves a throw from the success handler observable, where a trailing
  // .catch() would absorb it. This builds its own chain, so it does not guard the call sites.
  it('does not swallow a throw from the success handler', async () => {
    installFetchMock([{ method: 'POST', match: /\/diff\/documents/, json: { pairedWorkItems: [] } }]);
    const loadingContext = loadingContextStub();
    const bug = new Error('bug in the success handler');

    useDiffService()
      .sendDocumentsDiffRequest(
        new URLSearchParams('sourceProjectId=a&targetProjectId=b'),
        'cache',
        loadingContext,
        false,
      )
      .then(
        () => {
          throw bug;
        },
        () => {},
      );
    await settle();

    expect(rejections).toEqual([bug]);
  });
});
