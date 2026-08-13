import { useMemo, useState } from 'react';
import { type DiffField, type WorkItemField, fieldId } from '../types';

interface FieldsTransferListProps {
  /** Every field the project offers, as returned by /projects/{id}/workitem-fields. */
  fields: WorkItemField[];
  selected: DiffField[];
  onChange: (selected: DiffField[]) => void;
}

/** `Name [key]`, or `Name [key - Type]` for a type-specific field. Matches the legacy option label. */
function label(field: WorkItemField): string {
  return `${field.name} [${field.key}${field.wiTypeId ? ` - ${field.wiTypeName}` : ''}]`;
}

function byName(left: WorkItemField, right: WorkItemField): number {
  return left.name.localeCompare(right.name);
}

/**
 * Every whitespace-separated part of the term must appear in the label, case-insensitively - so
 * "desc tit" matches nothing but "tit desc" and "title desc" both match "Title [title]" only if both
 * parts are present. Same rule as the legacy filter.
 */
function matches(field: WorkItemField, term: string): boolean {
  const parts = term
    .toLowerCase()
    .trim()
    .split(/\s+/)
    .filter((part) => part.length > 0);
  const text = label(field).toLowerCase();
  return parts.every((part) => text.includes(part));
}

/**
 * The Available/Selected dual listbox for the diff fields, replacing the pair of `<select multiple>`s
 * that diff.js drove imperatively.
 *
 * One deliberate difference: both lists are sorted by name. The legacy page rebuilt only the *target*
 * list sorted on each move, so the available list started in REST order and became sorted after the
 * first Remove - an inconsistency with no upside on a 22-row list that has a filter above it.
 */
export default function FieldsTransferList({ fields, selected, onChange }: FieldsTransferListProps) {
  const [filter, setFilter] = useState('');
  const [availableHighlight, setAvailableHighlight] = useState<string[]>([]);
  const [selectedHighlight, setSelectedHighlight] = useState<string[]>([]);

  const selectedIds = useMemo(() => new Set(selected.map(fieldId)), [selected]);

  const availableFields = useMemo(
    () =>
      fields
        .filter((field) => !selectedIds.has(fieldId(field)))
        .filter((field) => matches(field, filter))
        .sort(byName),
    [fields, selectedIds, filter],
  );

  // Driven by the saved model, not by the visible list, so a field that is selected but no longer offered
  // by the project is still shown (and can be removed) instead of silently disappearing on save.
  const selectedFields = useMemo(() => {
    const byId = new Map(fields.map((field) => [fieldId(field), field]));
    return selected
      .map((field) => byId.get(fieldId(field)) ?? { key: field.key, name: field.key, wiTypeId: field.wiTypeId })
      .sort(byName);
  }, [fields, selected]);

  // A highlight can survive into a state where its option is no longer rendered (the filter hid it, or
  // the field moved lists), which would let an invisible row be moved. Ignore those.
  const movableAvailable = availableHighlight.filter((id) => availableFields.some((f) => fieldId(f) === id));
  const movableSelected = selectedHighlight.filter((id) => selectedFields.some((f) => fieldId(f) === id));

  const add = () => {
    const added = availableFields.filter((field) => movableAvailable.includes(fieldId(field)));
    onChange([...selected, ...added.map((field) => ({ key: field.key, wiTypeId: field.wiTypeId }))]);
    setAvailableHighlight([]);
  };

  const remove = () => {
    onChange(selected.filter((field) => !movableSelected.includes(fieldId(field))));
    setSelectedHighlight([]);
  };

  const highlightOf = (event: React.ChangeEvent<HTMLSelectElement>) =>
    Array.from(event.target.selectedOptions).map((option) => option.value);

  return (
    <div className="fields-transfer">
      <div className="fields-column">
        <div className="fields-column-header">
          <label htmlFor="available-fields">Available fields:</label>
          <input
            type="text"
            id="available-fields-filter"
            className="filter-input"
            aria-label="Filter available fields"
            value={filter}
            onChange={(event) => setFilter(event.target.value)}
          />
        </div>
        <select
          id="available-fields"
          multiple
          size={22}
          value={movableAvailable}
          onChange={(event) => setAvailableHighlight(highlightOf(event))}
        >
          {availableFields.map((field) => (
            <option key={fieldId(field)} value={fieldId(field)}>
              {label(field)}
            </option>
          ))}
        </select>
      </div>

      <div className="fields-buttons">
        <button
          type="button"
          id="add-button"
          className="toolbar-button"
          disabled={movableAvailable.length === 0}
          onClick={add}
        >
          Add &gt;
        </button>
        <button
          type="button"
          id="remove-button"
          className="toolbar-button"
          disabled={movableSelected.length === 0}
          onClick={remove}
        >
          &lt; Remove
        </button>
      </div>

      <div className="fields-column">
        <div className="fields-column-header">
          <label htmlFor="selected-fields">Fields selected for diff:</label>
        </div>
        <select
          id="selected-fields"
          multiple
          size={22}
          value={movableSelected}
          onChange={(event) => setSelectedHighlight(highlightOf(event))}
        >
          {selectedFields.map((field) => (
            <option key={fieldId(field)} value={fieldId(field)}>
              {label(field)}
            </option>
          ))}
        </select>
      </div>
    </div>
  );
}
