import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  ConfigurationButtons,
  PageLayout,
  type Revision,
  RevisionsTable,
  getScope,
  useConfirm,
} from '@grigoriev/react-sbb-polarion';
import { toast } from 'sonner';
import useRemote from '../../services/useRemote';
import useSettings, { DEFAULT_CONFIGURATION } from '../../services/useSettings';
import ChartPanel from '../queue/ChartPanel';
import ThreadsTable from '../queue/ThreadsTable';
import type { ChartSeries } from '../queue/WorkerChart';
import WorkersConfigurationTable, { SKIP_QUEUE } from '../queue/WorkersConfigurationTable';
import useQueueStatistics, { CPU_LOAD_CHART, type Statistics } from '../queue/useQueueStatistics';
import type { ExecutionQueueModel, FeatureInfo, QueueConfigurationMeta } from '../types';

const DEFAULT_INTERVAL_MINUTES = 1;

interface Settings {
  workers: Record<string, number>;
  threads: Record<string, number>;
  bundleTimestamp?: string;
}

const EMPTY: Settings = { workers: {}, threads: {} };

function pointsOf(statistics: Statistics, workerKey: string, featureId: string, value: 'queued' | 'executing') {
  return (statistics[workerKey]?.[featureId] ?? []).map((entry) => ({
    x: new Date(entry.timestamp).getTime(),
    y: Number(entry[value] ?? 0),
  }));
}

