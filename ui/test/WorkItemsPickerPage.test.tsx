import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import WorkItemsPickerPage from '../src/topics/WorkItemsPickerPage';
import { digestMessage } from '../src/topics/openWorkItemsDiff';
import { type FetchMock, type Route, installFetchMock } from './mockFetch';

// Behaviour of the port of multiple-work-items.jsp + WorkItemsDiffWidgetRenderer: the table, the query row,
// the selection rules and the handoff to workitems.html.

const origUrl = window.location.pathname + window.location.search;

const PROJECTS = [
  { id: 'elibrary', name: 'E-Library' },
  { id: 'drivepilot', name: 'Drive Pilot' },
];

const LINK_ROLES = [
  { id: 'relates_to', name: 'relates to', oppositeName: 'is related to' },
  { id: 'depends_on', name: 'depends on', oppositeName: 'is dependent on' },
];

const CONFIGURATIONS = [
  { name: 'Default', scope: '' },
  { name: 'Strict', scope: 'project/elibrary/' },
];

const workItem = (id: string) => ({
  id: id,
  projectId: 'elibrary',
  title: `Title of ${id}`,
  type: { id: 'task', name: 'Task', iconUrl: '/polarion/icons/task.svg' },
  status: { id: 'open', name: 'Open', iconUrl: null },
  severity: { id: 'major', name: 'Major', iconUrl: null },
  readable: true,
});

const PAGE_ONE = {
  totalCount: 3,
  page: 1,
  lastPage: 2,
  query: '',
  items: [workItem('EL-1'), workItem('EL-2')],
};

function routes(overrides: Route[] = []): Route[] {
  return [
    ...overrides,
    { method: 'GET', match: /\/projects$/, json: PROJECTS },
    { method: 'GET', match: /\/projects\/elibrary\/link-roles$/, json: LINK_ROLES },
    { method: 'GET', match: /\/settings\/diff\/names/, json: CONFIGURATIONS },
    { method: 'GET', match: /\/projects\/elibrary\/workitems\/search/, json: PAGE_ONE },
  ];
}

async function renderPage(fetchMock: FetchMock = installFetchMock(routes())) {
  render(<WorkItemsPickerPage />);
  await vi.waitFor(() => expect(document.querySelectorAll('.items-table .table-content-row').length).toBe(2));
  return fetchMock;
}

const rows = () => Array.from(document.querySelectorAll<HTMLTableRowElement>('.items-table .table-content-row'));
const checkboxes = () => Array.from(document.querySelectorAll<HTMLInputElement>('.items-table input.select-item'));
const compareButton = () => document.querySelector<HTMLButtonElement>('#compare-items')!;
const queryInput = () => document.querySelector<HTMLInputElement>('#source-query-input')!;

function setFieldValue(element: HTMLInputElement, value: string): void {
  Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value')!.set!.call(element, value);
  element.dispatchEvent(new Event('input', { bubbles: true }));
}

beforeEach(() => {
  window.history.replaceState({}, '', '?topic=compare-work-items&sourceProjectId=elibrary');
});

afterEach(() => {
  cleanup();
  localStorage.clear();
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
  window.history.replaceState({}, '', origUrl);
});

