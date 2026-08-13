import { useState } from 'react';
import WorkerChart, { type ChartSeries } from './WorkerChart';

/** The intervals the legacy page offered, in minutes. */
export const INTERVAL_OPTIONS = [1, 3, 5, 15, 30];

interface ChartPanelProps {
  title: string;
  series: ChartSeries[];
  yAxisTitle: string;
  intervalMinutes: number;
  onIntervalChange: (minutes: number) => void;
  /** Suffix for the ids the tests and labels use, i.e. the chart key. */
  chartKey: string;
}

/**
 * One collapsible chart panel: header with an expand toggle, the chart, and a footer with the history
 * interval and a reset-position link (which clears the user's pan/zoom).
 *
 * Collapsing unmounts the chart rather than hiding it, so a collapsed panel costs nothing on the 3s
 * refresh - the legacy page kept every hidden chart alive and updating.
 */
export default function ChartPanel({
  title,
  series,
  yAxisTitle,
  intervalMinutes,
  onIntervalChange,
  chartKey,
}: ChartPanelProps) {
  const [expanded, setExpanded] = useState(true);
  const [resetToken, setResetToken] = useState(0);

  return (
    <div className="chart-container" id={`chart-container-${chartKey}`}>
      <div className="chart-header">
        <h3>{title}</h3>
        <button
          type="button"
          className="chart-expand-button"
          id={`chart-expand-button-${chartKey}`}
          title="Expand/Collapse"
          aria-expanded={expanded}
          onClick={() => setExpanded((current) => !current)}
        >
          {expanded ? '-' : '+'}
        </button>
      </div>

      {expanded && (
        <div className="chart-expand-container" id={`chart-expand-container-${chartKey}`}>
          <div className="chart-canvas">
            <WorkerChart series={series} yAxisTitle={yAxisTitle} resetToken={resetToken} />
          </div>
          <div className="chart-footer">
            <div className="footer-item">
              <label htmlFor={`select-interval-${chartKey}`}>Show data within the last:</label>
              &nbsp;
              <select
                id={`select-interval-${chartKey}`}
                title="The lesser interval is the better productivity."
                value={intervalMinutes}
                onChange={(event) => onIntervalChange(Number(event.target.value))}
              >
                {INTERVAL_OPTIONS.map((minutes) => (
                  <option key={minutes} value={minutes}>
                    {minutes} min
                  </option>
                ))}
              </select>
            </div>
            <button
              type="button"
              id={`reset-chart-${chartKey}`}
              className="footer-item chart-reset-button"
              onClick={() => setResetToken((token) => token + 1)}
            >
              Reset position
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
