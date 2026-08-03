import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import Landing from '../src/admin/dev/Landing';
import { FEATURES } from '../src/features';
import { installFetchMock, jsonResponse } from './mockFetch';

// The dev-only landing page. It is excluded from the coverage gate as scaffolding, but the scope handoff
// it performs is worth pinning: the precedence between the URL, the cookie and global is easy to get
// wrong, and getting it wrong silently sends every feature link to the global scope.

const origUrl = window.location.pathname + window.location.search;
const COOKIE = 'diff-tool-dev-scope';

const PROJECTS = [{ id: 'elibrary', name: 'E-Library' }, { id: 'drivepilot' }];

const routes = () => [{ method: 'GET', match: /\/projects$/, json: PROJECTS }];

function forgetScopeCookie() {
  document.cookie = `${COOKIE}=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/`;
}

async function renderLanding() {
  render(<Landing />);
  await vi.waitFor(() => expect(document.querySelector('#dev-scope')).not.toBeNull());
}

const links = () =>
  Array.from(document.querySelectorAll<HTMLAnchorElement>('.landing-features a')).map((a) => a.getAttribute('href'));

function setFieldValue(element: HTMLSelectElement, value: string): void {
  Object.getOwnPropertyDescriptor(HTMLSelectElement.prototype, 'value')!.set!.call(element, value);
  element.dispatchEvent(new Event('change', { bubbles: true }));
}

beforeEach(() => {
  forgetScopeCookie();
  window.history.replaceState({}, '', '?');
});

afterEach(() => {
  cleanup();
  // Must not leak: another suite asserting the unscoped links would inherit this selection.
  forgetScopeCookie();
  vi.unstubAllGlobals();
  window.history.replaceState({}, '', origUrl);
});

describe('dev Landing', () => {
  it('offers the global scope plus every project, labelled', async () => {
    installFetchMock(routes());
    await renderLanding();

    await vi.waitFor(() => expect(document.querySelectorAll('#dev-scope option').length).toBe(3));
    expect(
      Array.from(document.querySelectorAll<HTMLOptionElement>('#dev-scope option')).map((o) => o.textContent),
    ).toEqual(['Repository (global scope)', 'E-Library (elibrary)', 'drivepilot']);
  });

  it('links every feature without a scope until one is picked', async () => {
    installFetchMock(routes());
    await renderLanding();

    expect(links()).toEqual(FEATURES.map((feature) => `?feature=${feature.id}`));
  });

  it('puts the chosen scope on every feature link', async () => {
    installFetchMock(routes());
    await renderLanding();
    await vi.waitFor(() => expect(document.querySelectorAll('#dev-scope option').length).toBe(3));

    setFieldValue(document.querySelector<HTMLSelectElement>('#dev-scope')!, 'project/elibrary/');

    await vi.waitFor(() =>
      expect(links()).toEqual(FEATURES.map((feature) => `?feature=${feature.id}&scope=project%2Felibrary%2F`)),
    );
  });

  // Deliberately two tests rather than one that picks, unmounts and re-renders: unmounting and mounting
  // again mid-test leaves vitest-browser-react unable to mount, which fails this test and every one after.
  it('writes the choice to a cookie', async () => {
    installFetchMock(routes());
    await renderLanding();
    await vi.waitFor(() => expect(document.querySelectorAll('#dev-scope option').length).toBe(3));

    setFieldValue(document.querySelector<HTMLSelectElement>('#dev-scope')!, 'project/drivepilot/');

    await vi.waitFor(() => expect(document.cookie).toContain(`${COOKIE}=project%2Fdrivepilot%2F`));
  });

  it('starts on the remembered scope when the URL carries none', async () => {
    document.cookie = `${COOKIE}=project%2Fdrivepilot%2F; path=/`;
    installFetchMock(routes());

    await renderLanding();

    await vi.waitFor(() =>
      expect(document.querySelector<HTMLSelectElement>('#dev-scope')!.value).toBe('project/drivepilot/'),
    );
    expect(links()[0]).toContain('scope=project%2Fdrivepilot%2F');
  });

  it('lets an explicit scope in the URL win over the remembered one', async () => {
    document.cookie = `${COOKIE}=project%2Fdrivepilot%2F; path=/`;
    window.history.replaceState({}, '', '?scope=project%2Felibrary%2F');
    installFetchMock(routes());

    await renderLanding();

    await vi.waitFor(() =>
      expect(document.querySelector<HTMLSelectElement>('#dev-scope')!.value).toBe('project/elibrary/'),
    );
  });

  it('explains what to configure when the projects cannot be read', async () => {
    installFetchMock([{ method: 'GET', match: /\/projects$/, respond: () => jsonResponse({}, 401) }]);
    await renderLanding();

    await vi.waitFor(() => expect(document.querySelector('.alert-error')?.textContent).toContain('VITE_BEARER_TOKEN'));
    // The feature links still work; only the project picker is degraded.
    expect(links()).toEqual(FEATURES.map((feature) => `?feature=${feature.id}`));
  });
});
