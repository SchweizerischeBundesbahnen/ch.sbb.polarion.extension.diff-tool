import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { MockInstance } from 'vitest';
import { cleanup } from 'vitest-browser-react';
import { isExportMessage } from '../src/embed/messages';
import type { ExportMessage } from '../src/embed/messages';
import DocumentsPage from '../src/pages/DocumentsPage';
import { jsonResponse } from './mockFetch';
import { fixture, pairDiff, renderViewer, restoreUrl } from './viewerHarness';

// The documents viewer embedded for a batch PDF export (embed=pdf-export): it renders the diff without its
// control pane and reports the loading to the embedding page with postMessage. The test page itself runs in a
// frame of the Vitest runner, so the messages go to window.parent, which is spied on here.

const DOCUMENTS =
  'sourceProjectId=elibrary&sourceSpaceId=Specification&sourceDocument=Catalog%20Specification' +
  '&targetProjectId=drivepilot&targetSpaceId=Design&targetDocument=Catalog%20Design&linkRole=relates_to&config=Default';

const EMBEDDED_URL = `/documents?${DOCUMENTS}&embed=pdf-export`;

let postMessage: MockInstance<Window['postMessage']>;

const exportMessages = (): ExportMessage[] =>
  postMessage.mock.calls.map(([message]) => message as unknown).filter(isExportMessage);

const lastMessage = (): ExportMessage | undefined => exportMessages().at(-1);

function renderDocuments(
  url: string,
  documentsDiff: () => Response = () => jsonResponse(fixture('documents-diff.json')),
) {
  renderViewer(
    url,
    [
      { method: 'POST', match: /\/diff\/documents$/, respond: documentsDiff },
      { method: 'POST', match: /\/diff\/document-workitems$/, respond: pairDiff },
      { method: 'POST', match: /\/diff\/documents-fields$/, json: fixture('documents-fields.json') ?? {} },
    ],
    <DocumentsPage />,
  );
}

beforeEach(() => {
  postMessage = vi.spyOn(window.parent, 'postMessage').mockImplementation(() => undefined);
});

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
  restoreUrl();
});

describe('Documents viewer embedded for a PDF export', () => {
  it('reports the progress and then that the diff is ready', async () => {
    renderDocuments(EMBEDDED_URL);

    await vi.waitFor(() => expect(lastMessage()?.type).toBe('ready'), { timeout: 10000 });

    const messages = exportMessages();
    const progress = messages.filter((message) => message.type === 'progress');
    expect(progress.length).toBeGreaterThan(0);
    const lastProgress = progress.at(-1) as Extract<ExportMessage, { type: 'progress' }>;
    expect(lastProgress.loaded).toBe(lastProgress.total);
    expect(lastMessage()).toMatchObject({ type: 'ready', diffsExist: true });
    expect(messages.filter((message) => message.type === 'ready')).toHaveLength(1);
    expect(postMessage.mock.calls[0][1]).toBe(window.location.origin);
  });

  it('renders without the control pane, laid out as when the pane is open', async () => {
    renderDocuments(EMBEDDED_URL);

    await vi.waitFor(() => expect(document.querySelector('#diff-body')).not.toBeNull(), { timeout: 10000 });

    expect(document.querySelector('.control-pane')).toBeNull();
    expect(document.querySelector('#diff-body')?.classList.contains('control-pane-expanded')).toBe(true);
  });

  it('reports a diff which could not be loaded', async () => {
    renderDocuments(EMBEDDED_URL, () => jsonResponse({ message: 'Document not found' }, 404));

    await vi.waitFor(() => expect(lastMessage()?.type).toBe('error'), { timeout: 10000 });

    expect(lastMessage()).toMatchObject({ type: 'error', message: expect.stringContaining('Document not found') });
    expect(exportMessages().some((message) => message.type === 'ready')).toBe(false);
  });

  it('reports a fields diff as ready', async () => {
    renderDocuments(`${EMBEDDED_URL}&compareAs=Fields`);

    await vi.waitFor(() => expect(lastMessage()?.type).toBe('ready'), { timeout: 10000 });
  });
});

describe('Documents viewer not embedded', () => {
  it('keeps its control pane and reports nothing', async () => {
    renderDocuments(`/documents?${DOCUMENTS}`);

    await vi.waitFor(() => expect(document.querySelector('.control-pane')).not.toBeNull(), { timeout: 10000 });
    await vi.waitFor(() => expect(document.querySelector('.wi-diff')).not.toBeNull(), { timeout: 10000 });

    expect(document.querySelector('#diff-body')?.classList.contains('control-pane-expanded')).toBe(false);
    expect(exportMessages()).toEqual([]);
  });
});
