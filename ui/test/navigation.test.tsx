import { type ReactNode } from 'react';
import { flushSync } from 'react-dom';
import { createRoot } from 'react-dom/client';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { type Router, usePathname, useRouter, useSearchParams } from '../src/router/navigation';

// The next/navigation replacement. What matters is the contract the call sites rely on: a reactive,
// reference-stable URLSearchParams, and a soft push() that re-renders subscribers without remounting.

let host: HTMLDivElement;
let root: ReturnType<typeof createRoot>;
const initialUrl = window.location.href;

beforeEach(() => {
  host = document.createElement('div');
  document.body.appendChild(host);
  root = createRoot(host);
});

afterEach(() => {
  flushSync(() => root.unmount());
  host.remove();
  window.history.replaceState(null, '', initialUrl);
});

/** Renders synchronously so assertions can run without waiting. */
function render(component: ReactNode): void {
  flushSync(() => root.render(component));
}

function probeText(): string | null {
  return host.querySelector('[data-testid="probe"]')?.textContent ?? null;
}

/** Captures the live router instance out of a render, the way a real component obtains it. */
let capturedRouter: Router;

function Probe({ param = 'config' }: { param?: string }) {
  const params = useSearchParams();
  capturedRouter = useRouter();
  return <span data-testid="probe">{params.get(param)}</span>;
}

describe('useSearchParams', () => {
  it('exposes the current query string', () => {
    window.history.replaceState(null, '', '/documents?sourceProjectId=elibrary&compareAs=Fields');

    function Multi() {
      const params = useSearchParams();
      return <span data-testid="probe">{`${params.get('sourceProjectId')}|${params.get('compareAs')}`}</span>;
    }
    render(<Multi />);

    expect(probeText()).toBe('elibrary|Fields');
  });

  it('returns a reference-stable snapshot while the query string is unchanged', () => {
    window.history.replaceState(null, '', '/documents?config=Default');
    const seen: URLSearchParams[] = [];

    function Collect({ tick }: { tick: number }) {
      seen.push(useSearchParams());
      return <span>{tick}</span>;
    }
    render(<Collect tick={1} />);
    render(<Collect tick={2} />);

    // A fresh URLSearchParams per call would loop useSyncExternalStore forever and thrash the
    // useMemo(..., [searchParams]) dependencies at the call sites.
    expect(seen.length).toBeGreaterThanOrEqual(2);
    expect(seen.every((params) => params === seen[0])).toBe(true);
  });

  it('rebuilds the snapshot when the query string changes', () => {
    window.history.replaceState(null, '', '/documents?config=A');
    const seen: URLSearchParams[] = [];

    function Collect() {
      seen.push(useSearchParams());
      capturedRouter = useRouter();
      return null;
    }
    render(<Collect />);
    flushSync(() => capturedRouter.push('/documents?config=B'));

    expect(seen.at(-1)).not.toBe(seen[0]);
    expect(seen.at(-1)!.get('config')).toBe('B');
  });

  it('re-renders subscribers after router.push and reflects the new params', () => {
    window.history.replaceState(null, '', '/documents?compareAs=Workitems');
    render(<Probe param="compareAs" />);
    expect(probeText()).toBe('Workitems');

    flushSync(() => capturedRouter.push('/documents?compareAs=Fields'));

    expect(probeText()).toBe('Fields');
    expect(window.location.search).toBe('?compareAs=Fields');
  });

  it('re-renders on popstate (browser back)', () => {
    window.history.replaceState(null, '', '/documents?config=A');
    render(<Probe />);

    window.history.pushState(null, '', '/documents?config=B');
    flushSync(() => window.dispatchEvent(new PopStateEvent('popstate')));

    expect(probeText()).toBe('B');
  });

  it('still tracks popstate after a subscriber unmounts and a new one mounts', () => {
    // Guards the listener bookkeeping: the window popstate listener is attached for the first
    // subscriber and detached only when the last one leaves, so an unmount must not deafen the next.
    window.history.replaceState(null, '', '/documents?config=A');
    render(<Probe />);
    render(<span />);
    render(<Probe />);

    window.history.pushState(null, '', '/documents?config=C');
    flushSync(() => window.dispatchEvent(new PopStateEvent('popstate')));

    expect(probeText()).toBe('C');
  });
});

describe('usePathname', () => {
  it('returns the current pathname', () => {
    window.history.replaceState(null, '', '/collections?x=1');

    function PathProbe() {
      return <span data-testid="probe">{usePathname()}</span>;
    }
    render(<PathProbe />);

    expect(probeText()).toBe('/collections');
  });
});

describe('useRouter', () => {
  it('returns the same instance across renders and components', () => {
    const seen: Router[] = [];

    function Collect() {
      seen.push(useRouter());
      return null;
    }
    render(
      <>
        <Collect />
        <Collect />
      </>,
    );
    render(
      <>
        <Collect />
        <Collect />
      </>,
    );

    // Stability is what makes it safe in dependency arrays, as next/navigation's is.
    expect(seen.length).toBeGreaterThanOrEqual(4);
    expect(seen.every((router) => router === seen[0])).toBe(true);
  });

  it('replace() swaps the current entry instead of adding one', () => {
    window.history.replaceState(null, '', '/workitems?config=1');
    render(<Probe />);
    const lengthBefore = window.history.length;

    flushSync(() => capturedRouter.replace('/workitems?config=2'));

    expect(probeText()).toBe('2');
    expect(window.history.length).toBe(lengthBefore);
  });

  it('push() adds a history entry', () => {
    window.history.replaceState(null, '', '/workitems?config=1');
    render(<Probe />);
    const lengthBefore = window.history.length;

    flushSync(() => capturedRouter.push('/workitems?config=2'));

    expect(window.history.length).toBe(lengthBefore + 1);
  });
});
