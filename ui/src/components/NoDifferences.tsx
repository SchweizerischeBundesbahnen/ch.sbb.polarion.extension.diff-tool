interface Props {
  /** True once the diff is loaded completely and found no differences. */
  visible: boolean;
}

/**
 * The notice below the comparison header of a diff without differences. Without it the result area stays
 * empty, which reads like a diff that did not load. It is part of the page, so a PDF export shows it too.
 */
export default function NoDifferences({ visible }: Readonly<Props>) {
  if (!visible) {
    return null;
  }
  return (
    <div className="no-differences" role="status">
      <span>No differences</span>
    </div>
  );
}
