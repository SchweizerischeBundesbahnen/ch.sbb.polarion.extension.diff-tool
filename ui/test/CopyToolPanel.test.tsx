import { afterEach, describe, expect, it, vi } from 'vitest';
import { createdDocumentLink } from '../src/formext/CopyToolPanel';
import { mountCopyToolPanel } from '../src/formext/mountCopyToolPanel';
import { $, clickCheckbox, mountPanel, selectOption, waitForPanel } from './formextHelpers';
import { type FetchMock, type Route, installFetchMock, jsonResponse } from './mockFetch';

// Behaviour of the port of CopyTool.js + the copy-tool.html fragment. Mounted the way Polarion mounts it,
// into a shadow root on a div carrying `data-props`.

const SPACES = [
  { id: 'design', name: 'Design' },
  { id: '_default', name: 'Default' },
];

function routes(overrides: Route[] = []): Route[] {
  return [
    ...overrides,
    { method: 'GET', match: /\/projects\/[^/]+\/spaces$/, json: SPACES },
    { method: 'GET', match: /\/settings\/diff\/names/, json: [{ name: 'Target Default' }, { name: 'Target Strict' }] },
    {
      method: 'POST',
      match: /\/duplicate/,
      json: { projectId: 'drivepilot', spaceId: 'design', name: 'Product Specification' },
    },
  ];
}

let panel: ReturnType<typeof mountPanel> | null = null;

async function open(fetchMock: FetchMock = installFetchMock(routes())) {
  panel = mountPanel(mountCopyToolPanel, 'copy-tool-panel', {
    linkRoles: [
      { id: '', name: 'none' },
      { id: 'relates_to', name: 'relates to / relates to' },
    ],
  });
  await waitForPanel(panel, 'create-document');
  return { shadow: panel.shadow, fetchMock: fetchMock };
}

/** Fills in everything the Create button requires. */
async function fillForm(shadow: ShadowRoot) {
  await selectOption(shadow, 'copy-project-selector', 'drivepilot');
  await selectOption(shadow, 'copy-space-selector', 'design');
  await selectOption(shadow, 'copy-link-role-selector', 'relates_to');
  await selectOption(shadow, 'copy-config-selector', 'Target Strict');
  await selectOption(shadow, 'handle-refs-selector', 'KEEP');
}

const createButton = (shadow: ShadowRoot) => $<HTMLButtonElement>(shadow, '#create-document');

