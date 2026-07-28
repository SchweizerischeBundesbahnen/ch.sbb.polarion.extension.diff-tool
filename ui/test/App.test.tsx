import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import App from '../src/App';
import { FEATURES, findFeature } from '../src/features';
import { installFetchMock } from './mockFetch';

// The admin feature router: `?feature=<id>` selects a page (the ids come from the extenders in
// META-INF/hivemodule.xml), anything unmatched renders the dev Landing stub.

const origUrl = window.location.pathname + window.location.search;

const aboutRoutes = () => [
  { method: 'GET', match: /\/version$/, json: { bundleName: 'Diff Tool', bundleVendor: 'SBB' } },
  { method: 'GET', match: /\/configuration-properties$/, json: { properties: [], obsoleteProperties: [] } },
  { method: 'GET', match: /\/configuration-status/, json: [] },
  { method: 'GET', match: /\/readme$/, respond: () => new Response('<h1>Readme</h1>', { status: 200 }) },
];

const authorizationRoutes = () => [
  { method: 'GET', match: /\/roles\?/, json: { globalRoles: ['admin'], projectRoles: [] } },
  { method: 'GET', match: /\/extension\/info$/, json: { version: { bundleBuildTimestamp: '2026-07-01 10:00' } } },
  { method: 'GET', match: /\/settings\/authorization\/names\/Default\/content/, json: { globalRoles: ['admin'] } },
  { method: 'GET', match: /\/revisions\?/, json: [] },
];

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  window.history.replaceState({}, '', origUrl);
});

describe('findFeature', () => {
  it('matches a known id and returns undefined otherwise', () => {
    expect(findFeature('about')?.id).toBe('about');
    expect(findFeature('diff-configurations')?.id).toBe('diff-configurations');
    expect(findFeature('execution-queue')?.id).toBe('execution-queue');
    expect(findFeature('project-duplication')?.id).toBe('project-duplication');
    expect(findFeature('merge-authorization')?.id).toBe('merge-authorization');
    expect(findFeature('nope')).toBeUndefined();
    expect(findFeature(null)).toBeUndefined();
  });

  it('does not claim rest-api, which points straight at the swagger endpoint', () => {
    expect(findFeature('rest-api')).toBeUndefined();
  });
});

describe('App router', () => {
  it('carries the container classes every RSP extension shares', async () => {
    installFetchMock([]);
    window.history.replaceState({}, '', '?');
    render(<App />);

    await vi.waitFor(() => expect(document.querySelector('.app')).not.toBeNull());
    // `.app` gives the page padding/font from RSP; `.standard-admin-page` is what scopes the styled
    // admin checkbox. Neither is optional - see the note in App.tsx.
    const root = document.querySelector('.app')!;
    expect(root.classList.contains('standard-admin-page')).toBe(true);
  });

  it('renders the Landing stub with every feature linked when no feature is selected', async () => {
    installFetchMock([]);
    window.history.replaceState({}, '', '?');
    render(<App />);

    await vi.waitFor(() => expect(document.querySelector('.landing-features')).not.toBeNull());
    const links = Array.from(document.querySelectorAll<HTMLAnchorElement>('.landing-features a')).map((a) =>
      a.getAttribute('href'),
    );
    expect(links).toEqual(FEATURES.map((feature) => `?feature=${feature.id}`));
  });

  it('falls back to Landing for an unknown feature', async () => {
    installFetchMock([]);
    window.history.replaceState({}, '', '?feature=does-not-exist');
    render(<App />);

    await vi.waitFor(() => expect(document.querySelector('.landing-features')).not.toBeNull());
  });

  it('carries the scope into the Landing feature links', async () => {
    installFetchMock([]);
    window.history.replaceState({}, '', '?scope=project/elibrary/');
    render(<App />);

    await vi.waitFor(() => expect(document.querySelector('.landing-features')).not.toBeNull());
    const first = document.querySelector<HTMLAnchorElement>('.landing-features a')!;
    expect(first.getAttribute('href')).toContain('scope=project%2Felibrary%2F');
  });

  it('renders the About page for ?feature=about', async () => {
    installFetchMock(aboutRoutes());
    window.history.replaceState({}, '', '?feature=about&embedded=true');
    render(<App />);

    await vi.waitFor(() => expect(document.querySelector('article.markdown-body')).not.toBeNull());
    expect(document.body.textContent).toContain('Diff Tool');
  });

  it('hides the dev Overview link when embedded, as Polarion always opens it', async () => {
    installFetchMock(aboutRoutes());
    window.history.replaceState({}, '', '?feature=about&embedded=true');
    render(<App />);

    await vi.waitFor(() => expect(document.querySelector('article.markdown-body')).not.toBeNull());
    expect(document.querySelector('.page-nav')).toBeNull();
  });

  it('renders the Merge Authorization page for ?feature=merge-authorization', async () => {
    installFetchMock(authorizationRoutes());
    window.history.replaceState({}, '', '?feature=merge-authorization&embedded=true&scope=project/elibrary/');
    render(<App />);

    await vi.waitFor(() => expect(document.querySelector('.role-groups')).not.toBeNull());
    expect(document.querySelector('.page > h1')!.textContent).toBe('Merge Authorization');
  });
});
