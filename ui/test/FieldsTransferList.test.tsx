import { type ReactNode, useState } from 'react';
import { flushSync } from 'react-dom';
import { createRoot } from 'react-dom/client';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import FieldsTransferList from '../src/admin/components/FieldsTransferList';
import type { DiffField, WorkItemField } from '../src/admin/types';

// The Available/Selected dual listbox. The interesting parts are the compound field identity (a key can
// appear once globally and once per work-item type), the multi-part filter, and the guard against moving
// a row that is highlighted but no longer visible.

const FIELDS: WorkItemField[] = [
  { key: 'title', name: 'Title' },
  { key: 'description', name: 'Description' },
  { key: 'status', name: 'Status' },
  // Same key twice: once global, once specific to a work-item type.
  { key: 'severity', name: 'Severity' },
  { key: 'severity', name: 'Severity', wiTypeId: 'defect', wiTypeName: 'Defect' },
];

/** Drives the component the way the page does, so onChange round-trips through state. */
function Harness({ initial = [] as DiffField[] }) {
  const [selected, setSelected] = useState<DiffField[]>(initial);
  return (
    <>
      <FieldsTransferList fields={FIELDS} selected={selected} onChange={setSelected} />
      <output data-testid="selected">{JSON.stringify(selected)}</output>
    </>
  );
}

// Rendered synchronously rather than through vitest-browser-react: the component is pure, so
// flushSync keeps every assertion immediately after the interaction that caused it.
let host: HTMLDivElement;
let root: ReturnType<typeof createRoot>;

beforeEach(() => {
  host = document.createElement('div');
  document.body.appendChild(host);
  root = createRoot(host);
});

afterEach(() => {
  flushSync(() => root.unmount());
  host.remove();
});

function render(component: ReactNode): void {
  flushSync(() => root.render(component));
}

/** Interactions that React must commit before the next assertion. */
function act(action: () => void): void {
  flushSync(action);
}

/**
 * Types into a controlled input. Assigning `.value` directly is not enough: React caches the last value
 * it saw on the node, and an `input` event whose value matches that cache is deduped away, so onChange
 * never fires. Going through the prototype setter updates the node without touching React's cache.
 */
function typeInto(input: HTMLInputElement, value: string): void {
  const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value')!.set!;
  setter.call(input, value);
  act(() => input.dispatchEvent(new Event('input', { bubbles: true })));
}

const optionsOf = (id: string) =>
  Array.from(document.querySelectorAll<HTMLOptionElement>(`#${id} option`)).map((option) => option.textContent);

function highlight(id: string, labels: string[]) {
  const select = document.querySelector<HTMLSelectElement>(`#${id}`)!;
  Array.from(select.options).forEach((option) => {
    option.selected = labels.includes(option.textContent ?? '');
  });
  act(() => select.dispatchEvent(new Event('change', { bubbles: true })));
}

const selectedModel = () => JSON.parse(document.querySelector('[data-testid="selected"]')!.textContent!);