export default function ExecutionQueuePage() {
  const scope = getScope();
  const { sendRequest } = useRemote();
  const settings = useSettings<ExecutionQueueModel>('executionQueue');
  // RSP's dialog rather than window.confirm: the browser's own is chrome-coloured, announces itself as
  // "localhost says" and cannot be styled, so on an otherwise entirely-ours admin page it reads as
  // though something broke rather than as though a question was asked.
  const { confirm, confirmDialog } = useConfirm();

  const [meta, setMeta] = useState<QueueConfigurationMeta | null>(null);
  const [saved, setSaved] = useState<Settings>(EMPTY);
  const [pendingWorkers, setPendingWorkers] = useState<Record<string, number>>({});
  const [pendingThreads, setPendingThreads] = useState<Record<string, number>>({});
  const [intervals, setIntervals] = useState<Record<string, number>>({});
  const [currentBundleTimestamp, setCurrentBundleTimestamp] = useState<string | undefined>(undefined);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [revisionsShown, setRevisionsShown] = useState(false);
  const [revisionsToken, setRevisionsToken] = useState(0);

  /**
   * Charts to render: every worker some feature is actually assigned to, in ascending order, plus the CPU
   * load chart, which is always shown. A worker nobody uses gets no chart, exactly as before.
   */
  const charts = useMemo(() => {
    const used = new Set(
      Object.values(saved.workers)
        .filter((worker) => worker !== SKIP_QUEUE)
        .map(String),
    );
    return [...[...used].sort((left, right) => Number(left) - Number(right)), CPU_LOAD_CHART];
  }, [saved.workers]);

  const {
    statistics,
    error: statisticsError,
    reset: resetChart,
    resetAll: resetCharts,
  } = useQueueStatistics({ charts: charts, intervals: intervals, enabled: meta !== null });

  const applySettings = useCallback((model: ExecutionQueueModel) => {
    setSaved({
      workers: model.workers ?? {},
      threads: model.threads ?? {},
      bundleTimestamp: model.bundleTimestamp,
    });
    // Pending edits belong to the configuration that was on screen; a freshly loaded one starts clean.
    setPendingWorkers({});
    setPendingThreads({});
  }, []);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const [metaResponse, infoResponse] = await Promise.all([
          sendRequest({ method: 'GET', url: '/queue/configuration-meta' }),
          sendRequest({ method: 'GET', url: '/extension/info' }),
        ]);
        if (!metaResponse.ok) {
          throw new Error(`Loading queue configuration failed (HTTP ${metaResponse.status})`);
        }
        const loadedMeta: QueueConfigurationMeta = await metaResponse.json();
        const timestamp = infoResponse.ok
          ? ((await infoResponse.json())?.version?.bundleBuildTimestamp as string | undefined)
          : undefined;
        const model = await settings.loadContent(DEFAULT_CONFIGURATION, scope);
        if (cancelled) {
          return;
        }
        setCurrentBundleTimestamp(timestamp);
        setMeta(loadedMeta);
        applySettings(model);
        setLoadError(null);
      } catch (error) {
        if (!cancelled) {
          setLoadError((error as Error).message);
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [sendRequest, settings, scope, applySettings]);

  // Every chart starts at the shortest interval, which is also the cheapest for the server.
  useEffect(() => {
    setIntervals((current) => {
      const next = { ...current };
      let changed = false;
      for (const chart of charts) {
        if (next[chart] === undefined) {
          next[chart] = DEFAULT_INTERVAL_MINUTES;
          changed = true;
        }
      }
      return changed ? next : current;
    });
  }, [charts]);

  const changeInterval = (chart: string, minutes: number) => {
    setIntervals((current) => ({ ...current, [chart]: minutes }));
    // Enlarging the window cannot be served from what we already hold, so drop the chart's history and
    // let the next poll re-request the whole interval. The new interval is passed explicitly - the state
    // update above has not been applied yet.
    resetChart(chart, minutes);
  };

  const save = async () => {
    const workers = { ...saved.workers, ...pendingWorkers };
    const threads = { ...saved.threads, ...pendingThreads };
    try {
      await settings.saveContent(DEFAULT_CONFIGURATION, scope, { workers: workers, threads: threads });
      setSaved((current) => ({ ...current, workers: workers, threads: threads }));
      setPendingWorkers({});
      setPendingThreads({});
      // Reassigning a feature changes which worker its series belongs to, so the accumulated history no
      // longer describes the current layout.
      resetCharts();
      setRevisionsToken((token) => token + 1);
      toast.success('Settings saved');
    } catch (error) {
      toast.error(`Settings not saved: ${(error as Error).message}`);
    }
  };

  const reload = async () => {
    try {
      applySettings(await settings.loadContent(DEFAULT_CONFIGURATION, scope));
      resetCharts();
      setLoadError(null);
    } catch (error) {
      setLoadError((error as Error).message);
    }
  };

  const cancel = async () => {
    if (await confirm('Are you sure you want to cancel editing and revert all changes made?')) {
      void reload();
    }
  };

  const revertToDefault = async () => {
    if (!(await confirm('Are you sure you want to return the default values?'))) {
      return;
    }
    try {
      applySettings(await settings.loadDefaultContent());
      toast.info('Reverted to default values. Save to apply.');
    } catch (error) {
      toast.error((error as Error).message);
    }
  };

  const revertToRevision = async (revision: Revision) => {
    try {
      applySettings(await settings.loadContent(DEFAULT_CONFIGURATION, scope, revision.name));
      toast.info(`Loaded revision ${revision.name}. Save to apply.`);
    } catch (error) {
      toast.error((error as Error).message);
    }
  };

  const featureLabel = (featureId: string) =>
    meta?.features.find((feature) => feature.id === featureId)?.label ?? featureId;

  /** Two series per feature assigned to the worker - queued and running - as the legacy chart had. */
  const workerSeries = (worker: string): ChartSeries[] =>
    Object.entries(saved.workers)
      .filter(([, assigned]) => String(assigned) === worker)
      .flatMap(([featureId]) => [
        { label: `${featureLabel(featureId)} (queue)`, points: pointsOf(statistics, worker, featureId, 'queued') },
        { label: `${featureLabel(featureId)} (running)`, points: pointsOf(statistics, worker, featureId, 'executing') },
      ]);

  const cpuSeries = (cpuLoad: FeatureInfo): ChartSeries[] => [
    {
      label: cpuLoad.label,
      points: (statistics.COMMON?.[CPU_LOAD_CHART] ?? []).map((entry) => ({
        x: new Date(entry.timestamp).getTime(),
        // The server reports a 0..1 fraction; the axis is a percentage.
        y: Number((Number(entry.value ?? 0) * 100).toFixed(2)),
      })),
    },
  ];

  const newerVersion =
    !!saved.bundleTimestamp && !!currentBundleTimestamp && saved.bundleTimestamp !== currentBundleTimestamp;

  return (
    <PageLayout title="Execution Queue Management Panel">
      {loadError && <div className="alert alert-error">{loadError}</div>}
      {statisticsError && <div className="alert alert-error">{statisticsError}</div>}
      {newerVersion && (
        <div className="alert alert-warning">
          These settings were saved by a different version of the extension. Review and save them again.
        </div>
      )}

      {meta && (
        <>
          <div className="chart-containers">
            {charts.map((chart) =>
              chart === CPU_LOAD_CHART ? (
                <ChartPanel
                  key={chart}
                  chartKey={chart}
                  title={meta.cpuLoad.label}
                  series={cpuSeries(meta.cpuLoad)}
                  yAxisTitle="Load (%)"
                  intervalMinutes={intervals[chart] ?? DEFAULT_INTERVAL_MINUTES}
                  onIntervalChange={(minutes) => changeInterval(chart, minutes)}
                />
              ) : (
                <ChartPanel
                  key={chart}
                  chartKey={chart}
                  title={`Worker-${chart}`}
                  series={workerSeries(chart)}
                  yAxisTitle="Requests"
                  intervalMinutes={intervals[chart] ?? DEFAULT_INTERVAL_MINUTES}
                  onIntervalChange={(minutes) => changeInterval(chart, minutes)}
                />
              ),
            )}
          </div>

          <h2>Workers Configuration</h2>
          <div className="workers-configuration-container">
            <WorkersConfigurationTable
              features={meta.features}
              workerCount={meta.workerCount}
              current={saved.workers}
              pending={pendingWorkers}
              onAssign={(featureId, worker) =>
                setPendingWorkers((current) => {
                  const next = { ...current };
                  if (worker === null) {
                    delete next[featureId];
                  } else {
                    next[featureId] = worker;
                  }
                  return next;
                })
              }
            />
            <ThreadsTable
              workerCount={meta.workerCount}
              maxThreads={meta.maxRecommendedThreads}
              current={saved.threads}
              pending={pendingThreads}
              onChange={(worker, threads) =>
                setPendingThreads((current) => {
                  const next = { ...current };
                  if (threads === null) {
                    delete next[String(worker)];
                  } else {
                    next[String(worker)] = threads;
                  }
                  return next;
                })
              }
            />
          </div>
        </>
      )}

      <ConfigurationButtons
        onSave={() => void save()}
        onCancel={() => void cancel()}
        onRevertToDefault={() => void revertToDefault()}
        onToggleRevisions={() => setRevisionsShown((shown) => !shown)}
        revisionsShown={revisionsShown}
      />

      {confirmDialog}

      {revisionsShown && (
        <RevisionsTable
          name={DEFAULT_CONFIGURATION}
          scope={scope}
          reloadToken={revisionsToken}
          loadRevisions={settings.loadRevisions}
          onRevert={(revision) => void revertToRevision(revision)}
        />
      )}

      <div className="quick-help">
        <h2>Quick Help</h2>
        <h3>Workers</h3>
        <p>
          Worker is a combination of a queue and its own executor. Current queue limit:{' '}
          <b>{meta?.queueCapacity ?? '?'}</b> entries.
        </p>
        <p>
          Executor fetches entries from the queue and processes them in parallel mode (if threads count for particular
          worker &gt; 1).
        </p>
        <p>
          You can point specific endpoint call (AKA on this page as <b>Feature</b>) to a specific worker. This gives the
          opportunity to fine-tune CPU consumption.
        </p>
        <h3>Threads count</h3>
        <p>
          Max threads count depends on CPU cores/logical processors count
          {meta ? ` (${meta.maxRecommendedThreads} here)` : ''}.
        </p>
        <p>
          Be careful setting high values for several workers because this can lead to high system resources consumption.
        </p>
      </div>
    </PageLayout>
  );
}
