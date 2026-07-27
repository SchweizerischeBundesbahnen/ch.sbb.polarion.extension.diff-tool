import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import { page } from 'vitest/browser';
import App from '../src/App';
import { installFetchMock } from './mockFetch';

// Docker-only snapshot of the Diff Configurations page: the configurations pane, the Available/Selected
// dual listbox, and the multiselects upgraded to the shared Polarion dropdown (chips + search) - the one
// control here that is not plain markup.

const origUrl = window.location.pathname + window.location.search;

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  window.history.replaceState({}, '', origUrl);
});

const FIELDS = [
  { key: 'title', name: 'Title' },
  { key: 'description', name: 'Description' },
  { key: 'status', name: 'Status' },
  { key: 'hyperlinks', name: 'Hyperlinks' },
  { key: 'linkedWorkItems', name: 'Linked WorkItems' },
  { key: 'severity', name: 'Severity', wiTypeId: 'defect', wiTypeName: 'Defect' },
];

describe('Diff Configurations page visual', () => {
  it('loaded (configurations pane, transfer list, role multiselects)', async () => {
    installFetchMock([
      { method: 'GET', match: /\/workitem-fields$/, json: FIELDS },
      {
        method: 'GET',
        match: /\/workitem-statuses$/,
        json: [
          { id: 'draft', name: 'Draft', wiTypeName: 'Requirement' },
          { id: 'open', name: 'Open' },
        ],
      },
      {
        method: 'GET',
        match: /\/hyperlink-roles$/,
        json: [{ id: 'ref', name: 'refers to', workItemTypeName: 'Requirement', combinedId: 'req#ref' }],
      },
      { method: 'GET', match: /\/linked-workitem-roles$/, json: [{ id: 'relates_to', name: 'relates to' }] },
      { method: 'GET', match: /\/extension\/info$/, json: { version: { bundleBuildTimestamp: '2026-07-01 10:00' } } },
      {
        method: 'GET',
        match: /\/settings\/diff\/names\?/,
        json: [
          { name: 'Default', scope: 'project/elibrary/' },
          { name: 'Strict', scope: 'project/elibrary/' },
        ],
      },
      {
        method: 'GET',
        match: /\/names\/[^/]+\/content/,
        json: {
          diffFields: [{ key: 'title' }, { key: 'description' }, { key: 'hyperlinks' }],
          statusesToIgnore: ['draft'],
          hyperlinkRoles: ['req#ref'],
          linkedWorkItemRoles: [],
          bundleTimestamp: '2026-07-01 10:00',
        },
      },
      { method: 'GET', match: /\/default-content$/, json: { diffFields: [{ key: 'title' }] } },
    ]);
    window.history.replaceState({}, '', '?feature=diff-configurations&embedded=true&scope=project/elibrary/');
    render(<App />);

    await vi.waitFor(() => expect(document.querySelectorAll('#selected-fields option').length).toBe(3));
    // The chips of the upgraded multiselect are rendered by the dropdown, not by React.
    await vi.waitFor(() => expect(document.querySelector('#hyperlink-settings-container')).not.toBeNull());

    const app = document.querySelector('.app') as HTMLElement;
    await page.viewport(1280, Math.ceil(app.scrollHeight) + 40);
    await expect(page.elementLocator(app)).toMatchScreenshot('diff-configurations-loaded');
  });
});
