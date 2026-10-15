import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup } from 'vitest-browser-react';
import DocumentsPage from '../src/pages/DocumentsPage';
import WorkItemsPage from '../src/pages/WorkItemsPage';
import { setFieldValue } from './formextHelpers';
import { fixture, openControlPane, pairDiff, renderViewer, restoreUrl } from './viewerHarness';

// The "Diff view" dropdown of the configuration pane. The view lives in the app context, not in the query
// string, and switching it only restyles the page: no diff is fetched again and no selection is lost.

const DOCUMENTS_URL =
  '/documents?sourceProjectId=elibrary&sourceSpaceId=Specification&sourceDocument=Catalog%20Specification' +
  '&targetProjectId=drivepilot&targetSpaceId=Design&targetDocument=Catalog%20Design&linkRole=relates_to';

const WORKITEMS_URL =
  '/workitems?sourceProjectId=elibrary&targetProjectId=elibrary&linkRole=relates_to&configuration=Default' +
  '&sourceRecordsPerPage=20&sourcePage=1&config=Default&ids=1';

const PAIR_TICKER = '[data-testid="EL-4977_DP-11559"] .merge-ticker input[type="checkbox"]';

function renderDocuments() {
  renderViewer(
    DOCUMENTS_URL,
    [
      { method: 'POST', match: /\/diff\/documents$/, json: fixture('documents-diff.json') ?? {} },
      { method: 'POST', match: /\/diff\/document-workitems$/, respond: pairDiff },
      { method: 'POST', match: /\/diff\/documents-fields$/, json: fixture('documents-fields.json') ?? {} },
    ],
    <DocumentsPage />,
  );
}

async function documentsLoaded() {
  await vi.waitFor(() => expect(document.querySelector('.header .merge-pane')).not.toBeNull(), { timeout: 10000 });
  await vi.waitFor(() => expect(document.querySelector(PAIR_TICKER)).not.toBeNull());
}

const diffBody = () => document.querySelector('#diff-body');

const viewSelect = () => document.querySelector<HTMLSelectElement>('#diff-view')!;

const diffRequests = () =>
  vi.mocked(fetch).mock.calls.filter(([input]) => /\/diff\//.test(typeof input === 'string' ? input : input.toString()))
    .length;

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  restoreUrl();
});

describe('Diff view switch', () => {
  it('opens side by side', async () => {
    renderDocuments();
    await documentsLoaded();
    await openControlPane();

    expect(viewSelect().value).toBe('side-by-side');
    expect(diffBody()).toHaveClass('diff-view-side-by-side');
    expect(diffBody()).not.toHaveClass('diff-view-unified');
  });

  it('switches to unified without reloading the diff or losing the selection', async () => {
    renderDocuments();
    await documentsLoaded();
    document.querySelector<HTMLInputElement>(PAIR_TICKER)!.click();
    await vi.waitFor(() => expect(document.querySelector<HTMLInputElement>(PAIR_TICKER)!.checked).toBe(true));
    await openControlPane();
    const requestsBefore = diffRequests();

    setFieldValue(viewSelect(), 'unified');

    await vi.waitFor(() => expect(diffBody()).toHaveClass('diff-view-unified'));
    expect(diffBody()).not.toHaveClass('diff-view-side-by-side');
    expect(document.querySelector<HTMLInputElement>(PAIR_TICKER)!.checked).toBe(true);
    expect(diffRequests()).toBe(requestsBefore);
  });

  it('keeps the view when the documents are compared as something else', async () => {
    renderDocuments();
    await documentsLoaded();
    await openControlPane();
    setFieldValue(viewSelect(), 'unified');
    await vi.waitFor(() => expect(diffBody()).toHaveClass('diff-view-unified'));

    setFieldValue(document.querySelector<HTMLSelectElement>('#target-type')!, 'Fields');

    await vi.waitFor(() => expect(document.querySelector('[data-testid="version-field-diff"]')).not.toBeNull(), {
      timeout: 10000,
    });
    expect(diffBody()).toHaveClass('diff-view-unified');
    expect(viewSelect().value).toBe('unified');
  });

  it('is offered by the work items viewer too', async () => {
    renderViewer(
      WORKITEMS_URL,
      [
        { method: 'POST', match: /\/diff\/workitems-pairs$/, json: fixture('workItems-pairs.json') ?? {} },
        { method: 'POST', match: /\/diff\/detached-workitems$/, json: fixture('detached-workitems.json') ?? {} },
      ],
      <WorkItemsPage />,
    );
    await vi.waitFor(() => expect(document.querySelector('.header .merge-pane')).not.toBeNull(), { timeout: 10000 });
    await openControlPane();

    setFieldValue(viewSelect(), 'unified');

    await vi.waitFor(() => expect(diffBody()).toHaveClass('diff-view-unified'));
  });

  it('shows the merge button with an arrow side by side, and without one in the unified view', async () => {
    renderDocuments();
    await documentsLoaded();
    const mergeButton = () => document.querySelector<HTMLButtonElement>('.merge-pane .merge-button .btn')!;
    expect(mergeButton()).toHaveTextContent('Merge into target');
    expect(mergeButton().querySelector('svg')).not.toBeNull();

    await openControlPane();
    setFieldValue(viewSelect(), 'unified');

    await vi.waitFor(() => expect(mergeButton().querySelector('svg')).toBeNull());
    expect(mergeButton()).toHaveTextContent('Merge into target');
  });

  // axe accepts a label that names only the hidden native <select>, see the link role direction case in
  // DocumentsDiff.visual.test.tsx.
  it('names the diff view combobox after its label', async () => {
    renderDocuments();
    await documentsLoaded();
    await openControlPane();
    const trigger = await vi.waitFor(() => {
      const found = document.querySelector('#diff-view + .searchable-dropdown .sd-trigger');
      expect(found).not.toBeNull();
      return found!;
    });
    expect(trigger).toHaveAccessibleName('Diff view:');
  });
});

