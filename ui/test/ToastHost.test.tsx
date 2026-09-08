import { afterEach, describe, expect, it, vi } from 'vitest';
import { mountCopyToolPanel } from '../src/formext/mountCopyToolPanel';
import { mountDiffToolPanel } from '../src/formext/mountDiffToolPanel';
import { reportFailure } from '../src/formext/reporting';
import { type MountedPanel, PANEL_PROPS, forgetRememberedSelections, mountPanel, waitForPanel } from './formextHelpers';
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

/**
 * Re-renders the fragment the way Polarion does: a brand new host div under the same id, mounted without
 * anyone having unmounted the previous root. Answers the container the previous panel was rendered into,
 * which is what says whether that root was ended or merely orphaned.
 */
function reRenderFragment(id: string, previous: MountedPanel): HTMLElement {
  const oldContainer = previous.shadow.querySelector('.form-wrapper') as HTMLElement;
  previous.host.remove();
  const host = document.createElement('div');
  host.id = id;
  host.dataset.props = JSON.stringify(PANEL_PROPS);
  document.body.appendChild(host);
  mountDiffToolPanel(`#${id}`);
  return oldContainer;
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

  // Polarion never unmounts these roots, so a re-rendered fragment used to leave the previous tree
  // running - and with it a host entry and a listener in this module's registry, calling setState on a
  // detached tree for the rest of the session. The mount path ends it now; see takeOverPanelRoot.
  it('ends the previous panel when Polarion re-renders the fragment', async () => {
    installFetchMock([{ method: 'GET', match: /\/spaces$/, json: [{ id: 'design', name: 'Design' }] }]);
    const first = mountPanel(mountDiffToolPanel, 'diff-tool-panel');
    await waitForPanel(first, 'compare-documents');

    const orphaned = reRenderFragment('diff-tool-panel', first);

    // An unmounted root empties its container; an orphaned one leaves the whole panel standing in it.
    await vi.waitFor(() => expect(orphaned.innerHTML).toBe(''));
    const remounted = document.querySelector<HTMLElement>('#diff-tool-panel')!;
    mounted = [
      {
        host: remounted,
        shadow: remounted.shadowRoot!,
        root: null as never,
        unmount: () => remounted.remove(),
      },
    ];
    // Only the surviving panel reports, so the report is not sent to a detached shadow root.
    await vi.waitFor(() => expect(remounted.shadowRoot!.querySelector('#compare-documents')).not.toBeNull());
    reportFailure('Document already exists');
    await vi.waitFor(() => expect(toastText(remounted.shadowRoot!, 'error')).toBe('Document already exists'));
    expect(toastText(first.shadow, 'error')).toBe('');
  });
});
