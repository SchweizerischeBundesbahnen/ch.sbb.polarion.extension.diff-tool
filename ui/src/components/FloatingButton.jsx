import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'

/**
 * The round marker between the two sides of a work item pair.
 *
 * With a clickHandler it is the expand/collapse control of the pair, so it renders a real button:
 * focusable, and operated by Enter and Space without a key handler of our own.
 *
 * Without one it reports a state, "no differences" or "could not be compared". That is not a control
 * and must stay out of the tab order, so it renders an image with its meaning as text.
 */
export default function FloatingButton({fontAwesomeIcon, clickHandler, disabled, label, expanded}) {
  const style = {
    cursor: disabled ? "inherit" : "pointer",
    color: disabled ? "#bbb" : "inherit",
  };

  if (!clickHandler) {
    return (
        <div style={style} className="floating-button" role="img" aria-label={label}>
          <FontAwesomeIcon icon={fontAwesomeIcon} />
        </div>
    );
  }

  return (
      <button type="button" style={style} className="floating-button" onClick={clickHandler}
              aria-label={label} aria-expanded={expanded}>
        <FontAwesomeIcon icon={fontAwesomeIcon} />
      </button>
  );
}
