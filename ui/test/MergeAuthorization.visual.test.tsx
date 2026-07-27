import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import { page } from 'vitest/browser';
import App from '../src/App';
import { installFetchMock } from './mockFetch';

// Docker-only snapshot of the Merge Authorization page. This is the surface where the styled admin
// checkbox matters most: it only renders correctly under the `.standard-admin-page` scope, with the
// column layout on the group container rather than the labels (see App.css).

const origUrl = window.location.pathname + window.location.search;

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  window.history.replaceState({}, '', origUrl);
});

const routes = [
  {
    method: 'GET',
    match: /\/roles\?/,
    json: {
      globalRoles: ['admin', 'developer', 'project_admin', 'user'],
      projectRoles: ['lead', 'reviewer'],
    },
  },
  { method: 'GET', match: /\/extension\/info$/, json: { version: { bundleBuildTimestamp: '2026-07-01 10:00' } } },
  {
    method: 'GET',
    match: /\/names\/Default\/content/,
    json: { globalRoles: ['admin'], projectRoles: ['lead'], bundleTimestamp: '2026-07-01 10:00' },
  },
  {
    method: 'GET',
    match: /\/revisions\?/,
    json: [
      { name: '3388', date: '2026-06-30 09:12', author: 'jane', description: 'granted lead' },
      { name: '2011', date: '2026-05-02 16:40', author: 'john' },
    ],
  },
];

async function renderPage() {
  installFetchMock(routes);
  window.history.replaceState({}, '', '?feature=merge-authorization&embedded=true&scope=project/elibrary/');
  render(<App />);
  await vi.waitFor(() => expect(document.querySelectorAll('.role-group input[type=checkbox]').length).toBe(6));
}

async function snapshot(name: string) {
  const app = document.querySelector('.app') as HTMLElement;
  await page.viewport(1280, Math.ceil(app.scrollHeight) + 40);
  await expect(page.elementLocator(app)).toMatchScreenshot(name);
}

describe('Merge Authorization page visual', () => {
  it('loaded (both role groups, checked state, action toolbar, quick help)', async () => {
    await renderPage();

    await snapshot('merge-authorization-loaded');
  });

  it('with the revisions table shown', async () => {
    await renderPage();

    Array.from(document.querySelectorAll<HTMLButtonElement>('.actions-pane button'))[3].click();
    await vi.waitFor(() => expect(document.querySelector('.revert-to-revision-button')).not.toBeNull());

    await snapshot('merge-authorization-revisions');
  });
});
