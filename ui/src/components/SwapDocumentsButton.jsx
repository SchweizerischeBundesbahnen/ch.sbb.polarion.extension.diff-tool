import {FontAwesomeIcon} from "@fortawesome/react-fontawesome";
import {faRightLeft} from "@fortawesome/free-solid-svg-icons";

export default function SwapDocumentsButton({clearedSelection, onClick}) {
  return (
      <button className="btn btn-secondary btn-xs swap-button" onClick={onClick}
              title={`Swap source and target documents. Be aware that ${clearedSelection} selection will be cleared by this action.`}
              aria-label="Swap documents">
        <FontAwesomeIcon icon={faRightLeft} />
      </button>
  );
}
