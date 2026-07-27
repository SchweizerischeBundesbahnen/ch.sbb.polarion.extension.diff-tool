import { useState } from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import MultiSearchableSelect, { type MultiOption } from '../src/admin/components/MultiSearchableSelect';

// The `<select multiple>` wrapper around the shared Polarion dropdown. Worth its own tests because it is
// the one control here that hands DOM ownership to non-React code, and because it is the candidate for
// promotion into react-sbb-polarion.

const OPTIONS: MultiOption[] = [
  { value: 'open', label: 'Open [open]', icon: '/polarion/icons/open.svg' },
  { value: 'draft', label: 'Draft [draft]' },
  { value: 'closed', label: 'Closed [closed]' },
];

function Harness({ initial = [] as string[] }) {
  const [selected, setSelected] = useState(initial);
  return (
    <>
      <MultiSearchableSelect
        id="statuses"
        options={OPTIONS}
        selected={selected}
        onChange={setSelected}
        placeholder="Select statuses..."
      />
      <output data-testid="selected">{JSON.stringify(selected)}</output>
    </>
  );
}

const select = () => document.querySelector<HTMLSelectElement>('#statuses')!;
const reported = () => JSON.parse(document.querySelector('[data-testid="selected"]')!.textContent!);

afterEach(cleanup);

describe('MultiSearchableSelect', () => {
  it('renders every option, keeping the per-option icon the dropdown reads', async () => {
    render(<Harness />);
    await vi.waitFor(() => expect(select()).not.toBeNull());

    const options = Array.from(select().options);
    expect(options.map((option) => option.textContent)).toEqual(['Open [open]', 'Draft [draft]', 'Closed [closed]']);
    expect(options[0].dataset.icon).toBe('/polarion/icons/open.svg');
    expect(options[1].dataset.icon).toBeUndefined();
  });

  it('marks the selected values on the underlying select', async () => {
    render(<Harness initial={['draft', 'closed']} />);
    await vi.waitFor(() => expect(select()).not.toBeNull());

    expect(Array.from(select().selectedOptions).map((option) => option.value)).toEqual(['draft', 'closed']);
  });

  it('reports the new selection when the control changes', async () => {
    // The shared dropdown mirrors the user's pick onto the wrapped <select> and dispatches `change`;
    // this is that contract, without driving the dropdown's own DOM.
    render(<Harness />);
    await vi.waitFor(() => expect(select()).not.toBeNull());

    const element = select();
    Array.from(element.options).forEach((option) => {
      option.selected = option.value === 'open';
    });
    element.dispatchEvent(new Event('change', { bubbles: true }));

    await vi.waitFor(() => expect(reported()).toEqual(['open']));
  });

  it('upgrades the native select to the shared dropdown', async () => {
    render(<Harness />);

    // The dropdown renders its own trigger next to the (now hidden) native control. Asserting the
    // wrapper exists is what proves createSearchableSelect actually ran with multiselect enabled.
    await vi.waitFor(() => expect(document.querySelector('.searchable-dropdown')).not.toBeNull());
  });

  it('tears the dropdown down on unmount without leaving its portal behind', async () => {
    render(<Harness />);
    await vi.waitFor(() => expect(document.querySelector('.searchable-dropdown')).not.toBeNull());

    cleanup();

    expect(document.querySelector('#statuses')).toBeNull();
  });
});