describe('WorkItemsPickerPage', () => {
  it('renders one row per work item with the five widget columns', async () => {
    await renderPage();

    const headers = Array.from(document.querySelectorAll('.items-table th')).map((th) => th.textContent);
    expect(headers).toEqual(['', 'ID', 'Title', 'Type', 'Status', 'Severity']);

    const cells = Array.from(rows()[0].querySelectorAll('td')).map((td) => td.textContent);
    expect(cells.slice(1)).toEqual(['EL-1', 'Title of EL-1', 'Task', 'Open', 'Major']);
  });

  it('links every ID into Polarion, behind the type icon Polarion shows there', async () => {
    await renderPage();

    const idCell = rows()[0].querySelectorAll('td')[1];
    expect(idCell.querySelector<HTMLAnchorElement>('a')!.getAttribute('href')).toBe(
      '/polarion/#/project/elibrary/workitem?id=EL-1',
    );
    expect(idCell.querySelector<HTMLImageElement>('img')!.getAttribute('src')).toBe('/polarion/icons/task.svg');
  });

  it('searches the project of the topic, with the default page size', async () => {
    const fetchMock = await renderPage();

    expect(
      fetchMock.mock.calls.some(([url]) =>
        String(url).includes('/projects/elibrary/workitems/search?page=1&recordsPerPage=20'),
      ),
    ).toBe(true);
  });

  it('offers the link roles with both directions of their name', async () => {
    await renderPage();

    const options = Array.from(document.querySelectorAll<HTMLOptionElement>('#link-role-selector option')).map(
      (option) => option.textContent,
    );
    expect(options).toContain('relates to / is related to');
  });

  it('preselects the first target project, link role and configuration', async () => {
    await renderPage();

    await vi.waitFor(() =>
      expect(document.querySelector<HTMLSelectElement>('#target-project-selector')!.value).toBe('elibrary'),
    );
    expect(document.querySelector<HTMLSelectElement>('#link-role-selector')!.value).toBe('relates_to');
    expect(document.querySelector<HTMLSelectElement>('#config-selector')!.value).toBe('Default');
  });

  it('keeps Compare disabled until something is selected', async () => {
    await renderPage();

    expect(compareButton().disabled).toBe(true);
    expect(compareButton().title).toBe('Please, select at least one item to be compared');

    checkboxes()[0].click();

    await vi.waitFor(() => expect(compareButton().disabled).toBe(false));
  });

  it('selects and clears every row on this page with the header checkbox', async () => {
    await renderPage();
    const selectAll = document.querySelector<HTMLInputElement>('.items-table input.select-all')!;

    selectAll.click();
    await vi.waitFor(() => expect(checkboxes().every((checkbox) => checkbox.checked)).toBe(true));

    selectAll.click();
    await vi.waitFor(() => expect(checkboxes().some((checkbox) => checkbox.checked)).toBe(false));
  });

  it('unticks the header checkbox as soon as one row is deselected', async () => {
    await renderPage();
    const selectAll = document.querySelector<HTMLInputElement>('.items-table input.select-all')!;

    selectAll.click();
    await vi.waitFor(() => expect(selectAll.checked).toBe(true));

    checkboxes()[0].click();

    await vi.waitFor(() => expect(selectAll.checked).toBe(false));
  });

  it('hands the selection over to the viewer through localStorage', async () => {
    const open = vi.spyOn(window, 'open').mockReturnValue(null);
    await renderPage();

    checkboxes()[0].click();
    checkboxes()[1].click();
    await vi.waitFor(() => expect(compareButton().disabled).toBe(false));
    compareButton().click();

    const hash = await digestMessage('EL-1,EL-2');
    await vi.waitFor(() => expect(localStorage.getItem(`${hash}_ids`)).toBe('EL-1,EL-2'));
    expect(open).toHaveBeenCalledWith(
      '/polarion/diff-tool-app/ui/app/workitems.html' +
        `?sourceProjectId=elibrary&targetProjectId=elibrary&linkRole=relates_to&config=Default&ids=${hash}`,
      '_blank',
    );
  });

  it('applies the query typed into the input and resets the page', async () => {
    const fetchMock = await renderPage();

    setFieldValue(queryInput(), 'type:task');
    queryInput().dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }));

    await vi.waitFor(() =>
      expect(fetchMock.mock.calls.some(([url]) => String(url).includes('query=type%3Atask'))).toBe(true),
    );
    expect(window.location.search).toContain('sourceQuery=type%3Atask');
  });

  it('drops the query and the page size on Reset', async () => {
    await renderPage();
    window.history.replaceState(
      {},
      '',
      '?topic=compare-work-items&sourceProjectId=elibrary&sourceQuery=x&sourceRecordsPerPage=5',
    );

    const reset = Array.from(document.querySelectorAll<HTMLButtonElement>('.query button')).find(
      (button) => button.textContent === 'Reset',
    )!;
    reset.click();

    await vi.waitFor(() => expect(window.location.search).not.toContain('sourceQuery'));
    expect(window.location.search).toContain('sourceRecordsPerPage=20');
  });

  it('clears the selection when the page changes', async () => {
    await renderPage();

    checkboxes()[0].click();
    await vi.waitFor(() => expect(compareButton().disabled).toBe(false));

    document.querySelector<HTMLButtonElement>('.paginator .paginator-link')!.click();

    await vi.waitFor(() => expect(compareButton().disabled).toBe(true));
  });

  it('shows the counts and the query popup in the footer', async () => {
    await renderPage(
      installFetchMock(
        routes([{ method: 'GET', match: /workitems\/search/, json: { ...PAGE_ONE, query: 'type:task' } }]),
      ),
    );

    // Polarion's own wording for a page out of a larger result set
    expect(document.querySelector('.table-counts')!.textContent).toBe('Showing 2 items of 3 found');
    expect(document.querySelector('.query-text')).toBeNull();

    document.querySelector<HTMLButtonElement>('.table-footer button.footer-icon')!.click();

    await vi.waitFor(() => expect(document.querySelector('.query-text')!.textContent).toBe('type:task'));
  });

  it('reports a row the user may not read instead of its fields', async () => {
    await renderPage(
      installFetchMock(
        routes([
          {
            method: 'GET',
            match: /workitems\/search/,
            json: {
              ...PAGE_ONE,
              items: [
                workItem('EL-1'),
                { id: 'EL-2', projectId: 'elibrary', readable: false, unavailableMessage: 'You cannot read this item' },
              ],
            },
          },
        ]),
      ),
    );

    expect(rows()[1].querySelector('.table-not-readable-cell')!.textContent).toBe('You cannot read this item');
    // ...and it cannot be selected, so it can never reach the comparison
    expect(rows()[1].querySelector('input.select-item')).toBeNull();
  });

  it('surfaces the message of a rejected query', async () => {
    installFetchMock(
      routes([
        {
          method: 'GET',
          match: /workitems\/search/,
          status: 400,
          json: { errorMessage: "Cannot parse 'type:('" },
        },
      ]),
    );
    window.history.replaceState({}, '', '?topic=compare-work-items&sourceProjectId=elibrary&sourceQuery=type%3A(');
    render(<WorkItemsPickerPage />);

    await vi.waitFor(() => expect(document.querySelector('.alert-error')!.textContent).toBe("Cannot parse 'type:('"));
  });

  it('does not search at all outside a project scope', async () => {
    window.history.replaceState({}, '', '?topic=compare-work-items&sourceProjectId=');
    const fetchMock = installFetchMock(routes());
    render(<WorkItemsPickerPage />);

    await vi.waitFor(() =>
      expect(document.querySelector('.table-empty-cell')!.textContent).toBe(
        'Open this page in a project to select work items',
      ),
    );
    expect(fetchMock.mock.calls.some(([url]) => String(url).includes('/workitems/search'))).toBe(false);
  });
});
