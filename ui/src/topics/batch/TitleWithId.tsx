import type { ReactNode } from 'react';

interface Props {
  title?: string | null;
  id: string;
  /** How the title is shown, a link for example; the title itself by default. */
  children?: ReactNode;
}

/**
 * A title with its ID below it, smaller and greyed out. When both are the same, or there is no title,
 * only one of them is shown.
 */
export default function TitleWithId({ title, id, children }: Readonly<Props>) {
  return (
    <span className="title-with-id">
      {children ?? (title || id)}
      {title && title !== id ? <div className="secondary-id">{id}</div> : null}
    </span>
  );
}
