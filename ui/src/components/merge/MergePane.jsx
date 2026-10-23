import {faArrowRightLong} from "@fortawesome/free-solid-svg-icons";
import {FontAwesomeIcon} from "@fortawesome/react-fontawesome";
import {SearchableSelect} from "@sbb-polarion/react-sbb-polarion";
import {useContext, useEffect, useState} from "react";
import Modal from "@/components/Modal";
import AppContext from "@/components/AppContext";
import {LEFT_TO_RIGHT} from "@/components/merge/useMergingContext";
import * as DiffTypes from "@/DiffTypes";
import * as DiffViewModes from "@/DiffViewModes";

export const LINK_ROLE_DIRECTION_DIRECT = "DIRECT";
export const LINK_ROLE_DIRECTION_REVERSE = "REVERSE";

const LINK_ROLE_DIRECTION_HINTS = {
  [LINK_ROLE_DIRECTION_DIRECT]: "Each created WorkItem links to its source WorkItem.",
  [LINK_ROLE_DIRECTION_REVERSE]: "Each source WorkItem links to the WorkItem created from it."
};

/** A pair with a source work item and no target one: merging it creates the target work item. */
const createsWorkItem = (pair) => typeof pair === 'object' && !!pair.leftWorkItem && !pair.rightWorkItem;

export default function MergePane({rightContext, diff_type, mergingContext, mergeCallback, loadingContext}) {
  const appContext = useContext(AppContext);

  const [display, setDisplay] = useState('none');
  const [modalVisible, setModalVisible] = useState(false);
  const [selectAllVisible, setSelectAllVisible] = useState(false);
  const [structuralChanges, setStructuralChanges] = useState(false);
  const [createdWorkItemsCount, setCreatedWorkItemsCount] = useState(0);
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

  const confirmMerge = () => {
    if (!mergingContext.documentContentDiffing) {
      const pairsToMerge = mergingContext.getSelectedValues();
      setStructuralChanges(containsStructuralChanges(pairsToMerge));
      // Only these diffs merge work items with a link role, so only they can create linked work items.
      const linksCreatedWorkItems = diff_type === DiffTypes.DOCUMENTS_DIFF || diff_type === DiffTypes.WORK_ITEMS_DIFF;
      setCreatedWorkItemsCount(linksCreatedWorkItems ? pairsToMerge.filter(createsWorkItem).length : 0);
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
    mergeCallback(LEFT_TO_RIGHT, linkRoleDirection);
  };

  const mergeDisabled = !rightContext || rightContext.revision || !rightContext.authorizedForMerge;
  const selectionCount = mergingContext.selectionCount;

  return (
      <>
        <div className="merge-pane" style={{ display: display }}>
          <div className="merge-pane-selection">
            <label className="select-all" style={{
              display: selectAllVisible ? "inline-flex" : "none"
            }}>
              <input className="form-check-input" type="checkbox"
                     ref={(el) => { if (el) { el.indeterminate = selectionCount > 0 && !mergingContext.selectAll; } }}
                     checked={mergingContext.selectAll} onChange={() => {
                mergingContext.setAndApplySelectAll(!mergingContext.selectAll);
              }}/>
              Select all
            </label>
            <span className="selection-count" aria-live="polite" data-testid="selection-count">
              {selectionCount > 0 ? `${selectionCount} selected` : ""}
            </span>
          </div>
          <span className="merge-pane-status">
            {(!rightContext || rightContext.revision) && <span>Target document not in HEAD, which locks merging into it</span>}
            {rightContext && !rightContext.authorizedForMerge && <span>You are not authorized to merge into target project</span>}
          </span>
          <div className="merge-button">
            <button className="btn btn-xs" disabled={mergeDisabled || selectionCount === 0} onClick={confirmMerge}
                    title="Merge selected items into the target document">
              Merge into target
              {/* Side by side, the button sits between the source and the target, and points into the target. */}
              {appContext.state.diffViewMode === DiffViewModes.SIDE_BY_SIDE && <FontAwesomeIcon icon={faArrowRightLong} />}
            </button>
          </div>
        </div>
        <Modal title="Merge confirmation" cancelButtonTitle="Cancel" actionButtonTitle="Merge" actionButtonHandler={merge}
               visible={modalVisible} setVisible={setModalVisible} className="modal-md" testId="merge-confirmation-modal">
          <p data-testid="merge-confirmation">Are you sure you want to merge selected items from source to target document?</p>
          {createdWorkItemsCount > 0
              && <div className="link-role-direction" data-testid="merge-confirmation-link-role-direction">
                <p>
                  {createdWorkItemsCount === 1
                      ? "1 selected WorkItem does not exist in the target yet. It will be created there and linked to its source WorkItem."
                      : `${createdWorkItemsCount} selected WorkItems do not exist in the target yet. They will be created there and linked to their source WorkItems.`}
                </p>
                <div className="select-set">
                  <label htmlFor="link-role-direction">Link role direction for created WorkItems:</label>
                  <SearchableSelect id="link-role-direction" value={linkRoleDirection} onChange={setLinkRoleDirection}
                                    searchable={false}
                                    options={[{id: LINK_ROLE_DIRECTION_DIRECT, name: "Direct"},
                                              {id: LINK_ROLE_DIRECTION_REVERSE, name: "Reverse"}]} />
                </div>
                <p className="link-role-direction-hint">{LINK_ROLE_DIRECTION_HINTS[linkRoleDirection]}</p>
              </div>
          }
          {structuralChanges
              && <p style={{fontWeight: 'bolder'}} data-testid="merge-confirmation-structural-changes-warning">Be aware that you have selected items to be merged which will lead to document&apos;s structural changes.
                After merge is finished, diffs view will automatically be reloaded to actualize documents structure.</p>
          }
        </Modal>
      </>
  );
}
