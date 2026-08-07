/**
 * Where the shared generic ES modules are served for this webapp context (GenericUiServlet resolves
 * `/ui/generic/*` out of the embedded generic.app). Both the entry point - which hands it to
 * react-sbb-polarion - and the breadcrumb bridge below load from here.
 */
export const GENERIC_MODULES_BASE = '/polarion/diff-tool-app/ui/generic/js/modules/';
