import { afterEach, describe, expect, it, vi } from 'vitest';
import { mountMergeToolPanel } from '../src/formext/mountMergeToolPanel';
import { $, forgetRememberedSelections, mountPanel, selectOption, setFieldValue, waitForPanel } from './formextHelpers';
import { type FetchMock, type Route, installFetchMock, jsonResponse } from './mockFetch';
import { clearToasts, toasted } from './toasts';

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

/** A job which has produced its result, i.e. what polling answers with once the merge is over. */
const finishedJob = (mergeResult: unknown) => ({
  jobId: 'J-1',
  state: 'FINISHED',
  logUrl: '/polarion/job-report?jobId=J-1',
  mergeResult: mergeResult,
});

/** A job which is still running, i.e. what polling answers with in the meantime. */
const RUNNING_JOB = { jobId: 'J-1', state: 'RUNNING', logUrl: '/polarion/job-report?jobId=J-1' };

function routes(overrides: Route[] = []): Route[] {
  return [
    ...overrides,
    { method: 'GET', match: /\/projects\/[^/]+\/spaces$/, json: SPACES },
    { method: 'GET', match: /\/documents$/, json: DOCUMENTS },
    { method: 'POST', match: /\/merge\/chapter$/, json: { jobId: 'J-1', logUrl: '/polarion/job-report?jobId=J-1' } },
    { method: 'GET', match: /\/merge\/chapter\/jobs\/J-1$/, json: finishedJob(MERGED) },
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

/** What the confirmation dialog says, empty while it is not open. */
const confirmationText = (shadow: ShadowRoot) => shadow.querySelector('#merge-confirmation')?.textContent ?? '';

/** Clicks Merge and confirms, which is what starts a merge. */
async function startMerge(shadow: ShadowRoot) {
  mergeButton(shadow).click();
  await vi.waitFor(() => expect(shadow.querySelector('#merge-confirmation')).not.toBeNull());
  $<HTMLButtonElement>(shadow, '.rsp-modal-footer .sbb-btn--primary').click();
}

/** What the result dialog says, empty while it is not open. */
const dialogText = (shadow: ShadowRoot) => shadow.querySelector('.merge-result-dialog')?.textContent ?? '';

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
    $<HTMLButtonElement>(shadow, '.rsp-modal-footer .sbb-btn--secondary').click();

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
        copyWorkItemLayouts: false,
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

  it('states in a dialog what the merge did, and reloads the document only once it is closed', async () => {
    const { shadow } = await open();
    await fillForm(shadow);
    await vi.waitFor(() => expect(mergeButton(shadow).disabled).toBe(false));

    await startMerge(shadow);

    await vi.waitFor(() => expect(dialogText(shadow)).toContain('Chapter merged'));
    expect(dialogText(shadow)).toContain('Merged as chapter 3.2');
    expect(dialogText(shadow)).toContain('2 workitem(s) created');
    expect(dialogText(shadow)).toContain('1 workitem layout(s) copied');
    // the merge report itself, whose entries carry no text of their own
    expect(dialogText(shadow)).toContain("workitem 'DP-100' created");
    // The document is reloaded when the user acknowledges the result, not from under them.
    expect(vi.mocked(reloadDocument)).not.toHaveBeenCalled();

    $<HTMLButtonElement>(shadow, '#merge-result-ok').click();

    await vi.waitFor(() => expect(vi.mocked(reloadDocument)).toHaveBeenCalled());
    expect(shadow.querySelector('.merge-result-dialog')).toBeNull();
  });

  it('does not reload the document when the merge put nothing into it', async () => {
    const { shadow } = await open(
      installFetchMock(
        routes([
          {
            method: 'GET',
            match: /\/merge\/chapter\/jobs\/J-1$/,
            json: finishedJob({ success: false, mergeReport: { prohibited: [], logs: 'chapter not found' } }),
          },
        ]),
      ),
    );
    await fillForm(shadow);
    await vi.waitFor(() => expect(mergeButton(shadow).disabled).toBe(false));

    await startMerge(shadow);

    await vi.waitFor(() => expect(dialogText(shadow)).toContain('Chapter not merged'));
    $<HTMLButtonElement>(shadow, '#merge-result-ok').click();

    await vi.waitFor(() => expect(shadow.querySelector('.merge-result-dialog')).toBeNull());
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
            // a running job is a normal answer, not an error - it must not be a 404
            respond: () => (attempts++ === 0 ? jsonResponse(RUNNING_JOB) : jsonResponse(finishedJob(MERGED))),
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

    expect(await toasted(shadow, 'error')).toBe("Parameter 'sourceDocument' should be provided");
    expect(vi.mocked(reloadDocument)).not.toHaveBeenCalled();
  });

  it('names the reason when the merge itself failed', async () => {
    const { shadow } = await open(
      installFetchMock(
        routes([
          {
            method: 'GET',
            match: /\/merge\/chapter\/jobs\/J-1$/,
            json: finishedJob({ success: false, mergeNotAuthorized: true }),
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
            json: finishedJob({ success: false, targetModuleHasStructuralChanges: true }),
          },
        ]),
      ),
    );
    await fillForm(shadow);
    await vi.waitFor(() => expect(mergeButton(shadow).disabled).toBe(false));

    await startMerge(shadow);

    await vi.waitFor(() => expect(dialogText(shadow)).toContain('changed meanwhile'));
  });

  it('reports a job which finished without a result as a failure of its own', async () => {
    const { shadow } = await open(
      installFetchMock(
        routes([
          {
            method: 'GET',
            match: /\/merge\/chapter\/jobs\/J-1$/,
            json: { jobId: 'J-1', state: 'FINISHED', statusMessage: 'Chapter merge failed: boom' },
          },
        ]),
      ),
    );
    await fillForm(shadow);
    await vi.waitFor(() => expect(mergeButton(shadow).disabled).toBe(false));

    await startMerge(shadow);

    expect(await toasted(shadow, 'error')).toBe('Chapter merge failed: boom');
    expect(shadow.querySelector('.merge-result-dialog')).toBeNull();
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

    expect(await toasted(shadow, 'error')).toBe('Error merging chapter');
  });

  it('shows the progress overlay while the merge runs', async () => {
    // The result is not there on the first ask, so the panel is still waiting for it while this is asserted
    const { shadow } = await open(
      installFetchMock(
        routes([
          {
            method: 'GET',
            match: /\/merge\/chapter\/jobs\/J-1$/,
            respond: () => jsonResponse(RUNNING_JOB),
          },
        ]),
      ),
    );
    await fillForm(shadow);
    await vi.waitFor(() => expect(mergeButton(shadow).disabled).toBe(false));

    await startMerge(shadow);

    await vi.waitFor(() => expect($(shadow, '#merge-in-progress-message').textContent).toContain('Merging chapter 2'));
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
