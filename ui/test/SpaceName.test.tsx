import { afterEach, describe, expect, it } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import SpaceName from '../src/components/SpaceName';

// A space in the viewer header and the merge dialog: its title, then its ID in a muted element of its own.

const shown = () => document.querySelector('.space-name')!;
const spaceId = () => shown().querySelector('.space-id');

afterEach(() => {
  cleanup();
});

describe('SpaceName', () => {
  it('shows the title, then the ID in its own element', async () => {
    await render(<SpaceName title="Specs (old)" id="Specification" />);

    expect(shown().firstChild!.textContent).toBe('Specs (old)');
    expect(spaceId()!.textContent).toBe('Specification');
  });

  it('shows only the ID of a space without its own title', async () => {
    await render(<SpaceName title="Specification" id="Specification" />);

    expect(shown().textContent).toBe('Specification');
    expect(spaceId()).toBeNull();
  });

  it('shows the default space as "Default Space"', async () => {
    await render(<SpaceName title={null} id="_default" />);

    expect(shown().textContent).toBe('Default Space');
    expect(spaceId()).toBeNull();
  });
});
