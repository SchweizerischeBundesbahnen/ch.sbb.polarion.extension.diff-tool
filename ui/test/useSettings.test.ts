import { afterEach, describe, expect, it, vi } from 'vitest';
import useSettings, { DEFAULT_CONFIGURATION } from '../src/services/useSettings';
import { installFetchMock, jsonResponse } from './mockFetch';

// The generic framework's named-settings REST contract, as every settings page will use it. Asserting
// the exact URLs, methods and bodies here is the cheapest guard against a silent contract drift.

afterEach(() => {
  vi.unstubAllGlobals();
});

const BASE = '/polarion/diff-tool/rest/internal/settings/authorization';

describe('useSettings', () => {
  it('returns the same service instance for a feature, so injected callbacks are stable', () => {
    // RevisionsTable and ConfigurationsPane put these callbacks in effect dependencies; a fresh
    // identity per render would refetch on every state change of the hosting page.
    const first = useSettings('authorization');
    const second = useSettings('authorization');

    expect(second).toBe(first);
    expect(second.loadRevisions).toBe(first.loadRevisions);
    expect(useSettings('diff')).not.toBe(first);
  });

  it('lists configuration names for a scope', async () => {
    const fetchMock = installFetchMock([{ match: /\/names\?/, json: [{ name: 'Default', scope: '' }] }]);

    const names = await useSettings('authorization').loadConfigurationNames('project/elibrary/');

    expect(String(fetchMock.mock.calls[0][0])).toBe(`${BASE}/names?scope=project%2Felibrary%2F`);
    expect(names).toEqual([{ name: 'Default', scope: '' }]);
  });

  it('loads content, and a specific revision when asked', async () => {
    const fetchMock = installFetchMock([{ match: /\/content\?/, json: { globalRoles: ['admin'] } }]);
    const settings = useSettings<{ globalRoles: string[] }>('authorization');

    await settings.loadContent(DEFAULT_CONFIGURATION, 'project/elibrary/');
    await settings.loadContent(DEFAULT_CONFIGURATION, 'project/elibrary/', '1234');

    expect(String(fetchMock.mock.calls[0][0])).toBe(`${BASE}/names/Default/content?scope=project%2Felibrary%2F`);
    expect(String(fetchMock.mock.calls[1][0])).toBe(
      `${BASE}/names/Default/content?scope=project%2Felibrary%2F&revision=1234`,
    );
  });

  it('loads default content without a scope, which the generic API is indifferent to', async () => {
    const fetchMock = installFetchMock([{ match: /\/default-content$/, json: {} }]);

    await useSettings('authorization').loadDefaultContent();

    expect(String(fetchMock.mock.calls[0][0])).toBe(`${BASE}/default-content`);
  });

  it('saves content as a JSON PUT', async () => {
    const fetchMock = installFetchMock([{ method: 'PUT', match: /\/content\?/, json: {} }]);

    await useSettings('authorization').saveContent(DEFAULT_CONFIGURATION, '', { globalRoles: ['admin'] });

    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toBe(`${BASE}/names/Default/content?scope=`);
    expect(init!.method).toBe('PUT');
    expect(init!.body).toBe('{"globalRoles":["admin"]}');
    expect((init!.headers as Record<string, string>)['Content-Type']).toBe('application/json');
  });

  it('creates a configuration with an empty body, letting the backend seed the defaults', async () => {
    const fetchMock = installFetchMock([{ method: 'PUT', match: /\/content\?/, json: {} }]);

    await useSettings('authorization').createConfiguration('New config', '');

    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toBe(`${BASE}/names/New%20config/content?scope=`);
    expect(init!.body).toBe('');
  });

  it('renames by POSTing the new name as a bare JSON string', async () => {
    const fetchMock = installFetchMock([{ method: 'POST', match: /\/names\//, json: {} }]);

    await useSettings('authorization').renameConfiguration('Old', '', 'New');

    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toBe(`${BASE}/names/Old?scope=`);
    expect(init!.method).toBe('POST');
    expect(init!.body).toBe('"New"');
  });

  it('deletes a configuration', async () => {
    const fetchMock = installFetchMock([{ method: 'DELETE', match: /\/names\//, json: {} }]);

    await useSettings('authorization').deleteConfiguration('Obsolete', 'project/elibrary/');

    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toBe(`${BASE}/names/Obsolete?scope=project%2Felibrary%2F`);
    expect(init!.method).toBe('DELETE');
  });

  it('lists revisions', async () => {
    const fetchMock = installFetchMock([{ match: /\/revisions\?/, json: [{ name: '1234' }] }]);

    const revisions = await useSettings('authorization').loadRevisions(DEFAULT_CONFIGURATION, '');

    expect(String(fetchMock.mock.calls[0][0])).toBe(`${BASE}/names/Default/revisions?scope=`);
    expect(revisions).toEqual([{ name: '1234' }]);
  });

  it('surfaces the server errorMessage when a read fails', async () => {
    installFetchMock([{ match: /\/content\?/, respond: () => jsonResponse({ errorMessage: 'no such setting' }, 404) }]);

    await expect(useSettings('authorization').loadContent('Nope', '')).rejects.toThrow('no such setting');
  });

  it('surfaces the server errorMessage when a write fails', async () => {
    installFetchMock([
      { method: 'PUT', match: /\/content\?/, respond: () => jsonResponse({ errorMessage: 'read only' }, 403) },
    ]);

    await expect(useSettings('authorization').saveContent('Default', '', {})).rejects.toThrow('read only');
  });

  it('accepts a plain `message` error body too, which some endpoints return', async () => {
    installFetchMock([{ match: /\/names\?/, respond: () => jsonResponse({ message: 'service unavailable' }, 503) }]);

    await expect(useSettings('authorization').loadConfigurationNames('')).rejects.toThrow('service unavailable');
  });

  it('falls back to the status code when the error body carries no message', async () => {
    installFetchMock([{ match: /\/revisions\?/, respond: () => new Response('boom', { status: 500 }) }]);

    await expect(useSettings('authorization').loadRevisions('Default', '')).rejects.toThrow('HTTP 500');
  });
});
