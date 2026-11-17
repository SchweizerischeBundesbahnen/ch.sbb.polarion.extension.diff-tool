import { a11yViolations } from '@sbb-polarion/react-sbb-polarion/testing';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import BatchExportProgressModal from '../src/topics/batch/BatchExportProgressModal';
import '../src/topics/batch/batch.css';
import type { DocumentPair } from '../src/topics/batch/batchExport';
import type { BatchRow, BatchState } from '../src/topics/batch/useBatchDiffExport';

const pair = (name: string, title?: string): DocumentPair => ({
  source: { projectId: 'elibrary', spaceId: 'Specification', name: name, title: title },
  target: { projectId: 'drivepilot', spaceId: 'Design', name: name },
  sourceRevision: '',
  targetRevision: '',
});

const row = (name: string, update: Partial<BatchRow>): BatchRow => ({
  pair: pair(name),
  fileName: `${name}.pdf`,
  state: 'waiting',
  ...update,
});

const RUNNING: BatchState = {
  status: 'running',
  processed: 2,
  errors: true,
  rows: [
    row('A', { state: 'finished' }),
    row('B', { state: 'error', error: 'Document not found' }),
    row('C', { state: 'diffing', progress: { loaded: 3, total: 12 } }),
    row('D', { state: 'waiting' }),
  ],
};

const dialog = () => document.querySelector<HTMLElement>('.batch-export-progress')!;
const rowElements = () => Array.from(document.querySelectorAll<HTMLElement>('.batch-export-row'));
const visibleButtons = () =>
  Array.from(document.querySelectorAll<HTMLButtonElement>('.rsp-modal-footer button'))
    .filter((button) => getComputedStyle(button).display !== 'none')
    .map((button) => button.textContent);

afterEach(async () => {
  await cleanup();
});

describe('BatchExportProgressModal', () => {
  it('renders nothing while no run is shown', async () => {
    await render(
      <BatchExportProgressModal
        state={{ status: 'closed', rows: [], processed: 0, errors: false }}
        onStop={vi.fn()}
        onClose={vi.fn()}
      />,
    );

    expect(document.querySelector('.batch-export-progress')).toBeNull();
  });

  it('shows each pair with its state, and how far the run is', async () => {
    await render(<BatchExportProgressModal state={RUNNING} onStop={vi.fn()} onClose={vi.fn()} />);

    expect(rowElements().map((element) => element.className)).toEqual([
      'batch-export-row finished',
      'batch-export-row error',
      'batch-export-row diffing',
      'batch-export-row waiting',
    ]);
    expect(rowElements()[0].textContent).toContain('Specification / A');
    expect(rowElements()[0].textContent).toContain('drivepilot / Design / A');
    expect(rowElements()[0].textContent).toContain('A.pdf');
    expect(rowElements()[1].textContent).toContain('Document not found');
    expect(rowElements()[2].textContent).toContain('Comparing work items: 3 of 12');
    expect(rowElements()[2].querySelector('[role="progressbar"]')!.getAttribute('aria-valuenow')).toBe('25');
    expect(dialog().querySelector('.batch-export-outcome')!.textContent).toBe('2 of 4 finished');
    expect(visibleButtons()).toEqual(['Stop']);
  });

  it('shows a diff of unknown length as going on', async () => {
    const state: BatchState = { ...RUNNING, rows: [row('A', { state: 'diffing' })], processed: 0 };

    await render(<BatchExportProgressModal state={state} onStop={vi.fn()} onClose={vi.fn()} />);

    expect(rowElements()[0].textContent).toContain('Comparing documents');
    expect(rowElements()[0].querySelector('.mini-progress.indeterminate')).not.toBeNull();
  });

  it('stops the run while it goes on', async () => {
    const onStop = vi.fn();
    await render(<BatchExportProgressModal state={RUNNING} onStop={onStop} onClose={vi.fn()} />);

    document.querySelector<HTMLButtonElement>('.rsp-modal-footer .sbb-btn--primary')!.click();

    expect(onStop).toHaveBeenCalledTimes(1);
  });

  it('says how the run ended and offers to close', async () => {
    const onClose = vi.fn();
    const state: BatchState = {
      ...RUNNING,
      status: 'finished',
      processed: 4,
      rows: [row('A', { state: 'finished' }), row('B', { state: 'error' })],
    };
    await render(<BatchExportProgressModal state={state} onStop={vi.fn()} onClose={onClose} />);

    expect(dialog().querySelector('.result')!.textContent).toBe('Export finished with errors');
    expect(visibleButtons()).toEqual(['Close']);
    document.querySelector<HTMLButtonElement>('.rsp-modal-footer .sbb-btn--secondary')!.click();
    expect(onClose).toHaveBeenCalled();
  });

  it('says when the user stopped the run', async () => {
    const state: BatchState = { ...RUNNING, status: 'interrupted', rows: [row('A', { state: 'interrupted' })] };

    await render(<BatchExportProgressModal state={state} onStop={vi.fn()} onClose={vi.fn()} />);

    expect(dialog().querySelector('.result')!.textContent).toBe('Export interrupted by user');
  });
});

describe('BatchExportProgressModal, accessibility', () => {
  it('has no WCAG A/AA violations while running', async () => {
    await render(<BatchExportProgressModal state={RUNNING} onStop={vi.fn()} onClose={vi.fn()} />);

    expect(await a11yViolations(document.querySelector('.rsp-modal')!)).toEqual([]);
  });
});
