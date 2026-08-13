import { Toaster } from '@grigoriev/react-sbb-polarion';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import { formatDuration, formatTime } from '../src/admin/duplication/JobsTable';
import ProjectDuplicationPage from '../src/admin/pages/ProjectDuplicationPage';
import { type FetchMock, type Route, installFetchMock, jsonResponse } from './mockFetch';

// Behaviour of the port of project_duplication.js: the form and its validation, scheduling a job, and
// the jobs table with its expandable log frames.

const origUrl = window.location.pathname + window.location.search;

const PROJECTS = [{ id: 'elibrary', name: 'E-Library' }, { id: 'drivepilot' }];

const RUNNING_JOB = {
  jobId: 'job-1',
  jobName: 'Duplicate elibrary',
  state: 'RUNNING',
  statusType: null,
  statusMessage: 'Exporting template',
  startTime: 1_780_000_000_000,
  completeness: 0.42,
  logUrl: '/polarion/job-log?id=job-1',
};

const FINISHED_JOB = {
  jobId: 'job-0',
  jobName: 'Duplicate drivepilot',
  state: 'FINISHED',
  statusType: 'OK',
  statusMessage: 'Done',
  startTime: 1_779_000_000_000,
  finishTime: 1_779_000_090_000,
  completeness: 1,
  logUrl: '/polarion/job-log?id=job-0',
};

function routes(overrides: Route[] = []): Route[] {
  return [
    ...overrides,
    { method: 'GET', match: /\/projects$/, json: PROJECTS },
    { method: 'GET', match: /\/projects\/duplicate\/jobs$/, json: [FINISHED_JOB] },
    {
      method: 'POST',
      match: /\/projects\/duplicate$/,
      json: { jobId: 'job-9', jobName: 'Duplicate elibrary', state: 'RUNNING', logUrl: '/polarion/job-log?id=job-9' },
    },
  ];
}

function Page() {
  return (
    <>
      <ProjectDuplicationPage />
      <Toaster />
    </>
  );
}

async function renderPage(fetchMock: FetchMock = installFetchMock(routes())) {
  render(<Page />);
  await vi.waitFor(() => expect(document.querySelector('#duplication-form')).not.toBeNull());
  return fetchMock;
}

function setFieldValue(element: HTMLInputElement | HTMLSelectElement, value: string): void {
  const prototype = element instanceof HTMLSelectElement ? HTMLSelectElement.prototype : HTMLInputElement.prototype;
  Object.getOwnPropertyDescriptor(prototype, 'value')!.set!.call(element, value);
  element.dispatchEvent(new Event(element instanceof HTMLSelectElement ? 'change' : 'input', { bubbles: true }));
}

async function fillForm() {
  setFieldValue(document.querySelector<HTMLSelectElement>('#source-project')!, 'elibrary');
  setFieldValue(document.querySelector<HTMLInputElement>('#targetProjectId')!, 'my_new_project');
  setFieldValue(document.querySelector<HTMLInputElement>('#location')!, '/MyProjects/my_new_project');
  setFieldValue(document.querySelector<HTMLInputElement>('#trackerPrefix')!, 'MNP');
  await vi.waitFor(() => expect(document.querySelector<HTMLInputElement>('#trackerPrefix')!.value).toBe('MNP'));
}

const startButton = () => document.querySelector<HTMLButtonElement>('#start-duplication')!;

beforeEach(() => {
  window.history.replaceState({}, '', '?feature=project-duplication&embedded=true');
});

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
  window.history.replaceState({}, '', origUrl);
});

describe('formatting helpers', () => {
  it('renders a dash when there is no timestamp', () => {
    expect(formatTime(undefined)).toBe('—');
    expect(formatDuration(undefined)).toBe('—');
  });

  it('renders minutes and seconds, and only seconds under a minute', () => {
    expect(formatDuration(1_000_000, 1_000_000 + 90_000)).toBe('1m 30s');
    expect(formatDuration(1_000_000, 1_000_000 + 5_000)).toBe('5s');
  });

  it('counts up against the current clock while a job is still running', () => {
    vi.spyOn(Date, 'now').mockReturnValue(1_000_000 + 30_000);

    expect(formatDuration(1_000_000, undefined)).toBe('30s');
  });
});

