import { useEffect, useRef } from 'react';

export interface MergeOutcome {
  /** Whether the merge put anything into the target document, which is what makes a reload necessary. */
  merged: boolean;
  title: string;
  /** What the merge did, or why it did not happen - one line each. */
  lines: string[];
  /** The merge report as the server logged it, shown as it is because its entries carry no text of their own. */
  logs?: string | null;
  /** Where the Polarion job report of this merge can be read. */
  logUrl?: string | null;
}

interface MergeResultDialogProps {
  outcome: MergeOutcome | null;
  onClose: () => void;
}

/**
 * What a chapter merge did, shown when it has finished.
 *
 * A dialog rather than the toast the other panels report with: a merge changes the document the user is
 * looking at, the page is reloaded once this is closed, and what it has to say - counts, warnings, the
 * merge report - is more than a line. Closing it is therefore the user's acknowledgement, not a timeout.
 *
 * Built on the native `<dialog>`, like react-sbb-polarion's own modal, so the browser supplies the top
 * layer (the properties pane it is opened from is a narrow column that would otherwise clip it), the
 * backdrop, Escape-to-close and the focus handling. RSP's modal itself is not used because it always
 * renders a Cancel and an OK button, and there is only one thing to do with a result: acknowledge it.
 */
export default function MergeResultDialog({ outcome, onClose }: MergeResultDialogProps) {
  const dialog = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    if (outcome) {
      dialog.current?.showModal();
    }
  }, [outcome]);

  if (!outcome) {
    return null;
  }

  return (
    <dialog
      ref={dialog}
      className="merge-result-dialog"
      aria-label={outcome.title}
      onCancel={(event) => {
        event.preventDefault();
        onClose();
      }}
    >
      <header>
        <h2>{outcome.title}</h2>
        <button type="button" className="merge-result-close" aria-label="Close" onClick={onClose}>
          ×
        </button>
      </header>

      <div className="merge-result-content">
        <ul>
          {outcome.lines.map((line) => (
            <li key={line}>{line}</li>
          ))}
        </ul>

        {outcome.logs ? <pre className="merge-result-logs">{outcome.logs}</pre> : null}

        {outcome.logUrl ? (
          <p>
            <a href={outcome.logUrl} target="_blank" rel="noreferrer">
              Open the job log
            </a>
          </p>
        ) : null}
      </div>

      <footer>
        <button type="button" id="merge-result-ok" className="sbb-btn sbb-btn--primary" onClick={onClose}>
          Close
        </button>
      </footer>
    </dialog>
  );
}
