import type { ReactNode } from 'react';

interface PanelShellProps {
  /** `comparison` or `copy` - the legacy per-panel id prefix, kept so the ids stay recognisable. */
  prefix: 'comparison' | 'copy';
  /** Progress message; a non-null value shows the blocking overlay. */
  busy: string | null;
  error?: string | null;
  warning?: string | null;
  children: ReactNode;
}

/**
 * The frame both Document Properties panels share: the blocking progress overlay and the two alert
 * slots. Ports `GenericMixin.actionInProgress` / `showAlert` / `hideAlerts`, with the DOM poking replaced
 * by conditional rendering - which also removes the legacy quirk that the overlay's message was written
 * with `innerHTML`.
 *
 * `.in-progress-overlay` is positioned absolutely against the container, whose `.form-wrapper` class is
 * set on the shadow container by the mount function.
 */
export default function PanelShell({ prefix, busy, error, warning, children }: PanelShellProps) {
  return (
    <>
      <div className={busy !== null ? 'in-progress-overlay show' : 'in-progress-overlay'}>
        <span className="sbb-spinner" role="img" aria-label="Loading" style={{ width: 48, height: 48 }} />
        <span id={`${prefix}-in-progress-message`}>{busy}</span>
      </div>

      {children}

      {error ? <div className="alert alert-error">{error}</div> : null}
      {warning ? <div className="alert alert-warning">{warning}</div> : null}
    </>
  );
}
