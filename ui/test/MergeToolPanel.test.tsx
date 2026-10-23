import { a11yViolations } from '@sbb-polarion/react-sbb-polarion/testing';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { mountMergeToolPanel } from '../src/formext/mountMergeToolPanel';
import { $, forgetRememberedSelections, mountPanel, selectOption, setFieldValue, waitForPanel } from './formextHelpers';
import { type FetchMock, type Route, installFetchMock, jsonResponse } from './mockFetch';
import { clearToasts } from './toasts';

// Behaviour of the "Documents Merge" panel, mounted the way Polarion mounts it: into a shadow root on a
// div carrying `data-props`. The open document is the *target* of the merge, the picked one is the source.

vi.mock('../src/formext/documentReload', () => ({ reloadDocument: vi.fn() }));
const { reloadDocument } = await import('../src/formext/documentReload');

const SPACES = [
  { id: 'design', name: 'Design' },
  { id: '_default', name: 'Default' },
];

const DOCUMENTS = [
  { id: 'Requirements', title: 'Requirements' },
  { id: 'Concept', title: 'Concept' },
];

const MERGED = {
  success: true,
  chapterMergeInfo: {
    insertedOutlineNumber: '3.2',
    createdWorkItemIds: ['DP-100', 'DP-101'],
    movedWorkItemIds: [],
    referencedWorkItemIds: [],
    copiedLayoutTypeIds: ['requirement'],
  },
  mergeReport: { warnings: [], logs: "2026-01-01 00:00:00: 'CREATED' -- chapter '2' -- workitem 'DP-100' created" },
};

/** Where the server says the result of a started merge is asked for. */
const JOB_URL = '/polarion/diff-tool/rest/internal/merge/chapter/jobs/J-1';

/** What starting a merge answers with: the job it is polled by, named in the Location header. */
const startedJob = () => new Response(null, { status: 202, headers: { Location: JOB_URL } });

/**
 * What polling answers with once the merge is over: the browser follows the redirect to the result of the
 * merge, so the answer the panel sees is that result.
 */
const finishedJob = (mergeResult: unknown) => jsonResponse(mergeResult);

/** What polling answers with while the merge is still running. */
const runningJob = (progressMessage?: string) =>
  jsonResponse({ status: 'IN_PROGRESS', progressMessage: progressMessage }, 202);

function routes(overrides: Route[] = []): Route[] {
  return [
    ...overrides,
    { method: 'GET', match: /\/projects\/[^/]+\/spaces$/, json: SPACES },
    { method: 'GET', match: /\/documents$/, json: DOCUMENTS },
    { method: 'POST', match: /\/merge\/chapter$/, respond: () => startedJob() },
    { method: 'GET', match: /\/merge\/chapter\/jobs\/J-1$/, respond: () => finishedJob(MERGED) },
  ];
}

let panel: ReturnType<typeof mountPanel> | null = null;

async function open(fetchMock: FetchMock = installFetchMock(routes())) {
  panel = mountPanel(mountMergeToolPanel, 'merge-tool-panel');
  await waitForPanel(panel, 'merge-chapter');
  return { shadow: panel.shadow, fetchMock: fetchMock };
}

/** Fills in everything the Merge button requires. */
async function fillForm(shadow: ShadowRoot) {
  await selectOption(shadow, 'merge-project-selector', 'drivepilot');
  await selectOption(shadow, 'merge-space-selector', 'design');
  await selectOption(shadow, 'merge-document-selector', 'Requirements');
  setFieldValue($<HTMLInputElement>(shadow, '#merge-source-chapter-input'), '2');
  setFieldValue($<HTMLInputElement>(shadow, '#merge-target-chapter-input'), '3.1');
}

const mergeButton = (shadow: ShadowRoot) => $<HTMLButtonElement>(shadow, '#merge-chapter');

/** What the dialog says at the stage it is at, empty while it is not open. */
const dialogText = (shadow: ShadowRoot) => shadow.querySelector('.merge-dialog')?.textContent ?? '';

