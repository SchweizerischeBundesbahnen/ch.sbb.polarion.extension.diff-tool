import {faArrowLeftLong, faArrowRightLong} from "@fortawesome/free-solid-svg-icons";
import MergeButton from "@/components/merge/MergeButton";
import {useContext, useEffect, useState} from "react";
import Modal from "@/components/Modal";
import AppContext from "@/components/AppContext";
import {LEFT_TO_RIGHT, RIGHT_TO_LEFT} from "@/components/merge/useMergingContext";

export default function MergePane({leftContext, rightContext, mergingContext, mergeCallback, loadingContext}) {
  const context = useContext(AppContext);

  const [display, setDisplay] = useState('none');
  const [modalVisible, setModalVisible] = useState(false);
  const [selectAllVisible, setSelectAllVisible] = useState(false);
  const [direction, setDirection] = useState(-1);
  const [structuralChanges, setStructuralChanges] = useState(false);

  useEffect(() => {
    if (loadingContext.diffsLoadingProgress === 100 && !loadingContext.diffsLoadingErrors) {
      setTimeout(() => setDisplay('flex'), 1000);
    } else {
      setDisplay('none');
    }
  }, [loadingContext.diffsLoadingProgress, loadingContext.diffsLoadingErrors]);

  useEffect(() => {
    context.state.setControlPaneAccessible(!modalVisible);
  }, [modalVisible]);

  useEffect(() => {
    setSelectAllVisible(context.state.diffsExist);
  }, [context.state.diffsExist]);

  const confirmMerge = (direction) => {
    setDirection(direction);
    if (!mergingContext.documentContentDiffing) {
      const pairsToMerge = mergingContext.getSelectedValues();
      setStructuralChanges(containsStructuralChanges(pairsToMerge));
    }
    setModalVisible(true);
  };

  const containsStructuralChanges = (pairsToMerge) => {
    for (const pair of pairsToMerge) {
      // In case of comparing documents  fields we keep fieldIds in the 'pair' variable
      if (typeof pair === 'string') {
        return false;
      }
      // If left/right item is missing then it's creation/deletion action. If 'movedOutlineNumber' filled - move action.
      if (!pair.leftWorkItem || !pair.rightWorkItem || pair.leftWorkItem.movedOutlineNumber || pair.rightWorkItem.movedOutlineNumber) {
        return true;
      }
    }
    return false;
  };

  const merge = () => {
    if (direction >= 0) {
      mergeCallback(direction);
    }
  };

  return (
      <>
        <div className="row g-0 merge-pane" style={{ display: display }}>
          <div className="col collapsed-border merge-button-container" style={{justifyContent: "right"}}>
            <div style={{
              flexGrow: "1",
              textWrap: "nowrap"
            }}>
              <label className="select-all" style={{
                display: selectAllVisible ? "inline-block" : "none"
              }}>
                <input className="form-check-input" type="checkbox" checked={mergingContext.selectAll} onChange={() => {
                  mergingContext.setAndApplySelectAll(!mergingContext.selectAll);
                }}/>
                select all
              </label>
            </div>
            {!rightContext || rightContext.revision && <span>Target document not in HEAD, which locks merging into it</span>}
            {rightContext && !rightContext.authorizedForMerge && <span>You are not authorized to merge into target project</span>}
            <MergeButton fontAwesomeIcon={faArrowRightLong} clickHandler={() => confirmMerge(LEFT_TO_RIGHT)}
                         style={{justifyContent: "right"}} disabled={!rightContext || rightContext.revision || !rightContext.authorizedForMerge || mergingContext.selectionCount === 0}/>
          </div>
          <div className="col collapsed-border merge-button-container">
            <MergeButton fontAwesomeIcon={faArrowLeftLong} clickHandler={() => confirmMerge(RIGHT_TO_LEFT)}
                         style={{justifyContent: "left"}} disabled={!leftContext || leftContext.revision || !leftContext.authorizedForMerge || mergingContext.selectionCount === 0}/>
            {!leftContext || leftContext.revision && <span>Source document not in HEAD, which locks merging into it</span>}
            {leftContext && !leftContext.authorizedForMerge && <span>You are not authorized to merge into source project</span>}
          </div>
        </div>
        <Modal title="Merge confirmation" cancelButtonTitle="Cancel" actionButtonTitle="Merge" actionButtonHandler={merge}
               visible={modalVisible} setVisible={setModalVisible} className="modal-md" testId="merge-confirmation-modal">
          <p>Are you sure you want to merge selected items {direction === LEFT_TO_RIGHT ? "from source to target" : "from target to source"} document?</p>
          {structuralChanges
              && <p style={{fontWeight: 'bolder'}}>Be aware that you have selected items to be merged which will lead to document&apos;s structural changes.
                After merge is finished, diffs view will automatically be reloaded to actualize documents structure.</p>
          }
        </Modal>
      </>
  );
}
