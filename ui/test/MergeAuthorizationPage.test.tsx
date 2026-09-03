import { Toaster } from '@sbb-polarion/react-sbb-polarion';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import MergeAuthorizationPage from '../src/admin/pages/MergeAuthorizationPage';
import { answerConfirm } from './confirmDialog';
import { type FetchMock, installFetchMock, jsonResponse } from './mockFetch';

// The page is react-sbb-polarion's AuthorizationSettings, which has its own suite there - picking roles,
// saving, the confirmations, the revision table. What is diff-tool's own, and therefore what is tested
// here, is the wiring: the feature name the service reads and writes, that the roles come from generic's
// /roles endpoint, the sorted option order this page adds, and the Quick Help text.

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
  await vi.waitFor(() => expect(document.querySelectorAll('.roles-group .sd-trigger-multi')).toHaveLength(2));
  return fetchMock;
}

/** The dropdown opens, closes and picks on mousedown, so the interactions here drive that event. */
const mousedown = (node: Element) =>
  node.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true }));

/** Each role set is a multi-select SearchableSelect, which inserts itself right after the <select> the
 *  component ids. Addressing it from that id keeps these helpers off the page order. */
const trigger = (kind: 'global' | 'project'): HTMLElement => {
  const container = document.querySelector(`#${kind}-roles`)?.nextElementSibling;
  if (!(container instanceof HTMLElement)) {
    throw new Error(`no ${kind} roles control`);
  }
  return container.querySelector<HTMLElement>('.sd-trigger-multi')!;
};

/** The roles one control offers, in the order it lists them. The popup renders its options only while
 *  open, and every dropdown keeps its own portal in the body - hence the open, and the aria-controls. */
const listedRoles = (kind: 'global' | 'project'): string[] => {
  mousedown(trigger(kind));
  const listbox = document.getElementById(trigger(kind).getAttribute('aria-controls')!)!;
  const labels = Array.from(listbox.querySelectorAll('.option')).map((option) => (option.textContent ?? '').trim());
  mousedown(trigger(kind));
  return labels;
};

/** The roles granted in one control, as the chips painted on its trigger. */
const granted = (kind: 'global' | 'project'): string[] =>
  Array.from(trigger(kind).querySelectorAll('.sd-chip-label')).map((chip) => (chip.textContent ?? '').trim());

/** Ticks (or unticks) one role and waits for its chip to follow, which is what proves React took the
 *  change - so a Save right after reads the new selection rather than the previous render's. */
async function toggleRole(kind: 'global' | 'project', role: string) {
  const wasGranted = granted(kind).includes(role);
  mousedown(trigger(kind));
  const listbox = document.getElementById(trigger(kind).getAttribute('aria-controls')!)!;
  const option = Array.from(listbox.querySelectorAll('.option')).find((o) => (o.textContent ?? '').trim() === role);
  if (!option) {
    throw new Error(`no option for role "${role}"`);
  }
  mousedown(option);
  mousedown(trigger(kind));
  await vi.waitFor(() => expect(granted(kind).includes(role)).toBe(!wasGranted));
}

const toolbarButton = (index: number) =>
  Array.from(document.querySelectorAll<HTMLButtonElement>('.actions-pane button'))[index];

beforeEach(() => {
  window.history.replaceState({}, '', `?feature=merge-authorization&embedded=true&scope=${encodeURIComponent(SCOPE)}`);
});

afterEach(() => {
  cleanup();
  document.querySelectorAll('.sd-portal').forEach((portal) => portal.remove());
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

    expect(listedRoles('global')).toEqual(['admin', 'developer', 'user']);
    expect(listedRoles('project')).toEqual(['lead', 'reviewer']);
  });

  it('shows which roles are granted', async () => {
    await renderPage();

    expect(granted('global')).toEqual(['admin']);
    expect(granted('project')).toEqual([]);
  });

  it('saves the granted roles under the authorization setting', async () => {
    const fetchMock = await renderPage();

    await toggleRole('global', 'developer');
    await toggleRole('project', 'lead');
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
    await toggleRole('global', 'developer');

    toolbarButton(2).click();
    await answerConfirm('OK');

    await vi.waitFor(() =>
      expect(fetchMock.mock.calls.some(([url]) => String(url).includes('/default-content'))).toBe(true),
    );
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === 'PUT')).toBe(false);
    await vi.waitFor(() => expect(granted('global')).toEqual(['admin']));
  });

  it('reports a failure to load', async () => {
    // Rendered directly rather than through renderPage(): with the roles unavailable there are no
    // controls to wait for, which is the whole point of the case.
    installFetchMock(routes([{ method: 'GET', match: /\/roles\?/, respond: () => jsonResponse({}, 500) }]));
    render(<Page />);

    await vi.waitFor(() => expect(document.querySelector('.alert-error')).not.toBeNull());
    expect(document.querySelectorAll('.roles-group .sd-trigger-multi')).toHaveLength(0);
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
