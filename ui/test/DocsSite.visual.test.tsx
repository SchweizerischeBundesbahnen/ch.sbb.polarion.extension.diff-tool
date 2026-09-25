import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import { page } from 'vitest/browser';
import App from '../src/App';
import { installFetchMock } from './mockFetch';
import { settleBeforeCapture, settleLayout } from './visualHelpers';

// Docker-only full-page snapshots of the documentation site: an article (DocPage) in its frame - breadcrumb,
// sidebar with search, navigation and "on this page", prev/next - the search results, and the "not generated"
// fallback. The article HTML is mocked, since the real ones are rendered by Maven; it carries no <pre>/<code>,
// whose monospace face the reference image picks non-deterministically.

// The real search index is a build artifact that follows the markdown; pin it so the captures do not change
// whenever an article does.
vi.mock('../src/docs/search-index.json', () => ({
  default: [
    {
      doc: 'configuration',
      docTitle: 'Configuration',
      anchor: 'chapter-merge-timeouts',
      title: 'Chapter merge timeouts',
      text: 'How long a chapter merge may run, and how long its result is kept.',
    },
    {
      doc: 'user-guide',
      docTitle: 'User Guide',
      anchor: 'chapter-merge',
      title: 'Chapter merge',
      text: 'Open the document into which content should be placed.',
    },
  ],
}));

const ARTICLE =
  '<h1 id="configuration">Configuration</h1>' +
  '<p>How to make the Diff Tool available in a project, how to tune it and where its own settings are.</p>' +
  '<h2 id="documents-comparison-form">Documents comparison form</h2>' +
  '<p>Insert the extension into the sections of the Document Properties Sidebar and save the configuration.</p>' +
  '<h2 id="chapter-merge-timeouts">Chapter merge timeouts</h2>' +
  '<p>A chapter merge runs in the background and is polled by the panel which started it.</p>' +
  '<h3 id="result-timeout">Result timeout</h3>' +
  '<p>How long the result of a finished merge is kept for the panel to pick up.</p>' +
  '<h2 id="extension-configuration">Extension configuration</h2>' +
  '<p>Each administration page carries either a Quick Help section or content that explains itself.</p>';

const origUrl = window.location.pathname + window.location.search + window.location.hash;

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
  window.history.replaceState({}, '', origUrl);
});

function mount(respond: () => Response) {
  installFetchMock([{ match: /\/html\/configuration\.html$/, respond }]);
  window.history.replaceState({}, '', '?feature=configuration&embedded=true');
  render(<App />);
}

/** Types into a controlled input the way React's onChange listens for (bypassing its value tracker). */
function typeInto(input: HTMLInputElement, text: string) {
  const setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value')!.set!;
  setter.call(input, text);
  input.dispatchEvent(new Event('input', { bubbles: true }));
}

const frames = (): Promise<void> =>
  new Promise((resolve) => requestAnimationFrame(() => requestAnimationFrame(() => resolve())));

async function shot(name: string) {
  const app = document.querySelector('.app') as HTMLElement;
  await settleLayout();
  await page.viewport(1280, Math.ceil(app.scrollHeight) + 40);
  await settleBeforeCapture();
  await expect(page.elementLocator(app)).toMatchScreenshot(name);
}

describe.skipIf(!__PIXEL_REFERENCES__)('Documentation site visual', () => {
  it('article with breadcrumb, sidebar navigation, on-this-page and prev/next', async () => {
    // No real scrolling: the capture must not depend on where a smooth scroll happens to be.
    vi.spyOn(window.HTMLElement.prototype, 'scrollIntoView').mockImplementation(() => {});
    mount(() => new Response(ARTICLE, { status: 200 }));
    await vi.waitFor(() => expect(document.querySelector('.docs-onthispage .docs-toc-link')).not.toBeNull());

    // Pin the highlighted section instead of leaving it to the IntersectionObserver's first callback: let that
    // callback run, then select the first entry, which is what the reader would see.
    await frames();
    document.querySelector<HTMLButtonElement>('.docs-onthispage .docs-toc-link')!.click();
    await vi.waitFor(() =>
      expect(document.querySelector('.docs-toc-link-active')?.textContent).toBe('Documents comparison form'),
    );
    await shot('docs-article');
  });

  it('search results listed under the search box', async () => {
    mount(() => new Response(ARTICLE, { status: 200 }));
    await vi.waitFor(() => expect(document.querySelector('article.markdown-body')).not.toBeNull());

    const input = document.querySelector<HTMLInputElement>('.docs-search-input')!;
    input.focus();
    typeInto(input, 'chapter merge');
    await vi.waitFor(() => expect(document.querySelectorAll('.docs-search-result')).toHaveLength(2));
    await shot('docs-search-results');
  });

  it('"not generated" fallback linking to the source on GitHub', async () => {
    mount(() => new Response('', { status: 404 }));
    await vi.waitFor(() => expect(document.body.textContent).toContain('has not been generated'));
    await shot('docs-not-generated');
  });
});
