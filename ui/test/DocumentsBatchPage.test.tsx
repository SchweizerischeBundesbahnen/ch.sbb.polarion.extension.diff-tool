import { pageViolations } from '@sbb-polarion/react-sbb-polarion/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import DocumentsBatchPage from '../src/topics/DocumentsBatchPage';
import type { BatchDiffExport } from '../src/topics/batch/useBatchDiffExport';
import { setFieldValue } from './formextHelpers';
import { type FetchMock, type Route, installFetchMock, jsonResponse } from './mockFetch';

// The "Documents diff to PDF" topic: the documents table, pairing each selected document with its counterpart in the
// target project, the revisions of a pair, and what is handed to the batch runner. The runner itself is
// replaced here; useBatchDiffExport.test.tsx covers it.

const origUrl = window.location.pathname + window.location.search;

const PROJECTS = [
  { id: 'elibrary', name: 'E-Library' },
  { id: 'drivepilot', name: 'Drive Pilot' },
];

const LINK_ROLES = [{ id: 'relates_to', name: 'relates to', oppositeName: 'is related to' }];

const CONFIGURATIONS = [{ name: 'Default', scope: '' }];

const document_ = (spaceId: string, id: string, title: string, spaceTitle: string = spaceId) => ({
  id: id,
  projectId: 'elibrary',
  spaceId: spaceId,
  spaceTitle: spaceTitle,
  title: title,
  type: { id: 'req', name: 'Requirements', iconUrl: null },
  status: { id: 'draft', name: 'Draft', iconUrl: null },
  updated: 1_780_000_000_000,
  readable: true,
});

const SEARCH = {
  totalCount: 3,
  page: 1,
  lastPage: 1,
  query: 'project.id:elibrary',
  items: [
    // The Design space has a title of its own, the Specification space does not
    document_('Design', 'Catalog Design', 'Catalog Design', 'Design Documents'),
    document_('Specification', 'Catalog Specification', 'Catalog Specification'),
    { id: 'Secret', projectId: 'elibrary', readable: false, unavailableMessage: 'You cannot read this item' },
  ],
};

const REVISIONS = [
  { name: '120', baselineName: 'db. Release 1' },
  { name: '100', baselineName: null },
];

/** The target project has the Design document only. */
function resolve(_url: string, init?: RequestInit): Response {
  const asked = JSON.parse((init?.body as string) ?? '[]') as { spaceId: string; name: string }[];
  return jsonResponse(
    asked.map((document) =>
      document.spaceId === 'Design'
        ? { ...document, found: true, spaceTitle: 'Design Documents', title: `${document.name} (target)` }
        : { ...document, found: false, unavailableMessage: 'Document not found' },
    ),
  );
}

function routes(overrides: Route[] = []): Route[] {
  return [
    ...overrides,
    { method: 'GET', match: /\/projects$/, json: PROJECTS },
    { method: 'GET', match: /\/projects\/elibrary\/link-roles$/, json: LINK_ROLES },
    { method: 'GET', match: /\/settings\/diff\/names/, json: CONFIGURATIONS },
    { method: 'GET', match: /\/projects\/elibrary\/documents\/search/, json: SEARCH },
    {
      method: 'GET',
      match: /\/projects\/drivepilot\/documents\/search/,
      json: {
        ...SEARCH,
        items: [{ ...document_('Other', 'Replacement', 'Replacement', 'Other Space'), projectId: 'drivepilot' }],
      },
    },
    { method: 'POST', match: /\/documents\/resolve$/, respond: resolve },
    { method: 'GET', match: /\/revisions$/, json: REVISIONS },
  ];
}

function stubBatch(): BatchDiffExport {
  return {
    state: { status: 'closed', rows: [], processed: 0, errors: false },
    start: vi.fn(),
    stop: vi.fn(),
    close: vi.fn(),
  };
}

