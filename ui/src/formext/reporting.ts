import { toast } from 'sonner';

/**
 * What a Document Properties panel tells the user about an operation it ran.
 *
 * The two panels used to put their failures in the form, as plain red text under the button
 * (`GenericMixin.showAlert`). They report through these functions now, which is `sonner`'s `toast` - the
 * same toasts every administration page of this extension already raises, through the same host (RSP's
 * `Toaster`, see ToastHost.tsx), and the same arrangement pdf-exporter and docx-exporter use in their
 * export panels.
 *
 * Toasts rather than a block in the form because these are events, not state: the message no longer takes
 * a place in a layout that has to make room for it, and cannot be scrolled away from the button that
 * produced it. What stays in the form is what describes a *state* - a list that could not be loaded, so
 * the dropdown under it is empty, and the document a copy created, which is a link the user has to click.
 */

/**
 * How long a failure stays, and how it can be sent away.
 *
 * It waits to be dismissed, which is what the red text it replaces did: it names something the user has
 * to read, usually the server's own message. The close button is therefore not optional - nothing else
 * would take it off the screen.
 */
const FAILURE = { duration: Infinity, closeButton: true } as const;

/** Reports an operation that failed, with whatever the server said about it. */
export const reportFailure = (message: string): void => {
  toast.error(message, FAILURE);
};

/** Takes back whatever was last reported, which is what a panel does before it starts an operation. */
export const clearReports = (): void => {
  toast.dismiss();
};