/** What the confirmation stage says, empty while the dialog is at another stage or closed. */
const confirmationText = (shadow: ShadowRoot) => shadow.querySelector('#merge-confirmation')?.textContent ?? '';

/** What the result stage says, empty while the dialog is at another stage or closed. */
const resultText = (shadow: ShadowRoot) => shadow.querySelector('#merge-result')?.textContent ?? '';

/** Clicks Merge and confirms, which is what starts a merge. */
async function startMerge(shadow: ShadowRoot) {
  mergeButton(shadow).click();
  await vi.waitFor(() => expect(shadow.querySelector('#merge-confirmation')).not.toBeNull());
  $<HTMLButtonElement>(shadow, '#merge-confirm').click();
}

afterEach(() => {
  panel?.unmount();
  panel = null;
  forgetRememberedSelections();
  clearToasts();
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
  vi.mocked(reloadDocument).mockClear();
});

describe('mountMergeToolPanel', () => {
  it('reports a missing mount target instead of throwing', () => {
    const error = vi.spyOn(console, 'error').mockImplementation(() => {});

    expect(mountMergeToolPanel('#nowhere')).toBeUndefined();
    expect(error).toHaveBeenCalled();
  });

  it('renders into a shadow root under the merge prefix class', async () => {
    const { shadow } = await open();

    expect(shadow.querySelector('.merge.form-wrapper.sbb-ui')).not.toBeNull();
    expect(document.querySelector('#merge-chapter')).toBeNull();
  });
});

