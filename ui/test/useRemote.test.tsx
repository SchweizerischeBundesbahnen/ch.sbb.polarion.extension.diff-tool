import { afterEach, describe, expect, it, vi } from 'vitest';
import useRemote from '../src/services/useRemote';
import { installFetchMock, jsonResponse } from './mockFetch';

// useRemote is the single REST seam. Its contract: same-origin URL under /polarion/diff-tool/rest, the
// /internal base for session auth and /api plus a bearer header when a token is configured, and a
// synthesised 503 rather than a thrown exception when the network is unreachable.

afterEach(() => {
  vi.unstubAllGlobals();
  vi.unstubAllEnvs();
});

describe('useRemote', () => {
  it('targets the session-authenticated /internal base and sends no Authorization header', async () => {
    const fetchMock = installFetchMock([{ match: /./, json: { ok: true } }]);

    const response = await useRemote().sendRequest({ method: 'GET', url: '/projects' });

    expect(await response.json()).toEqual({ ok: true });
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe('/polarion/diff-tool/rest/internal/projects');
    expect((init!.headers as Record<string, string>).Authorization).toBeUndefined();
  });

  it('is same-origin: no scheme or host is prefixed', async () => {
    // The dev-server proxy forwards /polarion/** to VITE_BASE_URL, so the app must never build an
    // absolute URL itself - that is what used to require CORS.
    const fetchMock = installFetchMock([{ match: /./, json: {} }]);

    await useRemote().sendRequest({ method: 'GET', url: '/extension/info' });

    expect(String(fetchMock.mock.calls[0][0])).toMatch(/^\/polarion\//);
  });

  it('switches to the token-authenticated /api base when a bearer token is configured', async () => {
    vi.stubEnv('VITE_BEARER_TOKEN', 'test-token');
    const fetchMock = installFetchMock([{ match: /./, json: {} }]);

    await useRemote().sendRequest({ method: 'GET', url: '/projects' });

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe('/polarion/diff-tool/rest/api/projects');
    expect((init!.headers as Record<string, string>).Authorization).toBe('Bearer test-token');
  });

  it('sets Content-Type only when a contentType is given', async () => {
    const fetchMock = installFetchMock([{ method: 'POST', match: /./, json: {} }]);

    await useRemote().sendRequest({ method: 'POST', url: '/diff/documents', body: '{}' });
    await useRemote().sendRequest({
      method: 'POST',
      url: '/diff/documents',
      body: '{}',
      contentType: 'application/json',
    });

    const headersOf = (call: number) => fetchMock.mock.calls[call][1]!.headers as Record<string, string>;
    expect(headersOf(0)['Content-Type']).toBeUndefined();
    expect(headersOf(1)['Content-Type']).toBe('application/json');
  });

  it('forwards method and body unchanged', async () => {
    const fetchMock = installFetchMock([{ method: 'PUT', match: /./, json: {} }]);

    await useRemote().sendRequest({ method: 'PUT', url: '/settings/diff/names/Default/content', body: '{"a":1}' });

    const init = fetchMock.mock.calls[0][1]!;
    expect(init.method).toBe('PUT');
    expect(init.body).toBe('{"a":1}');
  });

  it('resolves to a 503 with a readable message when the network is unreachable', async () => {
    // fetch() rejects on a network error; the app surfaces that as a normal response so every caller's
    // handleResponse path works. This is what the "Be sure Polarion is started" alert renders from.
    vi.stubGlobal(
      'fetch',
      vi.fn(() => Promise.reject(new TypeError('Failed to fetch'))),
    );

    const response = await useRemote().sendRequest({ method: 'GET', url: '/projects' });

    expect(await response.json()).toEqual({
      message:
        'Network error occurred when attempting to fetch a resource. Be sure Polarion is started and accessible.',
    });
  });

  it('passes server error responses through untouched', async () => {
    installFetchMock([{ match: /./, respond: () => jsonResponse({ errorMessage: 'boom' }, 500) }]);

    const response = await useRemote().sendRequest({ method: 'GET', url: '/projects' });

    expect(response.status).toBe(500);
    expect(await response.json()).toEqual({ errorMessage: 'boom' });
  });
});
