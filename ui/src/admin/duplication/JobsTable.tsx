import { useState } from 'react';
import type { DuplicationJobInfo } from '../types';
import { isRunning } from './useDuplicationJobs';

interface JobsTableProps {
  jobs: DuplicationJobInfo[];
  error: string | null;
  /** Jobs whose log frame starts open (the one just scheduled). */
  initiallyExpanded?: string[];
  /** Changes on every poll, so an open log of a running job reloads. */
  pollCount: number;
}

const EM_DASH = '—';

/** Local `YYYY-MM-DD HH:mm`, as the legacy page rendered it. */
export function formatTime(millis?: number): string {
  if (!millis) {
    return EM_DASH;
  }
  const date = new Date(millis);
  const pad = (value: number) => String(value).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

/** Elapsed time; for a job still running this counts up against the current clock. */
export function formatDuration(start?: number, end?: number): string {
  if (!start) {
    return EM_DASH;
  }
  const seconds = Math.floor(((end || Date.now()) - start) / 1000);
  const minutes = Math.floor(seconds / 60);
  return minutes > 0 ? `${minutes}m ${seconds % 60}s` : `${seconds}s`;
}

export default function JobsTable({ jobs, error, initiallyExpanded = [], pollCount }: JobsTableProps) {
  const [expanded, setExpanded] = useState<string[]>(initiallyExpanded);
  const [manuallyToggled, setManuallyToggled] = useState<string[]>([]);

  // A newly scheduled job opens its own log without stealing a choice the user already made about it.
  const isExpanded = (jobId: string) =>
    expanded.includes(jobId) || (initiallyExpanded.includes(jobId) && !manuallyToggled.includes(jobId));

  const toggle = (jobId: string) => {
    setManuallyToggled((current) => (current.includes(jobId) ? current : [...current, jobId]));
    setExpanded((current) => (isExpanded(jobId) ? current.filter((id) => id !== jobId) : [...current, jobId]));
  };

  /**
   * A running job's log grows, so its frame is reloaded on each poll by varying the URL. A finished
   * job's log is stable and keeps a constant URL, which leaves it scrolled where the user left it.
   */
  const logSrc = (job: DuplicationJobInfo) => {
    if (!job.logUrl) {
      return undefined;
    }
    if (!isRunning(job)) {
      return job.logUrl;
    }
    return `${job.logUrl}${job.logUrl.includes('?') ? '&' : '?'}_ts=${pollCount}`;
  };

  return (
    <table className="jobs-table" id="jobs-table">
      <thead>
        <tr>
          <th>Job</th>
          <th>Started</th>
          <th>Duration</th>
          <th>State</th>
          <th>Status / Message</th>
          <th>Progress</th>
        </tr>
      </thead>
      <tbody id="jobs-tbody">
        {error && (
          <tr>
            <td colSpan={6}>Failed to load jobs: {error}</td>
          </tr>
        )}
        {!error && jobs.length === 0 && (
          <tr>
            <td colSpan={6}>No duplication jobs yet.</td>
          </tr>
        )}
        {jobs.map((job) => (
          <FragmentRow key={job.jobId} job={job} expanded={isExpanded(job.jobId)} onToggle={toggle} src={logSrc(job)} />
        ))}
      </tbody>
    </table>
  );
}

function FragmentRow({
  job,
  expanded,
  onToggle,
  src,
}: {
  job: DuplicationJobInfo;
  expanded: boolean;
  onToggle: (jobId: string) => void;
  src?: string;
}) {
  const stateLabel = job.statusType ? `${job.state} (${job.statusType})` : (job.state ?? '');
  const message = job.statusMessage || job.currentTaskName || '';
  const progress = job.completeness != null ? `${Math.round(job.completeness * 100)}%` : EM_DASH;

  return (
    <>
      <tr
        className="row-clickable"
        data-job-id={job.jobId}
        aria-expanded={expanded}
        onClick={() => onToggle(job.jobId)}
      >
        <td>
          <div>
            <strong>{job.jobName}</strong>
          </div>
          <div className="job-id">{job.jobId}</div>
        </td>
        <td>{formatTime(job.startTime || job.creationTime)}</td>
        <td>{formatDuration(job.startTime || job.creationTime, job.finishTime)}</td>
        <td className={`state-${job.state ?? ''} status-${job.statusType ?? ''}`}>{stateLabel}</td>
        <td>{message}</td>
        <td>{progress}</td>
      </tr>
      {expanded && (
        <tr className="job-log-row" data-job-log-for={job.jobId}>
          <td colSpan={6}>
            <iframe className="job-log-frame" src={src} title={`Log for ${job.jobId}`} />
          </td>
        </tr>
      )}
    </>
  );
}