describe('ProjectDuplicationPage', () => {
  it('offers every project, labelling the ones that have a name', async () => {
    await renderPage();

    const options = Array.from(document.querySelectorAll<HTMLOptionElement>('#source-project option')).map(
      (option) => option.textContent,
    );
    expect(options).toEqual(expect.arrayContaining(['E-Library (elibrary)', 'drivepilot']));
  });

  it('lists existing jobs with their state, message and progress', async () => {
    await renderPage();

    await vi.waitFor(() => expect(document.querySelector('[data-job-id="job-0"]')).not.toBeNull());
    const cells = Array.from(document.querySelectorAll('[data-job-id="job-0"] td')).map((td) => td.textContent);
    expect(cells[3]).toBe('FINISHED (OK)');
    expect(cells[4]).toBe('Done');
    expect(cells[5]).toBe('100%');
  });

  it('shows an empty state when there are no jobs', async () => {
    await renderPage(installFetchMock(routes([{ method: 'GET', match: /\/duplicate\/jobs$/, json: [] }])));

    await vi.waitFor(() => expect(document.body.textContent).toContain('No duplication jobs yet.'));
  });

  it('renders a job that carries almost no detail without inventing any', async () => {
    await renderPage(
      installFetchMock(routes([{ method: 'GET', match: /\/duplicate\/jobs$/, json: [{ jobId: 'bare' }] }])),
    );

    await vi.waitFor(() => expect(document.querySelector('[data-job-id="bare"]')).not.toBeNull());
    const cells = Array.from(document.querySelectorAll('[data-job-id="bare"] td')).map((td) => td.textContent);
    expect(cells[1]).toBe('—'); // no start or creation time
    expect(cells[2]).toBe('—'); // hence no duration
    expect(cells[3]).toBe(''); // no state
    expect(cells[4]).toBe(''); // no message
    expect(cells[5]).toBe('—'); // no progress
  });

  it('opens a log row even for a job with no log URL', async () => {
    await renderPage(
      installFetchMock(
        routes([{ method: 'GET', match: /\/duplicate\/jobs$/, json: [{ jobId: 'nolog', state: 'FINISHED' }] }]),
      ),
    );
    await vi.waitFor(() => expect(document.querySelector('[data-job-id="nolog"]')).not.toBeNull());

    document.querySelector<HTMLElement>('[data-job-id="nolog"]')!.click();

    await vi.waitFor(() => expect(document.querySelector('iframe.job-log-frame')).not.toBeNull());
    expect(document.querySelector('iframe.job-log-frame')!.getAttribute('src')).toBeNull();
  });

  it('refuses to submit an incomplete form, naming the missing fields', async () => {
    const fetchMock = await renderPage();

    startButton().click();

    await vi.waitFor(() => expect(document.body.textContent).toContain('Please fill in:'));
    expect(document.body.textContent).toContain('sourceProjectId');
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === 'POST')).toBe(false);
  });

  it('posts the whole request and reports the scheduled job', async () => {
    const fetchMock = await renderPage();
    await fillForm();

    startButton().click();

    await vi.waitFor(() => {
      const post = fetchMock.mock.calls.find(([, init]) => init?.method === 'POST');
      expect(post).toBeDefined();
      expect(JSON.parse(String(post![1]!.body))).toEqual({
        sourceProjectId: 'elibrary',
        targetProjectId: 'my_new_project',
        location: '/MyProjects/my_new_project',
        trackerPrefix: 'MNP',
      });
    });
    await vi.waitFor(() => expect(document.body.textContent).toContain("Job 'Duplicate elibrary' scheduled"));
  });

  it('surfaces the server message when scheduling fails', async () => {
    const fetchMock = installFetchMock(
      routes([
        {
          method: 'POST',
          match: /\/projects\/duplicate$/,
          respond: () => new Response('Project id already taken', { status: 400 }),
        },
      ]),
    );
    await renderPage(fetchMock);
    await fillForm();

    startButton().click();

    await vi.waitFor(() => expect(document.body.textContent).toContain('Project id already taken'));
  });

  it('opens and closes a job log when its row is clicked', async () => {
    await renderPage();
    await vi.waitFor(() => expect(document.querySelector('[data-job-id="job-0"]')).not.toBeNull());

    document.querySelector<HTMLElement>('[data-job-id="job-0"]')!.click();
    await vi.waitFor(() => expect(document.querySelector('iframe.job-log-frame')).not.toBeNull());
    expect(document.querySelector('iframe.job-log-frame')!.getAttribute('src')).toBe('/polarion/job-log?id=job-0');

    document.querySelector<HTMLElement>('[data-job-id="job-0"]')!.click();
    await vi.waitFor(() => expect(document.querySelector('iframe.job-log-frame')).toBeNull());
  });

  it('keeps a finished job log at a stable URL so it does not reload under the user', async () => {
    await renderPage();
    await vi.waitFor(() => expect(document.querySelector('[data-job-id="job-0"]')).not.toBeNull());
    document.querySelector<HTMLElement>('[data-job-id="job-0"]')!.click();

    await vi.waitFor(() => expect(document.querySelector('iframe.job-log-frame')).not.toBeNull());
    // No cache-busting parameter: a finished log is immutable.
    expect(document.querySelector('iframe.job-log-frame')!.getAttribute('src')).not.toContain('_ts=');
  });

  it('cache-busts the log of a running job so new output appears', async () => {
    await renderPage(installFetchMock(routes([{ method: 'GET', match: /\/duplicate\/jobs$/, json: [RUNNING_JOB] }])));
    await vi.waitFor(() => expect(document.querySelector('[data-job-id="job-1"]')).not.toBeNull());

    document.querySelector<HTMLElement>('[data-job-id="job-1"]')!.click();

    await vi.waitFor(() => expect(document.querySelector('iframe.job-log-frame')).not.toBeNull());
    expect(document.querySelector('iframe.job-log-frame')!.getAttribute('src')).toContain('_ts=');
  });

  it('polls quickly while a job is still running', async () => {
    await renderPage(installFetchMock(routes([{ method: 'GET', match: /\/duplicate\/jobs$/, json: [RUNNING_JOB] }])));

    await vi.waitFor(() => expect(document.querySelector('#refresh-note')!.textContent).toContain('every 3s'));
  });

  it('drops to the idle cadence when nothing is running', async () => {
    // Deliberately a separate test rather than a cleanup() and second render inside one: unmounting and
    // re-rendering mid-test left vitest-browser-react unable to mount again, which broke this test and
    // every one after it.
    await renderPage();

    await vi.waitFor(() =>
      expect(document.querySelector('#refresh-note')!.textContent).toContain('next refresh in 30s'),
    );
  });

  it('reports a failure to load the jobs inside the table', async () => {
    await renderPage(
      installFetchMock(
        routes([
          { method: 'GET', match: /\/duplicate\/jobs$/, respond: () => jsonResponse({ errorMessage: 'no' }, 500) },
        ]),
      ),
    );

    await vi.waitFor(() => expect(document.body.textContent).toContain('Failed to load jobs'));
  });

  it('reports a failure to load the projects', async () => {
    await renderPage(
      installFetchMock(routes([{ method: 'GET', match: /\/projects$/, respond: () => jsonResponse({}, 500) }])),
    );

    await vi.waitFor(() => expect(document.body.textContent).toContain('Failed to load projects'));
  });
});
