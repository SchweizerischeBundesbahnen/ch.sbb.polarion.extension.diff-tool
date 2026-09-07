import type { CSSProperties, ReactNode } from 'react';

/**
 * The rows both Document Properties panels are built from.
 *
 * One row is a three-track grid - the checkbox gutter, the label, the control - so every checkbox and
 * every label text of the whole panel lands on the same two x positions, and every control on a third.
 * These components are what guarantees the markup `diff-tool.css` expects, which is why neither panel
 * writes `<div className="property-wrapper">` by hand any more.
 *
 * The checkbox is a **sibling** of its label rather than a child of it, which is what gives it a gutter
 * of its own. `htmlFor` still makes the text toggle the switch.
 *
 * Ported from pdf-exporter's and docx-exporter's `export/formRows.tsx`, which these panels are a sibling
 * of - the three extensions' side panels are looked at next to each other. Two deliberate differences,
 * both because this extension has no export dialog and so only ever lays a form out in a narrow pane:
 * there is no `grows` opt-in, a cell taking the room its label leaves being the only thing a 360px pane
 * has space for; and there is no `ids` prefix, each panel's ids being the legacy fragment's own.
 */

/**
 * Reserves a control's space while hiding it, which is how every optional value field of these panels
 * behaves: `visibility` rather than `display`, so ticking a checkbox does not reflow the rows around it.
 */
const reserved = (shown: boolean): CSSProperties | undefined => (shown ? undefined : { visibility: 'hidden' });

const classes = (...names: (string | false | undefined)[]): string => names.filter(Boolean).join(' ');

export interface FieldCellProps {
  children: ReactNode;
  /** The cell replaces the label rather than following it, starting where the label texts start. */
  wide?: boolean;
  /** `false` keeps the cell's space but hides it - see {@link reserved}. */
  shown?: boolean;
}

/**
 * The cell a control sits in. Always an element of its own, never the control itself: a
 * `SearchableSelect` renders the native `<select>` it upgrades *and* the visible `.searchable-dropdown`
 * next to it, and two grid items in one track would place the second one on a row of its own.
 */
export function FieldCell({ children, wide, shown = true }: Readonly<FieldCellProps>) {
  return (
    <div className={classes('field', wide && 'field-wide')} style={reserved(shown)}>
      {children}
    </div>
  );
}

interface RowBase {
  /**
   * Row modifiers: `hide` for a row a mode switch takes away, `sub-row` for one that belongs to the row
   * above it. The exporters' `full-row` and `tight` are not carried: the first only means something in a
   * two-column section, and every label here fits the shared column.
   */
  className?: string;
  /** The id of the row element itself, where something needs to address the row rather than the control. */
  rowId?: string;
  children?: ReactNode;
}

export interface FieldRowProps extends RowBase {
  label: ReactNode;
  /** The control the label names. */
  labelFor: string;
  title?: string;
}

/** A row whose label names the control beside it. The gutter stays empty, so the label still lines up. */
export function FieldRow({ label, labelFor, title, className, rowId, children }: Readonly<FieldRowProps>) {
  return (
    <div className={classes('property-wrapper', className)} id={rowId}>
      <label htmlFor={labelFor} title={title}>
        {label}
      </label>
      {children}
    </div>
  );
}

export interface SwitchRowProps extends RowBase {
  /** The id of the checkbox, which is also what the label points at. */
  id: string;
  label: ReactNode;
  title?: string;
  checked: boolean;
  onChange: (checked: boolean) => void;
}

/** A row the user switches on, with whatever value it carries beside it. */
export function SwitchRow({
  id,
  label,
  title,
  checked,
  onChange,
  className,
  rowId,
  children,
}: Readonly<SwitchRowProps>) {
  return (
    <div className={classes('property-wrapper', className)} id={rowId}>
      <input id={id} type="checkbox" checked={checked} onChange={(event) => onChange(event.target.checked)} />
      <label htmlFor={id} title={title}>
        {label}
      </label>
      {children}
    </div>
  );
}

export interface RadioOption<T extends string> {
  id: string;
  label: string;
  /** The value the row reports when this option is the chosen one. */
  value: T;
}

export interface RadioPairProps<T extends string> {
  /** The radio group's shared `name`, which is what makes the options exclusive. */
  name: string;
  options: RadioOption<T>[];
  value: T;
  onChange: (value: T) => void;
}

/**
 * A row's value expressed as a choice between named options, side by side while there is room for both.
 *
 * Each radio sits **inside** its own label rather than beside it, which is what lets the pair be laid
 * out as two boxes with a gap between them - the legacy markup put a `margin-right` on every label of
 * the group instead, and so also on the last one.
 *
 * Generic in the value, so a panel holding its choice as a union (`'manual' | 'list'`) gets that union
 * back from `onChange` rather than a bare string it would have to assert.
 */
export function RadioPair<T extends string>({ name, options, value, onChange }: Readonly<RadioPairProps<T>>) {
  return (
    <div className="option-pair">
      {options.map((option) => (
        <label key={option.id} htmlFor={option.id}>
          <input
            id={option.id}
            type="radio"
            name={name}
            value={option.value}
            checked={value === option.value}
            onChange={() => onChange(option.value)}
          />
          {option.label}
        </label>
      ))}
    </div>
  );
}
