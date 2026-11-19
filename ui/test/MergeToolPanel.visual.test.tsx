import { afterEach, describe, expect, it, vi } from 'vitest';
import { page } from 'vitest/browser';
import { mountMergeToolPanel } from '../src/formext/mountMergeToolPanel';
import { $, forgetRememberedSelections, mountPanel, selectOption, setFieldValue, waitForPanel } from './formextHelpers';
import { installFetchMock } from './mockFetch';
import { settleBeforeCapture, settleLayout } from './visualHelpers';

// Docker-only snapshots of the "Documents Merge" panel: its space list open, which shows each space by its
// title with its ID as the hint, and the confirmation it asks for before a merge, which names the source by
// project name and space title. The spaces here have titles which differ from their IDs.

let panel: ReturnType<typeof mountPanel> | null = null;

afterEach(() => {
  panel?.unmount();
  panel = null;
  vi.unstubAllGlobals();
  forgetRememberedSelections();
});

describe.skipIf(!__PIXEL_REFERENCES__)('Documents Merge panel visual', () => {
  it('space list open, each space by its title with its ID as the hint', async () => {
    installFetchMock([
      {
        method: 'GET',
        match: /\/spaces$/,
        json: [
          { id: 'Specification', name: 'Specification', title: 'Requirements Specification' },
          { id: 'design', name: 'design', title: 'Design Documents' },
          { id: 'Testing', name: 'Testing', title: 'Testing' },
          { id: '_default', name: 'Default Space', title: 'Default Space' },
        ],
      },
      { method: 'GET', match: /\/documents$/, json: [] },
    ]);
    panel = mountPanel(mountMergeToolPanel, 'merge-tool-panel');
    await waitForPanel(panel, 'merge-chapter');
    const shadow = panel.shadow;
    await selectOption(shadow, 'merge-project-selector', 'drivepilot');
    await selectOption(shadow, 'merge-space-selector', 'design');

    // Opening focuses the search box, whose blinking caret would change pixels between frames.
    const noCaret = document.createElement('style');
    noCaret.textContent = 'input { caret-color: transparent !important; }';
    shadow.appendChild(noCaret);

    await page.viewport(720, 600);
    await settleLayout();
    // The shared dropdown keeps its instance on the <select> it wraps, and opens on a mousedown on its trigger.
    // Every dropdown of the panel has a portal of its own, so the list is looked up in this one's.
    const select = $<HTMLSelectElement>(shadow, '#merge-space-selector') as HTMLSelectElement & {
      _searchableDropdown: { trigger: HTMLElement; portal: HTMLElement };
    };
    const dropdown = select._searchableDropdown;
    dropdown.trigger.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true, composed: true }));
    const list = await vi.waitFor(() => {
      const options = dropdown.portal.querySelector<HTMLElement>('.options');
      expect(options?.querySelectorAll('.option-hint')).toHaveLength(2);
      return options!;
    });
    await settleBeforeCapture();
    await expect(page.elementLocator(list)).toMatchScreenshot('merge-tool-panel-space-list');
  });

  it('merge confirmation, naming the source by project name and space title', async () => {
    installFetchMock([
      { method: 'GET', match: /\/spaces$/, json: [{ id: 'design', name: 'design', title: 'Design Documents' }] },
      { method: 'GET', match: /\/documents$/, json: [{ id: 'Requirements', title: 'Requirements' }] },
    ]);
    panel = mountPanel(mountMergeToolPanel, 'merge-tool-panel');
    await waitForPanel(panel, 'merge-chapter');
    const shadow = panel.shadow;

    await selectOption(shadow, 'merge-project-selector', 'drivepilot');
    await selectOption(shadow, 'merge-space-selector', 'design');
    await selectOption(shadow, 'merge-document-selector', 'Requirements');
    setFieldValue($<HTMLInputElement>(shadow, '#merge-source-chapter-input'), '2');
    setFieldValue($<HTMLInputElement>(shadow, '#merge-target-chapter-input'), '3.1');
    const merge = $<HTMLButtonElement>(shadow, '#merge-chapter');
    await vi.waitFor(() => expect(merge.disabled).toBe(false));
    merge.click();
    await vi.waitFor(() => expect(shadow.querySelector('#merge-confirmation')).not.toBeNull());

    await page.viewport(720, 600);
    await settleLayout();
    await settleBeforeCapture();
    await expect(page.elementLocator($<HTMLDialogElement>(shadow, '.merge-dialog'))).toMatchScreenshot(
      'merge-tool-panel-confirmation',
    );
  });
});
