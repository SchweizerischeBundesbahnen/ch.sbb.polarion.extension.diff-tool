interface ThreadsTableProps {
  workerCount: number;
  maxThreads: number;
  /** Persisted thread counts: worker number -> threads. */
  current: Record<string, number>;
  /** Pending values, only for workers the user edited. */
  pending: Record<string, number>;
  onChange: (worker: number, threads: number | null) => void;
}

export default function ThreadsTable({ workerCount, maxThreads, current, pending, onChange }: ThreadsTableProps) {
  /**
   * Clamped on blur, matching the legacy adjustThreadsCount: anything not a positive number clears the
   * field, and a value above the machine's recommended maximum is pulled down to it. The input also
   * carries min/max so the browser's own spinner respects the same bounds.
   */
  const clampOnBlur = (worker: number, raw: string) => {
    const value = parseInt(raw, 10);
    if (!value || value <= 0) {
      onChange(worker, null);
      return;
    }
    onChange(worker, Math.min(value, maxThreads));
  };

  return (
    <div className="workers-configuration-table has-delimiter" id="workers-threads">
      <table>
        <thead>
          <tr>
            <th>Worker</th>
            <th>Current Threads Count</th>
            <th>Assign a new value</th>
          </tr>
        </thead>
        <tbody>
          {Array.from({ length: workerCount }, (_, index) => index + 1).map((worker) => (
            <tr key={worker} id={`worker-settings-${worker}`}>
              <td>Worker-{worker}</td>
              <td id={`current-threads-${worker}`} className="center-column-value">
                {current[String(worker)] ?? '-'}
              </td>
              <td>
                <input
                  type="number"
                  className="number-input"
                  id={`new-threads-${worker}`}
                  placeholder="New value"
                  min={1}
                  max={maxThreads}
                  title={`Max threads count: ${maxThreads}`}
                  value={pending[String(worker)] === undefined ? '' : String(pending[String(worker)])}
                  onChange={(event) => onChange(worker, event.target.value === '' ? null : Number(event.target.value))}
                  onBlur={(event) => clampOnBlur(worker, event.target.value)}
                />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
