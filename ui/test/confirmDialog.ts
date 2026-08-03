import { expect, vi } from 'vitest';

/**
 * Answers the confirmation dialog the admin pages render in place of the former `window.confirm`
 * (react-sbb-polarion's `useConfirm`, built on its Modal).
 *
 * Shared by the three settings pages so they all drive it the same way. Selectors are RSP's Modal
 * markup - if they ever change, this one helper is the only thing to fix.
 */
export async function answerConfirm(label: 'OK' | 'Cancel'): Promise<void> {
  await vi.waitFor(() => expect(document.querySelector('.rsp-modal')).not.toBeNull());
  const button = Array.from(document.querySelectorAll<HTMLButtonElement>('.rsp-modal-footer .sbb-btn')).find(
    (candidate) => (candidate.textContent ?? '').trim() === label,
  );
  if (!button) {
    throw new Error(`confirmation dialog button "${label}" not found`);
  }
  button.click();
  // The dialog unmounts as it settles; waiting for that keeps a following assertion from racing it.
  await vi.waitFor(() => expect(document.querySelector('.rsp-modal')).toBeNull());
}