afterEach(() => {
  panel?.unmount();
  panel = null;
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

describe('createdDocumentLink', () => {
  it('spells out the space in both the label and the editor URL', () => {
    const link = createdDocumentLink(
      { projectId: 'drivepilot', spaceId: 'design', name: 'My Doc' },
      '//host/polarion/',
    );

    expect(link.text).toBe('//host/polarion/#/project/drivepilot/design/My Doc');
    expect(link.href).toBe('//host/polarion/#/project/drivepilot/wiki/design/My%20Doc');
  });

  it('omits the default space, which has no path segment in Polarion', () => {
    const link = createdDocumentLink({ projectId: 'p', spaceId: '_default', name: 'Doc' }, '//host/polarion/');

    expect(link.text).toBe('//host/polarion/#/project/p/Doc');
    expect(link.href).toBe('//host/polarion/#/project/p/wiki/Doc');
  });

  it('treats a missing space the same way', () => {
    expect(createdDocumentLink({ projectId: 'p', name: 'Doc' }, '//h/').href).toBe('//h/#/project/p/wiki/Doc');
  });
});

describe('mountCopyToolPanel', () => {
  it('reports a missing mount target instead of throwing', () => {
    const error = vi.spyOn(console, 'error').mockImplementation(() => {});

    expect(mountCopyToolPanel('#nowhere')).toBeUndefined();
    expect(error).toHaveBeenCalled();
  });

  it('renders into a shadow root under the copy prefix class', async () => {
    const { shadow } = await open();

    expect(shadow.querySelector('.copy.form-wrapper.sbb-ui')).not.toBeNull();
    expect(document.querySelector('#create-document')).toBeNull();
  });
});

describe('CopyToolPanel', () => {
  it('starts on the configurations the server injected for the source project', async () => {
    const { shadow } = await open();

    expect(
      Array.from($<HTMLSelectElement>(shadow, '#copy-config-selector').options).map((option) => option.value),
    ).toEqual(expect.arrayContaining(['Default', 'Strict']));
    expect($<HTMLSelectElement>(shadow, '#copy-config-selector').value).toBe('Default');
  });

  it('reloads the configurations from the target project once one is chosen', async () => {
    const { shadow, fetchMock } = await open();

    await selectOption(shadow, 'copy-project-selector', 'drivepilot');

    await vi.waitFor(() =>
      expect(
        fetchMock.mock.calls.some(([url]) => String(url).endsWith('/settings/diff/names?scope=project/drivepilot/')),
      ).toBe(true),
    );
    // The source project's names are gone and the target's first one is preselected.
    await vi.waitFor(() => expect($<HTMLSelectElement>(shadow, '#copy-config-selector').value).toBe('Target Default'));
    expect(
      Array.from($<HTMLSelectElement>(shadow, '#copy-config-selector').options).map((option) => option.value),
    ).not.toContain('Strict');
  });

  it('offers the "none" link role the server prepends, which alone cannot enable Create', async () => {
    const { shadow } = await open();
    await fillForm(shadow);
    await vi.waitFor(() => expect(createButton(shadow).disabled).toBe(false));

    await selectOption(shadow, 'copy-link-role-selector', '');

    await vi.waitFor(() => expect(createButton(shadow).disabled).toBe(true));
  });

  it('requires every field before Create is available', async () => {
    const { shadow } = await open();
    expect(createButton(shadow).disabled).toBe(true);

    await selectOption(shadow, 'copy-project-selector', 'drivepilot');
    await selectOption(shadow, 'copy-space-selector', 'design');
    await selectOption(shadow, 'copy-link-role-selector', 'relates_to');
    await vi.waitFor(() => expect($<HTMLSelectElement>(shadow, '#copy-config-selector').value).toBe('Target Default'));
    // Still missing the referenced-workitems behaviour.
    expect(createButton(shadow).disabled).toBe(true);

    await selectOption(shadow, 'handle-refs-selector', 'KEEP');

    await vi.waitFor(() => expect(createButton(shadow).disabled).toBe(false));
  });

  it('posts the duplication request against the source document', async () => {
    const { shadow, fetchMock } = await open();
    await fillForm(shadow);
    await vi.waitFor(() => expect(createButton(shadow).disabled).toBe(false));
    clickCheckbox(shadow, 'copy-comments-checkbox');

    createButton(shadow).click();

    await vi.waitFor(() => {
      const post = fetchMock.mock.calls.find(([, init]) => init?.method === 'POST');
      expect(post).toBeDefined();
      expect(String(post![0])).toContain(
        '/projects/elibrary/spaces/specification/documents/Product%20Specification/duplicate',
      );
      expect(JSON.parse(String(post![1]!.body))).toEqual({
        targetDocumentIdentifier: { projectId: 'drivepilot', spaceId: 'design', name: 'Product Specification' },
        targetDocumentTitle: 'Product Specification',
        linkRoleId: 'relates_to',
        configName: 'Target Strict',
        handleReferences: 'KEEP',
        copyDocumentComments: true,
      });
    });
  });

  it('carries the source revision when the editor shows one', async () => {
    installFetchMock(routes());
    panel = mountPanel(mountCopyToolPanel, 'copy-tool-panel', { sourceRevision: '4711' });
    await waitForPanel(panel, 'create-document');
    const fetchMock = installFetchMock(routes());
    await fillForm(panel.shadow);
    await vi.waitFor(() => expect(createButton(panel!.shadow).disabled).toBe(false));

    createButton(panel.shadow).click();

    await vi.waitFor(() => {
      const post = fetchMock.mock.calls.find(([, init]) => init?.method === 'POST');
      expect(String(post![0])).toContain('/duplicate?revision=4711');
    });
  });

  it('links to the document it created', async () => {
    const { shadow } = await open();
    await fillForm(shadow);
    await vi.waitFor(() => expect(createButton(shadow).disabled).toBe(false));

    createButton(shadow).click();

    await vi.waitFor(() => expect(shadow.querySelector('#creation-success')).not.toBeNull());
    const anchor = $<HTMLAnchorElement>(shadow, '#creation-success a');
    expect(anchor.getAttribute('href')).toContain('#/project/drivepilot/wiki/design/Product%20Specification');
    expect(anchor.target).toBe('_blank');
  });

  it('surfaces the server message when the duplication fails', async () => {
    const { shadow } = await open(
      installFetchMock(
        routes([
          {
            method: 'POST',
            match: /\/duplicate/,
            respond: () => jsonResponse({ message: 'Document already exists' }, 400),
          },
        ]),
      ),
    );
    await fillForm(shadow);
    await vi.waitFor(() => expect(createButton(shadow).disabled).toBe(false));

    createButton(shadow).click();

    await vi.waitFor(() => expect(shadow.querySelector('.alert-error')?.textContent).toBe('Document already exists'));
  });

  it('falls back to a generic message when the failure body is not the expected JSON', async () => {
    const { shadow } = await open(
      installFetchMock(
        routes([
          {
            method: 'POST',
            match: /\/duplicate/,
            respond: () => new Response('<html>Gateway Timeout</html>', { status: 504 }),
          },
        ]),
      ),
    );
    await fillForm(shadow);
    await vi.waitFor(() => expect(createButton(shadow).disabled).toBe(false));

    createButton(shadow).click();

    await vi.waitFor(() => expect(shadow.querySelector('.alert-error')?.textContent).toBe('Error creating document'));
  });

  it('blocks the panel while the document is being created', async () => {
    let release: (value: Response) => void = () => {};
    const { shadow } = await open();
    await fillForm(shadow);
    await vi.waitFor(() => expect(createButton(shadow).disabled).toBe(false));
    vi.stubGlobal(
      'fetch',
      vi.fn(() => new Promise<Response>((resolve) => (release = resolve))),
    );

    createButton(shadow).click();

    await vi.waitFor(() => expect(shadow.querySelector('.in-progress-overlay.show')).not.toBeNull());
    expect(shadow.querySelector('#copy-in-progress-message')?.textContent).toBe('Creating a document');

    release(jsonResponse({ projectId: 'drivepilot', spaceId: 'design', name: 'Product Specification' }));
    await vi.waitFor(() => expect(shadow.querySelector('.in-progress-overlay.show')).toBeNull());
  });

  it('shows the alert when the spaces cannot be loaded', async () => {
    const { shadow } = await open(
      installFetchMock(
        routes([{ method: 'GET', match: /\/spaces$/, respond: () => jsonResponse({ message: 'boom' }, 500) }]),
      ),
    );

    await selectOption(shadow, 'copy-project-selector', 'drivepilot');

    await vi.waitFor(() =>
      expect(shadow.querySelector('.alert-error')?.textContent).toBe('Error occurred loading spaces'),
    );
  });

  it('names the project whose configurations could not be loaded', async () => {
    const { shadow } = await open(
      installFetchMock(
        routes([
          { method: 'GET', match: /\/settings\/diff\/names/, respond: () => jsonResponse({ message: 'boom' }, 500) },
        ]),
      ),
    );

    await selectOption(shadow, 'copy-project-selector', 'drivepilot');

    await vi.waitFor(() =>
      expect(shadow.querySelector('.alert-error')?.textContent).toBe(
        'Error occurred loading project [drivepilot] diff configuration',
      ),
    );
  });
});