const rows = () => Array.from(document.querySelectorAll<HTMLTableRowElement>('.items-table .table-content-row'));
const checkbox = (index: number) => rows()[index].querySelector<HTMLInputElement>('input.select-item')!;
const exportButton = () => document.querySelector<HTMLButtonElement>('#compare-items')!;
const cellText = (row: number, column: number) => rows()[row].querySelectorAll('td')[column].textContent;

async function renderPage(batch: BatchDiffExport = stubBatch(), fetchMock: FetchMock = installFetchMock(routes())) {
  render(<DocumentsBatchPage batch={batch} />);
  await vi.waitFor(() => expect(rows()).toHaveLength(3));
  return fetchMock;
}

function requests(fetchMock: FetchMock, pattern: RegExp) {
  return fetchMock.mock.calls.filter(([input]) => pattern.test(typeof input === 'string' ? input : input.toString()));
}

beforeEach(() => {
  window.history.replaceState(
    {},
    '',
    '?topic=documents-diff-to-pdf&sourceProjectId=elibrary&targetProjectId=drivepilot&linkRole=relates_to',
  );
});

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
  window.history.replaceState({}, '', origUrl);
});

describe('DocumentsBatchPage', () => {
  it('lists the documents of the project, ordered as the server sends them', async () => {
    await renderPage();

    const headers = () => Array.from(document.querySelectorAll('.items-table th')).map((th) => th.textContent);
    await vi.waitFor(() => expect(headers()[6]).toBe('Target document in "Drive Pilot" project'));
    expect(headers()).toEqual([
      '',
      'Space',
      'Document',
      'Type',
      'Status',
      'Revision',
      'Target document in "Drive Pilot" project',
      'Target revision',
    ]);
    expect(cellText(0, 1)).toContain('Design Documents');
    expect(cellText(0, 2)).toBe('Catalog Design');
    expect(rows()[2].textContent).toBe('You cannot read this item');
  });

  it('shows a title with its ID below it only where they differ', async () => {
    await renderPage();
    await vi.waitFor(() => expect(cellText(0, 6)).toContain('(target)'));

    const cell = (row: number, column: number) => rows()[row].querySelectorAll('td')[column];
    const secondary = (row: number, column: number) => cell(row, column).querySelector('.secondary-id')?.textContent;

    // Space: the Design space has a title, the Specification space does not
    expect(cell(0, 1).textContent).toBe('Design DocumentsDesign');
    expect(secondary(0, 1)).toBe('Design');
    expect(cell(1, 1).textContent).toBe('Specification');
    expect(secondary(1, 1)).toBeUndefined();
    // Document: the title is the name
    expect(secondary(0, 2)).toBeUndefined();
    // Target: the space title and the document title, with the IDs below
    expect(cell(0, 6).querySelector('.title-with-id')!.firstChild!.textContent).toBe(
      'Design Documents / Catalog Design (target)',
    );
    expect(secondary(0, 6)).toBe('Design / Catalog Design');
  });

  it('shows the name below a document title which differs from it', async () => {
    const search = { ...SEARCH, items: [document_('Design', 'catalog-design', 'Catalog Design')] };
    installFetchMock(routes([{ method: 'GET', match: /\/projects\/elibrary\/documents\/search/, json: search }]));
    render(<DocumentsBatchPage batch={stubBatch()} />);
    await vi.waitFor(() => expect(rows()).toHaveLength(1));

    const documentCell = rows()[0].querySelectorAll('td')[2];
    expect(documentCell.querySelector('a')!.textContent).toBe('Catalog Design');
    expect(documentCell.querySelector('.secondary-id')!.textContent).toBe('catalog-design');
  });

  it('shows a target whose titles are its IDs on one line', async () => {
    const sameAsIds = (_url: string, init?: RequestInit) =>
      jsonResponse(
        (JSON.parse((init?.body as string) ?? '[]') as { spaceId: string; name: string }[]).map((asked) => ({
          ...asked,
          found: true,
          spaceTitle: asked.spaceId,
          title: asked.name,
        })),
      );
    await renderPage(
      stubBatch(),
      installFetchMock(routes([{ method: 'POST', match: /\/documents\/resolve$/, respond: sameAsIds }])),
    );

    await vi.waitFor(() => expect(cellText(0, 6)).toBe('Design / Catalog Design'));
    expect(rows()[0].querySelectorAll('td')[6].querySelector('.secondary-id')).toBeNull();
  });

  it('shows the IDs of a target whose space title alone differs', async () => {
    const spaceTitled = (_url: string, init?: RequestInit) =>
      jsonResponse(
        (JSON.parse((init?.body as string) ?? '[]') as { spaceId: string; name: string }[]).map((asked) => ({
          ...asked,
          found: true,
          spaceTitle: `${asked.spaceId} Documents`,
          title: asked.name,
        })),
      );
    await renderPage(
      stubBatch(),
      installFetchMock(routes([{ method: 'POST', match: /\/documents\/resolve$/, respond: spaceTitled }])),
    );

    const targetCell = () => rows()[0].querySelectorAll('td')[6];
    await vi.waitFor(() =>
      expect(targetCell().querySelector('.secondary-id')?.textContent).toBe('Design / Catalog Design'),
    );
    expect(targetCell().querySelector('.title-with-id')!.firstChild!.textContent).toBe(
      'Design Documents / Catalog Design',
    );
  });

  it('offers a target document of a space without a title by its space ID', async () => {
    const candidate = {
      ...document_('Other', 'Replacement', 'Replacement'),
      projectId: 'drivepilot',
      spaceTitle: null,
    };
    await renderPage(
      stubBatch(),
      installFetchMock(
        routes([
          {
            method: 'GET',
            match: /\/projects\/drivepilot\/documents\/search/,
            json: { ...SEARCH, items: [candidate] },
          },
        ]),
      ),
    );
    await vi.waitFor(() => expect(cellText(1, 6)).toContain('Document not found'));

    rows()[1].querySelector<HTMLButtonElement>('.target-cell .icon-button')!.click();
    const option = await vi.waitFor(() => {
      const found = rows()[1].querySelector('.target-cell select option[value="Other/Replacement"]');
      expect(found).not.toBeNull();
      return found!;
    });
    expect(option.textContent).toBe('Other / Replacement');
  });

  it('pairs each document with the document of the same space and name in the target project', async () => {
    const fetchMock = await renderPage();

    await vi.waitFor(() => expect(cellText(0, 6)).toContain('Design Documents / Catalog Design (target)'));
    expect(cellText(1, 6)).toContain('Document not found');
    // One request for the whole page, and the unreadable row is not asked for
    const lookups = requests(fetchMock, /documents\/resolve/);
    expect(lookups).toHaveLength(1);
    expect(JSON.parse(lookups[0][1]!.body as string)).toEqual([
      { spaceId: 'Design', name: 'Catalog Design' },
      { spaceId: 'Specification', name: 'Catalog Specification' },
    ]);
  });

  it('exports nothing until a document is selected', async () => {
    await renderPage();

    expect(exportButton().disabled).toBe(true);
    expect(exportButton().title).toBe('Please, select at least one document to be exported');
  });

  it('hands the selected pairs and the options to the runner', async () => {
    const batch = stubBatch();
    await renderPage(batch);
    await vi.waitFor(() => expect(cellText(0, 6)).toContain('(target)'));

    checkbox(0).click();
    await vi.waitFor(() => expect(exportButton().disabled).toBe(false));
    expect(exportButton().textContent).toBe('Export diff to PDF');
    exportButton().click();

    expect(batch.start).toHaveBeenCalledWith(
      [
        {
          source: {
            projectId: 'elibrary',
            spaceId: 'Design',
            spaceTitle: 'Design Documents',
            name: 'Catalog Design',
            title: 'Catalog Design',
          },
          target: {
            projectId: 'drivepilot',
            spaceId: 'Design',
            spaceTitle: 'Design Documents',
            name: 'Catalog Design',
            title: 'Catalog Design (target)',
          },
          sourceRevision: '',
          targetRevision: '',
        },
      ],
      expect.objectContaining({
        linkRole: 'relates_to',
        config: 'Default',
        compareAs: 'Workitems',
        pdf: { paperSize: 'A4', orientation: 'landscape' },
      }),
    );
  });

  it('blocks the export of a document without a counterpart', async () => {
    await renderPage();
    await vi.waitFor(() => expect(cellText(1, 6)).toContain('Document not found'));

    checkbox(1).click();

    await vi.waitFor(() =>
      expect(exportButton().title).toBe('Please, pick a target document for each selected document'),
    );
    expect(exportButton().disabled).toBe(true);
    expect(document.querySelector('.selection-summary')!.textContent).toContain('1 without a target document');
  });

  it('lets another target document be picked', async () => {
    const batch = stubBatch();
    await renderPage(batch);
    await vi.waitFor(() => expect(cellText(1, 6)).toContain('Document not found'));
    checkbox(1).click();

    rows()[1].querySelector<HTMLButtonElement>('.target-cell .icon-button')!.click();
    const select = await vi.waitFor(() => {
      const found = rows()[1].querySelector<HTMLSelectElement>('.target-cell select');
      expect(found?.querySelector('option[value="Other/Replacement"]') ?? null).not.toBeNull();
      return found!;
    });
    // The picker offers titles only
    expect(select.querySelector('option[value="Other/Replacement"]')!.textContent).toBe('Other Space / Replacement');
    setFieldValue(select, 'Other/Replacement');

    await vi.waitFor(() => expect(cellText(1, 6)).toContain('Other Space / Replacement'));
    await vi.waitFor(() => expect(exportButton().disabled).toBe(false));
    exportButton().click();
    const [pairs] = vi.mocked(batch.start).mock.calls[0];
    expect(pairs[0].target).toEqual({
      projectId: 'drivepilot',
      spaceId: 'Other',
      spaceTitle: 'Other Space',
      name: 'Replacement',
      title: 'Replacement',
    });
  });

  it('compares a selected document at the revision chosen for it', async () => {
    const batch = stubBatch();
    const fetchMock = await renderPage(batch);
    await vi.waitFor(() => expect(cellText(0, 6)).toContain('(target)'));
    expect(requests(fetchMock, /\/revisions$/)).toHaveLength(0);

    checkbox(0).click();
    const select = await vi.waitFor(() => {
      const found = rows()[0].querySelectorAll('td')[5].querySelector<HTMLSelectElement>('select');
      expect(found?.querySelector('option[value="120"]') ?? null).not.toBeNull();
      return found!;
    });
    expect(select.querySelector('option[value="120"]')!.textContent).toBe('120 | db. Release 1');
    setFieldValue(select, '120');

    await vi.waitFor(() => expect(exportButton().disabled).toBe(false));
    exportButton().click();
    const [pairs] = vi.mocked(batch.start).mock.calls[0];
    expect(pairs[0].sourceRevision).toBe('120');
    expect(pairs[0].targetRevision).toBe('');
  });

  it('asks for a link role to pair documents of different projects', async () => {
    window.history.replaceState(
      {},
      '',
      '?topic=documents-diff-to-pdf&sourceProjectId=elibrary&targetProjectId=drivepilot',
    );
    await renderPage();
    await vi.waitFor(() => expect(cellText(0, 6)).toContain('(target)'));

    checkbox(0).click();

    await vi.waitFor(() =>
      expect(exportButton().title).toBe(
        'Please, select the link role which pairs the work items of different documents',
      ),
    );

    // The fields comparison pairs no work items
    setFieldValue(document.querySelector<HTMLSelectElement>('#batch-compare-as')!, 'Fields');
    await vi.waitFor(() => expect(exportButton().disabled).toBe(false));
  });

  it('pairs a document with itself in its own project, which needs no link role', async () => {
    window.history.replaceState({}, '', '?topic=documents-diff-to-pdf&sourceProjectId=elibrary');
    const fetchMock = installFetchMock(
      routes([
        {
          method: 'POST',
          match: /\/projects\/elibrary\/documents\/resolve$/,
          respond: (_url, init) =>
            jsonResponse(
              (JSON.parse(init!.body as string) as { spaceId: string; name: string }[]).map((document) => ({
                ...document,
                found: true,
                title: document.name,
              })),
            ),
        },
      ]),
    );
    const batch = stubBatch();
    await renderPage(batch, fetchMock);
    await vi.waitFor(() => expect(cellText(0, 6)).toContain('Design / Catalog Design'));

    checkbox(0).click();

    await vi.waitFor(() => expect(exportButton().disabled).toBe(false));
  });

  it('keeps the selection when the query changes', async () => {
    await renderPage();
    checkbox(0).click();
    await vi.waitFor(() => expect(document.querySelector('.selection-summary')).not.toBeNull());

    window.history.pushState({}, '', `${window.location.search}&sourceQuery=type%3Areq`);
    window.dispatchEvent(new PopStateEvent('popstate'));

    await vi.waitFor(() => expect(document.querySelector('.selection-summary .count')!.textContent).toBe('1'));
    document.querySelector<HTMLButtonElement>('.selection-summary button')!.click();
    await vi.waitFor(() => expect(document.querySelector('.selection-summary')).toBeNull());
  });

  it('offers only the options of the chosen comparison', async () => {
    await renderPage();

    expect(document.querySelector('#batch-hideChaptersIfNoDifference')).not.toBeNull();
    expect(document.querySelector('#batch-compareOnlyMutualFields')).toBeNull();

    setFieldValue(document.querySelector<HTMLSelectElement>('#batch-compare-as')!, 'Fields');

    await vi.waitFor(() => expect(document.querySelector('#batch-compareOnlyMutualFields')).not.toBeNull());
    expect(document.querySelector('#batch-hideChaptersIfNoDifference')).toBeNull();
  });

  it('shows the link role, the configuration and the diff view among the comparison options', async () => {
    await renderPage();

    expect(document.querySelector('#link-role-selector')).toBeNull();
    expect(document.querySelector('#config-selector')).toBeNull();
    const [comparison, pdf] = Array.from(document.querySelectorAll('.export-options fieldset'));
    const comparisonIds = Array.from(comparison.querySelectorAll('.option-row select')).map((select) => select.id);
    expect(comparisonIds).toEqual(['batch-compare-as', 'batch-link-role', 'batch-config', 'batch-diff-view']);
    expect(pdf.querySelector('#batch-diff-view')).toBeNull();
    await vi.waitFor(() =>
      expect(document.querySelector<HTMLSelectElement>('#batch-link-role')!.value).toBe('relates_to'),
    );
  });

  it('offers the link role only to the work items comparison', async () => {
    await renderPage();

    setFieldValue(document.querySelector<HTMLSelectElement>('#batch-compare-as')!, 'Fields');
    await vi.waitFor(() => expect(document.querySelector('#batch-link-role')).toBeNull());

    setFieldValue(document.querySelector<HTMLSelectElement>('#batch-compare-as')!, 'Workitems');
    await vi.waitFor(() => expect(document.querySelector('#batch-link-role')).not.toBeNull());
  });
});

describe('DocumentsBatchPage, accessibility', () => {
  it('has no WCAG A/AA violations as loaded', async () => {
    await renderPage();
    await vi.waitFor(() => expect(cellText(0, 6)).toContain('(target)'));

    expect(await pageViolations()).toEqual([]);
  });

  it('has no WCAG A/AA violations with a document selected', async () => {
    await renderPage();
    await vi.waitFor(() => expect(cellText(0, 6)).toContain('(target)'));
    checkbox(0).click();
    await vi.waitFor(() => expect(exportButton().disabled).toBe(false));

    expect(await pageViolations()).toEqual([]);
  });
});
