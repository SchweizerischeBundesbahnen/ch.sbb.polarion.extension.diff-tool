import { type ReactNode, useContext } from 'react';
import { flushSync } from 'react-dom';
import { createRoot } from 'react-dom/client';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import AppContext from '../src/components/AppContext';
import AppShell from '../src/components/AppShell';
import ErrorBoundary from '../src/components/ErrorBoundary';
import PublicShell from '../src/components/PublicShell';
import { installFetchMock } from './mockFetch';

// The three components that replaced the Next.js App Router conventions: the root layout's context
// provider (AppShell), its page frame (PublicShell) and the src/app/error.js segment boundary
// (ErrorBoundary).

let host: HTMLDivElement;
let root: ReturnType<typeof createRoot>;

beforeEach(() => {
  host = document.createElement('div');
  document.body.appendChild(host);
  root = createRoot(host);
});

afterEach(() => {
  flushSync(() => root.unmount());
  host.remove();
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

function render(component: ReactNode): void {
  flushSync(() => root.render(component));
}

describe('PublicShell', () => {
  it('renders children inside the token-scoped page frame', () => {
    render(
      <PublicShell>
        <p data-testid="child">content</p>
      </PublicShell>,
    );

    const main = host.querySelector('main')!;
    // Both classes matter: `.app` carries the full-viewport layout, `.sbb-ui` scopes generic's --sbb-*
    // design tokens to this app's subtree (issue #515).
    expect(main.className).toBe('app sbb-ui');
    expect(main.querySelector('[data-testid="child"]')!.textContent).toBe('content');
  });
});

describe('ErrorBoundary', () => {
  it('renders children while nothing throws', () => {
    render(
      <ErrorBoundary>
        <p data-testid="child">fine</p>
      </ErrorBoundary>,
    );

    expect(host.querySelector('[data-testid="child"]')!.textContent).toBe('fine');
    expect(host.textContent).not.toContain('Something went wrong!');
  });

  it('shows the fallback and logs when a child throws', () => {
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => {});

    function Boom(): ReactNode {
      throw new Error('kaboom');
    }
    render(
      <ErrorBoundary>
        <Boom />
      </ErrorBoundary>,
    );

    expect(host.textContent).toContain('Something went wrong!');
    expect(host.querySelector('button')!.textContent).toBe('Try again');
    expect(consoleError).toHaveBeenCalled();
  });

  it('clears the error when "Try again" is pressed', () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
    let shouldThrow = true;

    function Flaky(): ReactNode {
      if (shouldThrow) {
        throw new Error('kaboom');
      }
      return <p data-testid="child">recovered</p>;
    }
    render(
      <ErrorBoundary>
        <Flaky />
      </ErrorBoundary>,
    );
    expect(host.textContent).toContain('Something went wrong!');

    shouldThrow = false;
    flushSync(() => host.querySelector('button')!.click());

    expect(host.querySelector('[data-testid="child"]')!.textContent).toBe('recovered');
  });
});

describe('AppShell', () => {
  it('publishes the app context to descendants', () => {
    installFetchMock([{ match: /./, json: {} }]);
    let seen: Record<string, unknown> | undefined;

    function Consumer() {
      // AppContext is created untyped in JS (createContext(null)), so TS narrows it to null here; the
      // viewer reads context.state at runtime.
      seen = (useContext(AppContext) as unknown as { state: Record<string, unknown> }).state;
      return null;
    }
    render(
      <AppShell>
        <Consumer />
      </AppShell>,
    );

    // A representative slice of useAppContext's state plus its setters, which is what every viewer
    // component reads through context.state.
    expect(seen).toBeDefined();
    expect(seen).toHaveProperty('dataLoaded');
    expect(seen).toHaveProperty('headerPinned');
    expect(typeof seen!.setControlPaneAccessible).toBe('function');
  });

  it('renews the session at most once per minute of user activity', async () => {
    const fetchMock = installFetchMock([{ match: /renewal=true/, json: {} }]);
    render(<AppShell>{null}</AppShell>);

    document.dispatchEvent(new MouseEvent('mousemove', { bubbles: true }));
    document.dispatchEvent(new MouseEvent('mousemove', { bubbles: true }));

    // useSessionRenewal seeds its timestamp at mount, so activity inside the first minute is throttled
    // away entirely - no request at all.
    await vi.waitFor(() => expect(fetchMock).not.toHaveBeenCalled());
  });

  it('its scroll handler is currently a no-op, by inheritance from the Next.js version', () => {
    // Pins the behaviour documented in AppShell: the handler reads document.body.scrollTop, which is
    // always 0 in standards mode (documentElement is the scrolling element), so headerPinned never
    // flips - and the "pinned" class it would produce has no CSS rule anywhere. Asserting it keeps the
    // port honest: if someone makes this work, this test fails and they must decide deliberately.
    installFetchMock([{ match: /./, json: {} }]);
    let seen: Record<string, unknown> | undefined;

    function Consumer() {
      // AppContext is created untyped in JS (createContext(null)), so TS narrows it to null here; the
      // viewer reads context.state at runtime.
      seen = (useContext(AppContext) as unknown as { state: Record<string, unknown> }).state;
      return null;
    }
    render(
      <AppShell>
        <Consumer />
      </AppShell>,
    );

    flushSync(() => document.body.dispatchEvent(new Event('scroll')));

    expect(seen!.headerPinned).toBe(false);
  });

  it('detaches its scroll listener on unmount', () => {
    installFetchMock([{ match: /./, json: {} }]);
    const remove = vi.spyOn(document.body, 'removeEventListener');

    render(<AppShell>{null}</AppShell>);
    flushSync(() => root.unmount());
    root = createRoot(host);

    expect(remove).toHaveBeenCalledWith('scroll', expect.any(Function));
  });
});