async function switchToUnified() {
  await openControlPane();
  setFieldValue(viewSelect(), 'unified');
  await vi.waitFor(() => expect(diffBody()).toHaveClass('diff-view-unified'));
}

const leaves = (scope: string) => [...document.querySelectorAll<HTMLElement>(`${scope} .diff-leaf`)];

describe('Unified field diff', () => {
  it('shows each field in one leaf, with the removed and the added parts', async () => {
    renderDocuments();
    await documentsLoaded();
    await switchToUnified();

    const pair = '[data-testid="EL-4977_DP-11559"] [data-testid="Description"]';
    await vi.waitFor(() => expect(leaves(pair)).toHaveLength(1));
    const leaf = leaves(pair)[0];
    expect(leaf).toHaveClass('unified');
    expect(leaf).toHaveTextContent('Some new Modified requirement');

    const removed = leaf.querySelector('.diff-html-removed')!;
    const added = leaf.querySelector('.diff-html-added')!;
    expect(getComputedStyle(removed).display).not.toBe('none');
    expect(getComputedStyle(added).display).not.toBe('none');
  });

  it('shows a work item missing in the target from its source diff', async () => {
    renderDocuments();
    await documentsLoaded();
    await switchToUnified();

    const pair = '[data-testid="EL-11600_NONE"] [data-testid="Title"]';
    await vi.waitFor(() => expect(leaves(pair)).toHaveLength(1));
    expect(leaves(pair)[0]).toHaveTextContent('Very important requirement');
  });

  it('goes back to two leaves side by side', async () => {
    renderDocuments();
    await documentsLoaded();
    await switchToUnified();

    setFieldValue(viewSelect(), 'side-by-side');

    const pair = '[data-testid="EL-4977_DP-11559"] [data-testid="Description"]';
    await vi.waitFor(() => expect(leaves(pair)).toHaveLength(2));
    expect(leaves(pair)[0]).toHaveClass('left');
    expect(leaves(pair)[0]).toHaveTextContent('Some new requirement');
    expect(leaves(pair)[1]).toHaveClass('right');
    expect(leaves(pair)[1]).toHaveTextContent('Modified requirement');
  });

  it('shows the document content blocks in one leaf, keeping their merge tickers', async () => {
    renderViewer(
      `${DOCUMENTS_URL}&config=Default&compareAs=Content`,
      [{ method: 'POST', match: /\/diff\/documents-content$/, json: fixture('documents-content.json') ?? {} }],
      <DocumentsPage />,
    );
    await vi.waitFor(() => expect(document.querySelector('.diff-viewer .merge-ticker')).not.toBeNull(), {
      timeout: 10000,
    });
    const viewers = document.querySelectorAll('.diff-viewer').length;
    await switchToUnified();

    await vi.waitFor(() => expect(document.querySelectorAll('.diff-leaf.unified')).toHaveLength(viewers));
    expect(document.querySelectorAll('.diff-leaf.left, .diff-leaf.right')).toHaveLength(0);
    expect(document.querySelectorAll('.diff-viewer .merge-ticker')).toHaveLength(viewers);
  });
});

