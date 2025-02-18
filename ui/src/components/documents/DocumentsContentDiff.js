import {useContext, useEffect, useState} from "react";
import AppContext from "@/components/AppContext";
import {useSearchParams} from "next/navigation";
import useDiffService from "@/services/useDiffService";
import useLoadingContext from "@/components/loading/useLoadingContext";
import CollectionHeader from "@/components/collections/CollectionHeader";
import DocumentProjectHeader from "@/components/documents/DocumentProjectHeader";
import DocumentHeader from "@/components/documents/DocumentHeader";
import MergePane from "@/components/merge/MergePane";
import ErrorsOverlay from "@/components/ErrorsOverlay";
import MergeInProgressOverlay from "@/components/merge/MergeInProgressOverlay";
import Modal from "@/components/Modal";
import MergeResultModal from "@/components/merge/MergeResultModal";
import {v4 as uuidv4} from "uuid";
import AppAlert from "@/components/AppAlert";
import {useMergingContext} from "@/components/merge/useMergingContext";
import Loading from "@/components/loading/Loading";
import ContentAnchorsDiff, {CONTENT_ABOVE, CONTENT_BELOW} from "@/components/documents/ContentAnchorsDiff";

const REQUIRED_PARAMS = ['sourceProjectId', 'sourceSpaceId', 'sourceDocument', 'targetProjectId', 'targetSpaceId', 'targetDocument'];

export default function DocumentsContentDiff({ enclosingCollections }) {
  const context = useContext(AppContext);
  const searchParams = useSearchParams();
  const diffService = useDiffService();

  const loadingContext = useLoadingContext();
  const mergingContext = useMergingContext();

  const [docsData, setDocsData] = useState(null);
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
      loadingContext.contentDiffLoadingFinishedWithError(`Following parameters are missing: [${missingParams.join(", ")}]`);
      return;
    }

    diffService.sendDocumentsContentDiffRequest(searchParams, uuidv4(), loadingContext)
        .then((data) => setDocsData(data));
  }, [searchParams]);

  useEffect(() => {
    context.state.setDiffsExist(docsData && docsData.pairedContentAnchors && docsData.pairedContentAnchors.filter(anchorsPair => containsDiff(anchorsPair)));
  }, [docsData]);

  useEffect(() => {
    // selectAllTrigger of 0 value is initial value which should be ignored
    if (mergingContext.selectAllTrigger > 0) {
      docsData.pairedContentAnchors.map((pair, index) => {
        if (containsDiffAbove(pair)) {
          console.log("diff above: ", pair);
          mergingContext.setPairSelected(index + CONTENT_ABOVE, pair, mergingContext.selectAll);
        }
        if (containsDiffBelow(pair)) {
          console.log("diff below: ", pair);
          mergingContext.setPairSelected(index + CONTENT_BELOW, pair, mergingContext.selectAll)
        }
      });
    }
  }, [mergingContext.selectAllTrigger]);

  const containsDiff = (anchorsPair) => {
    return containsDiffAbove(anchorsPair) || containsDiffBelow(anchorsPair);
  }

  const containsDiffAbove = (anchorsPair) => {
    return (anchorsPair.leftAnchor && anchorsPair.leftAnchor.diffAbove) || (anchorsPair.rightAnchor && anchorsPair.rightAnchor.diffAbove);
  }

  const containsDiffBelow = (anchorsPair) => {
    return (anchorsPair.leftAnchor && anchorsPair.leftAnchor.diffBelow) || (anchorsPair.rightAnchor && anchorsPair.rightAnchor.diffBelow);
  }

  if (loadingContext.contentDiffLoading) return <Loading message="Loading content diff" />;

  if (loadingContext.contentDiffLoadingError || !docsData || !docsData.leftDocument || !docsData.rightDocument) {
    return <AppAlert title="Error occurred loading diff data!"
                     message={loadingContext.contentDiffLoadingError && !loadingContext.contentDiffLoadingError.includes("<html")
                         ? loadingContext.contentDiffLoadingError
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
          <DocumentProjectHeader document={docsData.leftDocument}/>
          <DocumentProjectHeader document={docsData.rightDocument}/>
        </>}
      </div>
      <div className="row g-0">
        <DocumentHeader document={docsData.leftDocument}/>
        <DocumentHeader document={docsData.rightDocument}/>
      </div>

      <MergePane leftContext={docsData.leftDocument} rightContext={docsData.rightDocument} mergingContext={mergingContext} mergeCallback={() => {}} loadingContext={loadingContext}/>
    </div>

    <ErrorsOverlay loadingContext={loadingContext}/>
    <MergeInProgressOverlay mergeInProgress={mergeInProgress}/>

    <Modal title="Merge error" cancelButtonTitle="Close" visible={mergeErrorModalVisible} setVisible={setMergeErrorModalVisible} className="modal-md error">
      <p>{mergeError}</p>
    </Modal>

    <MergeResultModal visible={mergeReportModalVisible} visibilityCallback={setMergeReportModalVisible}
                      mergeDeniedWarning={mergeDeniedWarning} structuralChangesWarning={structuralChangesWarning} mergeNotAuthorizedWarning={mergeNotAuthorizedWarning} mergeReport={mergeReport}/>

    {docsData.pairedContentAnchors.map((pair, index) => {
      return <ContentAnchorsDiff key={index} anchorsPair={pair} currentIndex={index} mergingContext={mergingContext} />
    })}
  </div>;
}