describe('FieldsTransferList', () => {
  it('lists every field as available when nothing is selected, sorted by name', () => {
    render(<Harness />);

    expect(optionsOf('available-fields')).toEqual([
      'Description [description]',
      'Severity [severity]',
      'Severity [severity - Defect]',
      'Status [status]',
      'Title [title]',
    ]);
    expect(optionsOf('selected-fields')).toEqual([]);
  });

  it('keeps the two lists disjoint, distinguishing the global and type-specific field of the same key', () => {
    render(<Harness initial={[{ key: 'severity', wiTypeId: 'defect' }]} />);

    expect(optionsOf('selected-fields')).toEqual(['Severity [severity - Defect]']);
    // The global `severity` must still be offered - it is a different field.
    expect(optionsOf('available-fields')).toContain('Severity [severity]');
    expect(optionsOf('available-fields')).not.toContain('Severity [severity - Defect]');
  });

  it('moves highlighted fields to the selected list and reports them', () => {
    render(<Harness />);

    highlight('available-fields', ['Title [title]', 'Status [status]']);
    act(() => document.querySelector<HTMLButtonElement>('#add-button')!.click());

    // Appended in the order of the available list, which is name-sorted; the model is a set of fields,
    // so its order carries no meaning - the rendered list is sorted independently.
    expect(selectedModel()).toEqual([{ key: 'status' }, { key: 'title' }]);
    expect(optionsOf('selected-fields')).toEqual(['Status [status]', 'Title [title]']);
    expect(optionsOf('available-fields')).not.toContain('Title [title]');
  });

  it('moves fields back out of the selected list', () => {
    render(<Harness initial={[{ key: 'title' }, { key: 'status' }]} />);

    highlight('selected-fields', ['Title [title]']);
    act(() => document.querySelector<HTMLButtonElement>('#remove-button')!.click());

    expect(selectedModel()).toEqual([{ key: 'status' }]);
    expect(optionsOf('available-fields')).toContain('Title [title]');
  });

  it('preserves the work-item type when a type-specific field is added', () => {
    render(<Harness />);

    highlight('available-fields', ['Severity [severity - Defect]']);
    act(() => document.querySelector<HTMLButtonElement>('#add-button')!.click());

    expect(selectedModel()).toEqual([{ key: 'severity', wiTypeId: 'defect' }]);
  });

  it('disables the move buttons until something is highlighted', () => {
    render(<Harness initial={[{ key: 'title' }]} />);
    const add = document.querySelector<HTMLButtonElement>('#add-button')!;
    const remove = document.querySelector<HTMLButtonElement>('#remove-button')!;

    expect(add.disabled).toBe(true);
    expect(remove.disabled).toBe(true);

    highlight('available-fields', ['Status [status]']);
    expect(document.querySelector<HTMLButtonElement>('#add-button')!.disabled).toBe(false);

    highlight('selected-fields', ['Title [title]']);
    expect(document.querySelector<HTMLButtonElement>('#remove-button')!.disabled).toBe(false);
  });

  it('filters the available list by every whitespace-separated part of the term', () => {
    render(<Harness />);
    const filter = document.querySelector<HTMLInputElement>('#available-fields-filter')!;

    typeInto(filter, 'sev');
    expect(optionsOf('available-fields')).toEqual(['Severity [severity]', 'Severity [severity - Defect]']);

    // Both parts must match, which is how the one type-specific variant is singled out.
    typeInto(filter, 'sev defect');
    expect(optionsOf('available-fields')).toEqual(['Severity [severity - Defect]']);

    typeInto(filter, 'nothing matches this');
    expect(optionsOf('available-fields')).toEqual([]);
  });

  it('does not move a field that the filter has hidden since it was highlighted', () => {
    // The legacy page deselected hidden options for the same reason: otherwise Add moves a row the user
    // can no longer see.
    render(<Harness />);
    highlight('available-fields', ['Title [title]']);

    const filter = document.querySelector<HTMLInputElement>('#available-fields-filter')!;
    typeInto(filter, 'severity');

    expect(document.querySelector<HTMLButtonElement>('#add-button')!.disabled).toBe(true);
    act(() => document.querySelector<HTMLButtonElement>('#add-button')!.click());
    expect(selectedModel()).toEqual([]);
  });

  it('still shows a selected field the project no longer offers, so it can be removed', () => {
    render(<Harness initial={[{ key: 'retiredField' }]} />);

    // Falls back to the raw key as the label rather than dropping the row, which would silently discard
    // it from the model on the next save.
    expect(optionsOf('selected-fields')).toEqual(['retiredField [retiredField]']);

    highlight('selected-fields', ['retiredField [retiredField]']);
    act(() => document.querySelector<HTMLButtonElement>('#remove-button')!.click());
    expect(selectedModel()).toEqual([]);
  });
});
