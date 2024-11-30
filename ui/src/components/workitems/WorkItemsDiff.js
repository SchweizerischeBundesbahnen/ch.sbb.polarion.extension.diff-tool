import {useContext, useEffect, useState} from "react";
import AppContext from "@/components/AppContext";
import {useSearchParams} from "next/navigation";
import {v4 as uuidv4} from "uuid";
import ProjectHeader from "@/components/workitems/ProjectHeader";
import Loading from "@/components/Loading";
import Error from "@/components/Error";
import useLoadingContext from "@/components/useLoadingContext";
import useDiffService from "@/services/useDiffService";
import WorkItemsPairDiff from "@/components/workitems/WorkItemsPairDiff";
import {useMergingContext, LEFT_TO_RIGHT, RIGHT_TO_LEFT} from "@/components/documents/useMergingContext";
import ProgressBar from "@/components/documents/ProgressBar";
import ErrorsOverlay from "@/components/documents/ErrorsOverlay";
import MergeInProgressOverlay from "@/components/documents/MergeInProgressOverlay";
import Modal from "@/components/Modal";
import MergePane from "@/components/documents/MergePane";

const REQUIRED_PARAMS = ['sourceProjectId', 'targetProjectId', 'linkRole', 'ids'];

export default function WorkItemsDiff() {
  const context = useContext(AppContext);
  const searchParams = useSearchParams();
  const diffService = useDiffService();

  const loadingContext = useLoadingContext();
  const mergingContext = useMergingContext();

  const [configCacheId, setConfigCacheId] = useState("");
  const [workItemsData, setWorkItemsData] = useState(null);
  const [mergeInProgress, setMergeInProgress] = useState(false);
  const [mergeError, setMergeError] = useState("");
  const [mergeErrorModalVisible, setMergeErrorModalVisible] = useState(false);
  const [mergeReport, setMergeReport] = useState({});
  const [mergeDeniedWarning, setMergeDeniedWarning] = useState(false);  // means that merge operation was denied because displayed state of documents is not last one
  const [mergeNotAuthorizedWarning, setMergeNotAuthorizedWarning] = useState(false);  // means that merge to target document is not authorized for current user
  const [structuralChangesWarning, setStructuralChangesWarning] = useState(false);  // means that documents have undergone structural changes as a result of merge operation
  const [mergeReportModalVisible, setMergeReportModalVisible] = useState(false);
  const [mergeLogsVisible, setMergeLogsVisible] = useState(false);

  const mergeCallback = (direction) => {
    if (direction === LEFT_TO_RIGHT) {
      console.log("Merging from left to right");
    } else if (direction === RIGHT_TO_LEFT) {
      console.log("Merging from right to left");
    }
  };

  useEffect(() => {
    const missingParams = REQUIRED_PARAMS.filter(param => !searchParams.get(param));
    if (missingParams.length > 0) {
      loadingContext.pairsLoadingFinishedWithError(`Following parameters are missing: [${missingParams.join(", ")}]`);
      return;
    }

    setConfigCacheId(uuidv4());

    diffService.sendFindWorkItemsPairsRequest(searchParams, loadingContext)
        .then((data) => setWorkItemsData(data));
  }, [searchParams]);

  const dataLoadedCallback = (index, error, diffExists) => {
    diffExists && context.state.setDiffsExist(true);
    loadingContext.diffLoadingFinished(index, error);
  };


  if (loadingContext.pairsLoading) return <Loading message="Loading paired WorkItems" />;

  if (loadingContext.pairsLoadingError || !workItemsData || !workItemsData.leftProject || !workItemsData.rightProject /*|| !workItemsData.pairedWorkItems*/) {
    return <Error title="Error occurred loading diff data!"
                  message={loadingContext.pairsLoadingError && !loadingContext.pairsLoadingError.includes("<html")
                      ? loadingContext.pairsLoadingError
                      : "Data wasn't loaded, please contact system administrator to diagnose the problem"} />;
  }

  return <div id="diff-body" className={`doc-diff ${context.state.controlPaneExpanded ? "control-pane-expanded" : ""}`}>
    <div className={`header container-fluid g-0 sticky-top ${context.state.headerPinned ? "pinned" : ""}`}>
      <div className="row g-0">
        <ProjectHeader project={workItemsData.leftProject} />
        <ProjectHeader project={workItemsData.rightProject} />
      </div>
      <ProgressBar loadingContext={loadingContext} />
      <MergePane leftContext={workItemsData.leftProject} rightContext={workItemsData.rightProject} mergingContext={mergingContext} mergeCallback={mergeCallback} loadingContext={loadingContext} />
    </div>

    <ErrorsOverlay loadingContext={loadingContext} />
    <MergeInProgressOverlay mergeInProgress={mergeInProgress} />

    <Modal title="Merge error" cancelButtonTitle="Close" visible={mergeErrorModalVisible} setVisible={setMergeErrorModalVisible} className="modal-md error">
      <p>{mergeError}</p>
    </Modal>
    <Modal title="Merge Report" cancelButtonTitle="Close" visible={mergeReportModalVisible}
           setVisible={setMergeReportModalVisible} className="modal-xl">
      <div style={{
        marginBottom: "10px"
      }}>
        {mergeDeniedWarning && <p>Merge was aborted because some structural changes were done in target document meanwhile.</p>}
        {mergeNotAuthorizedWarning && <p>You are not authorized to execute such merge request.</p>}
        {!mergeDeniedWarning && !mergeNotAuthorizedWarning &&
            <>
              <p>
                Merge operation completed with following result:
              </p>
              <ul>
                {mergeReport.created?.length > 0 && <li><strong>{mergeReport.created.length}</strong> items were created.</li>}
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
      <p style={{display: mergeDeniedWarning || (mergeReport.conflicted && mergeReport.conflicted.length > 0) ? 'block' : 'none'}}>Please, reload the page to actualize the state and try again.</p>
    </Modal>

    {workItemsData.pairedWorkItems.map((pair, index) => {
      return <WorkItemsPairDiff
        key={index}
        workItemsPair={pair}
        leftProject={workItemsData.leftProject}
        rightProject={workItemsData.rightProject}
        pairedWorkItemsLinkRole={searchParams.get('linkRole')} configName={searchParams.get('config')} configCacheId={configCacheId} dataLoadedCallback={dataLoadedCallback}
        currentIndex={index} loadingContext={loadingContext} mergingContext={mergingContext}
        createdReportEntries={mergeReport.created || []} modifiedReportEntries={mergeReport.modified || []} />
    })}
  </div>;
}
