import { Toaster } from '@grigoriev/react-sbb-polarion';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import DiffConfigurationsPage from '../src/admin/pages/DiffConfigurationsPage';
import { answerConfirm } from './confirmDialog';
import { type FetchMock, type Route, installFetchMock, jsonResponse } from './mockFetch';

// Behaviour of the port of diff.js: the four project lists feeding the controls, the named-configuration
// pane, conditional visibility of the two role multiselects, save/cancel/default, and the
// newer-version-of-the-extension warning.

const origUrl = window.location.pathname + window.location.search;

const FIELDS = [
  { key: 'title', name: 'Title' },
  { key: 'description', name: 'Description' },
  { key: 'hyperlinks', name: 'Hyperlinks' },
  { key: 'linkedWorkItems', name: 'Linked WorkItems' },
];
const STATUSES = [
  { id: 'open', name: 'Open', iconUrl: '/polarion/icons/open.svg' },
  { id: 'draft', name: 'Draft', wiTypeName: 'Requirement' },
];
const HYPERLINK_ROLES = [
  { id: 'ref', name: 'refers to', workItemTypeId: 'req', workItemTypeName: 'Requirement', combinedId: 'req#ref' },
];
const LINKED_ROLES = [{ id: 'relates_to', name: 'relates to' }];
const INFO = { version: { bundleBuildTimestamp: '2026-07-01 10:00' } };

const STORED = {
  diffFields: [{ key: 'title' }],
  statusesToIgnore: ['draft'],
  hyperlinkRoles: [],
  linkedWorkItemRoles: [],
  bundleTimestamp: '2026-07-01 10:00',
};

function routes(overrides: Route[] = []): Route[] {
  return [
    ...overrides,
    { method: 'GET', match: /\/workitem-fields$/, json: FIELDS },
    { method: 'GET', match: /\/workitem-statuses$/, json: STATUSES },
    { method: 'GET', match: /\/hyperlink-roles$/, json: HYPERLINK_ROLES },
    { method: 'GET', match: /\/linked-workitem-roles$/, json: LINKED_ROLES },
    { method: 'GET', match: /\/extension\/info$/, json: INFO },
    { method: 'GET', match: /\/settings\/diff\/names\?/, json: [{ name: 'Default', scope: 'project/elibrary/' }] },
    { method: 'GET', match: /\/names\/Default\/content/, json: STORED },
    { method: 'GET', match: /\/default-content$/, json: { diffFields: [{ key: 'title' }], statusesToIgnore: [] } },
    { method: 'GET', match: /\/revisions\?/, json: [{ name: '3388', date: '2026-06-30', author: 'jane' }] },
    { method: 'PUT', match: /\/names\/Default\/content/, json: {} },
  ];
}

function Page() {
  return (
    <>
      <DiffConfigurationsPage />
      <Toaster />
    </>
  );
}

async function renderPage(fetchMock: FetchMock = installFetchMock(routes())) {
  render(<Page />);
  await vi.waitFor(() => expect(document.querySelectorAll('#selected-fields option').length).toBeGreaterThan(0));
  return fetchMock;
}

const optionsOf = (id: string) =>
  Array.from(document.querySelectorAll<HTMLOptionElement>(`#${id} option`)).map((option) => option.textContent);

const toolbarButton = (index: number) =>
  Array.from(document.querySelectorAll<HTMLButtonElement>('.actions-pane button'))[index];

beforeEach(() => {
  window.history.replaceState({}, '', '?feature=diff-configurations&embedded=true&scope=project/elibrary/');
});

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
  window.history.replaceState({}, '', origUrl);
  document.cookie = 'selected-configuration-diff=; path=/; max-age=0';
});

