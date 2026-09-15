import { type Root, createRoot } from 'react-dom/client';
import MergeToolPanel from './MergeToolPanel';
import panelStyle from './diff-tool.css?inline';
import { readPanelProps } from './panelProps';
import { mountInShadow, resetParentsOverflowHidden, takeOverPanelRoot } from './shadowMount';

/**
 * Entry point for the "Documents Merge" Document Properties panel, built by Vite into a fixed-name module
 * (`assets/mergeToolPanel.js`). The server-rendered fragment
 * (webapp/diff-tool/html/merge-tool.html, produced by BaseFormExtension) dynamically imports this module
 * and calls `mountMergeToolPanel("#merge-tool-panel")`.
 *
 * Same shadow-root arrangement as the other two panels (see mountCopyToolPanel.tsx and shadowMount.ts);
 * only the container's prefix class differs, which is what keeps the three panels' ids apart on the one
 * Document Properties page they share.
 */
export function mountMergeToolPanel(selector: string): Root | undefined {
  const host = document.querySelector<HTMLElement>(selector);
  if (!host) {
    console.error(`diff-tool: merge panel mount target "${selector}" not found.`);
    return undefined;
  }
  resetParentsOverflowHidden(host);

  // Wrapped so a fragment Polarion re-rendered ends the panel it replaces instead of orphaning it -
  // see takeOverPanelRoot. Polarion never unmounts these roots itself.
  return takeOverPanelRoot(selector, () => {
    const container = mountInShadow(host, {
      containerClassName: 'merge form-wrapper sbb-ui',
      styleTexts: [panelStyle],
    });
    const root = createRoot(container);
    root.render(<MergeToolPanel props={readPanelProps(host)} />);
    return root;
  });
}
