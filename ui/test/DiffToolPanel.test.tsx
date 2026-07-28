import { afterEach, describe, expect, it, vi } from 'vitest';
import { mountDiffToolPanel } from '../src/formext/mountDiffToolPanel';
import { readPanelProps } from '../src/formext/panelProps';
import { $, PANEL_PROPS, clickCheckbox, mountPanel, selectOption, waitForPanel } from './formextHelpers';
import { type FetchMock, type Route, installFetchMock, jsonResponse } from './mockFetch';

// Behaviour of the port of DiffTool.js + the diff-tool.html fragment. Mounted exactly as Polarion mounts
// it - into a shadow root on a div carrying `data-props` - so these also cover the entry point, the props
// parsing and the shadow-root arrangement.

const SPACES = [
  { id: 'design', name: 'Design' },
  { id: '_default', name: 'Default' },
];
const DOCUMENTS = [
  { id: 'Design Spec', title: 'Design Specification' },
  { id: 'Test Plan', title: 'Test Plan' },
];
const REVISIONS = [
  { name: '300', baselineName: 'Release 2' },
  { name: '200', baselineName: null },
  { name: '100', baselineName: 'Release 1' },
];

function routes(overrides: Route[] = []): Route[] {
  return [
    ...overrides,
    { method: 'GET', match: /\/projects\/[^/]+\/spaces$/, json: SPACES },
    { method: 'GET', match: /\/spaces\/[^/]+\/documents$/, json: DOCUMENTS },
    { method: 'GET', match: /\/documents\/[^/]+\/revisions$/, json: REVISIONS },
  ];
}

let panel: ReturnType<typeof mountPanel> | null = null;

async function open(fetchMock: FetchMock = installFetchMock(routes())) {
  panel = mountPanel(mountDiffToolPanel, 'diff-tool-panel');
  await waitForPanel(panel, 'compare-documents');
  return { shadow: panel.shadow, fetchMock: fetchMock };
}

/** Walks the target selection down to a document in another project. */
async function pickTargetDocument(shadow: ShadowRoot) {
  await selectOption(shadow, 'comparison-project-selector', 'drivepilot');
  await selectOption(shadow, 'comparison-space-selector', 'design');
  await selectOption(shadow, 'document-selector', 'Design Spec');
}

const compareButton = (shadow: ShadowRoot) => $<HTMLButtonElement>(shadow, '#compare-documents');

afterEach(() => {
  panel?.unmount();
  panel = null;
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
  localStorage.clear();
});

describe('readPanelProps', () => {
  it('reads the JSON the server escaped into the attribute', () => {
    const host = document.createElement('div');
    host.dataset.props = JSON.stringify({ sourceProjectId: 'elibrary', projects: [{ id: 'a', name: 'A' }] });

    const props = readPanelProps(host);

    expect(props.sourceProjectId).toBe('elibrary');
    expect(props.projects).toEqual([{ id: 'a', name: 'A' }]);
    // Absent keys fall back to empty rather than undefined, so the panel never maps over undefined.
    expect(props.configurations).toEqual([]);
  });

  it('degrades to empty props rather than throwing when the attribute is missing or malformed', () => {
    const missing = document.createElement('div');
    expect(readPanelProps(missing).projects).toEqual([]);

    const malformed = document.createElement('div');
    malformed.dataset.props = '{not json';
    vi.spyOn(console, 'error').mockImplementation(() => {});
    expect(readPanelProps(malformed).projects).toEqual([]);
  });
});

describe('mountDiffToolPanel', () => {
  it('reports a missing mount target instead of throwing', () => {
    const error = vi.spyOn(console, 'error').mockImplementation(() => {});

    expect(mountDiffToolPanel('#nowhere')).toBeUndefined();
    expect(error).toHaveBeenCalled();
  });

  it('renders into a shadow root, keeping the panel out of the shared page', async () => {
    const { shadow } = await open();

    expect(shadow.mode).toBe('open');
    // The container carries the token scope plus the classes the panel CSS is written against.
    expect(shadow.querySelector('.comparison.form-wrapper.sbb-ui')).not.toBeNull();
    // Nothing leaked into the light DOM: the ids only resolve through the shadow root.
    expect(document.querySelector('#compare-documents')).toBeNull();
  });

  it('un-clips the Polarion cell that would otherwise cut off the dropdown popups', async () => {
    const cell = document.createElement('td');
    const clipped = document.createElement('div');
    clipped.style.overflow = 'hidden';
    cell.appendChild(clipped);
    document.body.appendChild(cell);
    const host = document.createElement('div');
    host.id = 'diff-tool-panel';
    host.dataset.props = JSON.stringify(PANEL_PROPS);
    clipped.appendChild(host);

    const root = mountDiffToolPanel('#diff-tool-panel')!;

    expect(clipped.style.overflow).toBe('visible');
    root.unmount();
    cell.remove();
  });
});