const unifiedHeader = (pair: string) =>
  document.querySelector<HTMLElement>(`[data-testid="${pair}"] .wi-header.unified`);

describe('Unified pair header', () => {
  it('names the work item once and links the source, then the target', async () => {
    renderDocuments();
    await documentsLoaded();
    await switchToUnified();

    const header = unifiedHeader('EL-4977_DP-11559')!;
    expect(document.querySelectorAll('[data-testid="EL-4977_DP-11559"] .wi-header')).toHaveLength(1);
    expect(header.querySelector('.wi-title')).toHaveTextContent('New Requirement');
    const badges = [...header.querySelectorAll('.wi-badge')];
    expect(badges.map((badge) => badge.getAttribute('aria-label'))).toEqual([
      'Source WorkItem EL-4977',
      'Target WorkItem DP-11559',
    ]);
    expect(header.querySelector('.wi-tag')).toBeNull();
  });

  it('tags a work item that exists on one side only', async () => {
    renderDocuments();
    await documentsLoaded();
    await switchToUnified();

    expect(unifiedHeader('EL-156_NONE')!.querySelector('.wi-tag.only-in-source')).toHaveTextContent('Only in source');
    expect(unifiedHeader('NONE_DP-11570')!.querySelector('.wi-tag.only-in-target')).toHaveTextContent('Only in target');
  });

  it('tags a referenced work item with its revision', async () => {
    renderDocuments();
    await documentsLoaded();
    await switchToUnified();

    expect(unifiedHeader('EL-11575_EL-11575')!.querySelector('.wi-tag')).toHaveTextContent('Referenced, rev. 6480');
  });

  // The PDF export renders with WeasyPrint, which sizes an inline SVG by its attributes only: without
  // them, a Font Awesome icon prints hundreds of pixels wide.
  it('gives its icons a size the PDF export can read', async () => {
    renderDocuments();
    await documentsLoaded();
    await switchToUnified();

    const icons = [...document.querySelectorAll('.wi-header.unified svg')];
    expect(icons.length).toBeGreaterThan(0);
    for (const icon of icons) {
      expect(Number(icon.getAttribute('width'))).toBeGreaterThan(0);
      expect(Number(icon.getAttribute('height'))).toBeGreaterThan(0);
    }
  });

  it('keeps a chapter a heading of its level', async () => {
    renderDocuments();
    await documentsLoaded();
    await switchToUnified();

    const header = unifiedHeader('EL-145_DP-11550')!;
    expect(header).toHaveAttribute('role', 'heading');
    expect(header).toHaveAttribute('aria-level', '2');
  });

  it('keeps the target of every "moved to" and "moved from" link', async () => {
    renderDocuments();
    await documentsLoaded();
    await switchToUnified();

    const links = [...document.querySelectorAll<HTMLAnchorElement>('.wi-header.unified a.wi-tag.moved')];
    expect(links.length).toBeGreaterThan(0);
    for (const link of links) {
      expect(document.getElementById(link.getAttribute('href')!.substring(1))).not.toBeNull();
    }
  });
});

