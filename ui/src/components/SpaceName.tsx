import { spaceLabelParts } from '../utils/spaceLabel';

interface Props {
  title?: string | null;
  id: string;
}

/**
 * A space by its title, followed by its ID in a muted style. The ID is not put in parentheses,
 * so a title with its own parentheses cannot be mistaken for the ID.
 */
export default function SpaceName({ title, id }: Readonly<Props>) {
  const parts = spaceLabelParts(title, id);
  return (
    <span className="space-name">
      {parts.title}
      {parts.id ? <span className="space-id">{parts.id}</span> : null}
    </span>
  );
}
