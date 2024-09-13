import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'

export default function FloatingButton({fontAwesomeIcon, clickHandler, disabled}) {
  return (
      <div style={{
        cursor: disabled ? "inherit" : "pointer",
        color: disabled ? "#bbb" : "inherit",
      }} className="floating-button" onClick={clickHandler}>
        <FontAwesomeIcon icon={fontAwesomeIcon} />
      </div>
  );
}