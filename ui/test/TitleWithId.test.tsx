import { afterEach, describe, expect, it } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import TitleWithId from '../src/topics/batch/TitleWithId';

// The cell content of the batch export table: a title, with its ID below it only where the two differ.

const shown = () => document.querySelector('.title-with-id')!;
const secondary = () => shown().querySelector('.secondary-id');

afterEach(() => {
  cleanup();
});

describe('TitleWithId', () => {
  it('shows the title once when it is the ID', async () => {
    await render(<TitleWithId title="Design" id="Design" />);

    expect(shown().textContent).toBe('Design');
    expect(secondary()).toBeNull();
  });

  it('shows the ID below a title which differs from it', async () => {
    await render(<TitleWithId title="Design Documents" id="Design" />);

    expect(shown().firstChild!.textContent).toBe('Design Documents');
    expect(secondary()!.textContent).toBe('Design');
  });

  it('shows the ID once when there is no title', async () => {
    await render(<TitleWithId title={null} id="Design" />);

    expect(shown().textContent).toBe('Design');
    expect(secondary()).toBeNull();
  });

  it('shows the title as its children render it', async () => {
    await render(
      <TitleWithId title="Catalog" id="catalog-1">
        <a href="#catalog">Catalog</a>
      </TitleWithId>,
    );

    expect(shown().querySelector('a')!.textContent).toBe('Catalog');
    expect(secondary()!.textContent).toBe('catalog-1');
  });
});
