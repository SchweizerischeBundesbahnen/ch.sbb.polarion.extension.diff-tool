import { describe, expect, it } from 'vitest';
import { spaceLabelParts, spaceOption } from '../src/utils/spaceLabel';

describe('spaceLabelParts', () => {
  it('returns the title and the ID', () => {
    expect(spaceLabelParts('Requirements', 'Specification')).toEqual({ title: 'Requirements', id: 'Specification' });
  });

  it('returns only the ID of a space without its own title', () => {
    expect(spaceLabelParts('Specification', 'Specification')).toEqual({ title: 'Specification' });
    expect(spaceLabelParts(' ', 'Specification')).toEqual({ title: 'Specification' });
    expect(spaceLabelParts(null, 'Specification')).toEqual({ title: 'Specification' });
    expect(spaceLabelParts(undefined, 'Specification')).toEqual({ title: 'Specification' });
  });

  it('returns the default space as "Default Space"', () => {
    expect(spaceLabelParts('Default Space', '_default')).toEqual({ title: 'Default Space' });
    expect(spaceLabelParts('_default', '_default')).toEqual({ title: 'Default Space' });
    expect(spaceLabelParts(null, '_default')).toEqual({ title: 'Default Space' });
  });

  it('returns a default space with its own title like any other space', () => {
    expect(spaceLabelParts('Home', '_default')).toEqual({ title: 'Home', id: '_default' });
  });
});

describe('spaceOption', () => {
  it('turns a space into a dropdown option, with its ID as the hint', () => {
    expect(spaceOption({ id: 'design', title: 'Design' })).toEqual({ id: 'design', name: 'Design', hint: 'design' });
  });

  it('gives a space without its own title no hint', () => {
    expect(spaceOption({ id: 'design', title: 'design' })).toEqual({ id: 'design', name: 'design' });
    expect(spaceOption({ id: '_default', title: 'Default Space' })).toEqual({ id: '_default', name: 'Default Space' });
  });
});
