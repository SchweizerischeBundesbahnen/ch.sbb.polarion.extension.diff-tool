import { SearchableSelect } from '@grigoriev/react-sbb-polarion';
import type { DuplicationRequest, ProjectInfo } from '../types';

interface DuplicationFormProps {
  projects: ProjectInfo[];
  projectsError: string | null;
  value: DuplicationRequest;
  onChange: (value: DuplicationRequest) => void;
  onSubmit: () => void;
  submitting: boolean;
}

/** Field order drives the "Please fill in: ..." message, so it matches the form top to bottom. */
const FIELDS: { key: keyof DuplicationRequest; label: string; placeholder: string; hint: string }[] = [
  {
    key: 'targetProjectId',
    label: 'New project ID:',
    placeholder: 'e.g. my_new_project',
    hint: 'Must be unique. Allowed: letters, digits, underscore, hyphen.',
  },
  {
    key: 'location',
    label: 'Location:',
    placeholder: '/MyProjects/my_new_project',
    hint: 'Repository path where the new project will live.',
  },
  {
    key: 'trackerPrefix',
    label: 'Tracker prefix:',
    placeholder: 'MNP',
    hint: 'Prefix for new work item IDs (trailing dash added automatically).',
  },
];

/** The names of the empty fields, in form order; empty when the request is complete. */
export function missingFields(value: DuplicationRequest): string[] {
  return (['sourceProjectId', 'targetProjectId', 'location', 'trackerPrefix'] as const).filter(
    (key) => !value[key].trim(),
  );
}

export default function DuplicationForm({
  projects,
  projectsError,
  value,
  onChange,
  onSubmit,
  submitting,
}: DuplicationFormProps) {
  const set = (key: keyof DuplicationRequest, fieldValue: string) => onChange({ ...value, [key]: fieldValue });

  return (
    <div className="duplication-form" id="duplication-form">
      <div className="form-row">
        <label htmlFor="source-project">Source project:</label>
        <SearchableSelect
          id="source-project"
          value={value.sourceProjectId}
          onChange={(selected) => set('sourceProjectId', selected)}
          options={projects.map((project) => ({
            id: project.id,
            name: project.name ? `${project.name} (${project.id})` : project.id,
          }))}
          placeholder={projectsError ? '(failed to load)' : 'Select source project...'}
          allowEmpty
        />
        <span className="field-hint">Existing project to clone.</span>
      </div>

      {FIELDS.map((field) => (
        <div className="form-row" key={field.key}>
          <label htmlFor={field.key}>{field.label}</label>
          <input
            type="text"
            id={field.key}
            placeholder={field.placeholder}
            value={value[field.key]}
            onChange={(event) => set(field.key, event.target.value)}
          />
          <span className="field-hint">{field.hint}</span>
        </div>
      ))}

      <div className="actions">
        <button
          type="button"
          id="start-duplication"
          className="toolbar-button"
          disabled={submitting}
          onClick={onSubmit}
        >
          Start duplication
        </button>
      </div>
    </div>
  );
}
