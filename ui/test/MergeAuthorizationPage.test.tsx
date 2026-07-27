import { Toaster } from '@grigoriev/react-sbb-polarion';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import MergeAuthorizationPage from '../src/admin/pages/MergeAuthorizationPage';
import { type FetchMock, installFetchMock, jsonResponse } from './mockFetch';

// Behaviour of the port of authorization.js: load the available roles plus the granted ones, toggle,
// save, cancel-with-confirm, revert-to-default and the revisions list.

const origUrl = window.location.pathname + window.location.search;

const ROLES = { globalRoles: ['developer', 'admin'], projectRoles: ['project_admin'] };
const GRANTED = { globalRoles: ['admin'], projectRoles: [], bundleTimestamp: '2026-07-01 10:00' };
const INFO = { version: { bundleBuildTimestamp: '2026-07-01 10:00' } };

function routes(overrides: Parameters<typeof installFetchMock>[0] = []) {
  return [
    ...overrides,
    { method: 'GET', match: /\/roles\?/, json: ROLES },
    { method: 'GET', match: /\/extension\/info$/, json: INFO },
    { method: 'GET', match: /\/names\/Default\/content/, json: GRANTED },
    { method: 'GET', match: /\/default-content$/, json: { globalRoles: ['admin'], projectRoles: [] } },
    { method: 'GET', match: /\/revisions\?/, json: [{ name: '1234', date: '2026-07-01', author: 'me' }] },
    { method: 'PUT', match: /\/names\/Default\/content/, json: {} },
  ];
}

/** Toaster is mounted alongside, as main.tsx does, so toast notifications are actually rendered. */
function Page() {
  return (
    <>
      <MergeAuthorizationPage />
      <Toaster />
    </>
  );
}

async function renderPage(fetchMock: FetchMock = installFetchMock(routes())) {
  render(<Page />);
  await vi.waitFor(() =>
    expect(document.querySelectorAll('.role-group input[type=checkbox]').length).toBeGreaterThan(0),
  );
  return fetchMock;
}

function checkboxFor(role: string): HTMLInputElement {
  const label = Array.from(document.querySelectorAll('.role-group label')).find((element) =>
    element.textContent?.trim().startsWith(role),
  );
  return label!.querySelector('input[type=checkbox]') as HTMLInputElement;
}

beforeEach(() => {
  window.history.replaceState({}, '', '?feature=merge-authorization&embedded=true&scope=project/elibrary/');
});

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
  window.history.replaceState({}, '', origUrl);
});

