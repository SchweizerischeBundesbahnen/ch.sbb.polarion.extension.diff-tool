import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import { page } from 'vitest/browser';
import App from '../src/App';
import { installFetchMock } from './mockFetch';

// Docker-only full-page snapshot of the About page: the shared react-sbb-polarion About component fed
// this extension's endpoints (mocked). Covers the extension-info / properties / status tables and the
// README article.

const origUrl = window.location.pathname + window.location.search;

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  window.history.replaceState({}, '', origUrl);
});

describe.skipIf(!__PIXEL_REFERENCES__)('About page visual', () => {
  it('loaded (info + properties + status tables, README article)', async () => {
    installFetchMock([
      {
        method: 'GET',
        match: /\/version$/,
        json: {
          bundleName: 'Diff Tool extension for Polarion ALM',
          bundleVendor: 'SBB AG',
          supportEmail: 'support@example.com',
          automaticModuleName: 'ch.sbb.polarion.extension.diff_tool',
          bundleVersion: '9.1.2',
          bundleBuildTimestamp: '2026-07-01 10:00',
        },
      },
      {
        method: 'GET',
        match: /\/configuration-properties$/,
        json: {
          properties: [
            {
              key: 'ch.sbb.polarion.extension.diff-tool.chunk.size',
              value: '2',
              defaultValue: '2',
              description: 'Number of parallel REST requests the UI issues',
            },
          ],
          obsoleteProperties: [],
        },
      },
      {
        method: 'GET',
        match: /\/configuration-status/,
        json: [{ name: 'PDF Exporter', status: 'OK', details: 'v13.4.0' }],
      },
      {
        method: 'GET',
        match: /\/readme$/,
        respond: () =>
          new Response('<h1>Diff Tool</h1><p>Compares and merges Polarion documents and work items.</p>', {
            status: 200,
          }),
      },
    ]);
    window.history.replaceState({}, '', '?feature=about&embedded=true');
    render(<App />);

    await vi.waitFor(() => expect(document.querySelector('article.markdown-body')).not.toBeNull());
    const app = document.querySelector('.app') as HTMLElement;
    await page.viewport(1280, Math.ceil(app.scrollHeight) + 40);
    await expect(page.elementLocator(app)).toMatchScreenshot('about-loaded');
  });
});