describe('DiffToolPanel', () => {
  it('offers the projects the server injected', async () => {
    const { shadow } = await open();

    const options = Array.from($<HTMLSelectElement>(shadow, '#comparison-project-selector').options).map(
      (option) => option.textContent,
    );
    expect(options).toEqual(expect.arrayContaining(['E-Library', 'Drive Pilot']));
  });

  it('preselects the first configuration the server sent', async () => {
    const { shadow } = await open();

    expect($<HTMLSelectElement>(shadow, '#comparison-config-selector').value).toBe('Default');
  });

  it('loads the spaces of the chosen project, then the documents of the chosen space', async () => {
    const { shadow, fetchMock } = await open();

    await selectOption(shadow, 'comparison-project-selector', 'drivepilot');

    await vi.waitFor(() =>
      expect(fetchMock.mock.calls.some(([url]) => String(url).endsWith('/projects/drivepilot/spaces'))).toBe(true),
    );
    await selectOption(shadow, 'comparison-space-selector', 'design');
    await vi.waitFor(() =>
      expect(
        fetchMock.mock.calls.some(([url]) => String(url).endsWith('/projects/drivepilot/spaces/design/documents')),
      ).toBe(true),
    );
    await vi.waitFor(() =>
      expect(
        Array.from($<HTMLSelectElement>(shadow, '#document-selector').options).map((option) => option.textContent),
      ).toEqual(expect.arrayContaining(['Design Specification', 'Test Plan'])),
    );
  });

  it('clears the space and document when the project changes', async () => {
    const { shadow } = await open();
    await pickTargetDocument(shadow);

    await selectOption(shadow, 'comparison-project-selector', 'elibrary');

    await vi.waitFor(() => expect($<HTMLSelectElement>(shadow, '#comparison-space-selector').value).toBe(''));
    expect($<HTMLSelectElement>(shadow, '#document-selector').value).toBe('');
  });

  it('keeps Compare disabled until a target document is chosen', async () => {
    const { shadow } = await open();
    expect(compareButton(shadow).disabled).toBe(true);

    await pickTargetDocument(shadow);

    await vi.waitFor(() => expect(compareButton(shadow).disabled).toBe(false));
  });

  it('opens the comparison with everything the viewer needs', async () => {
    const openSpy = vi.spyOn(window, 'open').mockReturnValue(null);
    const { shadow } = await open();
    await pickTargetDocument(shadow);
    await vi.waitFor(() => expect(compareButton(shadow).disabled).toBe(false));
    await selectOption(shadow, 'comparison-link-role-selector', 'relates_to');
    await selectOption(shadow, 'comparison-config-selector', 'Strict');

    compareButton(shadow).click();

    const url = String(openSpy.mock.calls[0][0]);
    const query = new URLSearchParams(url.split('?')[1]);
    expect(query.get('sourceProjectId')).toBe('elibrary');
    expect(query.get('sourceSpaceId')).toBe('specification');
    expect(query.get('sourceDocument')).toBe('Product Specification');
    expect(query.get('targetProjectId')).toBe('drivepilot');
    expect(query.get('targetSpaceId')).toBe('design');
    expect(query.get('targetDocument')).toBe('Design Spec');
    expect(query.get('linkRole')).toBe('relates_to');
    expect(query.get('config')).toBe('Strict');
    expect(query.get('branched')).toBe('false');
  });

  it('compares the source document against itself when asked to', async () => {
    const openSpy = vi.spyOn(window, 'open').mockReturnValue(null);
    const { shadow } = await open();

    clickCheckbox(shadow, 'compare-with-same-checkbox');

    // The whole target selection is hidden and Compare is immediately available.
    await vi.waitFor(() => expect(compareButton(shadow).disabled).toBe(false));
    expect($<HTMLElement>(shadow, '#comparison-link-role-wrapper').className).toContain('hide');
    expect($<HTMLElement>(shadow, '#compare-as-branched-wrapper').className).toContain('hide');

    compareButton(shadow).click();

    const query = new URLSearchParams(String(openSpy.mock.calls[0][0]).split('?')[1]);
    expect(query.get('targetProjectId')).toBe('elibrary');
    expect(query.get('targetSpaceId')).toBe('specification');
    expect(query.get('targetDocument')).toBe('Product Specification');
  });

  it('drops the link role when comparing branched documents, which pair through their own role', async () => {
    const openSpy = vi.spyOn(window, 'open').mockReturnValue(null);
    const { shadow } = await open();
    await pickTargetDocument(shadow);
    await selectOption(shadow, 'comparison-link-role-selector', 'relates_to');

    clickCheckbox(shadow, 'compare-as-branched-checkbox');

    await vi.waitFor(() => expect($<HTMLElement>(shadow, '#comparison-link-role-wrapper').className).toContain('hide'));
    compareButton(shadow).click();

    const url = String(openSpy.mock.calls[0][0]);
    expect(url).toContain('&branched=true');
    expect(url).not.toContain('linkRole');
  });

  it('lists the revisions of the target document, newest first behind HEAD', async () => {
    const { shadow } = await open();
    await pickTargetDocument(shadow);

    clickCheckbox(shadow, 'revision-select-from-list');

    await vi.waitFor(() => expect(shadow.querySelector('#revision-selector')).not.toBeNull());
    await vi.waitFor(() =>
      expect(
        Array.from($<HTMLSelectElement>(shadow, '#revision-selector').options).map((option) => option.textContent),
      ).toEqual(['HEAD', '300 | Release 2', '200', '100 | Release 1']),
    );
    // HEAD is preselected, which is what the empty targetRevision means to the viewer.
    expect($<HTMLSelectElement>(shadow, '#revision-selector').value).toBe('');
  });

  it('narrows the revision list to baselines and reselects the newest one still shown', async () => {
    const { shadow } = await open();
    await pickTargetDocument(shadow);
    clickCheckbox(shadow, 'revision-select-from-list');
    await vi.waitFor(() => expect($<HTMLSelectElement>(shadow, '#revision-selector').options.length).toBe(4));

    clickCheckbox(shadow, 'baseline-checkbox');

    await vi.waitFor(() =>
      expect(
        Array.from($<HTMLSelectElement>(shadow, '#revision-selector').options).map((option) => option.textContent),
      ).toEqual(['300 | Release 2', '100 | Release 1']),
    );
    expect($<HTMLSelectElement>(shadow, '#revision-selector').value).toBe('300');
  });

  it('sends the revision picked from the list', async () => {
    const openSpy = vi.spyOn(window, 'open').mockReturnValue(null);
    const { shadow } = await open();
    await pickTargetDocument(shadow);
    clickCheckbox(shadow, 'revision-select-from-list');
    await selectOption(shadow, 'revision-selector', '200');

    compareButton(shadow).click();

    expect(String(openSpy.mock.calls[0][0])).toContain('&targetRevision=200');
  });

  it('sends a manually entered revision, and steps it with the spinner carets', async () => {
    const openSpy = vi.spyOn(window, 'open').mockReturnValue(null);
    const { shadow } = await open();
    await pickTargetDocument(shadow);

    // Stepping up from an empty field reaches 1, as the native spinner did.
    const [up, down] = Array.from(shadow.querySelectorAll<HTMLButtonElement>('.sbb-number-spin button'));
    up.click();
    up.click();
    down.click();

    await vi.waitFor(() => expect($<HTMLInputElement>(shadow, '#select-revision-manual-input').value).toBe('1'));
    compareButton(shadow).click();
    expect(String(openSpy.mock.calls[0][0])).toContain('&targetRevision=1');
  });

  it('passes the work items filter through to the viewer', async () => {
    const openSpy = vi.spyOn(window, 'open').mockReturnValue(null);
    const { shadow } = await open();
    await pickTargetDocument(shadow);

    clickCheckbox(shadow, 'use-work-items-filter');
    await vi.waitFor(() => expect(shadow.querySelector('#work-items-filter-input')).not.toBeNull());
    const input = $<HTMLInputElement>(shadow, '#work-items-filter-input');
    Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value')!.set!.call(input, 'EL-1 EL-2');
    input.dispatchEvent(new Event('input', { bubbles: true }));
    clickCheckbox(shadow, 'include-work-items');

    compareButton(shadow).click();

    const hash = new URLSearchParams(String(openSpy.mock.calls[0][0]).split('?')[1]).get('additionalParams')!;
    expect(JSON.parse(localStorage.getItem(`${hash}_additionalParams`)!).filter).toEqual({
      value: 'EL-1 EL-2',
      type: 'include',
    });
  });

  it('shows the alert when a list cannot be loaded', async () => {
    const { shadow } = await open(
      installFetchMock(
        routes([{ method: 'GET', match: /\/spaces$/, respond: () => jsonResponse({ message: 'boom' }, 500) }]),
      ),
    );

    await selectOption(shadow, 'comparison-project-selector', 'drivepilot');

    await vi.waitFor(() =>
      expect(shadow.querySelector('.alert-error')?.textContent).toBe('Error occurred loading spaces'),
    );
  });

  it('blocks the panel with a progress overlay while a list is loading', async () => {
    let release: (value: Response) => void = () => {};
    const { shadow } = await open(
      installFetchMock(routes([{ method: 'GET', match: /\/spaces$/, respond: () => jsonResponse(SPACES) }])),
    );
    // Re-mock so the spaces request hangs until this test lets it finish.
    vi.stubGlobal(
      'fetch',
      vi.fn(() => new Promise<Response>((resolve) => (release = resolve))),
    );

    await selectOption(shadow, 'comparison-project-selector', 'drivepilot');

    await vi.waitFor(() => expect(shadow.querySelector('.in-progress-overlay.show')).not.toBeNull());
    expect(shadow.querySelector('#comparison-in-progress-message')?.textContent).toBe('Loading spaces');

    release(jsonResponse(SPACES));
    await vi.waitFor(() => expect(shadow.querySelector('.in-progress-overlay.show')).toBeNull());
  });
});