describe('MergeAuthorizationPage', () => {
  it('lists every available role, sorted, with only the granted ones checked', async () => {
    await renderPage();

    // Sorted because the server reads project roles out of an unordered Set.
    const globalLabels = Array.from(document.querySelectorAll('.role-group')[0].querySelectorAll('label')).map(
      (label) => label.textContent?.trim(),
    );
    expect(globalLabels).toEqual(['admin', 'developer']);
    expect(checkboxFor('admin').checked).toBe(true);
    expect(checkboxFor('developer').checked).toBe(false);
  });

  it('requests the roles for the page scope', async () => {
    const fetchMock = await renderPage();

    const rolesCall = fetchMock.mock.calls.find(([url]) => String(url).includes('/roles?'));
    expect(String(rolesCall![0])).toContain('scope=project%2Felibrary%2F');
  });

  it('shows an empty-state message when the scope has no project roles', async () => {
    await renderPage(
      installFetchMock(
        routes([{ method: 'GET', match: /\/roles\?/, json: { globalRoles: ['admin'], projectRoles: [] } }]),
      ),
    );

    expect(document.querySelectorAll('.role-group')[1].textContent).toContain('No project roles in this scope');
  });

  it('saves the checked roles', async () => {
    const fetchMock = await renderPage();

    checkboxFor('developer').click();
    document.querySelector<HTMLButtonElement>('.actions-pane button')!.click();

    await vi.waitFor(() => {
      const put = fetchMock.mock.calls.find(([, init]) => init?.method === 'PUT');
      expect(put).toBeDefined();
      expect(JSON.parse(String(put![1]!.body))).toEqual({
        globalRoles: ['admin', 'developer'],
        projectRoles: [],
      });
    });
  });

  it('unchecking removes a role from the saved set', async () => {
    const fetchMock = await renderPage();

    checkboxFor('admin').click();
    document.querySelector<HTMLButtonElement>('.actions-pane button')!.click();

    await vi.waitFor(() => {
      const put = fetchMock.mock.calls.find(([, init]) => init?.method === 'PUT');
      expect(JSON.parse(String(put![1]!.body)).globalRoles).toEqual([]);
    });
  });

  it('reports a failed save instead of silently losing it', async () => {
    const fetchMock = installFetchMock(
      routes([{ method: 'PUT', match: /\/content/, respond: () => jsonResponse({ errorMessage: 'read only' }, 403) }]),
    );
    await renderPage(fetchMock);

    document.querySelector<HTMLButtonElement>('.actions-pane button')!.click();

    await vi.waitFor(() => expect(document.body.textContent).toContain('read only'));
  });

  it('re-reads the settings on Cancel, but only after confirmation', async () => {
    const fetchMock = await renderPage();
    const contentCalls = () => fetchMock.mock.calls.filter(([url]) => String(url).includes('/content')).length;
    const before = contentCalls();
    const cancel = Array.from(document.querySelectorAll<HTMLButtonElement>('.actions-pane button'))[1];

    vi.spyOn(window, 'confirm').mockReturnValue(false);
    cancel.click();
    expect(contentCalls()).toBe(before);

    vi.spyOn(window, 'confirm').mockReturnValue(true);
    cancel.click();
    await vi.waitFor(() => expect(contentCalls()).toBeGreaterThan(before));
  });

  it('loads the default values on Default without persisting them', async () => {
    const fetchMock = await renderPage();
    vi.spyOn(window, 'confirm').mockReturnValue(true);

    Array.from(document.querySelectorAll<HTMLButtonElement>('.actions-pane button'))[2].click();

    await vi.waitFor(() =>
      expect(fetchMock.mock.calls.some(([url]) => String(url).includes('/default-content'))).toBe(true),
    );
    // Same contract as the legacy page: Default fills the form, Save persists it.
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === 'PUT')).toBe(false);
  });

  it('does not load the defaults when the Default confirmation is declined', async () => {
    const fetchMock = await renderPage();
    vi.spyOn(window, 'confirm').mockReturnValue(false);

    Array.from(document.querySelectorAll<HTMLButtonElement>('.actions-pane button'))[2].click();

    expect(fetchMock.mock.calls.some(([url]) => String(url).includes('/default-content'))).toBe(false);
  });

  it('toggles the revisions table', async () => {
    await renderPage();
    const revisions = Array.from(document.querySelectorAll<HTMLButtonElement>('.actions-pane button'))[3];

    revisions.click();
    await vi.waitFor(() => expect(document.body.textContent).toContain('1 234'));

    revisions.click();
    await vi.waitFor(() => expect(document.body.textContent).not.toContain('1 234'));
  });

  it('loads a revision into the form when its revert arrow is pressed, without persisting it', async () => {
    const fetchMock = await renderPage();
    Array.from(document.querySelectorAll<HTMLButtonElement>('.actions-pane button'))[3].click();
    await vi.waitFor(() => expect(document.querySelector('.revert-to-revision-button')).not.toBeNull());

    document.querySelector<HTMLButtonElement>('.revert-to-revision-button')!.click();

    await vi.waitFor(() =>
      expect(fetchMock.mock.calls.some(([url]) => String(url).includes('revision=1234'))).toBe(true),
    );
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === 'PUT')).toBe(false);
  });

  it('reports a failure while loading a revision', async () => {
    const fetchMock = installFetchMock(
      routes([{ method: 'GET', match: /revision=1234/, respond: () => jsonResponse({ errorMessage: 'gone' }, 404) }]),
    );
    await renderPage(fetchMock);
    Array.from(document.querySelectorAll<HTMLButtonElement>('.actions-pane button'))[3].click();
    await vi.waitFor(() => expect(document.querySelector('.revert-to-revision-button')).not.toBeNull());

    document.querySelector<HTMLButtonElement>('.revert-to-revision-button')!.click();

    await vi.waitFor(() => expect(document.body.textContent).toContain('gone'));
  });

  it('reports a failure while loading the default values', async () => {
    const fetchMock = installFetchMock(
      routes([
        {
          method: 'GET',
          match: /\/default-content$/,
          respond: () => jsonResponse({ errorMessage: 'no defaults' }, 500),
        },
      ]),
    );
    await renderPage(fetchMock);
    vi.spyOn(window, 'confirm').mockReturnValue(true);

    Array.from(document.querySelectorAll<HTMLButtonElement>('.actions-pane button'))[2].click();

    await vi.waitFor(() => expect(document.body.textContent).toContain('no defaults'));
  });

  it('reports a failure while re-reading on Cancel', async () => {
    const fetchMock = await renderPage();
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    // Re-point content reads at a failure only now, so the initial load succeeded.
    installFetchMock(
      routes([
        {
          method: 'GET',
          match: /\/names\/Default\/content/,
          respond: () => jsonResponse({ errorMessage: 'vanished' }, 404),
        },
      ]),
    );

    Array.from(document.querySelectorAll<HTMLButtonElement>('.actions-pane button'))[1].click();

    await vi.waitFor(() => expect(document.querySelector('.alert-error')!.textContent).toContain('vanished'));
    expect(fetchMock).toBeDefined();
  });

  it('treats a settings model with no role arrays as nothing granted', async () => {
    await renderPage(installFetchMock(routes([{ method: 'GET', match: /\/names\/Default\/content/, json: {} }])));

    expect(checkboxFor('admin').checked).toBe(false);
    expect(checkboxFor('developer').checked).toBe(false);
  });

  it('surfaces a load failure', async () => {
    installFetchMock([
      { method: 'GET', match: /\/roles\?/, respond: () => jsonResponse({ errorMessage: 'no permission' }, 403) },
      { method: 'GET', match: /\/extension\/info$/, json: INFO },
    ]);
    render(<Page />);

    await vi.waitFor(() => expect(document.querySelector('.alert-error')).not.toBeNull());
  });

  it('warns when the stored settings came from a different build of the extension', async () => {
    installFetchMock(
      routes([
        {
          method: 'GET',
          match: /\/names\/Default\/content/,
          json: { ...GRANTED, bundleTimestamp: '2020-01-01 00:00' },
        },
      ]),
    );
    render(<Page />);

    await vi.waitFor(() => expect(document.querySelector('.alert-warning')).not.toBeNull());
  });

  it('does not warn when the stored settings match the deployed build', async () => {
    await renderPage();

    expect(document.querySelector('.alert-warning')).toBeNull();
  });
});
