import { SearchableSelect } from '@grigoriev/react-sbb-polarion';
import type { FeatureInfo } from '../types';

/** Worker 0 means "not queued at all" and is shown as a dash rather than a number. */
export const SKIP_QUEUE = 0;

interface WorkersConfigurationTableProps {
  features: FeatureInfo[];
  workerCount: number;
  /** Persisted assignment: feature id -> worker number. */
  current: Record<string, number>;
  /** Pending reassignment: feature id -> worker number, only for features the user changed. */
  pending: Record<string, number>;
  onAssign: (featureId: string, worker: number | null) => void;
}

export default function WorkersConfigurationTable({
  features,
  workerCount,
  current,
  pending,
  onAssign,
}: WorkersConfigurationTableProps) {
  const options = [
    { id: String(SKIP_QUEUE), name: 'Skip Queue' },
    ...Array.from({ length: workerCount }, (_, index) => ({ id: String(index + 1), name: String(index + 1) })),
  ];

  return (
    <div className="workers-configuration-table" id="features-workers">
      <table>
        <thead>
          <tr>
            <th>Feature</th>
            <th>Current Worker</th>
            <th>Assign new</th>
          </tr>
        </thead>
        <tbody>
          {features.map((feature) => {
            const assigned = current[feature.id] ?? SKIP_QUEUE;
            return (
              <tr key={feature.id}>
                <td>
                  {/* The label and tooltip come from the Feature enum via /queue/configuration-meta; they
                      used to be a hardcoded map in this page's JavaScript. */}
                  <span id={`feature-${feature.id}`}>{feature.label}</span>
                  <div className="more-info" id={`feature-more-info-${feature.id}`} title={feature.description} />
                </td>
                <td id={`current-worker-${feature.id}`} className="center-column-value">
                  {assigned === SKIP_QUEUE ? '-' : assigned}
                </td>
                <td>
                  <SearchableSelect
                    id={`new-worker-${feature.id}`}
                    value={pending[feature.id] === undefined ? '' : String(pending[feature.id])}
                    onChange={(value) => onAssign(feature.id, value === '' ? null : Number(value))}
                    // The worker it already uses is not offered, as in the legacy page.
                    options={options.filter((option) => option.id !== String(assigned))}
                    placeholder="Choose new worker..."
                    allowEmpty
                  />
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
