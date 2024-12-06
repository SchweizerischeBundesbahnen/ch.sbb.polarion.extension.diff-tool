import {useContext, useEffect, useState} from "react";
import {useSearchParams} from "next/navigation";
import AppContext from "../AppContext";
import Error from "@/components/Error";
import FieldsDiff from "@/components/diff/FieldsDiff";
import Loading from "@/components/loading/Loading";
import DocumentHeader from "@/components/documents/DocumentHeader";
import ProgressBar from "@/components/loading/ProgressBar";
import ErrorsOverlay from "@/components/ErrorsOverlay";
import useLoadingContext from "@/components/loading/useLoadingContext";
import {useMergingContext} from "@/components/merge/useMergingContext";
import MergePane from "@/components/merge/MergePane";
import useDiffService from "@/services/useDiffService";
import Modal from "@/components/Modal";
import MergeInProgressOverlay from "@/components/merge/MergeInProgressOverlay";
import useImageUtils from "@/utils/useImageUtils";

const REQUIRED_PARAMS = ['sourceProjectId', 'sourceSpaceId', 'sourceDocument', 'targetProjectId', 'targetSpaceId', 'targetDocument'];

export default function DocumentsFieldsDiff() {
  const context = useContext(AppContext);
  const searchParams = useSearchParams();
  const diffService = useDiffService();

  const loadingContext = useLoadingContext();
  const mergingContext = useMergingContext();

  const [fieldsData, setFieldsData] = useState(null);
  const [fieldsDiffs, setFieldsDiffs] = useState(null);
  const [mergeInProgress, setMergeInProgress] = useState(false);
  const [mergeError, setMergeError] = useState("");
  const [mergeErrorModalVisible, setMergeErrorModalVisible] = useState(false);
  const [mergeReport, setMergeReport] = useState({});
  const [mergeNotAuthorizedWarning, setMergeNotAuthorizedWarning] = useState(false);  // means that merge to target document is not authorized for current user
  const [mergeReportModalVisible, setMergeReportModalVisible] = useState(false);
  const [mergeLogsVisible, setMergeLogsVisible] = useState(false);
  const imageUtils = useImageUtils();

  useEffect(() => {
    const missingParams = REQUIRED_PARAMS.filter(param => !searchParams.get(param));
    if (missingParams.length > 0) {
      loadingContext.pairsLoadingFinishedWithError(`Following parameters are missing: [${missingParams.join(", ")}]`);
      return;
    }

    loadDiff();

  }, [searchParams]);

  const loadDiff = () => {
    mergingContext.resetRegistry();
    diffService.getDocumentsFieldsDiff(searchParams, context.state.compareEnumsById, context.state.compareOnlyMutualFields, loadingContext)
        .then((data)=> {
              setFieldsData(data);
              let diffs = data.pairedFields.filter(fieldDiff => fieldDiff.leftField.htmlDiff && fieldDiff.rightField.htmlDiff).map((fieldDiff, index) => {
                mergingContext.setPairSelected(index, fieldDiff.leftField.id, false);
                const leftName = fieldDiff.leftField.name;
                const rightName = fieldDiff.rightField.name;
                return {
                  id: fieldDiff.leftField.id,
                  name: !rightName || leftName === rightName ? leftName : !leftName ? rightName : (leftName + ' / ' + rightName),
                  // TODO does fixImagesPathInDocumentWorkItem work for document field images???
                  oldValue: imageUtils.fixImagesPathInDocumentWorkItem(fieldDiff.leftField.htmlDiff || "", null, data.leftDocument),
                  newValue: imageUtils.fixImagesPathInDocumentWorkItem(fieldDiff.rightField.htmlDiff || "", null, data.rightDocument),
                  issues: fieldDiff.issues
                }
              });
              setFieldsDiffs(diffs);
              context.state.setDiffsExist(diffs.length > 0);
            }
        );
  };

  useEffect(() => {
    if (fieldsData) {
      mergingContext.resetSelection();
      loadDiff();
    }
  }, [context.state.compareEnumsById, context.state.compareOnlyMutualFields]); // Reload diff if some control pane settings changed

  useEffect(() => {
    // selectAllTrigger of 0 value is initial value which should be ignored
    if (mergingContext.selectAllTrigger > 0) {
      fieldsDiffs.map((f, index) => mergingContext.setPairSelected(index, f.id, mergingContext.selectAll));
    }
  }, [mergingContext.selectAllTrigger]);

  const mergeCallback = (direction) => {
    setMergeInProgress(true);
    diffService.sendDocumentsFieldsMergeRequest(searchParams, direction, loadingContext, mergingContext, fieldsData)
        .then((data) => {
          setMergeReport(data.mergeReport);
          setMergeNotAuthorizedWarning(!data.success && data.mergeNotAuthorized);
          setMergeLogsVisible(false);
          setMergeReportModalVisible(true);
        })
        .catch((error) => {
          console.log(error);
          setMergeError(error && (typeof error === 'string' || error instanceof String) && !error.includes("<html")
              ? error : "Error occurred merging selected fields, please contact system administrator to diagnose the problem");
          setMergeErrorModalVisible(true);
        }).finally(() => setMergeInProgress(false));
  };

  const setWarningModalVisibleAndReloadIfNeeded = (visible) => {
    setMergeReportModalVisible(visible);
    if (!visible) {
      loadDiff();
    }
  }

  if (loadingContext.fieldsDiffLoading) return <Loading message="Loading fields diff" />;

  if (loadingContext.fieldsDiffLoadingError || !fieldsData || !fieldsData.leftDocument || !fieldsData.rightDocument) {
    return <Error title="Error occurred loading diff data!"
                  message={loadingContext.fieldsDiffLoadingError && !loadingContext.fieldsDiffLoadingError.includes("<html")
                      ? loadingContext.fieldsDiffLoadingError
                      : "Data wasn't loaded, please contact system administrator to diagnose the problem"} />;
  }

  const changeSelected = (fieldId) => {
    return (event) => {
      event.stopPropagation();
      let index = fieldsDiffs.map(d => d.id).indexOf(fieldId);
      mergingContext.setPairSelected(index, fieldId, !mergingContext.isIndexSelected(index));
    }
  };

  return <div id="diff-body" className={`doc-diff ${context.state.controlPaneExpanded ? "control-pane-expanded" : ""}`}>
    <div className={`header container-fluid g-0 sticky-top ${context.state.headerPinned ? "pinned" : ""}`}>
      <div className="row g-0">
        <DocumentHeader document={fieldsData.leftDocument} />
        <DocumentHeader document={fieldsData.rightDocument} />
      </div>

      <ProgressBar loadingContext={loadingContext} />
      <MergePane leftContext={fieldsData.leftDocument} rightContext={fieldsData.rightDocument} mergingContext={mergingContext} mergeCallback={mergeCallback} loadingContext={loadingContext} />
    </div>

    <ErrorsOverlay loadingContext={loadingContext} />
    <MergeInProgressOverlay mergeInProgress={mergeInProgress} />

    <Modal title="Merge error" cancelButtonTitle="Close" visible={mergeErrorModalVisible} setVisible={setMergeErrorModalVisible} className="modal-md error">
      <p>{mergeError}</p>
    </Modal>
    <Modal title="Merge Report" cancelButtonTitle="Close" visible={mergeReportModalVisible}
           setVisible={setWarningModalVisibleAndReloadIfNeeded} className="modal-xl">
      <div style={{
        marginBottom: "10px"
      }}>
        {mergeNotAuthorizedWarning && <p>You are not authorized to execute such merge request.</p>}
        {!mergeNotAuthorizedWarning &&
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
    </Modal>
    {/* style={{position: 'relative', height: 'auto'}}*/}
    {fieldsDiffs && fieldsDiffs.map((diff, index) => (
        <div className="row g-0" key={index} style={{position: 'relative'}}>
          <div className="merge-ticker">
            <div className="form-check">
              <input className="form-check-input" type="checkbox" checked={mergingContext.isIndexSelected(index)} onChange={changeSelected(diff.id)}/>
            </div>
          </div>
          <FieldsDiff fieldId={diff.id} fieldName={diff.name} oldValue={diff.oldValue} newValue={diff.newValue} issues={diff.issues} fieldsDiff={true}/>
        </div>
    ))
    }

</div>
}
