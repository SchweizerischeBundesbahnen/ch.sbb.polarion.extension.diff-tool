import { afterEach, describe, expect, it, vi } from 'vitest';
import { page } from 'vitest/browser';
import { mountCopyToolPanel } from '../src/formext/mountCopyToolPanel';
import { mountPanel, waitForPanel } from './formextHelpers';
import { installFetchMock } from './mockFetch';

// Docker-only snapshot of the "Documents Copy" Document Properties panel, captured through its shadow
// host - so this also proves the shadow root carries the styling (see DiffToolPanel.visual.test.tsx).

let panel: ReturnType<typeof mountPanel> | null = null;

afterEach(() => {
  panel?.unmount();
  panel = null;
  vi.unstubAllGlobals();
});

describe('Documents Copy panel visual', () => {
  it('loaded', async () => {
    installFetchMock([{ method: 'GET', match: /\/spaces$/, json: [{ id: 'design', name: 'Design' }] }]);
    panel = mountPanel(mountCopyToolPanel, 'copy-tool-panel', {
      linkRoles: [
        { id: '', name: 'none' },
        { id: 'relates_to', name: 'relates to / relates to' },
      ],
    });
    await waitForPanel(panel, 'create-document');
    await vi.waitFor(() => expect(panel!.shadow.querySelectorAll('.searchable-dropdown').length).toBe(5));

    const container = panel.shadow.querySelector('.form-wrapper') as HTMLElement;
    await page.viewport(720, Math.ceil(container.scrollHeight) + 40);
    await expect(page.elementLocator(panel.host)).toMatchScreenshot('copy-tool-panel-loaded');
  });
});
