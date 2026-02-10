import {useContext, useEffect, useState} from "react";
import {useSearchParams} from "next/navigation";
import AppContext from "../AppContext";
import AppAlert from "@/components/AppAlert";
import Loading from "@/components/loading/Loading";
import WorkItemsPairDiff from "@/components/documents/workitems/WorkItemsPairDiff";
import DocumentHeader from "@/components/documents/DocumentHeader";
import ProgressBar from "@/components/loading/ProgressBar";
import {v4 as uuidv4} from 'uuid';
import ErrorsOverlay from "@/components/ErrorsOverlay";
import useLoadingContext from "@/components/loading/useLoadingContext";
import {useMergingContext} from "@/components/merge/useMergingContext";
import MergePane from "@/components/merge/MergePane";
import useDiffService from "@/services/useDiffService";
import Modal from "@/components/Modal";
import MergeInProgressOverlay from "@/components/merge/MergeInProgressOverlay";
import CollectionHeader from "@/components/collections/CollectionHeader";
import DocumentProjectHeader from "@/components/documents/DocumentProjectHeader";
import MergeResultModal from "@/components/merge/MergeResultModal";
import {DIFF_SIDES} from "@/components/diff/DiffLeaf";

const REQUIRED_PARAMS = ['sourceProjectId', 'sourceSpaceId', 'sourceDocument', 'targetProjectId', 'targetSpaceId', 'targetDocument', 'linkRole'];

