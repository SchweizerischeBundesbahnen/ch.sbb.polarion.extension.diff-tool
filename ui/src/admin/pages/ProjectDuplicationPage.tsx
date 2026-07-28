import { useEffect, useState } from 'react';
import { PageLayout } from '@grigoriev/react-sbb-polarion';
import { toast } from 'sonner';
import useRemote from '../../services/useRemote';
import DuplicationForm, { missingFields } from '../duplication/DuplicationForm';
import JobsTable from '../duplication/JobsTable';
import useDuplicationJobs from '../duplication/useDuplicationJobs';
import type { DuplicationJobInfo, DuplicationRequest, ProjectInfo } from '../types';

const EMPTY_REQUEST: DuplicationRequest = {
  sourceProjectId: '',
  targetProjectId: '',
  location: '',
  trackerPrefix: '',
};

export default function ProjectDuplicationPage() {
  const { sendRequest } = useRemote();

  const [projects, setProjects] = useState<ProjectInfo[]>([]);
  const [projectsError, setProjectsError] = useState<string | null>(null);
  const [request, setRequest] = useState<DuplicationRequest>(EMPTY_REQUEST);
  const [submitting, setSubmitting] = useState(false);
  const [scheduledJobIds, setScheduledJobIds] = useState<string[]>([]);

  const { jobs, error: jobsError, nextRefreshMs, anyRunning, refresh, pollCount } = useDuplicationJobs();

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const response = await sendRequest({ method: 'GET', url: '/projects' });
        if (!response.ok) {
          throw new Error(`Failed to load projects (HTTP ${response.status})`);
        }
        const loaded = (await response.json()) as ProjectInfo[];
        if (!cancelled) {
          setProjects(loaded);
          setProjectsError(null);
        }
      } catch (error) {
        if (!cancelled) {
          setProjectsError((error as Error).message);
          toast.error((error as Error).message);
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [sendRequest]);

  const start = async () => {
    const missing = missingFields(request);
    if (missing.length > 0) {
      toast.error(`Please fill in: ${missing.join(', ')}`);
      return;
    }
    setSubmitting(true);
    try {
      const response = await sendRequest({
        method: 'POST',
        url: '/projects/duplicate',
        contentType: 'application/json',
        body: JSON.stringify(request),
      });
      const text = await response.text();
      if (!response.ok) {
        throw new Error(text || `HTTP ${response.status}`);
      }
      const job = JSON.parse(text) as DuplicationJobInfo;
      // Open the new job's log straight away, so the user sees it working.
      setScheduledJobIds((current) => [...current, job.jobId]);
      toast.success(`Job '${job.jobName}' scheduled (id: ${job.jobId}). Watch progress in the table below.`);
      refresh();
    } catch (error) {
      toast.error(`Failed to start duplication: ${(error as Error).message}`);
    } finally {
      setSubmitting(false);
    }
  };

  const refreshNote = anyRunning
    ? `(auto-refreshing every ${nextRefreshMs / 1000}s)`
    : `(idle; next refresh in ${nextRefreshMs / 1000}s)`;

  return (
    <PageLayout title="Project Duplication">
      <DuplicationForm
        projects={projects}
        projectsError={projectsError}
        value={request}
        onChange={setRequest}
        onSubmit={() => void start()}
        submitting={submitting}
      />

      <h2>
        Duplication Jobs{' '}
        <span className="auto-refresh-note" id="refresh-note">
          {refreshNote}
        </span>
      </h2>
      <div className="jobs-section">
        <JobsTable jobs={jobs} error={jobsError} initiallyExpanded={scheduledJobIds} pollCount={pollCount} />
      </div>

      <div className="quick-help">
        <h2>Quick Help</h2>
        <h3>How it works</h3>
        <p>
          The selected source project is exported to a temporary template, and a new project is created from that
          template at the specified location with the chosen tracker prefix. The temporary template is removed once
          duplication finishes.
        </p>
        <p>
          Duplication runs as an asynchronous Polarion job. The list above shows all duplication jobs (newest first) and
          refreshes automatically while any job is still running. Click a row to open / close its log.
        </p>
        <p>This action is restricted to global Polarion administrators.</p>
      </div>
    </PageLayout>
  );
}