describe('DiffConfigurationsPage', () => {
  it('loads the project lists for the scope project and shows the stored selection', async () => {
    const fetchMock = await renderPage();

    expect(fetchMock.mock.calls.some(([url]) => String(url).includes('/projects/elibrary/workitem-fields'))).toBe(true);
    expect(optionsOf('selected-fields')).toEqual(['Title [title]']);
    expect(optionsOf('available-fields')).not.toContain('Title [title]');
  });

  it('renders the statuses multiselect with labels and icons from the REST payload', async () => {
    await renderPage();

    const options = Array.from(document.querySelectorAll<HTMLOptionElement>('#statuses-to-ignore option'));
    expect(options.map((option) => option.textContent)).toEqual(['Open [open]', 'Draft [draft - Requirement]']);
    expect(options[0].dataset.icon).toBe('/polarion/icons/open.svg');
    expect(options.find((option) => option.value === 'draft')!.selected).toBe(true);
  });

  it('hides the two role multiselects until their field is selected', async () => {
    await renderPage();
    expect(document.querySelector('#hyperlink-settings-container')).toBeNull();
    expect(document.querySelector('#linked-workitem-settings-container')).toBeNull();

    const available = document.querySelector<HTMLSelectElement>('#available-fields')!;
    Array.from(available.options).forEach((option) => {
      option.selected = option.textContent === 'Hyperlinks [hyperlinks]';
    });
    available.dispatchEvent(new Event('change', { bubbles: true }));
    document.querySelector<HTMLButtonElement>('#add-button')!.click();

    await vi.waitFor(() => expect(document.querySelector('#hyperlink-settings-container')).not.toBeNull());
    // Only the hyperlink one - linkedWorkItems is still unselected.
    expect(document.querySelector('#linked-workitem-settings-container')).toBeNull();
    expect(optionsOf('hyperlink-roles')).toEqual(['[Requirement] refers to']);
  });

  it('saves the whole model under the selected configuration', async () => {
    const fetchMock = await renderPage();

    toolbarButton(0).click();

    await vi.waitFor(() => {
      const put = fetchMock.mock.calls.find(([, init]) => init?.method === 'PUT');
      expect(put).toBeDefined();
      expect(String(put![0])).toContain('/settings/diff/names/Default/content?scope=project%2Felibrary%2F');
      expect(JSON.parse(String(put![1]!.body))).toEqual({
        diffFields: [{ key: 'title' }],
        statusesToIgnore: ['draft'],
        hyperlinkRoles: [],
        linkedWorkItemRoles: [],
      });
    });
  });

  it('saves field changes made in the transfer list', async () => {
    const fetchMock = await renderPage();

    const available = document.querySelector<HTMLSelectElement>('#available-fields')!;
    Array.from(available.options).forEach((option) => {
      option.selected = option.textContent === 'Description [description]';
    });
    available.dispatchEvent(new Event('change', { bubbles: true }));
    document.querySelector<HTMLButtonElement>('#add-button')!.click();
    await vi.waitFor(() => expect(optionsOf('selected-fields')).toContain('Description [description]'));

    toolbarButton(0).click();

    await vi.waitFor(() => {
      const put = fetchMock.mock.calls.find(([, init]) => init?.method === 'PUT');
      expect(JSON.parse(String(put![1]!.body)).diffFields).toEqual([{ key: 'title' }, { key: 'description' }]);
    });
  });

  it('reports a failed save', async () => {
    const fetchMock = installFetchMock(
      routes([{ method: 'PUT', match: /\/content/, respond: () => jsonResponse({ errorMessage: 'read only' }, 403) }]),
    );
    await renderPage(fetchMock);

    toolbarButton(0).click();

    await vi.waitFor(() => expect(document.body.textContent).toContain('read only'));
  });

  it('loads the default values on Default without persisting them', async () => {
    const fetchMock = await renderPage();

    toolbarButton(2).click();
    await answerConfirm('OK');

    await vi.waitFor(() =>
      expect(fetchMock.mock.calls.some(([url]) => String(url).includes('/default-content'))).toBe(true),
    );
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === 'PUT')).toBe(false);
  });

  it('re-reads the configuration on Cancel, but only after confirmation', async () => {
    const fetchMock = await renderPage();
    const contentReads = () =>
      fetchMock.mock.calls.filter(([url, init]) => String(url).includes('/content') && init?.method !== 'PUT').length;
    const before = contentReads();

    toolbarButton(1).click();
    await answerConfirm('Cancel');
    expect(contentReads()).toBe(before);

    toolbarButton(1).click();
    await answerConfirm('OK');
    await vi.waitFor(() => expect(contentReads()).toBeGreaterThan(before));
  });

  it('surfaces a failure while re-reading on Cancel', async () => {
    await renderPage();
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

    toolbarButton(1).click();
    await answerConfirm('OK');

    await vi.waitFor(() => expect(document.querySelector('.alert-error')!.textContent).toContain('vanished'));
  });

  it('toggles the revisions table', async () => {
    await renderPage();

    toolbarButton(3).click();
    await vi.waitFor(() => expect(document.body.textContent).toContain('3 388'));
  });

  it('loads a revision into the form when its revert arrow is pressed, without persisting it', async () => {
    const fetchMock = await renderPage();
    toolbarButton(3).click();
    await vi.waitFor(() => expect(document.querySelector('.revert-to-revision-button')).not.toBeNull());

    document.querySelector<HTMLButtonElement>('.revert-to-revision-button')!.click();

    await vi.waitFor(() =>
      expect(fetchMock.mock.calls.some(([url]) => String(url).includes('revision=3388'))).toBe(true),
    );
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === 'PUT')).toBe(false);
  });

  it('reports a failure while loading a revision', async () => {
    const fetchMock = installFetchMock(
      routes([{ method: 'GET', match: /revision=3388/, respond: () => jsonResponse({ errorMessage: 'gone' }, 404) }]),
    );
    await renderPage(fetchMock);
    toolbarButton(3).click();
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

    toolbarButton(2).click();
    await answerConfirm('OK');

    await vi.waitFor(() => expect(document.body.textContent).toContain('no defaults'));
  });

  it('does not load the defaults when the Default confirmation is declined', async () => {
    const fetchMock = await renderPage();

    // answerConfirm waits for the dialog, so reaching it at all proves the page asked first.
    toolbarButton(2).click();
    await answerConfirm('Cancel');

    expect(fetchMock.mock.calls.some(([url]) => String(url).includes('/default-content'))).toBe(false);
  });

  it('reveals the linked-WorkItem roles multiselect when that field is selected', async () => {
    await renderPage(
      installFetchMock(
        routes([
          {
            method: 'GET',
            match: /\/names\/Default\/content/,
            json: { ...STORED, diffFields: [{ key: 'linkedWorkItems' }] },
          },
        ]),
      ),
    );

    expect(document.querySelector('#linked-workitem-settings-container')).not.toBeNull();
    expect(optionsOf('linked-workitem-roles')).toEqual(['relates to']);
    expect(document.querySelector('#hyperlink-settings-container')).toBeNull();
  });

  it('falls back to the plain role id when a hyperlink role has no combinedId', async () => {
    await renderPage(
      installFetchMock(
        routes([
          {
            method: 'GET',
            match: /\/hyperlink-roles$/,
            json: [{ id: 'ref', name: 'refers to', workItemTypeName: 'Requirement' }],
          },
          {
            method: 'GET',
            match: /\/names\/Default\/content/,
            json: { ...STORED, diffFields: [{ key: 'hyperlinks' }] },
          },
        ]),
      ),
    );

    const option = document.querySelector<HTMLOptionElement>('#hyperlink-roles option')!;
    expect(option.value).toBe('ref');
  });

  it('still renders when the extension info request fails', async () => {
    // bundleTimestamp is then unknown, so the newer-version comparison is simply skipped.
    await renderPage(
      installFetchMock(routes([{ method: 'GET', match: /\/extension\/info$/, respond: () => jsonResponse({}, 500) }])),
    );

    expect(optionsOf('selected-fields')).toEqual(['Title [title]']);
    expect(document.querySelector('.alert-warning')).toBeNull();
  });

  it('tolerates a stored model with no arrays at all', async () => {
    installFetchMock(routes([{ method: 'GET', match: /\/names\/Default\/content/, json: {} }]));
    render(<Page />);

    await vi.waitFor(() => expect(document.querySelector('#available-fields')).not.toBeNull());
    expect(optionsOf('selected-fields')).toEqual([]);
    expect(document.querySelector('#hyperlink-settings-container')).toBeNull();
  });

  it('warns when a newer build changed the default field set', async () => {
    installFetchMock(
      routes([
        { method: 'GET', match: /\/names\/Default\/content/, json: { ...STORED, bundleTimestamp: '2020-01-01 00:00' } },
        {
          method: 'GET',
          match: /\/default-content$/,
          json: { diffFields: [{ key: 'title' }, { key: 'outlineNumber' }] },
        },
      ]),
    );
    render(<Page />);

    await vi.waitFor(() => expect(document.querySelector('.alert-warning')).not.toBeNull());
  });

  it('does not warn when a newer build left the default field set unchanged', async () => {
    // The legacy page also compared the two arrays by identity, which is never equal, so it warned on
    // every build change regardless of whether anything had actually changed.
    installFetchMock(
      routes([
        { method: 'GET', match: /\/names\/Default\/content/, json: { ...STORED, bundleTimestamp: '2020-01-01 00:00' } },
      ]),
    );
    render(<Page />);
    await vi.waitFor(() => expect(document.querySelectorAll('#selected-fields option').length).toBeGreaterThan(0));

    expect(document.querySelector('.alert-warning')).toBeNull();
  });

  it('refuses to work outside a project scope instead of requesting nonsense URLs', async () => {
    window.history.replaceState({}, '', '?feature=diff-configurations&embedded=true');
    const fetchMock = installFetchMock(routes());
    render(<Page />);

    await vi.waitFor(() => expect(document.querySelector('.alert-error')).not.toBeNull());
    expect(document.querySelector('.alert-error')!.textContent).toContain('project scope');
    expect(fetchMock.mock.calls.some(([url]) => String(url).includes('/workitem-fields'))).toBe(false);
  });

  it('surfaces a failure to load the project lists', async () => {
    installFetchMock(
      routes([
        { method: 'GET', match: /\/workitem-fields$/, respond: () => jsonResponse({ errorMessage: 'nope' }, 500) },
      ]),
    );
    render(<Page />);

    await vi.waitFor(() => expect(document.querySelector('.alert-error')).not.toBeNull());
  });
});
