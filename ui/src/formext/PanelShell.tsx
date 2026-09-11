import type { ReactNode } from 'react';
import ToastHost from './ToastHost';

interface PanelShellProps {
  /** `comparison`, `copy` or `merge` - the per-panel id prefix, kept so the ids stay recognisable. */
  prefix: 'comparison' | 'copy' | 'merge';
  /** Progress message; a non-null value shows the blocking overlay. */
  busy: string | null;
  /**
   * Why a list the panel offers is empty, where that is the case.
   *
   * The one kind of message that stays in the form. What happened during an operation is an event and is
   * reported as a toast (see reporting.ts); this is a state, and a toast that came and went would leave a
   * dropdown that is quietly empty for no stated reason.
   */
  error?: string | null;
  children: ReactNode;
}

/**
 * The frame the Document Properties panels share: the query container the rows are laid out against, the
 * blocking progress overlay, the toast host and the alert slot.
 *
 * Ports `GenericMixin.actionInProgress` / `showAlert` / `hideAlerts`, with the DOM poking replaced by
 * conditional rendering - which also removes the legacy quirk that the overlay's message was written with
 * `innerHTML`.
 *
 * `.diff-form` is what carries the layout: the three column measurements every row is laid out against,
 * and `position: relative`, so the overlay covers the form and nothing else. It used to resolve against
 * `.form-wrapper`, which RSP does not position - so on the shared Document Properties page the overlay
 * was sized from the initial containing block rather than from the panel.
 */
export default function PanelShell({ prefix, busy, error, children }: PanelShellProps) {
  return (
    <div className="diff-form">
      {/* Outside the form's rows, and rendered whether or not anything is on screen: the host decides
          among the two panels' hosts which of them reports - see ToastHost.tsx. */}
      <ToastHost />

      <div className={busy !== null ? 'in-progress-overlay show' : 'in-progress-overlay'}>
        <span className="sbb-spinner" role="img" aria-label="Loading" />
        <span id={`${prefix}-in-progress-message`}>{busy}</span>
      </div>

      {children}

      {/* Only where there is something to say: an empty block would keep its padding above the buttons. */}
      {error ? (
        <div className="notifications">
          <div className="alert alert-error">{error}</div>
        </div>
      ) : null}
    </div>
  );
}
