import { Toaster } from '@sbb-polarion/react-sbb-polarion';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import MergeAuthorizationPage from '../src/admin/pages/MergeAuthorizationPage';
import { answerConfirm } from './confirmDialog';
import { type FetchMock, installFetchMock, jsonResponse } from './mockFetch';

// The page is react-sbb-polarion's AuthorizationSettings, which has its own suite there - toggling,
// saving, the confirmations, the revision table. What is diff-tool's own, and therefore what is tested
// here, is the wiring: the feature name the service reads and writes, that the roles come from generic's
// /roles endpoint, the sorted checkbox order this page adds, and the Quick Help text.

const origUrl = window.location.pathname + window.location.search;
const SCOPE = 'project/elibrary/';

// Deliberately unsorted, with a role in each list that sorts before the first: ISecurityService returns
// unordered collections, so the page sorts them.
const ROLES = { globalRoles: ['developer', 'admin', 'user'], projectRoles: ['reviewer', 'lead'] };
const GRANTED = { globalRoles: ['admin'], projectRoles: [] };

function routes(overrides: Parameters<typeof installFetchMock>[0] = []) {
  return [
    ...overrides,
    { method: 'GET', match: /\/roles\?/, json: ROLES },
    { method: 'GET', match: /\/settings\/authorization\/names\/Default\/content/, json: GRANTED },
    { method: 'GET', match: /\/settings\/authorization\/default-content$/, json: { globalRoles: ['admin'] } },
    { method: 'GET', match: /\/revisions\?/, json: [{ name: '1234', date: '2026-07-01', author: 'me' }] },
    { method: 'PUT', match: /\/settings\/authorization\/names\/Default\/content/, json: {} },
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
    expect(document.querySelectorAll('.roles-list input[type=checkbox]').length).toBeGreaterThan(0),
  );
  return fetchMock;
}

const roleLabels = (groupIndex: number) =>
  Array.from(document.querySelectorAll('.roles-group')[groupIndex].querySelectorAll('.roles-list li')).map((li) =>
    li.textContent?.trim(),
  );

function checkboxFor(role: string): HTMLInputElement {
  const item = Array.from(document.querySelectorAll('.roles-list li')).find(
    (element) => element.textContent?.trim() === role,
  );
  if (!item) {
    throw new Error(`no checkbox for role "${role}"`);
  }
  return item.querySelector('input[type=checkbox]') as HTMLInputElement;
}

const toolbarButton = (index: number) =>
  Array.from(document.querySelectorAll<HTMLButtonElement>('.actions-pane button'))[index];

beforeEach(() => {
  window.history.replaceState({}, '', `?feature=merge-authorization&embedded=true&scope=${encodeURIComponent(SCOPE)}`);
});

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
  window.history.replaceState({}, '', origUrl);
});

describe('MergeAuthorizationPage', () => {
  it("reads the roles from generic's endpoint for the current scope", async () => {
    const fetchMock = await renderPage();

    expect(
      fetchMock.mock.calls.some(([url]) => String(url).endsWith(`/roles?scope=${encodeURIComponent(SCOPE)}`)),
    ).toBe(true);
  });

  it('sorts both role groups, which arrive unordered', async () => {
    await renderPage();

    expect(roleLabels(0)).toEqual(['admin', 'developer', 'user']);
    expect(roleLabels(1)).toEqual(['lead', 'reviewer']);
  });

  it('shows which roles are granted', async () => {
    await renderPage();

    expect(checkboxFor('admin').checked).toBe(true);
    expect(checkboxFor('developer').checked).toBe(false);
    expect(checkboxFor('lead').checked).toBe(false);
  });

  it('saves the granted roles under the authorization setting', async () => {
    const fetchMock = await renderPage();

    checkboxFor('developer').click();
    checkboxFor('lead').click();
    toolbarButton(0).click();

    await vi.waitFor(() => {
      const put = fetchMock.mock.calls.find(([, init]) => init?.method === 'PUT');
      expect(put).toBeDefined();
      // The feature name is this extension's contribution; the payload shape is the component's.
      expect(String(put![0])).toContain('/settings/authorization/names/Default/content');
      expect(JSON.parse(String(put![1]!.body))).toEqual({
        globalRoles: ['admin', 'developer'],
        projectRoles: ['lead'],
      });
    });
    await vi.waitFor(() => expect(document.body.textContent).toContain('successfully saved'));
  });

  it('loads the defaults on Default without persisting them', async () => {
    const fetchMock = await renderPage();
    checkboxFor('developer').click();

    toolbarButton(2).click();
    await answerConfirm('OK');

    await vi.waitFor(() =>
      expect(fetchMock.mock.calls.some(([url]) => String(url).includes('/default-content'))).toBe(true),
    );
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === 'PUT')).toBe(false);
    await vi.waitFor(() => expect(checkboxFor('developer').checked).toBe(false));
  });

  it('reports a failure to load', async () => {
    // Rendered directly rather than through renderPage(): with the roles unavailable there are no
    // checkboxes to wait for, which is the whole point of the case.
    installFetchMock(routes([{ method: 'GET', match: /\/roles\?/, respond: () => jsonResponse({}, 500) }]));
    render(<Page />);

    await vi.waitFor(() => expect(document.querySelector('.alert-error')).not.toBeNull());
    expect(document.querySelectorAll('.roles-list input[type=checkbox]')).toHaveLength(0);
  });

  it('renders the merge-specific Quick Help', async () => {
    await renderPage();

    const quickHelp = document.querySelector('.quick-help')!.textContent ?? '';
    expect(quickHelp).toContain('The diffing functionality is unrestricted');
    expect(quickHelp).toContain('only users with the global admin role have permission to merge');
  });

  it('no longer warns that the setting was written by another bundle', async () => {
    // Dropped on purpose: a role setting has no schema that can go stale, and the timestamp is stamped at
    // save time, so the banner fired after every plugin upgrade. Nothing reads /extension/info now.
    const fetchMock = await renderPage(
      installFetchMock(
        routes([
          {
            method: 'GET',
            match: /\/settings\/authorization\/names\/Default\/content/,
            json: { ...GRANTED, bundleTimestamp: '2020-01-01 00:00' },
          },
        ]),
      ),
    );

    expect(document.querySelector('.alert-warning')).toBeNull();
    expect(fetchMock.mock.calls.some(([url]) => String(url).includes('/extension/info'))).toBe(false);
  });
});
