import { useEffect, useRef } from 'react';
import { type SearchableDropdownInstance, createSearchableSelect } from '@grigoriev/react-sbb-polarion';

export interface MultiOption {
  value: string;
  label: string;
  /** Per-option icon URL, rendered by the shared dropdown from `data-icon`. */
  icon?: string;
}

interface MultiSearchableSelectProps {
  id: string;
  options: MultiOption[];
  selected: string[];
  onChange: (selected: string[]) => void;
  placeholder: string;
}

/**
 * A `<select multiple>` upgraded to the shared Polarion multiselect (chips + built-in search), so these
 * combos look exactly like every other extension's.
 *
 * react-sbb-polarion has no multiselect component yet, but its bundled `createSearchableSelect` forwards
 * options straight into the vendored SearchableDropdown, which supports `multiselect: true`. This is the
 * archetypal candidate for promotion into RSP - three pages here already want it - at which point this
 * file becomes an import.
 *
 * The `<select>` stays React-controlled. The dropdown mirrors the user's choice back onto it and
 * dispatches `change`, so onChange keeps firing; conversely a change driven from React state needs an
 * explicit syncFromElement(), because assigning `option.selected` fires no event. The dropdown's own
 * MutationObserver only watches childList, so it re-syncs when the option list changes but not when only
 * the selection does.
 */
export default function MultiSearchableSelect({
  id,
  options,
  selected,
  onChange,
  placeholder,
}: MultiSearchableSelectProps) {
  const selectRef = useRef<HTMLSelectElement>(null);
  const dropdownRef = useRef<SearchableDropdownInstance | null>(null);

  useEffect(() => {
    const element = selectRef.current;
    if (!element) {
      return;
    }
    dropdownRef.current = createSearchableSelect(element, { multiselect: true, placeholder: placeholder });
    return () => {
      dropdownRef.current?.destroy();
      dropdownRef.current = null;
    };
    // Created once for the lifetime of the control; the option list is kept in sync by React and picked
    // up by the dropdown's own childList observer.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    // syncFromElement is not in the vendored .d.ts (which declares only selectValue/destroy plus an
    // index signature), hence the cast. Widening it upstream would remove this.
    const sync = dropdownRef.current?.syncFromElement;
    if (typeof sync === 'function') {
      (sync as () => void).call(dropdownRef.current);
    }
  }, [selected, options]);

  return (
    <select
      id={id}
      multiple
      ref={selectRef}
      value={selected}
      onChange={(event) => onChange(Array.from(event.target.selectedOptions).map((option) => option.value))}
    >
      {options.map((option) => (
        <option key={option.value} value={option.value} data-icon={option.icon}>
          {option.label}
        </option>
      ))}
    </select>
  );
}
