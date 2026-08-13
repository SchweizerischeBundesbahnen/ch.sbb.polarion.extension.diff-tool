import type { Root } from 'react-dom/client';
import { vi } from 'vitest';
import type { PanelProps } from '../src/formext/panelProps';

// Helpers for the two Document Properties panel test files. The panels are mounted the way Polarion
// mounts them - a bare host div carrying `data-props`, into whose SHADOW ROOT the entry point renders -
// so everything has to be queried through `shadowRoot`, not `document`.

export const PANEL_PROPS: PanelProps = {
  sourceProjectId: 'elibrary',
  sourceSpaceId: 'specification',
  sourceDocument: 'Product Specification',
  sourceDocumentTitle: 'Product Specification',
  sourceRevision: '',
  projects: [
    { id: 'elibrary', name: 'E-Library' },
    { id: 'drivepilot', name: 'Drive Pilot' },
  ],
  linkRoles: [
    { id: 'relates_to', name: 'relates to / relates to' },
    { id: 'branched_from', name: 'branched from / branched to' },
  ],
  configurations: ['Default', 'Strict'],
  handleReferencesTypes: [
    { id: 'DEFAULT', title: 'Remove when no counterpart found', description: 'Removes it.' },
    { id: 'KEEP', title: 'Keep when no counterpart found', description: 'Keeps it.' },
  ],
};

export interface MountedPanel {
  host: HTMLElement;
  shadow: ShadowRoot;
  root: Root;
  unmount: () => void;
}

type MountFn = (selector: string) => Root | undefined;

/** Creates the fragment div Polarion would render, then mounts the panel into its shadow root. */
export function mountPanel(mount: MountFn, id: string, props: Partial<PanelProps> = {}): MountedPanel {
  const host = document.createElement('div');
  host.id = id;
  host.dataset.props = JSON.stringify({ ...PANEL_PROPS, ...props });
  document.body.appendChild(host);

  const root = mount(`#${id}`)!;
  return {
    host: host,
    shadow: host.shadowRoot!,
    root: root,
    unmount: () => {
      root.unmount();
      host.remove();
    },
  };
}

/** Waits until React has committed and the panel's primary button exists. */
export async function waitForPanel(panel: MountedPanel, buttonId: string): Promise<void> {
  await vi.waitFor(() => {
    if (!panel.shadow.querySelector(`#${buttonId}`)) {
      throw new Error(`${buttonId} not rendered yet`);
    }
  });
}

export function $<T extends Element>(shadow: ShadowRoot, selector: string): T {
  const found = shadow.querySelector<T>(selector);
  if (!found) {
    throw new Error(`no element matching ${selector} in the panel`);
  }
  return found;
}

/**
 * Sets a form control's value the way a user would. Assigning `.value` directly is not enough: React
 * caches the last value it wrote on the DOM node and would dedupe the event, so the setter has to be
 * invoked through the prototype descriptor React patched.
 */
export function setFieldValue(element: HTMLInputElement | HTMLSelectElement, value: string): void {
  const prototype = element instanceof HTMLSelectElement ? HTMLSelectElement.prototype : HTMLInputElement.prototype;
  Object.getOwnPropertyDescriptor(prototype, 'value')!.set!.call(element, value);
  element.dispatchEvent(new Event(element instanceof HTMLSelectElement ? 'change' : 'input', { bubbles: true }));
}

/** Picks an option in one of the panel's searchable dropdowns (a real <select> underneath). */
export async function selectOption(shadow: ShadowRoot, selectId: string, value: string): Promise<void> {
  const select = $<HTMLSelectElement>(shadow, `#${selectId}`);
  await vi.waitFor(() => {
    if (!Array.from(select.options).some((option) => option.value === value)) {
      throw new Error(`option ${value} not in #${selectId} yet`);
    }
  });
  setFieldValue(select, value);
}

/**
 * Forgets every remembered dropdown selection (see src/formext/rememberedSelection.ts). The panels
 * persist a user's choice in a cookie, which outlives a test - so without this each test would inherit
 * whatever the previous one picked.
 */
export function forgetRememberedSelections(): void {
  for (const part of document.cookie.split('; ')) {
    const name = part.split('=')[0];
    if (name.startsWith('searchable_dropdown_')) {
      document.cookie = `${name}=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/`;
    }
  }
}

export function clickCheckbox(shadow: ShadowRoot, id: string): void {
  $<HTMLInputElement>(shadow, `#${id}`).click();
}
