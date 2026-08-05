import { useEffect } from 'react';
import { GENERIC_MODULES_BASE } from './genericModules';

interface BreadcrumbConfig {
  /** Stable per-extension marker, also the id of the injected loader script. */
  marker: string;
  /** The topic's own label, shown last and emphasized. */
  title: string;
  /** The parent topic's label, shown before the separator. */
  parent?: string;
  /** Absolute URL of the topic icon, a Polarion-served svg. */
  icon?: string;
}

interface BreadcrumbBridge {
  install: (config: BreadcrumbConfig) => void;
}

const LOADER_ID = 'sbb-breadcrumb-bridge-loader';

/**
 * Shows the current Diff Tool topic in the Polarion app-header breadcrumb, through the shared generic
 * BreadcrumbBridge injected into the shell window.
 *
 * This is a port of the deleted `webapp/diff-tool/js/breadcrumb.js`, not react-sbb-polarion's
 * `BreadcrumbInjector`, for two reasons. The bridge takes a `parent` as well as a `title`, which is what
 * renders "Diff Tool › <icon> Multiple Work Items" rather than one flat label - RSP's component has no such
 * prop. And RSP's component returns early when the loader script is already in the shell head, so the
 * breadcrumb of the first topic opened would stick for the whole session: navigating from Multiple Work Items
 * to Collections kept the old label. Calling `install()` on every mount is what re-labels it.
 */
export default function BreadcrumbTopic({ marker, title, parent, icon }: Readonly<BreadcrumbConfig>) {
  useEffect(() => {
    try {
      const shell = window.top as (Window & { SbbBreadcrumbBridge?: BreadcrumbBridge }) | null;
      if (!shell) {
        return;
      }

      // The optional keys are left out rather than set to undefined, as the legacy script had it: the bridge
      // sees the same object either way, and this keeps the two implementations comparable.
      const config: BreadcrumbConfig = { marker: marker, title: title };
      if (parent) {
        config.parent = parent;
      }
      if (icon) {
        config.icon = icon;
      }
      if (shell.SbbBreadcrumbBridge) {
        shell.SbbBreadcrumbBridge.install(config);
        return;
      }

      const shellDocument = shell.document;
      if (!shellDocument?.head) {
        return;
      }
      shellDocument.getElementById(LOADER_ID)?.remove();

      const loader = shellDocument.createElement('script');
      loader.id = LOADER_ID;
      loader.type = 'text/javascript';
      loader.src = `${GENERIC_MODULES_BASE}BreadcrumbBridge.js`;
      loader.dataset.marker = marker;
      loader.dataset.title = title;
      if (parent) {
        loader.dataset.parent = parent;
      }
      if (icon) {
        loader.dataset.icon = icon;
      }
      shellDocument.head.appendChild(loader);
    } catch {
      // No accessible shell window (a standalone dev page, or a cross-origin one): the breadcrumb is chrome,
      // never a precondition for the page below it.
    }
  }, [marker, title, parent, icon]);

  return null;
}
