import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'

export default function MergeButton({fontAwesomeIcon, clickHandler, style, disabled, title}) {
  return (
      <div style={{
        opacity: disabled ? '.5' : '1',
        ...style
      }} className="merge-button" onClick={() => !disabled && clickHandler()}>
        <button className="btn btn-secondary btn-xs" disabled={disabled} title={title}>
          <FontAwesomeIcon icon={fontAwesomeIcon} />
        </button>
      </div>
  );
}