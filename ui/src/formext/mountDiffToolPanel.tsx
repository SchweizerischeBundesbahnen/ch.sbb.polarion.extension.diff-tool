import { type Root, createRoot } from 'react-dom/client';
import DiffToolPanel from './DiffToolPanel';
import panelStyle from './diff-tool.css?inline';
import { readPanelProps } from './panelProps';
import { mountInShadow, resetParentsOverflowHidden } from './shadowMount';

/**
 * Entry point for the "Documents Comparison" Document Properties panel, built by Vite into a
 * fixed-name module (`assets/diffToolPanel.js`; the lib entry key sets the output name - see
 * vite.formext.config.js). The server-rendered fragment
 * (webapp/diff-tool/html/diff-tool.html, produced by BaseFormExtension) dynamically imports this module
 * and calls `mountDiffToolPanel("#diff-tool-panel")`.
 *
 * The panel mounts inside a **shadow root** on that fragment div so its styles are fully encapsulated on
 * the shared Document Properties page - which for this extension hosts two panels of its own (see
 * shadowMount.ts). The wrapper classes the panel CSS expects (`comparison form-wrapper`) plus the token
 * scope (`sbb-ui`) are reproduced on the inner container, and diff-tool.css is bundled via `?inline` and
 * injected into the shadow alongside react-sbb-polarion's stylesheet.
 */
export function mountDiffToolPanel(selector: string): Root | undefined {
  const host = document.querySelector<HTMLElement>(selector);
  if (!host) {
    console.error(`diff-tool: comparison panel mount target "${selector}" not found.`);
    return undefined;
  }
  // Polarion clips each Document Properties field with `overflow: hidden`, which would cut off the
  // dropdown popups. Must start from the host: closest() does not cross the shadow boundary.
  resetParentsOverflowHidden(host);

  const container = mountInShadow(host, {
    containerClassName: 'comparison form-wrapper sbb-ui',
    styleTexts: [panelStyle],
  });
  const root = createRoot(container);
  root.render(<DiffToolPanel props={readPanelProps(host)} />);
  // Returned so a test (or a future dev harness) can unmount; the Polarion fragment ignores it.
  return root;
}
