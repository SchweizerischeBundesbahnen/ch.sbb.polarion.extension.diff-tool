import Modal from "@/components/Modal";
import {useState} from "react";

export default function MergeResultModal({visible, visibilityCallback, mergeDeniedWarning, structuralChangesWarning, mergeNotAuthorizedWarning, mergeReport}) {

  const [mergeLogsVisible, setMergeLogsVisible] = useState(false);

  const onClose = () => {
    setMergeLogsVisible(false);
  };

  return (
      <Modal title="Merge Report" cancelButtonTitle="Close" visible={visible}
             setVisible={visibilityCallback} className="modal-xl" onClose={onClose}>
        <div style={{
          marginBottom: "10px"
        }}>
          {mergeDeniedWarning && <p>Merge was aborted because some structural changes were done in target document meanwhile.</p>}
          {mergeNotAuthorizedWarning && <p>You are not authorized to execute such merge request.</p>}
          {!mergeNotAuthorizedWarning && !mergeDeniedWarning &&
              <>
                <p>
                  Merge operation completed with following result:
                </p>
                <ul>
                  {mergeReport.created?.length > 0 && <li><strong>{mergeReport.created.length}</strong> items were created.</li>}
                  {mergeReport.copied?.length > 0 && <li><strong>{mergeReport.copied.length}</strong> items were copied.</li>}
                  {mergeReport.deleted?.length > 0 && <li><strong>{mergeReport.deleted.length}</strong> items were deleted.</li>}
                  {mergeReport.modified?.length > 0 && <li>Content of <strong>{mergeReport.modified.length}</strong> items were modified.</li>}
                  {mergeReport.moved?.length > 0 && <li><strong>{mergeReport.moved.length}</strong> items were moved (structural changes).</li>}
                  {mergeReport.moveFailed?.length > 0 && <li><strong>{mergeReport.moveFailed.length}</strong> items were not moved because target document
                    does not contain destination chapter. Please, merge first parent nodes of mentioned work items.</li>}
                  {mergeReport.conflicted?.length > 0 && <li><strong>{mergeReport.conflicted.length}</strong> items were not merged because of concurrent modifications.</li>}
                  {mergeReport.prohibited?.length > 0 && <li><strong>{mergeReport.prohibited.length}</strong> items were not merged because such operation is logically prohibited.</li>}
                  {mergeReport.creationFailed?.length > 0 && <li><strong>{mergeReport.creationFailed.length}</strong> items were not merged because work items referenced in source document
                    do not have counterparts in target project.</li>}
                  {mergeReport.detached?.length > 0 && <li><strong>{mergeReport.detached.length}</strong> items were moved out of documents.</li>}
                  {mergeReport.warnings?.length > 0 && <li><strong>{mergeReport.warnings.length}</strong> warnings.</li>}
                </ul>
                {mergeReport.logs && !mergeLogsVisible && <p><a href="#" onClick={() => setMergeLogsVisible(true)}>See full log</a></p>}
                {mergeReport.logs && mergeLogsVisible && <pre style={{
                  padding: "10px",
                  background: "#444",
                  color: "#eee",
                  overflow: "auto",
                  borderRadius: "5px",
                  fontSize: ".8em",
                  height: "400px",
                  textWrap: "wrap"
                }}>{mergeReport.logs}</pre>}
              </>
          }
        </div>
        <div style={{display: !mergeDeniedWarning && structuralChangesWarning ? 'block' : 'none'}}>
          <p>The target document has just undergone some structural changes, which mean that no more merge actions are allowed using the current state of the view.
            The page will automatically be reloaded to actualize documents state.</p>
        </div>
        <p style={{display: mergeDeniedWarning || (mergeReport && mergeReport.conflicted && mergeReport.conflicted.length > 0) ? 'block' : 'none'}}>Please, reload the page to actualize the state and try again.</p>
      </Modal>
  );
}