export default function DocumentsDiff({ enclosingCollections }) {
  const context = useContext(AppContext);
  const searchParams = useSearchParams();
  const diffService = useDiffService();

  const loadingContext = useLoadingContext();
  const mergingContext = useMergingContext({});

  const [configCacheId, setConfigCacheId] = useState("");
  const [docsData, setDocsData] = useState(null);
  const [leftChaptersDiffMarkers, setLeftChaptersDiffMarkers] = useState(new Map());
  const [rightChaptersDiffMarkers, setRightChaptersDiffMarkers] = useState(new Map());
  const [mergeInProgress, setMergeInProgress] = useState(false);
  const [mergeError, setMergeError] = useState("");
  const [mergeErrorModalVisible, setMergeErrorModalVisible] = useState(false);
  const [mergeReport, setMergeReport] = useState({});
  const [mergeDeniedWarning, setMergeDeniedWarning] = useState(false);  // means that merge operation was denied because displayed state of documents is not last one
  const [mergeNotAuthorizedWarning, setMergeNotAuthorizedWarning] = useState(false);  // means that merge to target document is not authorized for current user
  const [structuralChangesWarning, setStructuralChangesWarning] = useState(false);  // means that documents have undergone structural changes as a result of merge operation
  const [mergeReportModalVisible, setMergeReportModalVisible] = useState(false);

  useEffect(() => {
    const missingParams = REQUIRED_PARAMS.filter(param => !searchParams.get(param));
    if (missingParams.length > 0) {
      loadingContext.pairsLoadingFinishedWithError(`Following parameters are missing: [${missingParams.join(", ")}]`);
      return;
    }

    const cacheId = uuidv4();
    setConfigCacheId(cacheId);

    diffService.sendDocumentsDiffRequest(searchParams, cacheId, loadingContext)
        .then((data) => setDocsData(data));
  }, [searchParams]);

  useEffect(() => {
    if (docsData && docsData.pairedWorkItems && docsData.pairedWorkItems.length > 0) {
      loadingContext.resetDiffsLoadingState(docsData.pairedWorkItems.slice());
      mergingContext.resetSelection();
      setLeftChaptersDiffMarkers(new Map());
      setRightChaptersDiffMarkers(new Map());
    }
  }, [context.state.counterpartWorkItemsDiffer, context.state.compareEnumsById, context.state.allowReferencedWorkItemMerge]); // Reload diff of all work item pairs if specified properties changed

  useEffect(() => {
    context.state.setSelectedItemsCount(mergingContext.selectionCount);
  }, [mergingContext.selectionCount]);

  const dataLoadedCallback = (index, error, diffExists, resetDiffMarker, leftChapter, rightChapter, leftWiId, rightWiId) => {
    diffExists && context.state.setDiffsExist(true);
    loadingContext.diffLoadingFinished(index, error);

    markDiffInChaptersTree(leftChapter, leftWiId, leftChaptersDiffMarkers, diffExists, resetDiffMarker);
    markDiffInChaptersTree(rightChapter, rightWiId, rightChaptersDiffMarkers, diffExists, resetDiffMarker);
  };

  const markDiffInChaptersTree = (chapter, wiId, chaptersDiffMarkers, diffExists, resetDiffMarker) => {
    if (chapter && wiId) {
      const chapterParts = chapter.split('.');
      if (chapterParts.length > 0) {
        let ch = '';
        for (let i = 0; i < chapterParts.length; i++) {
          ch += ((ch.length > 0 ? '.' : '') + chapterParts[i]);
          if (!chaptersDiffMarkers.get(ch)) {
            chaptersDiffMarkers.set(ch, []);
          }
          if (diffExists) {
            chaptersDiffMarkers.get(ch).push(wiId);
          } else if (resetDiffMarker) {
            chaptersDiffMarkers.set(ch, chaptersDiffMarkers.get(ch).filter(e => e !== wiId));
          }
        }
      }
    }
  };

  const mergeCallback = (direction) => {
    setMergeInProgress(true);
    diffService.sendDocumentsMergeRequest(searchParams, direction, configCacheId, loadingContext, mergingContext, docsData,
                                          context.state.allowReferencedWorkItemMerge, context.state.preserveComments,
                                          context.state.copyMissingDocumentAttachments, context.state.updateAttachmentReferences)
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

  const selectChildren = (selectedPair) => {
    let childrenSelected = false;
    if (moved(selectedPair)) {
      loadingContext.pairs.map((pair, index) => {
        if (fullPair(pair) && subordinate(selectedPair, pair)) {
          mergingContext.setPairSelected(index, pair, true);
          childrenSelected = true;
        }
      });
    }
    return childrenSelected;
  };

  const setMirroredPairSelected = (selectedPair, selected) => {
    if (selectedPair.leftWorkItem && selectedPair.leftWorkItem.movedOutlineNumber) {
      loadingContext.pairs.map((pair, index) => {
        if (pair.rightWorkItem && selectedPair.leftWorkItem.movedOutlineNumber === pair.rightWorkItem.outlineNumber) {
          mergingContext.setPairSelected(index, pair, selected);
        }
      });
    } else if (selectedPair.rightWorkItem && selectedPair.rightWorkItem.movedOutlineNumber) {
      loadingContext.pairs.map((pair, index) => {
        if (pair.leftWorkItem && selectedPair.rightWorkItem.movedOutlineNumber === pair.leftWorkItem.outlineNumber) {
          mergingContext.setPairSelected(index, pair, selected);
        }
      });
    }
  };

  const unSelectionAllowed = (pairToCheck) => {
    if (!fullPair(pairToCheck)) {
      return true; // Only full pairs to be checked
    }
    for (let pair of loadingContext.pairs) {
      // If next pair was moved and is selected...
      if (moved(pair) && mergingContext.isPairSelected(pair)) {
        // ... it's children can't be unselected
        if (subordinate(pair, pairToCheck)) {
          return false;
        }
      }
    }
    return true;
  };

  const fullPair = (pair) => {
    return pair.leftWorkItem && pair.rightWorkItem;
  };

  const moved = (pair) => {
    return (pair.leftWorkItem && pair.leftWorkItem.movedOutlineNumber) || (pair.rightWorkItem && pair.rightWorkItem.movedOutlineNumber);
  };

  const subordinate = (parentPair, pairToCheck) => {
    return (pairToCheck.leftWorkItem && parentPair.leftWorkItem
            && pairToCheck.leftWorkItem.outlineNumber !== parentPair.leftWorkItem.outlineNumber
            && pairToCheck.leftWorkItem.outlineNumber.startsWith(parentPair.leftWorkItem.outlineNumber))
        || (pairToCheck.rightWorkItem && parentPair.rightWorkItem
            && pairToCheck.rightWorkItem.outlineNumber !== parentPair.rightWorkItem.outlineNumber
            && pairToCheck.rightWorkItem.outlineNumber.startsWith(parentPair.rightWorkItem.outlineNumber))
  };

  const setWarningModalVisibleAndReloadIfNeeded = (visible) => {
    setMergeReportModalVisible(visible);
    if (!visible && structuralChangesWarning) {
      location.reload();
    }
  }

  if (loadingContext.pairsLoading) return <Loading message="Loading paired WorkItems" />;

  if (loadingContext.pairsLoadingError || !docsData || !docsData.leftDocument || !docsData.rightDocument || !docsData.pairedWorkItems) {
    return <AppAlert title="Error occurred loading diff data!"
                     message={loadingContext.pairsLoadingError && !loadingContext.pairsLoadingError.includes("<html")
                      ? loadingContext.pairsLoadingError
                      : "Data wasn't loaded, please contact system administrator to diagnose the problem"} />;
  }

  return <div id="diff-body" className={`doc-diff ${context.state.controlPaneExpanded ? "control-pane-expanded" : ""}`}>
    <div className={`header container-fluid g-0 sticky-top ${context.state.headerPinned ? "pinned" : ""}`}>

      <div className="row g-0">
        {enclosingCollections && <>
          <CollectionHeader collection={enclosingCollections.leftCollection} side={DIFF_SIDES.LEFT} />
          <CollectionHeader collection={enclosingCollections.rightCollection} side={DIFF_SIDES.RIGHT} />
        </>}
        {!enclosingCollections && <>
          <DocumentProjectHeader document={docsData.leftDocument} side={DIFF_SIDES.LEFT} />
          <DocumentProjectHeader document={docsData.rightDocument} side={DIFF_SIDES.RIGHT} />
        </>}
      </div>
      <div className="row g-0">
        <DocumentHeader document={docsData.leftDocument} side={DIFF_SIDES.LEFT} />
        <DocumentHeader document={docsData.rightDocument} side={DIFF_SIDES.RIGHT} />
      </div>

      <ProgressBar loadingContext={loadingContext} />
      <MergePane leftContext={docsData.leftDocument} rightContext={docsData.rightDocument} mergingContext={mergingContext} mergeCallback={mergeCallback} loadingContext={loadingContext} />
    </div>

    <ErrorsOverlay loadingContext={loadingContext} />
    <MergeInProgressOverlay mergeInProgress={mergeInProgress} />

    <Modal title="Merge error" cancelButtonTitle="Close" visible={mergeErrorModalVisible} setVisible={setMergeErrorModalVisible} className="modal-md error">
      <p>{mergeError}</p>
    </Modal>

    <MergeResultModal visible={mergeReportModalVisible} visibilityCallback={setWarningModalVisibleAndReloadIfNeeded}
                      mergeDeniedWarning={mergeDeniedWarning} structuralChangesWarning={structuralChangesWarning} mergeNotAuthorizedWarning={mergeNotAuthorizedWarning} mergeReport={mergeReport}/>

    {loadingContext.pairs.map((pair, index) => {
      return <WorkItemsPairDiff key={index} leftDocument={docsData.leftDocument} rightDocument={docsData.rightDocument}
                                workItemsPair={pair} pairedWorkItemsLinkRole={searchParams.get('linkRole')}
                                configCacheId={configCacheId} dataLoadedCallback={dataLoadedCallback}
                                leftChaptersDiffMarkers={leftChaptersDiffMarkers} rightChaptersDiffMarkers={rightChaptersDiffMarkers}
                                currentIndex={index} loadingContext={loadingContext} mergingContext={mergingContext}
                                unSelectionAllowedCallback={unSelectionAllowed} selectChildrenCallback={selectChildren}
                                setMirroredPairSelectedCallback={setMirroredPairSelected}
                                createdReportEntries={mergeReport.created || []} modifiedReportEntries={mergeReport.modified || []}/>;

    })}
  </div>;
}
