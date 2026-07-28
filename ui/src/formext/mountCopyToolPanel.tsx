import { type Root, createRoot } from 'react-dom/client';
import CopyToolPanel from './CopyToolPanel';
import panelStyle from './diff-tool.css?inline';
import { readPanelProps } from './panelProps';
import { mountInShadow, resetParentsOverflowHidden } from './shadowMount';

/**
 * Entry point for the "Documents Copy" Document Properties panel, built by Vite into a fixed-name module
 * (`assets/copyToolPanel.js`). The server-rendered fragment
 * (webapp/diff-tool/html/copy-tool.html, produced by BaseFormExtension) dynamically imports this module
 * and calls `mountCopyToolPanel("#copy-tool-panel")`.
 *
 * Same shadow-root arrangement as the comparison panel (see mountDiffToolPanel.tsx and shadowMount.ts);
 * only the container's legacy prefix class differs, because the panel CSS and the legacy ids used
 * `copy` rather than `comparison`.
 */
export function mountCopyToolPanel(selector: string): Root | undefined {
  const host = document.querySelector<HTMLElement>(selector);
  if (!host) {
    console.error(`diff-tool: copy panel mount target "${selector}" not found.`);
    return undefined;
  }
  resetParentsOverflowHidden(host);

  const container = mountInShadow(host, {
    containerClassName: 'copy form-wrapper sbb-ui',
    styleTexts: [panelStyle],
  });
  const root = createRoot(container);
  root.render(<CopyToolPanel props={readPanelProps(host)} />);
  return root;
}
