import { afterEach, describe, expect, it, vi } from 'vitest';
import { page } from 'vitest/browser';
import { mountDiffToolPanel } from '../src/formext/mountDiffToolPanel';
import { mountPanel, waitForPanel } from './formextHelpers';
import { installFetchMock } from './mockFetch';
import { settleBeforeCapture } from './visualHelpers';

// Docker-only snapshots of the "Documents Comparison" Document Properties panel, in its two shapes: the
// default (manual revision entry) and the revision-list variant.
//
// These are the one check that the shadow root really carries the styling: the panel is captured through
// its host element, so what is photographed is what Polarion shows - RSP's bundled stylesheet plus
// diff-tool.css injected into the shadow, with nothing from the surrounding page.

let panel: ReturnType<typeof mountPanel> | null = null;

afterEach(() => {
  panel?.unmount();
  panel = null;
  vi.unstubAllGlobals();
});

async function open() {
  installFetchMock([
    { method: 'GET', match: /\/spaces$/, json: [{ id: 'design', name: 'Design' }] },
    { method: 'GET', match: /\/documents$/, json: [{ id: 'Design Spec', title: 'Design Specification' }] },
    {
      method: 'GET',
      match: /\/revisions$/,
      json: [
        { name: '300', baselineName: 'Release 2' },
        { name: '200', baselineName: null },
      ],
    },
  ]);
  panel = mountPanel(mountDiffToolPanel, 'diff-tool-panel');
  await waitForPanel(panel, 'compare-documents');
  // The comboboxes are produced by the shared dropdown, not by React, so wait for them before capturing.
  await vi.waitFor(() => expect(panel!.shadow.querySelectorAll('.searchable-dropdown').length).toBeGreaterThan(4));
  return panel;
}

async function capture(name: string) {
  const host = panel!.host;
  const container = panel!.shadow.querySelector('.form-wrapper') as HTMLElement;
  await page.viewport(720, Math.ceil(container.scrollHeight) + 40);
  await settleBeforeCapture();
  await expect(page.elementLocator(host)).toMatchScreenshot(name);
}

describe.skipIf(!__PIXEL_REFERENCES__)('Documents Comparison panel visual', () => {
  it('loaded (manual revision entry)', async () => {
    await open();

    await capture('diff-tool-panel-loaded');
  });

  it('revision picked from the list, narrowed to baselines', async () => {
    const mounted = await open();
    const shadow = mounted.shadow;

    shadow.querySelector<HTMLInputElement>('#revision-select-from-list')!.click();
    await vi.waitFor(() => expect(shadow.querySelector('#revision-selector')).not.toBeNull());
    shadow.querySelector<HTMLInputElement>('#use-work-items-filter')!.click();
    await vi.waitFor(() => expect(shadow.querySelector('#work-items-filter-input')).not.toBeNull());

    await capture('diff-tool-panel-revision-list');
  });
});
