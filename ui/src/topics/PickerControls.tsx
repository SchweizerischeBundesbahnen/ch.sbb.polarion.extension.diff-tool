import { SearchableSelect } from '@grigoriev/react-sbb-polarion';
import type { SettingName } from '@grigoriev/react-sbb-polarion';
// The same icon the Document Properties panel's Compare button uses.
import compareIcon from '../formext/compare.svg';
import type { LinkRoleOption, ProjectInfo } from './types';

interface PickerControlsProps {
  compareDisabled: boolean;
  /** The tooltip explaining what still has to be selected; the widget showed it unconditionally. */
  compareTitle: string;
  onCompare: () => void;
  hint: string;
  /** Omitted by the collections picker, which shows the target project above its target table instead. */
  projects?: ProjectInfo[] | null;
  targetProjectId?: string;
  onTargetProjectChange?: (value: string) => void;
  linkRoles: LinkRoleOption[];
  linkRole: string;
  onLinkRoleChange: (value: string) => void;
  configurations: SettingName[];
  configuration: string;
  onConfigurationChange: (value: string) => void;
  error?: string | null;
}

/** The link role select, labelled with both directions of the role, as the widget rendered it. */
export function linkRoleOptions(linkRoles: LinkRoleOption[]) {
  return linkRoles.map((role) => ({
    id: role.id,
    name: role.oppositeName ? `${role.name} / ${role.oppositeName}` : role.name,
  }));
}

/** The target project select, which the collections picker renders inside its target query row. */
export function TargetProjectSelect({
  projects,
  value,
  onChange,
}: Readonly<{ projects: ProjectInfo[]; value: string; onChange: (value: string) => void }>) {
  return (
    <div className="target-project">
      <label htmlFor="target-project-selector">Target project:</label>
      <SearchableSelect
        id="target-project-selector"
        value={value}
        onChange={onChange}
        options={projects}
        placeholder="Select Project..."
      />
    </div>
  );
}

/**
 * The head of a picker page: the Compare button with its hint, and the selections both pickers share.
 */
export default function PickerControls({
  compareDisabled,
  compareTitle,
  onCompare,
  hint,
  projects,
  targetProjectId,
  onTargetProjectChange,
  linkRoles,
  linkRole,
  onLinkRoleChange,
  configurations,
  configuration,
  onConfigurationChange,
  error,
}: Readonly<PickerControlsProps>) {
  return (
    <>
      <div className="top-pane">
        <button
          type="button"
          className="compare-button"
          id="compare-items"
          title={compareTitle}
          disabled={compareDisabled}
          onClick={onCompare}
        >
          <img src={compareIcon} alt="" />
          Compare
        </button>
        <p>{hint}</p>
      </div>

      {error ? <div className="alert alert-error">{error}</div> : null}

      {projects && onTargetProjectChange ? (
        <TargetProjectSelect projects={projects} value={targetProjectId ?? ''} onChange={onTargetProjectChange} />
      ) : null}

      <div className="link-role">
        <label htmlFor="link-role-selector">Link role:</label>
        <SearchableSelect
          id="link-role-selector"
          value={linkRole}
          onChange={onLinkRoleChange}
          options={linkRoleOptions(linkRoles)}
          placeholder="Select Link Role..."
          allowEmpty
        />
      </div>

      <div className="configuration">
        <label htmlFor="config-selector">Configuration:</label>
        <SearchableSelect
          id="config-selector"
          value={configuration}
          onChange={onConfigurationChange}
          options={configurations.map((setting) => ({ id: setting.name, name: setting.name }))}
          placeholder="Select Configuration..."
        />
      </div>
    </>
  );
}
