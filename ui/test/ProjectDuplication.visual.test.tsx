import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import { page } from 'vitest/browser';
import App from '../src/App';
import { installFetchMock } from './mockFetch';
import { settleBeforeCapture } from './visualHelpers';

// Docker-only snapshot of the Project Duplication page: the request form (including the source-project
// combobox upgraded to the shared dropdown) and the jobs table with a finished and a running job.
//
// Every timestamp here is a fixed epoch value and both jobs carry a finishTime, so no duration is
// computed against the current clock - the rendering is the same on every run.

const origUrl = window.location.pathname + window.location.search;

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  window.history.replaceState({}, '', origUrl);
});

describe.skipIf(!__PIXEL_REFERENCES__)('Project Duplication page visual', () => {
  it('loaded (form, jobs table)', async () => {
    installFetchMock([
      {
        method: 'GET',
        match: /\/projects$/,
        json: [
          { id: 'elibrary', name: 'E-Library' },
          { id: 'drivepilot', name: 'Drive Pilot' },
        ],
      },
      {
        method: 'GET',
        match: /\/projects\/duplicate\/jobs$/,
        json: [
          {
            jobId: 'duplication-4711',
            jobName: 'Duplicate elibrary -> elibrary_copy',
            state: 'FINISHED',
            statusType: 'OK',
            statusMessage: 'Project created',
            startTime: 1_780_000_000_000,
            finishTime: 1_780_000_186_000,
            completeness: 1,
            logUrl: '/polarion/job-log?id=duplication-4711',
          },
          {
            jobId: 'duplication-4712',
            jobName: 'Duplicate drivepilot -> drivepilot_2026',
            state: 'FINISHED',
            statusType: 'FAILED',
            statusMessage: 'Target location already exists',
            startTime: 1_779_000_000_000,
            finishTime: 1_779_000_012_000,
            completeness: 0.15,
            logUrl: '/polarion/job-log?id=duplication-4712',
          },
        ],
      },
    ]);
    window.history.replaceState({}, '', '?feature=project-duplication&embedded=true');
    render(<App />);

    await vi.waitFor(() => expect(document.querySelectorAll('#jobs-tbody tr')).toHaveLength(2));
    // The combobox is rendered by the shared dropdown, not by React, so wait for it before capturing.
    await vi.waitFor(() => expect(document.querySelector('.searchable-dropdown')).not.toBeNull());

    const app = document.querySelector('.app') as HTMLElement;
    await page.viewport(1280, Math.ceil(app.scrollHeight) + 40);
    await settleBeforeCapture();
    await expect(page.elementLocator(app)).toMatchScreenshot('project-duplication-loaded');
  });
});