describe('MergeToolPanel', () => {
  it('requires the source document and both chapters before Merge is available', async () => {
    const { shadow } = await open();
    expect(mergeButton(shadow).disabled).toBe(true);

    await selectOption(shadow, 'merge-project-selector', 'drivepilot');
    await selectOption(shadow, 'merge-space-selector', 'design');
    await selectOption(shadow, 'merge-document-selector', 'Requirements');
    expect(mergeButton(shadow).disabled).toBe(true);

    setFieldValue($<HTMLInputElement>(shadow, '#merge-source-chapter-input'), '2');
    expect(mergeButton(shadow).disabled).toBe(true);

    setFieldValue($<HTMLInputElement>(shadow, '#merge-target-chapter-input'), '3.1');
    await vi.waitFor(() => expect(mergeButton(shadow).disabled).toBe(false));
  });

  it('copies the missing workitem layouts unless that is switched off', async () => {
    const { shadow, fetchMock } = await open();
    await fillForm(shadow);
    await vi.waitFor(() => expect(mergeButton(shadow).disabled).toBe(false));
    expect($<HTMLInputElement>(shadow, '#merge-copy-layouts-checkbox').checked).toBe(true);

    $<HTMLInputElement>(shadow, '#merge-copy-layouts-checkbox').click();
    await startMerge(shadow);

    await vi.waitFor(() => {
      const post = fetchMock.mock.calls.find(([, init]) => init?.method === 'POST');
      const body = JSON.parse(String(post![1]!.body)) as Record<string, unknown>;
      expect(body.copyWorkItemLayouts).toBe(false);
    });
  });

  it('rejects anything which is not an outline number', async () => {
    const { shadow } = await open();
    await fillForm(shadow);
    await vi.waitFor(() => expect(mergeButton(shadow).disabled).toBe(false));

    setFieldValue($<HTMLInputElement>(shadow, '#merge-target-chapter-input'), 'chapter 3');

    await vi.waitFor(() => expect(mergeButton(shadow).disabled).toBe(true));
  });

  it('loads the documents of the picked space', async () => {
    const { shadow, fetchMock } = await open();

    await selectOption(shadow, 'merge-project-selector', 'drivepilot');
    await selectOption(shadow, 'merge-space-selector', 'design');

    await vi.waitFor(() =>
      expect(
        fetchMock.mock.calls.some(([url]) => String(url).endsWith('/projects/drivepilot/spaces/design/documents')),
      ).toBe(true),
    );
    await vi.waitFor(() =>
      expect(
        Array.from($<HTMLSelectElement>(shadow, '#merge-document-selector').options).map((option) => option.value),
      ).toContain('Requirements'),
    );
  });

  it('states what the merge is about to do and waits to be confirmed', async () => {
    const { shadow, fetchMock } = await open();
    await fillForm(shadow);
    await vi.waitFor(() => expect(mergeButton(shadow).disabled).toBe(false));

    mergeButton(shadow).click();

    await vi.waitFor(() => expect(confirmationText(shadow)).toContain('Chapter 2 of Requirements'));
    expect(confirmationText(shadow)).toContain('copied');
    expect(confirmationText(shadow)).toContain('under');
    expect(confirmationText(shadow)).toContain('chapter 3.1 of Product Specification');
    expect(confirmationText(shadow)).toContain('Do you want to proceed?');
    // nothing happens until it is confirmed
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === 'POST')).toBe(false);
  });

  it('says that a move takes the workitems out of the source document', async () => {
    const { shadow } = await open();
    await fillForm(shadow);
    await selectOption(shadow, 'merge-mode-selector', 'MOVE');
    await selectOption(shadow, 'merge-insert-mode-selector', 'AFTER');
    await vi.waitFor(() => expect(mergeButton(shadow).disabled).toBe(false));

    mergeButton(shadow).click();

    await vi.waitFor(() => expect(confirmationText(shadow)).toContain('moved'));
    expect(confirmationText(shadow)).toContain('after');
    expect(confirmationText(shadow)).toContain('leave the source document');
  });

  it('merges nothing when the confirmation is cancelled', async () => {
    const { shadow, fetchMock } = await open();
    await fillForm(shadow);
    await vi.waitFor(() => expect(mergeButton(shadow).disabled).toBe(false));

    mergeButton(shadow).click();
    await vi.waitFor(() => expect(confirmationText(shadow)).not.toBe(''));
    $<HTMLButtonElement>(shadow, '#merge-cancel').click();

    await vi.waitFor(() => expect(confirmationText(shadow)).toBe(''));
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === 'POST')).toBe(false);
    expect(vi.mocked(reloadDocument)).not.toHaveBeenCalled();
  });

  it('posts the merge of the picked chapter into the document which is open', async () => {
    const { shadow, fetchMock } = await open();
    await fillForm(shadow);
    await vi.waitFor(() => expect(mergeButton(shadow).disabled).toBe(false));

    await startMerge(shadow);

    await vi.waitFor(() => {
      const post = fetchMock.mock.calls.find(([, init]) => init?.method === 'POST');
      expect(post).toBeDefined();
      expect(String(post![0])).toContain('/merge/chapter');
      expect(JSON.parse(String(post![1]!.body))).toEqual({
        sourceDocument: { projectId: 'drivepilot', spaceId: 'design', name: 'Requirements' },
        targetDocument: { projectId: 'elibrary', spaceId: 'specification', name: 'Product Specification' },
        mode: 'COPY',
        insertMode: 'UNDER',
        sourceChapterOutlineNumber: '2',
        targetChapterOutlineNumber: '3.1',
        referencedItems: 'KEEP_REFERENCE',
        copyWorkItemLayouts: true,
      });
    });
  });

  it('carries the chosen copy mode and insert mode', async () => {
    const { shadow, fetchMock } = await open();
    await fillForm(shadow);
    await selectOption(shadow, 'merge-mode-selector', 'MOVE');
    await selectOption(shadow, 'merge-insert-mode-selector', 'AFTER');
    await selectOption(shadow, 'merge-referenced-items-selector', 'SKIP');
    await vi.waitFor(() => expect(mergeButton(shadow).disabled).toBe(false));

    await startMerge(shadow);

    await vi.waitFor(() => {
      const post = fetchMock.mock.calls.find(([, init]) => init?.method === 'POST');
      const body = JSON.parse(String(post![1]!.body)) as Record<string, unknown>;
      expect(body.mode).toBe('MOVE');
      expect(body.insertMode).toBe('AFTER');
      expect(body.referencedItems).toBe('SKIP');
    });
  });

  it('states in the same dialog what the merge did, and reloads the document only once it is closed', async () => {
    const { shadow } = await open();
    await fillForm(shadow);
    await vi.waitFor(() => expect(mergeButton(shadow).disabled).toBe(false));

    await startMerge(shadow);

    await vi.waitFor(() => expect(dialogText(shadow)).toContain('Chapter merged'));
    // the dialog the merge was confirmed in is the one showing the result: its stage changed, not the dialog
    expect(confirmationText(shadow)).toBe('');
    expect(resultText(shadow)).toContain('Merged as chapter 3.2');
    expect(resultText(shadow)).toContain('2 workitem(s) created');
    expect(resultText(shadow)).toContain('1 workitem layout(s) copied');
    // the merge report itself, whose entries carry no text of their own
    expect(resultText(shadow)).toContain("workitem 'DP-100' created");
    // The document is reloaded when the user acknowledges the result, not from under them.
    expect(vi.mocked(reloadDocument)).not.toHaveBeenCalled();

    $<HTMLButtonElement>(shadow, '#merge-close').click();

    await vi.waitFor(() => expect(vi.mocked(reloadDocument)).toHaveBeenCalled());
    expect(shadow.querySelector('.merge-dialog')).toBeNull();
  });

  it('does not reload the document when the merge put nothing into it', async () => {
    const { shadow } = await open(
      installFetchMock(
        routes([
          {
            method: 'GET',
            match: /\/merge\/chapter\/jobs\/J-1$/,
            respond: () => finishedJob({ success: false, mergeReport: { prohibited: [], logs: 'chapter not found' } }),
          },
        ]),
      ),
    );
    await fillForm(shadow);
    await vi.waitFor(() => expect(mergeButton(shadow).disabled).toBe(false));

    await startMerge(shadow);

    await vi.waitFor(() => expect(dialogText(shadow)).toContain('Chapter not merged'));
    $<HTMLButtonElement>(shadow, '#merge-close').click();

    await vi.waitFor(() => expect(shadow.querySelector('.merge-dialog')).toBeNull());
    expect(vi.mocked(reloadDocument)).not.toHaveBeenCalled();
  });

  it('keeps asking the job whether it has finished while it is still running', async () => {
    let attempts = 0;
    const { shadow } = await open(
      installFetchMock(
        routes([
          {
            method: 'GET',
            match: /\/merge\/chapter\/jobs\/J-1$/,
            // a running merge is a normal answer, not an error - it must not be a 404
            respond: () => (attempts++ === 0 ? runningJob() : finishedJob(MERGED)),
          },
        ]),
      ),
    );
    await fillForm(shadow);
    await vi.waitFor(() => expect(mergeButton(shadow).disabled).toBe(false));

    await startMerge(shadow);

    await vi.waitFor(() => expect(attempts).toBeGreaterThan(1), { timeout: 10000 });
    await vi.waitFor(() => expect(dialogText(shadow)).toContain('Chapter merged'));
  }, 15000);

  it('asks for the result where the server said the merge is polled', async () => {
    // The started merge names its job in the Location header, and that URL is asked as given - it is an
    // absolute path, not one relative to the REST base.
    const { shadow, fetchMock } = await open();
    await fillForm(shadow);
    await vi.waitFor(() => expect(mergeButton(shadow).disabled).toBe(false));

    await startMerge(shadow);

    await vi.waitFor(() => expect(dialogText(shadow)).toContain('Chapter merged'));
    expect(fetchMock.mock.calls.some(([url]) => String(url) === JOB_URL)).toBe(true);
  });

  it('says what the merge is doing while it does it', async () => {
    // The merge report is written when the merge is over, so what it is doing right now is all the user has
    let attempts = 0;
    const { shadow } = await open(
      installFetchMock(
        routes([
          {
            method: 'GET',
            match: /\/merge\/chapter\/jobs\/J-1$/,
            respond: () => (attempts++ < 2 ? runningJob("Merged workitem 'DP-100'") : finishedJob(MERGED)),
          },
        ]),
      ),
    );
    await fillForm(shadow);
    await vi.waitFor(() => expect(mergeButton(shadow).disabled).toBe(false));

    await startMerge(shadow);

    await vi.waitFor(() => expect($(shadow, '#merge-progress').textContent).toContain("Merged workitem 'DP-100'"), {
      timeout: 10000,
    });
    await vi.waitFor(() => expect(dialogText(shadow)).toContain('Chapter merged'), { timeout: 10000 });
  }, 15000);

  it('reports a merge which was started without a job to ask for its result', async () => {
    const { shadow } = await open(
      installFetchMock(
        routes([
          {
            method: 'POST',
            match: /\/merge\/chapter$/,
            respond: () => new Response(null, { status: 202 }),
          },
        ]),
      ),
    );
    await fillForm(shadow);
    await vi.waitFor(() => expect(mergeButton(shadow).disabled).toBe(false));

    await startMerge(shadow);

    await vi.waitFor(() => expect(resultText(shadow)).toContain('without a job to ask for its result'));
  });

  it('reports what the server said when the merge could not be scheduled', async () => {
    const { shadow } = await open(
      installFetchMock(
        routes([
          {
            method: 'POST',
            match: /\/merge\/chapter$/,
            respond: () => jsonResponse({ message: "Parameter 'sourceDocument' should be provided" }, 400),
          },
        ]),
      ),
    );
    await fillForm(shadow);
    await vi.waitFor(() => expect(mergeButton(shadow).disabled).toBe(false));

    await startMerge(shadow);

    // in the dialog which was spinning a moment ago: a merge must not vanish without a word
    await vi.waitFor(() => expect(resultText(shadow)).toContain("Parameter 'sourceDocument' should be provided"));
    expect(dialogText(shadow)).toContain('Chapter not merged');
    expect(vi.mocked(reloadDocument)).not.toHaveBeenCalled();
  });

  it('names the reason when the merge itself failed', async () => {
    const { shadow } = await open(
      installFetchMock(
        routes([
          {
            method: 'GET',
            match: /\/merge\/chapter\/jobs\/J-1$/,
            respond: () => finishedJob({ success: false, mergeNotAuthorized: true }),
          },
        ]),
      ),
    );
    await fillForm(shadow);
    await vi.waitFor(() => expect(mergeButton(shadow).disabled).toBe(false));

    await startMerge(shadow);

    await vi.waitFor(() => expect(dialogText(shadow)).toContain('You are not authorized to merge into this document'));
  });

  it('asks the user to reload when the document was changed meanwhile', async () => {
    const { shadow } = await open(
      installFetchMock(
        routes([
          {
            method: 'GET',
            match: /\/merge\/chapter\/jobs\/J-1$/,
            respond: () => finishedJob({ success: false, targetModuleHasStructuralChanges: true }),
          },
        ]),
      ),
    );
    await fillForm(shadow);
    await vi.waitFor(() => expect(mergeButton(shadow).disabled).toBe(false));

    await startMerge(shadow);

    await vi.waitFor(() => expect(dialogText(shadow)).toContain('changed meanwhile'));
  });

  it('names what Polarion raised when the merge failed on it', async () => {
    // A merge which failed on something Polarion raised has no result of its own: what went wrong is in the
    // state of its job, and is the only thing the user can act on.
    const { shadow } = await open(
      installFetchMock(
        routes([
          {
            method: 'GET',
            match: /\/merge\/chapter\/jobs\/J-1$/,
            respond: () => jsonResponse({ status: 'FAILED', errorMessage: 'Node has been added before.' }, 409),
          },
        ]),
      ),
    );
    await fillForm(shadow);
    await vi.waitFor(() => expect(mergeButton(shadow).disabled).toBe(false));

    await startMerge(shadow);

    await vi.waitFor(() => expect(resultText(shadow)).toContain('Node has been added before.'));
    expect(dialogText(shadow)).toContain('Chapter not merged');
    expect(vi.mocked(reloadDocument)).not.toHaveBeenCalled();
  });

  it('falls back to a generic message when the failure body is not the expected JSON', async () => {
    const { shadow } = await open(
      installFetchMock(
        routes([
          {
            method: 'POST',
            match: /\/merge\/chapter$/,
            respond: () => new Response('<html>Gateway Timeout</html>', { status: 504 }),
          },
        ]),
      ),
    );
    await fillForm(shadow);
    await vi.waitFor(() => expect(mergeButton(shadow).disabled).toBe(false));

    await startMerge(shadow);

    await vi.waitFor(() => expect(resultText(shadow)).toContain('Error merging chapter'));
  });

  it('spins in the dialog the merge was confirmed in, which cannot be closed meanwhile', async () => {
    // The result is not there on the first ask, so the panel is still waiting for it while this is asserted
    const { shadow } = await open(
      installFetchMock(
        routes([
          {
            method: 'GET',
            match: /\/merge\/chapter\/jobs\/J-1$/,
            respond: () => runningJob(),
          },
        ]),
      ),
    );
    await fillForm(shadow);
    await vi.waitFor(() => expect(mergeButton(shadow).disabled).toBe(false));

    await startMerge(shadow);

    await vi.waitFor(() => expect($(shadow, '#merge-progress').textContent).toContain('Merging chapter 2'));
    expect(shadow.querySelector('#merge-progress .sbb-spinner')).not.toBeNull();
    // the question is answered and the merge is under way: nothing here can be dismissed
    expect(confirmationText(shadow)).toBe('');
    expect(shadow.querySelector('.merge-dialog-close')).toBeNull();
    expect($<HTMLButtonElement>(shadow, '#merge-close').disabled).toBe(true);
    // Escape is refused too
    $<HTMLDialogElement>(shadow, '.merge-dialog').dispatchEvent(new Event('cancel', { cancelable: true }));
    expect(shadow.querySelector('.merge-dialog')).not.toBeNull();
    expect(mergeButton(shadow).disabled).toBe(true);
  });

  it('states in the form why a list it offers is empty', async () => {
    const { shadow } = await open(
      installFetchMock(
        routes([{ method: 'GET', match: /\/projects\/[^/]+\/spaces$/, respond: () => jsonResponse({}, 500) }]),
      ),
    );

    await selectOption(shadow, 'merge-project-selector', 'drivepilot');

    await vi.waitFor(() => expect($(shadow, '.alert-error').textContent).toContain('spaces'));
  });
});