describe('Unified moved items', () => {
  it('shows the diff at the target position, with a link to the source position', async () => {
    renderDocuments();
    await documentsLoaded();
    await switchToUnified();

    const pair = '[data-testid="EL-158_DP-11563_reverse"]';
    const header = unifiedHeader('EL-158_DP-11563_reverse')!;
    expect(header).not.toHaveClass('moved-placeholder');
    expect(header).toHaveAttribute('role', 'heading');
    expect(header.querySelector('.wi-title')).toHaveTextContent('2.4 Performance Requirements');
    expect([...header.querySelectorAll('.wi-badge')].map((badge) => badge.textContent)).toEqual(['EL-158', 'DP-11563']);
    const moved = header.querySelector<HTMLAnchorElement>('a.wi-tag.moved')!;
    expect(moved).toHaveTextContent('Moved from 2.3');
    expect(moved.getAttribute('href')).toBe('#left2.3');
    expect(document.querySelector(`${pair} .merge-ticker input`)).not.toBeNull();
    await vi.waitFor(() => expect(document.querySelector(`${pair} .diff-leaf.unified`)).not.toBeNull());
  });

  it('shows the source position as a slim placeholder, linked to the target position', async () => {
    renderDocuments();
    await documentsLoaded();
    await switchToUnified();

    const pair = '[data-testid="EL-158_DP-11563_direct"]';
    const header = unifiedHeader('EL-158_DP-11563_direct')!;
    expect(header).toHaveClass('moved-placeholder');
    expect(header).not.toHaveAttribute('role');
    expect(header.querySelector('.wi-title')).toHaveTextContent('2.3 Performance Requirements');
    expect(header.querySelector('#left2\\.3')).not.toBeNull();
    expect(header.querySelector('a.wi-tag.moved')).toHaveTextContent('Moved to 2.4');
    expect(header.querySelector('a.wi-tag.moved')!.getAttribute('href')).toBe('#right2.4');
    expect(document.querySelector(`${pair} .merge-ticker input`)).toBeNull();
    expect(document.querySelector(`${pair} .floating-button`)).toBeNull();
    expect(document.querySelector(`${pair} .diff-leaf`)).toBeNull();
  });

  it('keeps the diff at the source position side by side', async () => {
    renderDocuments();
    await documentsLoaded();

    await vi.waitFor(() =>
      expect(document.querySelector('[data-testid="EL-158_DP-11563_direct"] .diff-leaf')).not.toBeNull(),
    );
    expect(document.querySelector('[data-testid="EL-158_DP-11563_reverse"] .diff-leaf')).toBeNull();
  });

  it('shares the field selection between both rows of a moved pair, across a switch of the view', async () => {
    const field = (id: string) => ({
      id: id,
      name: id,
      diffLeft: `<span class="diff-html-removed">${id} old</span>`,
      diffRight: `<span class="diff-html-added">${id} new</span>`,
      issues: [],
    });
    renderViewer(
      DOCUMENTS_URL,
      [
        { method: 'POST', match: /\/diff\/documents$/, json: fixture('documents-diff.json') ?? {} },
        {
          method: 'POST',
          match: /\/diff\/document-workitems$/,
          respond: (url: string, init?: RequestInit) =>
            JSON.parse((init?.body as string) ?? '{}').leftWorkItem?.id === 'EL-157'
              ? new Response(JSON.stringify({ fieldDiffs: [field('Description'), field('Title')] }), {
                  status: 200,
                  headers: { 'Content-Type': 'application/json' },
                })
              : pairDiff(url, init),
        },
        { method: 'POST', match: /\/diff\/documents-fields$/, json: fixture('documents-fields.json') ?? {} },
      ],
      <DocumentsPage />,
    );
    await documentsLoaded();
    await openControlPane();
    const perFieldSelection = document.querySelector<HTMLInputElement>('#per-field-merge-selection')!;
    if (!perFieldSelection.checked) {
      perFieldSelection.click();
    }
    setFieldValue(viewSelect(), 'unified');
    await vi.waitFor(() => expect(diffBody()).toHaveClass('diff-view-unified'));

    const fieldTicker = (row: string, name: string) =>
      document.querySelector<HTMLInputElement>(
        `[data-testid="EL-157_DP-11562${row}"] [aria-label="Select field ${name} for merge"]`,
      );
    const pairTicker = await vi.waitFor(() => {
      const found = document.querySelector<HTMLInputElement>(
        '[data-testid="EL-157_DP-11562_reverse"] .header .merge-ticker input',
      );
      expect(found).not.toBeNull();
      expect(fieldTicker('_reverse', 'Description')).not.toBeNull();
      return found!;
    });
    pairTicker.click();
    await vi.waitFor(() => expect(fieldTicker('_reverse', 'Description')!.checked).toBe(true));
    fieldTicker('_reverse', 'Description')!.click();
    await vi.waitFor(() => expect(fieldTicker('_reverse', 'Description')!.checked).toBe(false));

    setFieldValue(viewSelect(), 'side-by-side');

    await vi.waitFor(() => expect(fieldTicker('_direct', 'Description')).not.toBeNull());
    expect(fieldTicker('_direct', 'Description')!.checked).toBe(false);
    expect(fieldTicker('_direct', 'Title')!.checked).toBe(true);
    expect(fieldTicker('_reverse', 'Title')).toBeNull();
  });
});
