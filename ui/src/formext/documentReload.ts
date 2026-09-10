/**
 * Reloads the page the Document Properties panel sits on.
 *
 * Its own module so that a test can replace it: the panel reloads the page after a successful merge,
 * because the document open in the editor beside it now shows content it doesn't know about.
 */
export const reloadDocument = (): void => {
  window.location.reload();
};
