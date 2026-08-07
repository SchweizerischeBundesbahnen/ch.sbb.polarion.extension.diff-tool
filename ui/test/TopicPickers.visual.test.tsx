import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import { page } from 'vitest/browser';
import TopicsApp from '../src/topics/TopicsApp';
import { installFetchMock } from './mockFetch';

// Docker-only snapshots of the three navigation topics, which replaced the nav-topic JSPs and the tables the
// Java widget renderers produced. These pages own their whole look now (topics.css plus the
// react-sbb-polarion controls), with no Polarion stylesheet behind them - so a photograph is the only check
// that the table, the query row, the paginator and the big Compare button still hold together.

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
  { name: 'Default', scope: 'project/elibrary/' },
  { name: 'Strict', scope: 'project/elibrary/' },
];

const workItem = (id: string, readable = true) => ({
  id: id,
  projectId: 'elibrary',
  title: `Title of ${id}`,
  type: { id: 'task', name: 'Task' },
  status: { id: 'open', name: 'Open' },
  severity: { id: 'major', name: 'Major' },
  readable: readable,
  unavailableMessage: readable ? null : 'You do not have permission to read this item',
});

const collection = (id: string, projectId: string) => ({
  id: id,
  projectId: projectId,
  name: `Collection ${id}`,
  authorName: 'John Doe',
  created: 1_700_000_000_000,
  updated: 1_700_000_600_000,
  readable: true,
});

function routes() {
  return [
    { method: 'GET', match: /\/projects$/, json: PROJECTS },
    { method: 'GET', match: /\/link-roles$/, json: LINK_ROLES },
    { method: 'GET', match: /\/settings\/diff\/names\?/, json: CONFIGURATIONS },
    {
      method: 'GET',
      match: /\/workitems\/search/,
      json: {
        totalCount: 7,
        page: 1,
        lastPage: 3,
        query: 'project.id:elibrary AND (type:task)',
        items: [workItem('EL-1'), workItem('EL-2'), workItem('EL-3', false)],
      },
    },
    {
      method: 'GET',
      match: /\/collections\/search/,
      json: {
        totalCount: 2,
        page: 1,
        lastPage: 1,
        query: 'project.id:elibrary',
        items: [collection('c1', 'elibrary'), collection('c2', 'elibrary')],
      },
    },
  ];
}

async function renderTopic(topic: string, extraParams = '') {
  installFetchMock(routes());
  window.history.replaceState({}, '', `?topic=${topic}&sourceProjectId=elibrary${extraParams}`);
  render(<TopicsApp />);
}

async function snapshot(name: string) {
  const shell = document.querySelector('.diff-topics') as HTMLElement;
  await page.viewport(1280, Math.ceil(shell.scrollHeight) + 40);
  await expect(page.elementLocator(shell)).toMatchScreenshot(name);
}

beforeEach(() => {
  window.history.replaceState({}, '', origUrl);
});

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  window.history.replaceState({}, '', origUrl);
});

describe.skipIf(!__PIXEL_REFERENCES__)('navigation topics visual', () => {
  it('work items picker (table, selects, query row, footer, paginator)', async () => {
    await renderTopic('compare-work-items');
    await vi.waitFor(() => expect(document.querySelectorAll('.items-table .table-content-row').length).toBe(3));
    await vi.waitFor(() => expect(document.querySelector('.paginator')).not.toBeNull());

    await snapshot('work-items-picker');
  });

  it('work items picker with a selection, which enables Compare', async () => {
    await renderTopic('compare-work-items');
    await vi.waitFor(() => expect(document.querySelectorAll('.items-table input.select-item').length).toBe(2));

    document.querySelector<HTMLInputElement>('.items-table input.select-all')!.click();
    await vi.waitFor(() => expect(document.querySelector<HTMLButtonElement>('#compare-items')!.disabled).toBe(false));

    await snapshot('work-items-picker-selected');
  });

  it('collections picker (two columns split by the divider)', async () => {
    await renderTopic('compare-collections', '&targetProjectId=drivepilot');
    await vi.waitFor(() => expect(document.querySelectorAll('input[name="target-collection"]').length).toBe(2));

    await snapshot('collections-picker');
  });

  it('root topic', async () => {
    await renderTopic('diff-tool');
    await vi.waitFor(() => expect(document.querySelectorAll('.link-button').length).toBe(2));

    await snapshot('root-topic');
  });
});
