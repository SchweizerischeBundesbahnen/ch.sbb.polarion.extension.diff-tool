import {faArrowRightLong} from "@fortawesome/free-solid-svg-icons";
import MergeButton from "@/components/merge/MergeButton";
import {useContext, useEffect, useState} from "react";
import Modal from "@/components/Modal";
import AppContext from "@/components/AppContext";
import {LEFT_TO_RIGHT} from "@/components/merge/useMergingContext";
import * as DiffTypes from "@/DiffTypes";

export const LINK_ROLE_DIRECTION_DIRECT = "DIRECT";
export const LINK_ROLE_DIRECTION_REVERSE = "REVERSE";

export default function MergePane({leftContext, rightContext, diff_type, mergingContext, mergeCallback, loadingContext}) {
  const appContext = useContext(AppContext);

  const [display, setDisplay] = useState('none');
  const [modalVisible, setModalVisible] = useState(false);
  const [selectAllVisible, setSelectAllVisible] = useState(false);
  const [mergeDirection, setMergeDirection] = useState(-1);
  const [structuralChanges, setStructuralChanges] = useState(false);
  const [linkRoleDirection, setLinkRoleDirection] = useState(LINK_ROLE_DIRECTION_DIRECT);

  useEffect(() => {
    if (loadingContext.diffsLoadingProgress === 100 && !loadingContext.diffsLoadingErrors) {
      setTimeout(() => setDisplay('flex'), 1000);
    } else {
      setDisplay('none');
    }
  }, [loadingContext.diffsLoadingProgress, loadingContext.diffsLoadingErrors]);

  useEffect(() => {
    appContext.state.setControlPaneAccessible(!modalVisible);
  }, [modalVisible]);

  useEffect(() => {
    setSelectAllVisible(appContext.state.diffsExist);
  }, [appContext.state.diffsExist]);

  const confirmMerge = (direction) => {
    setMergeDirection(direction);
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
    if (mergeDirection >= 0) {
      mergeCallback(mergeDirection, linkRoleDirection);
    }
  };

  const mergeDisabled = !rightContext || rightContext.revision || !rightContext.authorizedForMerge;

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
            {(!rightContext || rightContext.revision) && <span>Target document not in HEAD, which locks merging into it</span>}
            {rightContext && !rightContext.authorizedForMerge && <span>You are not authorized to merge into target project</span>}
            {!mergeDisabled && (diff_type === DiffTypes.DOCUMENTS_DIFF || diff_type === DiffTypes.WORK_ITEMS_DIFF) && <label className="link-role-direction-label">
              <span className="link-role-direction-text">Link role direction for created WorkItems:</span>
              <select className="form-select link-role-direction-select" value={linkRoleDirection}
                      onChange={(e) => setLinkRoleDirection(e.target.value)} title="Link role direction for created WorkItems">
                <option value={LINK_ROLE_DIRECTION_DIRECT}>Direct</option>
                <option value={LINK_ROLE_DIRECTION_REVERSE}>Reverse</option>
              </select>
            </label>}
            <MergeButton fontAwesomeIcon={faArrowRightLong} clickHandler={() => confirmMerge(LEFT_TO_RIGHT)}
                         style={{justifyContent: "right"}} disabled={mergeDisabled || mergingContext.selectionCount === 0} />
          </div>
          <div className="col collapsed-border merge-button-container">
          </div>
        </div>
        <Modal title="Merge confirmation" cancelButtonTitle="Cancel" actionButtonTitle="Merge" actionButtonHandler={merge}
               visible={modalVisible} setVisible={setModalVisible} className="modal-md" testId="merge-confirmation-modal">
          <p data-testid="merge-confirmation">Are you sure you want to merge selected items {mergeDirection === LEFT_TO_RIGHT ? "from source to target" : "from target to source"} document?</p>
          {structuralChanges
              && <p style={{fontWeight: 'bolder'}} data-testid="merge-confirmation-structural-changes-warning">Be aware that you have selected items to be merged which will lead to document&apos;s structural changes.
                After merge is finished, diffs view will automatically be reloaded to actualize documents structure.</p>
          }
        </Modal>
      </>
  );
}
