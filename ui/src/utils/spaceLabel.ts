const DEFAULT_SPACE_ID = '_default';
const DEFAULT_SPACE_NAME = 'Default Space';

/** A space as `GET /projects/{id}/spaces` returns it. */
export interface SpaceInfo {
  id: string;
  title?: string | null;
}

/**
 * What is shown of a space: its title, and its ID when the ID adds something.
 * A space without its own title shows only its ID, the default space only "Default Space".
 */
export function spaceLabelParts(title: string | null | undefined, id: string): { title: string; id?: string } {
  const titled = Boolean(title?.trim()) && title !== id;
  if (id === DEFAULT_SPACE_ID && (!titled || title === DEFAULT_SPACE_NAME)) {
    return { title: DEFAULT_SPACE_NAME };
  }
  return titled ? { title: title!, id: id } : { title: id };
}

/**
 * A space as an option of a space dropdown: its title, with its ID as the hint the open list shows
 * small and italic at the right of the row.
 */
export function spaceOption(space: SpaceInfo): { id: string; name: string; hint?: string } {
  const parts = spaceLabelParts(space.title, space.id);
  return parts.id ? { id: space.id, name: parts.title, hint: parts.id } : { id: space.id, name: parts.title };
}
