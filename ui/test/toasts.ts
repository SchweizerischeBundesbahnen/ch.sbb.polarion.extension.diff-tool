import { toast } from 'sonner';
import { expect, vi } from 'vitest';

/**
 * Reading the toasts a Document Properties panel reported.
 *
 * The panels report through `sonner` (see src/formext/reporting.ts), whose markup this reads:
 * `[data-sonner-toast][data-type=...]` with the message in `[data-title]`.
 *
 * The host is inside the panel's shadow root (src/formext/ToastHost.tsx), so a root has to be passed in -
 * unlike the administration pages, whose `Toaster` is in the document. Which of the two panels' hosts is
 * the one rendering is decided by mount order, so a suite with a single panel mounted reads its own root.
 *
 * A toast is not on screen the moment `reportFailure(...)` returns - sonner queues one task before it
 * flushes the state into its host - so a toast is waited for rather than looked for. And the queue is a
 * module singleton that outlives a test: a host mounted by the next test is handed every toast still
 * active (that is how a toast raised before its host mounts is not lost), so a suite that reports has to
 * clear up after itself with {@link clearToasts}.
 */

export type ToastKind = 'error' | 'warning' | 'success';

/** What a toast of this kind says, or '' where there is none. */
export const toastText = (root: ShadowRoot | Document, kind: ToastKind): string =>
  root.querySelector(`[data-sonner-toast][data-type="${kind}"] [data-title]`)?.textContent ?? '';

/** Waits for a toast of this kind and answers what it says. */
export const toasted = (root: ShadowRoot | Document, kind: ToastKind): Promise<string> =>
  vi.waitFor(() => {
    const said = toastText(root, kind);
    expect(said, `no ${kind} toast`).not.toBe('');
    return said;
  });

/** Takes back every toast, so what one test reported cannot be read as the next one's. */
export const clearToasts = (): void => {
  toast.dismiss();
};
