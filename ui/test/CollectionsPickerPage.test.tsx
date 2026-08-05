import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import CollectionsPickerPage from '../src/topics/CollectionsPickerPage';
import { type FetchMock, type Route, installFetchMock } from './mockFetch';

// Behaviour of the port of collections.jsp + CollectionsDiffWidgetRenderer: two tables side by side, one
// radio selection per side, and the handoff to collections.html.

const origUrl = window.location.pathname + window.location.search;

const PROJECTS = [
  { id: 'elibrary', name: 'E-Library' },
  { id: 'drivepilot', name: 'Drive Pilot' },
];

const LINK_ROLES = [{ id: 'relates_to', name: 'relates to', oppositeName: 'is related to' }];
const CONFIGURATIONS = [{ name: 'Default', scope: '' }];

const collection = (id: string, projectId: string) => ({
  id: id,
  projectId: projectId,
  name: `Collection ${id}`,
  authorName: 'John Doe',
  created: 1_700_000_000_000,
  updated: 1_700_000_600_000,
  readable: true,
});

const page = (items: ReturnType<typeof collection>[]) => ({
  totalCount: items.length,
  page: 1,
  lastPage: 1,
  query: '',
  items: items,
});

function routes(overrides: Route[] = []): Route[] {
  return [
    ...overrides,
    { method: 'GET', match: /\/projects$/, json: PROJECTS },
    { method: 'GET', match: /\/projects\/elibrary\/link-roles$/, json: LINK_ROLES },
    { method: 'GET', match: /\/settings\/diff\/names/, json: CONFIGURATIONS },
    { method: 'GET', match: /\/projects\/elibrary\/collections\/search/, json: page([collection('c1', 'elibrary')]) },
    {
      method: 'GET',
      match: /\/projects\/drivepilot\/collections\/search/,
      json: page([collection('c2', 'drivepilot'), collection('c3', 'drivepilot')]),
    },
  ];
}

const columns = () => Array.from(document.querySelectorAll('.columns .column'));
const radios = (side: 'source' | 'target') =>
  Array.from(document.querySelectorAll<HTMLInputElement>(`input[name="${side}-collection"]`));
const compareButton = () => document.querySelector<HTMLButtonElement>('#compare-items')!;

async function renderPage(fetchMock: FetchMock = installFetchMock(routes())) {
  render(<CollectionsPickerPage />);
  await vi.waitFor(() => expect(radios('source').length).toBe(1));
  await vi.waitFor(() => expect(radios('target').length).toBe(2));
  return fetchMock;
}

beforeEach(() => {
  window.history.replaceState({}, '', '?topic=compare-collections&sourceProjectId=elibrary&targetProjectId=drivepilot');
});

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
  window.history.replaceState({}, '', origUrl);
});

describe('CollectionsPickerPage', () => {
  it('renders a table per side with the four widget columns', async () => {
    await renderPage();

    expect(columns().length).toBe(2);
    const headers = Array.from(columns()[0].querySelectorAll('.items-table th')).map((th) => th.textContent);
    expect(headers).toEqual(['', 'Name', 'Author', 'Created', 'Updated']);

    const cells = Array.from(columns()[0].querySelectorAll('.table-content-row td')).map((td) => td.textContent);
    expect(cells[1]).toBe('Collection c1');
    expect(cells[2]).toBe('John Doe');
    // The dates are rendered in the browser's locale, so only their presence is asserted here
    expect(cells[3]).not.toBe('');
    expect(cells[4]).not.toBe('');
  });

  it('searches the source project on the left and the target project on the right', async () => {
    const fetchMock = await renderPage();

    const searched = fetchMock.mock.calls.map(([url]) => String(url));
    expect(searched.some((url) => url.includes('/projects/elibrary/collections/search'))).toBe(true);
    expect(searched.some((url) => url.includes('/projects/drivepilot/collections/search'))).toBe(true);
  });

  it('puts the target project select above the right-hand table', async () => {
    await renderPage();

    expect(columns()[1].querySelector('#target-project-selector')).not.toBeNull();
    expect(columns()[0].querySelector('#target-project-selector')).toBeNull();
  });

  it('offers no "open in table" link, which collections have no view for', async () => {
    await renderPage();

    expect(document.querySelector('.open-in-table')).toBeNull();
  });

  it('enables Compare only once both sides have a collection', async () => {
    await renderPage();

    expect(compareButton().disabled).toBe(true);
    expect(compareButton().title).toBe(
      'Please, select one item in left table and one item in right table to be compared',
    );

    radios('source')[0].click();
    await vi.waitFor(() => expect(radios('source')[0].checked).toBe(true));
    expect(compareButton().disabled).toBe(true);

    radios('target')[0].click();

    await vi.waitFor(() => expect(compareButton().disabled).toBe(false));
  });

  it('opens the comparison with both collections and compareAs=Workitems', async () => {
    const open = vi.spyOn(window, 'open').mockReturnValue(null);
    await renderPage();

    radios('source')[0].click();
    radios('target')[1].click();
    await vi.waitFor(() => expect(compareButton().disabled).toBe(false));
    compareButton().click();

    expect(open).toHaveBeenCalledWith(
      '/polarion/diff-tool-app/ui/app/collections.html' +
        '?sourceProjectId=elibrary&sourceCollectionId=c1&targetProjectId=drivepilot&targetCollectionId=c3' +
        '&linkRole=relates_to&config=Default&compareAs=Workitems',
      '_blank',
    );
  });

  it('applies each side query independently', async () => {
    const fetchMock = await renderPage();

    const targetQuery = document.querySelector<HTMLInputElement>('#target-query-input')!;
    Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value')!.set!.call(targetQuery, 'name:release*');
    targetQuery.dispatchEvent(new Event('input', { bubbles: true }));
    targetQuery.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }));

    await vi.waitFor(() =>
      expect(
        fetchMock.mock.calls.some(
          ([url]) =>
            String(url).includes('drivepilot/collections/search') && String(url).includes('query=name%3Arelease*'),
        ),
      ).toBe(true),
    );
    expect(window.location.search).toContain('targetQuery=name%3Arelease*');
    expect(window.location.search).not.toContain('sourceQuery');
  });

  it('drops a chosen collection when its table is re-queried', async () => {
    await renderPage();

    radios('source')[0].click();
    radios('target')[0].click();
    await vi.waitFor(() => expect(compareButton().disabled).toBe(false));

    const apply = Array.from(columns()[1].querySelectorAll<HTMLButtonElement>('button')).find(
      (button) => button.textContent === 'Apply',
    )!;
    const targetQuery = document.querySelector<HTMLInputElement>('#target-query-input')!;
    Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value')!.set!.call(targetQuery, 'name:x');
    targetQuery.dispatchEvent(new Event('input', { bubbles: true }));
    apply.click();

    await vi.waitFor(() => expect(compareButton().disabled).toBe(true));
  });

  it('does not search the left side outside a project scope', async () => {
    window.history.replaceState({}, '', '?topic=compare-collections&sourceProjectId=&targetProjectId=drivepilot');
    const fetchMock = installFetchMock(routes());
    render(<CollectionsPickerPage />);

    await vi.waitFor(() =>
      expect(columns()[0].querySelector('.table-empty-cell')!.textContent).toBe(
        'Open this page in a project to select a collection',
      ),
    );
    expect(fetchMock.mock.calls.some(([url]) => String(url).includes('/elibrary/collections/search'))).toBe(false);
  });
});
