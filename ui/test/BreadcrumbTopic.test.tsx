import type { ReactNode } from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import BreadcrumbTopic from '../src/topics/BreadcrumbTopic';

// The port of the deleted webapp/diff-tool/js/breadcrumb.js. Under Vitest window.top is the test page itself,
// so both halves of the bridge handoff are observable here.

type Shell = Window & { SbbBreadcrumbBridge?: { install: (config: unknown) => void } };

// Vitest browser mode runs each test file in a same-origin iframe of the tester page, so window.top is a real
// other window here - which is exactly the shell the component talks to inside Polarion.
const shell = window.top as Shell;
const shellDocument = () => shell.document;
const loader = () => shellDocument().getElementById('sbb-breadcrumb-bridge-loader') as HTMLScriptElement | null;

// The component renders nothing, and the browser-mode render() needs an element to build a locator from.
const host = (children: ReactNode) => <div id="breadcrumb-host">{children}</div>;

// The shell document outlives a test file (browser mode runs them serially in one page), so a loader another
// file's page render left behind has to go before each test as well as after it.
beforeEach(() => {
  delete shell.SbbBreadcrumbBridge;
  loader()?.remove();
});

afterEach(() => {
  cleanup();
  delete shell.SbbBreadcrumbBridge;
  loader()?.remove();
  vi.restoreAllMocks();
});

describe('BreadcrumbTopic', () => {
  it('re-labels through an already installed bridge', async () => {
    const install = vi.fn();
    shell.SbbBreadcrumbBridge = { install: install };

    render(host(<BreadcrumbTopic marker="diff-tool" title="Collections" parent="Diff Tool" icon="/icons/c.svg" />));

    await vi.waitFor(() => expect(install).toHaveBeenCalled());
    expect(install).toHaveBeenCalledWith({
      marker: 'diff-tool',
      title: 'Collections',
      parent: 'Diff Tool',
      icon: '/icons/c.svg',
    });
    expect(loader()).toBeNull();
  });

  it('installs the bridge with the parent and the icon when it is not there yet', async () => {
    render(
      host(<BreadcrumbTopic marker="diff-tool" title="Multiple Work Items" parent="Diff Tool" icon="/icons/w.svg" />),
    );

    await vi.waitFor(() => expect(loader()).not.toBeNull());
    const script = loader()!;
    expect(script.src).toContain('/polarion/diff-tool-app/ui/generic/js/modules/BreadcrumbBridge.js');
    expect(script.dataset.marker).toBe('diff-tool');
    expect(script.dataset.title).toBe('Multiple Work Items');
    expect(script.dataset.parent).toBe('Diff Tool');
    expect(script.dataset.icon).toBe('/icons/w.svg');
  });

  it('leaves the parent out for a root topic', async () => {
    render(host(<BreadcrumbTopic marker="diff-tool" title="Diff Tool" icon="/icons/d.svg" />));

    await vi.waitFor(() => expect(loader()).not.toBeNull());
    expect(loader()!.dataset.parent).toBeUndefined();
  });

  it('replaces a loader left by an earlier topic instead of stacking a second one', async () => {
    // What the shell head looks like after the work items topic was opened and the bridge never loaded
    const stale = shellDocument().createElement('script');
    stale.id = 'sbb-breadcrumb-bridge-loader';
    stale.dataset.title = 'Multiple Work Items';
    shellDocument().head.appendChild(stale);

    render(host(<BreadcrumbTopic marker="diff-tool" title="Collections" parent="Diff Tool" icon="/icons/c.svg" />));

    await vi.waitFor(() => expect(loader()!.dataset.title).toBe('Collections'));
    expect(shellDocument().querySelectorAll('#sbb-breadcrumb-bridge-loader').length).toBe(1);
  });

  it('renders nothing and survives a shell it cannot reach', async () => {
    shell.SbbBreadcrumbBridge = {
      install: () => {
        throw new Error('cross-origin');
      },
    };

    render(host(<BreadcrumbTopic marker="diff-tool" title="Diff Tool" />));

    await vi.waitFor(() => expect(document.querySelector('#breadcrumb-host')).not.toBeNull());
    // The component contributes no markup, and a bridge that throws leaves the page alone
    expect(document.querySelector('#breadcrumb-host')!.innerHTML).toBe('');
    expect(loader()).toBeNull();
  });
});
