import { useCallback, useEffect } from 'react';
import { getCookie, setCookie } from '@sbb-polarion/react-sbb-polarion';

/**
 * Per-dropdown "remember my last choice" for the Document Properties panels.
 *
 * The legacy panels got this for free: the generic SearchableDropdown saved every selection to a cookie
 * (`_saveSelection`) and `DiffTool.js` / `CopyTool.js` called `restoreSelection()` on the project, link
 * role, configuration and referenced-workitems dropdowns while building the view - plus `refresh()`,
 * which restores too, on the space / document / revision dropdowns once their options had loaded. So a
 * user who compares against the same target project and configuration all day picked them once.
 *
 * That stopped working in the React port, in both directions: react-sbb-polarion's
 * `createSearchableSelect` passes `rememberSelection: false`, so the dropdown no longer writes the
 * cookie, and `SearchableSelect` drives the selection from React's controlled value instead of calling
 * `restoreSelection()`. Which is the right call for the library - a dropdown silently re-selecting
 * behind React's back on every `refresh()` would fight the controlled value - so the persistence belongs
 * here, where React stays the single source of truth.
 *
 * The cookie NAME is deliberately the one the legacy dropdown used, so choices remembered before this
 * upgrade are still found. Expiry differs: RSP's shared `setCookie` writes a year, the legacy dropdown
 * wrote 30 days. Not worth a private cookie writer.
 */
const cookieName = (selectId: string) => `searchable_dropdown_${selectId}`;

/** The remembered value for a dropdown, or `''` when there is none. */
export function rememberedSelection(selectId: string): string {
  return getCookie(cookieName(selectId)) ?? '';
}

/**
 * The remembered value, but only if it is still one of the offered options - a project that has since
 * been renamed, or a configuration that no longer exists in the target scope, must not come back. The
 * legacy `restoreSelection()` made the same check (`items.find(...)`) and cleared the selection when it
 * failed.
 */
export function rememberedIfOffered(selectId: string, optionIds: readonly string[]): string {
  const remembered = rememberedSelection(selectId);
  return remembered && optionIds.includes(remembered) ? remembered : '';
}

/**
 * Wraps a state setter so a **user's** choice is also remembered. Deliberately not used for the cascade
 * resets (picking a project clears the space below it): the legacy code cleared those `<select>`s
 * without going through `selectItem`, so the cookie survived, and the space is then re-adopted if the
 * new project happens to offer one with the same id - `_default` almost always does.
 */
export function useRemembering(selectId: string, setValue: (value: string) => void): (value: string) => void {
  return useCallback(
    (value: string) => {
      setValue(value);
      // An empty value is stored as empty rather than deleted; `rememberedSelection` treats both the
      // same, so clearing a dropdown correctly stops it from being restored.
      setCookie(cookieName(selectId), value);
    },
    [selectId, setValue],
  );
}

/**
 * Re-applies the remembered value once a dropdown's options arrive, which is what `refresh()` did for
 * the asynchronously loaded lists (spaces, documents, revisions).
 *
 * `optionIds` must be referentially stable (memoise it), since it is what decides when this re-runs.
 */
export function useAdoptRemembered(
  selectId: string,
  optionIds: readonly string[],
  setValue: (value: string) => void,
): void {
  useEffect(() => {
    const remembered = rememberedIfOffered(selectId, optionIds);
    if (remembered) {
      setValue(remembered);
    }
  }, [selectId, optionIds, setValue]);
}
