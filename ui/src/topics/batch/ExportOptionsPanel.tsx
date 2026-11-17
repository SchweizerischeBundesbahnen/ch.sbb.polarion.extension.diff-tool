import { SearchableSelect } from '@sbb-polarion/react-sbb-polarion';
import type { SettingName } from '@sbb-polarion/react-sbb-polarion';
import type { EmbedViewOptions } from '../../embed/exportMode';
import type { Orientation, PaperSize } from '../../services/pdfExport';
import { linkRoleOptions } from '../PickerControls';
import type { LinkRoleOption } from '../types';
import type { BatchSettings, CompareAs, DiffView } from './batchExport';

/** The options of a batch which this panel edits: everything but the link role and the configuration. */
export type ExportOptions = Omit<BatchSettings, 'linkRole' | 'config'>;

export const DEFAULT_EXPORT_OPTIONS: ExportOptions = {
  compareAs: 'Workitems',
  diffView: 'side-by-side',
  pdf: { paperSize: 'A4', orientation: 'landscape' },
  view: {
    hideChaptersIfNoDifference: true,
    showOutlineNumbersDiff: false,
    compareEnumsById: false,
    counterpartWorkItemsDiffer: false,
    compareOnlyMutualFields: true,
  },
};

const COMPARE_AS_OPTIONS: { id: CompareAs; name: string }[] = [
  { id: 'Workitems', name: 'Work items' },
  { id: 'Fields', name: 'Document fields' },
  { id: 'Content', name: 'Content between work items' },
];

const DIFF_VIEW_OPTIONS: { id: DiffView; name: string }[] = [
  { id: 'side-by-side', name: 'Side by side' },
  { id: 'unified', name: 'Unified' },
];

const PAPER_SIZE_OPTIONS: { id: PaperSize; name: string }[] = [
  { id: 'A4', name: 'A4' },
  { id: 'A3', name: 'A3' },
];

const ORIENTATION_OPTIONS: { id: Orientation; name: string }[] = [
  { id: 'landscape', name: 'Landscape' },
  { id: 'portrait', name: 'Portrait' },
];

/** The view options and the comparisons they apply to, as the viewer's control pane offers them. */
const VIEW_CHECKBOXES: { option: keyof EmbedViewOptions; label: string; appliesTo: CompareAs[] }[] = [
  { option: 'hideChaptersIfNoDifference', label: 'Hide chapters without differences', appliesTo: ['Workitems'] },
  { option: 'showOutlineNumbersDiff', label: 'Show differences of outline numbers', appliesTo: ['Workitems'] },
  { option: 'counterpartWorkItemsDiffer', label: 'Counterpart work items differ', appliesTo: ['Workitems'] },
  { option: 'compareEnumsById', label: 'Compare enums by ID', appliesTo: ['Workitems', 'Fields'] },
  { option: 'compareOnlyMutualFields', label: 'Compare only mutual fields', appliesTo: ['Fields'] },
];

interface Props {
  value: ExportOptions;
  onChange: (value: ExportOptions) => void;
  linkRoles: LinkRoleOption[];
  linkRole: string;
  onLinkRoleChange: (value: string) => void;
  configurations: SettingName[];
  configuration: string;
  onConfigurationChange: (value: string) => void;
}

function Checkbox({
  id,
  label,
  checked,
  onChange,
}: Readonly<{ id: string; label: string; checked: boolean; onChange: (checked: boolean) => void }>) {
  return (
    <div className="checkbox-row">
      <input id={id} type="checkbox" checked={checked} onChange={(event) => onChange(event.target.checked)} />
      <label htmlFor={id}>{label}</label>
    </div>
  );
}

/**
 * The options of a batch PDF export: how each pair is compared and shown, and how its PDF is laid out. They
 * are the options of the viewer's control pane, chosen once for the whole batch.
 */
export default function ExportOptionsPanel({
  value,
  onChange,
  linkRoles,
  linkRole,
  onLinkRoleChange,
  configurations,
  configuration,
  onConfigurationChange,
}: Readonly<Props>) {
  const setView = (option: keyof EmbedViewOptions, checked: boolean) =>
    onChange({ ...value, view: { ...value.view, [option]: checked } });

  return (
    <details className="export-options" open>
      <summary>Export options</summary>
      <div className="export-options-body">
        <fieldset>
          <legend>Comparison</legend>
          <div className="option-row">
            <label htmlFor="batch-compare-as">Compare</label>
            <SearchableSelect
              id="batch-compare-as"
              value={value.compareAs}
              onChange={(compareAs) => onChange({ ...value, compareAs: compareAs as CompareAs })}
              options={COMPARE_AS_OPTIONS}
              searchable={false}
            />
          </div>
          {value.compareAs === 'Workitems' ? (
            <div className="option-row">
              <label htmlFor="batch-link-role">Link role</label>
              <SearchableSelect
                id="batch-link-role"
                value={linkRole}
                onChange={onLinkRoleChange}
                options={linkRoleOptions(linkRoles)}
                placeholder="Select Link Role..."
                allowEmpty
              />
            </div>
          ) : null}
          <div className="option-row">
            <label htmlFor="batch-config">Configuration</label>
            <SearchableSelect
              id="batch-config"
              value={configuration}
              onChange={onConfigurationChange}
              options={configurations.map((setting) => ({ id: setting.name, name: setting.name }))}
              placeholder="Select Configuration..."
            />
          </div>
          <div className="option-row">
            <label htmlFor="batch-diff-view">Diff view</label>
            <SearchableSelect
              id="batch-diff-view"
              value={value.diffView}
              onChange={(diffView) => onChange({ ...value, diffView: diffView as DiffView })}
              options={DIFF_VIEW_OPTIONS}
              searchable={false}
            />
          </div>
          {VIEW_CHECKBOXES.filter((checkbox) => checkbox.appliesTo.includes(value.compareAs)).map((checkbox) => (
            <Checkbox
              key={checkbox.option}
              id={`batch-${checkbox.option}`}
              label={checkbox.label}
              checked={value.view[checkbox.option]}
              onChange={(checked) => setView(checkbox.option, checked)}
            />
          ))}
        </fieldset>

        <fieldset>
          <legend>PDF</legend>
          <div className="option-row">
            <label htmlFor="batch-paper-size">Paper size</label>
            <SearchableSelect
              id="batch-paper-size"
              value={value.pdf.paperSize}
              onChange={(paperSize) => onChange({ ...value, pdf: { ...value.pdf, paperSize: paperSize as PaperSize } })}
              options={PAPER_SIZE_OPTIONS}
              searchable={false}
            />
          </div>
          <div className="option-row">
            <label htmlFor="batch-orientation">Orientation</label>
            <SearchableSelect
              id="batch-orientation"
              value={value.pdf.orientation}
              onChange={(orientation) =>
                onChange({ ...value, pdf: { ...value.pdf, orientation: orientation as Orientation } })
              }
              options={ORIENTATION_OPTIONS}
              searchable={false}
            />
          </div>
        </fieldset>
      </div>
    </details>
  );
}
