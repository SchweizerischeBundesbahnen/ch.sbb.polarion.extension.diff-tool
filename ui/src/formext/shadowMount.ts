import styleText from '@sbb-polarion/react-sbb-polarion/style.css?inline';

interface ShadowMountOptions {
  /** Classes for the inner container React mounts into (token scope + the wrapper classes the panel's
   *  own CSS expects, e.g. `comparison form-wrapper sbb-ui`). */
  containerClassName?: string;
  /** Extension panel CSS to inject as `<style>` INSIDE the shadow root (bundled via `?inline`). */
  styleTexts?: string[];
}

/**
 * Attaches an open shadow root to `host` and returns a fresh container element inside it for React to
 * mount into.
 *
 * Why a shadow root: the Polarion Document Properties pane is a single shared page where several
 * extensions render their own panels, each possibly built against a different react-sbb-polarion
 * version - and diff-tool itself renders *two* panels there. Plain CSS is global by selector, so those
 * panels would clash. A shadow root gives true, two-way encapsulation: react-sbb-polarion's stylesheet
 * (injected as a `<style>` INSIDE the shadow) styles only this panel and cannot leak out, and the
 * page's / other extensions' styles cannot leak in. The SearchableDropdown popup portal is shadow-aware
 * (it appends into this root via `getRootNode()` and uses `composedPath()` for its outside-click
 * check), so the dropdowns are styled and isolated too.
 *
 * `?inline` gives the built stylesheet as a string; the extension's panel CSS is bundled the same way
 * and injected as an additional `<style>`, so no runtime `<link>` to a Polarion-served file is needed.
 */
export function mountInShadow(host: HTMLElement, options: ShadowMountOptions = {}): HTMLElement {
  const shadow = host.shadowRoot ?? host.attachShadow({ mode: 'open' });
  shadow.replaceChildren();

  const style = document.createElement('style');
  style.textContent = styleText;
  shadow.appendChild(style);

  // Apply the canonical SBB control font + base size (control-tokens.css `--sbb-control-font-family` /
  // `--sbb-control-font-size`, bundled in the stylesheet above and declared on `.sbb-ui`). Needed
  // because inside a shadow root both font-family and font-size inherit from the host - a bare <div>
  // in the Document Properties table - so without this the text falls back to the browser defaults
  // (serif, 16px) instead of Polarion's `body { font-size: 13px }` + Segoe UI. Targeting `.sbb-ui`
  // covers both the container and the SearchableDropdown portal (both carry the class); tokens read
  // from the bundled CSS with literal fallbacks matching their values.
  const baseStyle = document.createElement('style');
  baseStyle.textContent =
    '.sbb-ui { font-family: var(--sbb-control-font-family, "Segoe UI", "Selawik", "Open Sans", Arial, sans-serif); font-size: var(--sbb-control-font-size, 13px); }';
  shadow.appendChild(baseStyle);

  for (const css of options.styleTexts ?? []) {
    const panel = document.createElement('style');
    panel.textContent = css;
    shadow.appendChild(panel);
  }

  const container = document.createElement('div');
  if (options.containerClassName) {
    container.className = options.containerClassName;
  }
  shadow.appendChild(container);
  return container;
}

/**
 * Polarion's own markup wraps each Document Properties field in a `<div style="overflow: hidden">`
 * inside a `<td>`, which clips a dropdown popup that extends past the field's box. The legacy panels
 * called this on one of their `<select>` elements; the equivalent now has to start from the shadow
 * **host**, because `closest()` does not cross a shadow boundary and everything the panel renders lives
 * inside the shadow root.
 *
 * A shadow root does not fix an ancestor's `overflow: hidden`, so this is still required even though
 * the popup portals into the shadow.
 */
export function resetParentsOverflowHidden(element: Element | null): void {
  const parentCell = element?.closest('td');
  if (!parentCell) {
    return;
  }
  for (const child of parentCell.children) {
    if (child instanceof HTMLDivElement && child.style.overflow === 'hidden') {
      child.style.overflow = 'visible';
      break;
    }
  }
}