// A panel lives in a shadow root that its mount already gives `.sbb-ui`, so it is scanned through its host.
describe('MergeToolPanel, accessibility', () => {
  it('has no WCAG A/AA violations as opened', async () => {
    await open();
    expect(await a11yViolations(panel!.host)).toEqual([]);
  });

  it('has no WCAG A/AA violations with the form filled in', async () => {
    const { shadow } = await open();
    await fillForm(shadow);
    expect(await a11yViolations(panel!.host)).toEqual([]);
  });

  it('has no WCAG A/AA violations with the merge waiting to be confirmed', async () => {
    const { shadow } = await open();
    await fillForm(shadow);
    await vi.waitFor(() => expect(mergeButton(shadow).disabled).toBe(false));
    mergeButton(shadow).click();
    await vi.waitFor(() => expect(confirmationText(shadow)).toContain('Do you want to proceed?'));
    expect(await a11yViolations(panel!.host)).toEqual([]);
  });

  it('has no WCAG A/AA violations while the merge runs', async () => {
    const { shadow } = await open(
      installFetchMock(routes([{ method: 'GET', match: /\/merge\/chapter\/jobs\/J-1$/, respond: () => runningJob() }])),
    );
    await fillForm(shadow);
    await vi.waitFor(() => expect(mergeButton(shadow).disabled).toBe(false));
    await startMerge(shadow);
    await vi.waitFor(() => expect(shadow.querySelector('#merge-progress .sbb-spinner')).not.toBeNull());
    expect(await a11yViolations(panel!.host)).toEqual([]);
  });

  it('has no WCAG A/AA violations with the result of the merge shown', async () => {
    const { shadow } = await open();
    await fillForm(shadow);
    await vi.waitFor(() => expect(mergeButton(shadow).disabled).toBe(false));
    await startMerge(shadow);
    await vi.waitFor(() => expect(resultText(shadow)).toContain("workitem 'DP-100' created"));
    expect(await a11yViolations(panel!.host)).toEqual([]);
  });
});
