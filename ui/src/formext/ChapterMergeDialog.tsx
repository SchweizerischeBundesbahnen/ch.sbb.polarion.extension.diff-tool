import { type ReactNode, useEffect, useRef } from 'react';

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

/** Where a chapter merge stands: what it is about to do, that it is doing it, and what it did. */
export type MergeStage =
  { kind: 'confirm' } | { kind: 'running'; message: string } | { kind: 'result'; outcome: MergeOutcome };

interface ChapterMergeDialogProps {
  /** The stage the merge is at; `null` while there is no merge to speak of, and the dialog is closed. */
  stage: MergeStage | null;
  /** What the merge is about to do, in the user's own terms - shown while it waits to be confirmed. */
  summary: ReactNode;
  onConfirm: () => void;
  onCancel: () => void;
  onClose: () => void;
}

const TITLES = {
  confirm: 'Merge chapter',
  running: 'Merging chapter',
};

/**
 * The one dialog a chapter merge is carried out in: it states what the merge will do, then runs it in
 * place, then shows what it did. The title and the buttons follow the stage; the dialog itself stays
 * open all the way through, so the user's attention stays where the operation is.
 *
 * While the merge runs there is nothing to decide and nothing to undo, so the dialog cannot be closed:
 * no close button, no Escape. What can be closed is a result, and closing it is the acknowledgement the
 * panel reloads the document on.
 *
 * Built on the native `<dialog>`, like react-sbb-polarion's own modal, so the browser supplies the top
 * layer (the properties pane it is opened from is a narrow column that would otherwise clip it), the
 * backdrop, Escape-to-close and the focus handling. RSP's modal itself is not used because it always
 * renders a Cancel and an OK button and is always closable, and neither holds for all three stages.
 */
export default function ChapterMergeDialog({ stage, summary, onConfirm, onCancel, onClose }: ChapterMergeDialogProps) {
  const dialog = useRef<HTMLDialogElement>(null);
  const closeButton = useRef<HTMLButtonElement>(null);

  // Opened once, when there is a merge, and kept open while it moves from stage to stage.
  useEffect(() => {
    if (stage && !dialog.current?.open) {
      dialog.current?.showModal();
    }
  }, [stage]);

  // A result is there to be acknowledged, and the button which does it took the place of the one the user
  // last pressed - so it takes that focus too, rather than leaving the keyboard nowhere.
  useEffect(() => {
    if (stage?.kind === 'result') {
      closeButton.current?.focus();
    }
  }, [stage?.kind]);

  if (!stage) {
    return null;
  }

  const title = stage.kind === 'result' ? stage.outcome.title : TITLES[stage.kind];
  // A merge which is running is not a question, so there is nothing to dismiss until it has an answer.
  const dismiss = stage.kind === 'confirm' ? onCancel : stage.kind === 'result' ? onClose : null;

  return (
    <dialog
      ref={dialog}
      className="merge-dialog"
      aria-label={title}
      onCancel={(event) => {
        // Escape: prevented in every stage, since closing the dialog is the panel's decision to make
        event.preventDefault();
        dismiss?.();
      }}
    >
      <header>
        <h2>{title}</h2>
        {dismiss ? (
          <button type="button" className="merge-dialog-close" aria-label="Close" onClick={dismiss}>
            ×
          </button>
        ) : null}
      </header>

      <div className="merge-dialog-content">
        {stage.kind === 'confirm' ? <div id="merge-confirmation">{summary}</div> : null}

        {stage.kind === 'running' ? (
          <p id="merge-progress" className="merge-dialog-progress">
            <span className="sbb-spinner" role="img" aria-label="Merging" />
            <span>{stage.message}</span>
          </p>
        ) : null}

        {stage.kind === 'result' ? <Result outcome={stage.outcome} /> : null}
      </div>

      <footer>
        {stage.kind === 'confirm' ? (
          <>
            <button type="button" id="merge-cancel" className="sbb-btn sbb-btn--secondary" onClick={onCancel}>
              Cancel
            </button>
            <button type="button" id="merge-confirm" className="sbb-btn sbb-btn--primary" onClick={onConfirm}>
              Confirm
            </button>
          </>
        ) : (
          <button
            ref={closeButton}
            type="button"
            id="merge-close"
            className="sbb-btn sbb-btn--primary"
            disabled={stage.kind === 'running'}
            onClick={onClose}
          >
            Close
          </button>
        )}
      </footer>
    </dialog>
  );
}

/** What the merge did: its counts first, then the merge report it wrote and where the job log of it is. */
function Result({ outcome }: { outcome: MergeOutcome }) {
  return (
    <div id="merge-result">
      <ul>
        {outcome.lines.map((line) => (
          <li key={line}>{line}</li>
        ))}
      </ul>

      {outcome.logs ? <pre className="merge-dialog-logs">{outcome.logs}</pre> : null}

      {outcome.logUrl ? (
        <p>
          <a href={outcome.logUrl} target="_blank" rel="noreferrer">
            Open the job log
          </a>
        </p>
      ) : null}
    </div>
  );
}
