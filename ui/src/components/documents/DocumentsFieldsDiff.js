import {useContext, useEffect, useState} from "react";
import {useSearchParams} from "next/navigation";
import AppContext from "../AppContext";
import AppAlert from "@/components/AppAlert";
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
import MergeResultModal from "@/components/merge/MergeResultModal";
import CollectionHeader from "@/components/collections/CollectionHeader";
import DocumentProjectHeader from "@/components/documents/DocumentProjectHeader";

const REQUIRED_PARAMS = ['sourceProjectId', 'sourceSpaceId', 'sourceDocument', 'targetProjectId', 'targetSpaceId', 'targetDocument'];

export default function DocumentsFieldsDiff({ enclosingCollections }) {
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
    diffService.sendDocumentsFieldsDiffRequest(searchParams, context.state.compareEnumsById, context.state.compareOnlyMutualFields, loadingContext)
        .then((data)=> {
              setFieldsData(data);
              let diffs = data.pairedFields.filter(fieldDiff => fieldDiff.leftField.htmlDiff && fieldDiff.rightField.htmlDiff).map((fieldDiff, index) => {
                mergingContext.setPairSelected(index, fieldDiff.leftField.id, false);
                const leftName = fieldDiff.leftField.name;
                const rightName = fieldDiff.rightField.name;
                return {
                  id: fieldDiff.leftField.id,
                  name: !rightName || leftName === rightName ? leftName : !leftName ? rightName : (leftName + ' / ' + rightName),
                  oldValue: fieldDiff.leftField.htmlDiff || "",
                  newValue: fieldDiff.rightField.htmlDiff || "",
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

  const changeSelected = (fieldId) => {
    return (event) => {
      event.stopPropagation();
      let index = fieldsDiffs.map(d => d.id).indexOf(fieldId);
      mergingContext.setPairSelected(index, fieldId, !mergingContext.isIndexSelected(index));
    }
  };

  if (loadingContext.fieldsDiffLoading) return <Loading message="Loading fields diff" />;

  if (loadingContext.fieldsDiffLoadingError || !fieldsData || !fieldsData.leftDocument || !fieldsData.rightDocument) {
    return <AppAlert title="Error occurred loading diff data!"
                  message={loadingContext.fieldsDiffLoadingError && !loadingContext.fieldsDiffLoadingError.includes("<html")
                      ? loadingContext.fieldsDiffLoadingError
                      : "Data wasn't loaded, please contact system administrator to diagnose the problem"} />;
  }

  return <div id="diff-body" className={`doc-diff ${context.state.controlPaneExpanded ? "control-pane-expanded" : ""}`}>
    <div className={`header container-fluid g-0 sticky-top ${context.state.headerPinned ? "pinned" : ""}`}>
      <div className="row g-0">
        {enclosingCollections && <>
          <CollectionHeader collection={enclosingCollections.leftCollection}/>
          <CollectionHeader collection={enclosingCollections.rightCollection}/>
        </>}
        {!enclosingCollections && <>
          <DocumentProjectHeader document={fieldsData.leftDocument}/>
          <DocumentProjectHeader document={fieldsData.rightDocument}/>
        </>}
      </div>
      <div className="row g-0">
        <DocumentHeader document={fieldsData.leftDocument} />
        <DocumentHeader document={fieldsData.rightDocument} />
      </div>

      <ProgressBar loadingContext={loadingContext}/>
      <MergePane leftContext={fieldsData.leftDocument} rightContext={fieldsData.rightDocument} mergingContext={mergingContext} mergeCallback={mergeCallback} loadingContext={loadingContext}/>
    </div>

    <ErrorsOverlay loadingContext={loadingContext}/>
    <MergeInProgressOverlay mergeInProgress={mergeInProgress}/>

    <Modal title="Merge error" cancelButtonTitle="Close" visible={mergeErrorModalVisible} setVisible={setMergeErrorModalVisible} className="modal-md error">
      <p>{mergeError}</p>
    </Modal>

    <MergeResultModal visible={mergeReportModalVisible} visibilityCallback={setWarningModalVisibleAndReloadIfNeeded}
                      mergeNotAuthorizedWarning={mergeNotAuthorizedWarning} mergeReport={mergeReport}/>

    {fieldsDiffs && fieldsDiffs.map((diff, index) => (
        <div className="wi-diff row g-0" key={index} style={{position: 'relative'}}>
          <div className="merge-ticker">
            <div className="form-check">
              <input className="form-check-input" type="checkbox" checked={mergingContext.isIndexSelected(index)} onChange={changeSelected(diff.id)}/>
            </div>
          </div>
          <FieldsDiff fieldId={diff.id} fieldName={diff.name} oldValue={diff.oldValue} newValue={diff.newValue} issues={diff.issues}/>
        </div>
    ))}
  </div>;
}
