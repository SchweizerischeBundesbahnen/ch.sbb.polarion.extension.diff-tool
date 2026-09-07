import { afterEach, describe, expect, it, vi } from 'vitest';
import { mountCopyToolPanel } from '../src/formext/mountCopyToolPanel';
import { mountDiffToolPanel } from '../src/formext/mountDiffToolPanel';
import { reportFailure } from '../src/formext/reporting';
import { type MountedPanel, forgetRememberedSelections, mountPanel, waitForPanel } from './formextHelpers';
import { installFetchMock } from './mockFetch';
import { clearToasts, toastText } from './toasts';

// Both of this extension's panels are on the one Document Properties page, and `toast()` broadcasts to
// every mounted `Toaster` - so what these cover is that exactly one host renders. See
// src/formext/ToastHost.tsx.

// Sonner renders its host only while it has something to show, so this counts what is on screen.
const hosts = (panel: MountedPanel) => panel.shadow.querySelectorAll('[data-sonner-toast]').length;

let mounted: MountedPanel[] = [];

afterEach(() => {
  mounted.forEach((panel) => panel.unmount());
  mounted = [];
  forgetRememberedSelections();
  clearToasts();
  vi.unstubAllGlobals();
});

async function openBothPanels() {
  installFetchMock([{ method: 'GET', match: /\/spaces$/, json: [{ id: 'design', name: 'Design' }] }]);
  const comparison = mountPanel(mountDiffToolPanel, 'diff-tool-panel');
  await waitForPanel(comparison, 'compare-documents');
  const copy = mountPanel(mountCopyToolPanel, 'copy-tool-panel');
  await waitForPanel(copy, 'create-document');
  mounted = [comparison, copy];
  return { comparison: comparison, copy: copy };
}

describe('ToastHost', () => {
  it('reports a failure once, in the panel whose host is up', async () => {
    const { comparison, copy } = await openBothPanels();

    reportFailure('Document already exists');

    // The panel mounted last is the one reporting: mount order is the whole rule here.
    await vi.waitFor(() => expect(toastText(copy.shadow, 'error')).toBe('Document already exists'));
    expect(toastText(comparison.shadow, 'error')).toBe('');
    expect(hosts(comparison) + hosts(copy)).toBe(1);
  });

  it('hands the reporting back when the panel holding it goes, taking its report with it', async () => {
    const { comparison, copy } = await openBothPanels();
    reportFailure('Document already exists');
    await vi.waitFor(() => expect(toastText(copy.shadow, 'error')).toBe('Document already exists'));

    copy.unmount();
    mounted = [comparison];

    // The comparison panel takes over - and is handed nothing, sonner's replay of a still-active toast
    // being what would otherwise move the copy panel's report into it.
    await vi.waitFor(() => expect(hosts(comparison)).toBe(0));
    expect(toastText(comparison.shadow, 'error')).toBe('');

    // ...and it does report from then on, the reporting having actually changed hands.
    reportFailure('Error occurred loading spaces');
    await vi.waitFor(() => expect(toastText(comparison.shadow, 'error')).toBe('Error occurred loading spaces'));
  });
});
