import type { ReactNode } from 'react';

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
 * of - the three extensions' side panels are looked at next to each other. Four of that version's props
 * are deliberately not carried:
 *
 * - `grows`, because a cell taking the room its label leaves is the only thing a 360px pane has space
 *   for - so it is what every cell here does, rather than an opt-in.
 * - `ids`, the per-surface id prefix. This extension opens no export dialog over its panels, and each
 *   panel's ids are the legacy fragment's own.
 * - `shown`, which hid a cell with `visibility` instead of removing it, so that ticking a checkbox could
 *   not reflow the rows around it. What a switch reveals here is not a field beside it but whole
 *   sub-rows under it - the work items filter's radios and its list of ids - and reserving those would
 *   leave a permanent gap above the button rather than prevent a jump. They are removed when the switch
 *   is off.
 * - `title`, the tooltip a row's label carries. No row on either panel has one.
 */

const classes = (...names: (string | false | undefined)[]): string => names.filter(Boolean).join(' ');

export interface FieldCellProps {
  children: ReactNode;
  /** The cell replaces the label rather than following it, starting where the label texts start. */
  wide?: boolean;
}

/**
 * The cell a control sits in. Always an element of its own, never the control itself: a
 * `SearchableSelect` renders the native `<select>` it upgrades *and* the visible `.searchable-dropdown`
 * next to it, and two grid items in one track would place the second one on a row of its own.
 */
export function FieldCell({ children, wide }: Readonly<FieldCellProps>) {
  return <div className={classes('field', wide && 'field-wide')}>{children}</div>;
}

interface RowBase {
  /**
   * Row modifiers, of which `hide` - for a row a mode switch takes away - is the only one either panel
   * uses. A row belonging to the row above it is {@link SubRow}, which owns that class itself. The
   * exporters' `full-row` and `tight` are not carried: the first only means something in a two-column
   * section, and every label here fits the shared column.
   */
  className?: string;
  /** The id of the row element itself, where something needs to address the row rather than the control. */
  rowId?: string;
  children?: ReactNode;
}

export interface FieldRowProps extends RowBase {
  label: ReactNode;
  /**
   * The control the label names, where the row has exactly one.
   *
   * Left out for a row whose value is a **radio group**, which has no single control to name: pointing
   * the label at one of the radios would make a click on the row's own label choose that option. The
   * revision row is the case - `htmlFor="revision-enter-manually"` there meant clicking `Revision:`
   * switched the panel out of "Select from list" and threw away the revision the user had picked. The
   * legacy fragment pointed that label at a `<div>`, which is not a labelable element and so did
   * nothing at all; this leaves the label unassociated rather than associated with the wrong thing.
   *
   * Name the group through {@link labelId} + `RadioPair`'s `ariaLabelledBy` instead.
   */
  labelFor?: string;
  /** The id of the label element, for a group that has to point back at it to be named. */
  labelId?: string;
}

/** A row whose label names the control beside it. The gutter stays empty, so the label still lines up. */
export function FieldRow({ label, labelFor, labelId, className, rowId, children }: Readonly<FieldRowProps>) {
  return (
    <div className={classes('property-wrapper', className)} id={rowId}>
      <label htmlFor={labelFor} id={labelId}>
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
  checked: boolean;
  onChange: (checked: boolean) => void;
}

/** A row the user switches on, with whatever value it carries beside it. */
export function SwitchRow({ id, label, checked, onChange, className, rowId, children }: Readonly<SwitchRowProps>) {
  return (
    <div className={classes('property-wrapper', className)} id={rowId}>
      <input id={id} type="checkbox" checked={checked} onChange={(event) => onChange(event.target.checked)} />
      <label htmlFor={id}>{label}</label>
      {children}
    </div>
  );
}

export interface SubRowProps {
  /** The id of the row element, which is the legacy fragment's own. */
  rowId?: string;
  /** The cell starts where the label texts start rather than at the control column - `FieldCell.wide`. */
  wide?: boolean;
  children: ReactNode;
}

/**
 * A row that belongs to the row above it and carries nothing but a value: what a switch or a radio pair
 * revealed.
 *
 * It has no checkbox and no label of its own - the row above named it - so it is a cell and the row
 * modifier that ties it to that row, and there is nothing for a caller to vary but the id and which
 * column the cell starts in. That is why the cell is folded in here rather than passed as children:
 * every one of these rows has exactly one.
 *
 * Its own component so that no panel hand-writes `property-wrapper`, which is the whole point of this
 * module - the markup `diff-tool.css` is written against lives in one place. Pairing `sub-row` with
 * `property-wrapper` by hand at five call sites was also a class name away from silently losing the
 * row's layout.
 */
export function SubRow({ rowId, wide, children }: Readonly<SubRowProps>) {
  return (
    <div className="property-wrapper sub-row" id={rowId}>
      <FieldCell wide={wide}>{children}</FieldCell>
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
  /**
   * The id of the element naming the group - the row's label, where the group is a row's value. This is
   * how such a row is named, `FieldRow` deliberately not pointing its `<label>` at one of the radios
   * (see {@link FieldRowProps.labelFor}).
   */
  ariaLabelledBy?: string;
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
export function RadioPair<T extends string>({
  name,
  options,
  value,
  onChange,
  ariaLabelledBy,
}: Readonly<RadioPairProps<T>>) {
  return (
    <div className="option-pair" role="radiogroup" aria-labelledby={ariaLabelledBy}>
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
