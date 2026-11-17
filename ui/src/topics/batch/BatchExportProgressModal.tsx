import type { IconDefinition } from '@fortawesome/fontawesome-svg-core';
import {
  faArrowRightLong,
  faBan,
  faCircleCheck,
  faCircleExclamation,
  faClock,
} from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { Modal } from '@sbb-polarion/react-sbb-polarion';
import { documentLabel } from './batchExport';
import type { BatchRow, BatchState, RowState } from './useBatchDiffExport';

interface Props {
  state: BatchState;
  onStop: () => void;
  onClose: () => void;
}

const ICONS: Partial<Record<RowState, { icon: IconDefinition; label: string }>> = {
  waiting: { icon: faClock, label: 'Waiting' },
  finished: { icon: faCircleCheck, label: 'Exported' },
  error: { icon: faCircleExclamation, label: 'Failed' },
  interrupted: { icon: faBan, label: 'Interrupted' },
};

function resultMessage(state: BatchState): string {
  if (state.status === 'interrupted') {
    return 'Export interrupted by user';
  }
  return state.errors ? 'Export finished with errors' : 'Export successfully finished';
}

function RowStatus({ row }: Readonly<{ row: BatchRow }>) {
  switch (row.state) {
    case 'diffing': {
      const { loaded = 0, total = 0 } = row.progress ?? {};
      const percent = total > 0 ? Math.round((loaded * 100) / total) : null;
      return (
        <div className="row-status">
          <span>{percent === null ? 'Comparing documents' : `Comparing work items: ${loaded} of ${total}`}</span>
          <div
            className={`mini-progress ${percent === null ? 'indeterminate' : ''}`}
            role="progressbar"
            aria-label="Diff progress"
            aria-valuemin={0}
            aria-valuemax={100}
            aria-valuenow={percent ?? undefined}
          >
            <span style={percent === null ? undefined : { width: `${percent}%` }} />
          </div>
        </div>
      );
    }
    case 'converting':
      return <div className="row-status">Converting to PDF</div>;
    case 'finished':
      return <div className="row-status file-name">{row.fileName}</div>;
    default:
      return null;
  }
}

/**
 * The progress dialog of a batch PDF export: one row per document pair, its state as the run reaches it, and
 * how far the whole run is. Stop while the run goes on, Close once it is over.
 *
 * Follows the progress dialog of pdf-exporter's Bulk PDF Export, so both exports look and behave alike. RSP's
 * Modal always renders both of its footer buttons, so batch.css hides the one which does not apply.
 */
export default function BatchExportProgressModal({ state, onStop, onClose }: Readonly<Props>) {
  if (state.status === 'closed') {
    return null;
  }

  const running = state.status === 'running';
  const total = state.rows.length;
  const percent = total === 0 ? 0 : Math.round((state.processed * 100) / total);

  return (
    <Modal
      open
      title="Export diffs to PDF"
      okText="Stop"
      cancelText="Close"
      onOk={running ? onStop : onClose}
      onCancel={onClose}
    >
      <div className={`batch-export-progress ${running ? 'running' : 'done'}`}>
        <ul className="batch-export-rows" aria-label="Document pairs">
          {state.rows.map((row, index) => {
            const icon = ICONS[row.state];
            return (
              <li className={`batch-export-row ${row.state}`} key={`${row.fileName}-${index}`}>
                <span className="icon">
                  {icon ? (
                    <FontAwesomeIcon icon={icon.icon} title={icon.label} />
                  ) : (
                    <span className="sbb-spinner" role="img" aria-label="In progress" />
                  )}
                </span>
                <span className="pair">
                  <span className="name">{documentLabel(row.pair.source, row.pair.sourceRevision, false)}</span>
                  <FontAwesomeIcon icon={faArrowRightLong} className="arrow" />
                  <span className="name">{documentLabel(row.pair.target, row.pair.targetRevision, true)}</span>
                </span>
                <RowStatus row={row} />
                {row.error ? <div className="error-message">{row.error}</div> : null}
              </li>
            );
          })}
        </ul>

        <div className="batch-export-outcome">
          {running ? (
            <div
              className="progress-bar"
              role="progressbar"
              aria-label="Export progress"
              aria-valuemin={0}
              aria-valuemax={100}
              aria-valuenow={percent}
            >
              <span style={{ width: `${percent}%` }} />
              <span className="progress-label">
                {state.processed} of {total} finished
              </span>
            </div>
          ) : (
            <span
              className={`result ${state.status}${state.status === 'finished' && state.errors ? ' with-errors' : ''}`}
              role="status"
            >
              {resultMessage(state)}
            </span>
          )}
        </div>
      </div>
    </Modal>
  );
}
