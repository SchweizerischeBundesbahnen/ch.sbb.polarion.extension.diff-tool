import {useContext, useEffect, useState} from "react";
import AppContext from "@/components/AppContext";
import {useSearchParams} from "next/navigation";
import {v4 as uuidv4} from "uuid";
import ProjectHeader from "@/components/workitems/ProjectHeader";
import Loading from "@/components/loading/Loading";
import Error from "@/components/Error";
import useLoadingContext from "@/components/loading/useLoadingContext";
import useDiffService from "@/services/useDiffService";
import WorkItemsPairDiff from "@/components/workitems/WorkItemsPairDiff";
import {useMergingContext, LEFT_TO_RIGHT, RIGHT_TO_LEFT} from "@/components/merge/useMergingContext";
import ProgressBar from "@/components/loading/ProgressBar";
import ErrorsOverlay from "@/components/ErrorsOverlay";
import MergeInProgressOverlay from "@/components/merge/MergeInProgressOverlay";
import Modal from "@/components/Modal";
import MergePane from "@/components/merge/MergePane";
import MergeResultModal from "@/components/merge/MergeResultModal";

const REQUIRED_PARAMS = ['sourceProjectId', 'targetProjectId', 'linkRole', 'ids'];

export default function WorkItemsDiff() {
  const context = useContext(AppContext);
  const searchParams = useSearchParams();
  const diffService = useDiffService();

  const loadingContext = useLoadingContext();
  const mergingContext = useMergingContext();

  const [configCacheId, setConfigCacheId] = useState("");
  const [workItemsData, setWorkItemsData] = useState(null);
  const [redundancyModalVisible, setRedundancyModalVisible] = useState(false);
  const [mergeInProgress, setMergeInProgress] = useState(false);
  const [mergeError, setMergeError] = useState("");
  const [mergeErrorModalVisible, setMergeErrorModalVisible] = useState(false);
  const [mergeReport, setMergeReport] = useState({});
  const [mergeDeniedWarning, setMergeDeniedWarning] = useState(false);  // means that merge operation was denied because displayed state of documents is not last one
  const [mergeNotAuthorizedWarning, setMergeNotAuthorizedWarning] = useState(false);  // means that merge to target document is not authorized for current user
  const [structuralChangesWarning, setStructuralChangesWarning] = useState(false);  // means that documents have undergone structural changes as a result of merge operation
  const [mergeReportModalVisible, setMergeReportModalVisible] = useState(false);

  const mergeCallback = (direction) => {
    setMergeInProgress(true);
    diffService.sendWorkItemsMergeRequest(searchParams, direction, configCacheId, loadingContext, mergingContext)
        .then((data) => {
          setMergeReport(data.mergeReport);
          setMergeDeniedWarning(!data.success && data.targetModuleHasStructuralChanges);
          setMergeNotAuthorizedWarning(!data.success && data.mergeNotAuthorized);
          setStructuralChangesWarning(data.success && data.targetModuleHasStructuralChanges);
          setMergeReportModalVisible(true);
        })
        .catch((error) => {
          console.log(error);
          setMergeError(error && (typeof error === 'string' || error instanceof String) && !error.includes("<html")
              ? error : "Error occurred merging selected work items, please contact system administrator to diagnose the problem");
          setMergeErrorModalVisible(true);
        }).finally(() => setMergeInProgress(false));
  };

  useEffect(() => {
    const missingParams = REQUIRED_PARAMS.filter(param => !searchParams.get(param));
    if (missingParams.length > 0) {
      loadingContext.pairsLoadingFinishedWithError(`Following parameters are missing: [${missingParams.join(", ")}]`);
      return;
    }

    setConfigCacheId(uuidv4());

    diffService.sendFindWorkItemsPairsRequest(searchParams, loadingContext)
        .then((data) => {
          setWorkItemsData(data);
          setRedundancyModalVisible(data.leftWorkItemIdsWithRedundancy && data.leftWorkItemIdsWithRedundancy.length > 0);
        });
  }, [searchParams]);

  useEffect(() => {
    if (workItemsData && workItemsData.pairedWorkItems && workItemsData.pairedWorkItems.length > 0) {
      loadingContext.resetDiffsLoadingState(workItemsData.pairedWorkItems.slice());
      mergingContext.resetSelection();
    }
  }, [context.state.counterpartWorkItemsDiffer, context.state.compareEnumsById]); // Reload diff of all work item pairs if specified properties changed

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

    <Modal title="Redundant counterpart WorkItems" cancelButtonTitle="Close" visible={redundancyModalVisible} setVisible={setRedundancyModalVisible} className="modal-md">
      <p>Following WorkItems in source project have multiple counterpart WorkItems in target project by selected link role, and therefor single counterpart can not be chosen:</p>
      <ul>
        {workItemsData.leftWorkItemIdsWithRedundancy.map((id, index) => {
          return <li key={index}>{id}</li>
        })}
      </ul>
    </Modal>
    <Modal title="Merge error" cancelButtonTitle="Close" visible={mergeErrorModalVisible} setVisible={setMergeErrorModalVisible} className="modal-md error">
      <p>{mergeError}</p>
    </Modal>

    <MergeResultModal visible={mergeReportModalVisible} visibilityCallback={setMergeReportModalVisible}
                      mergeDeniedWarning={mergeDeniedWarning} structuralChangesWarning={structuralChangesWarning} mergeNotAuthorizedWarning={mergeNotAuthorizedWarning} mergeReport={mergeReport}/>

    {workItemsData.pairedWorkItems.map((pair, index) => {
      return <WorkItemsPairDiff key={index} workItemsPair={pair} leftProject={workItemsData.leftProject} rightProject={workItemsData.rightProject}
        pairedWorkItemsLinkRole={searchParams.get('linkRole')} configName={searchParams.get('config')} configCacheId={configCacheId} dataLoadedCallback={dataLoadedCallback}
        currentIndex={index} loadingContext={loadingContext} mergingContext={mergingContext}
        createdReportEntries={mergeReport.created || []} modifiedReportEntries={mergeReport.modified || []} />
    })}
  </div>;
}
