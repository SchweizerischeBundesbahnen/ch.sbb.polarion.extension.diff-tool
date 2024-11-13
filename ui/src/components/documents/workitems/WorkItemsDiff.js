import DiffContent from "@/components/documents/workitems/DiffContent";
import {useContext, useEffect, useState} from "react";
import {WorkItemHeader, LEFT, RIGHT} from "@/components/documents/workitems/WorkItemHeader";
import FloatingButton from "@/components/FloatingButton";
import {faChevronDown, faChevronUp, faEquals, faQuestion} from "@fortawesome/free-solid-svg-icons";
import useRemote from "@/services/useRemote";
import AppContext from "@/components/AppContext";
import {useSearchParams} from "next/navigation";
import MergeTicker from "@/components/documents/workitems/MergeTicker";
import Modal from "@/components/Modal";
import useDiffService from "@/services/useDiffService";

const CODES_TO_RETRY = [500, 502, 503, 504]; // 500 - Internal Server Error, 502 - Bad Gateway, 503 - Service Unavailable, 504 - Gateway Timeout
const RETRY_MARKER = "RETRY";

export default function WorkItemsDiff({leftDocument, rightDocument, workItemsPair, pairedWorkItemsLinkRole, configCacheId, dataLoadedCallback,
                                       leftChaptersDiffMarkers, rightChaptersDiffMarkers, currentIndex, loadingContext, mergingContext,
                                       unSelectionAllowedCallback, selectChildrenCallback, setMirroredPairSelectedCallback, createdReportEntries, modifiedReportEntries}) {
  const context = useContext(AppContext);
  const searchParams = useSearchParams();
  const remote = useRemote();
  const diffService = useDiffService();
  const [asHeaderInDocument] = useState(workItemsPair.leftWorkItem ?
      workItemsPair.leftWorkItem.outlineNumber && !workItemsPair.leftWorkItem.outlineNumber.includes('-') :
      workItemsPair.rightWorkItem.outlineNumber && !workItemsPair.rightWorkItem.outlineNumber.includes('-'));
  const [leftChapter, setLeftChapter] = useState('');
  const [rightChapter, setRightChapter] = useState('');
  const [expanded, setExpanded] = useState(true);
  const [onHold, setOnHold] = useState(true); // By default, all pairs are on hold, not to start loading data for all of them at once
  const [loading, setLoading] = useState(true); // By default, the component is considered in "loading" state, even if it's in fact "on hold"
  const [diffData, setDiffData] = useState(null);
  const [error, setError] = useState(null);
  const [diffs, setDiffs] = useState([]);
  const [dataLoadedFired, setDataLoadedFired] = useState(false);
  const [selected, setSelected] = useState(mergingContext.isIndexSelected(currentIndex));
  const [childrenSelectionModalVisible, setChildrenSelectionModalVisible] = useState(false);

  useEffect(() => {
    if (onHold && loadingContext.pairLoadingAllowed(currentIndex)) {
      setOnHold(false);
    }
  }, [currentIndex, loadingContext.loadedDiffs, loadingContext.reloadMarker]);

  useEffect(() => {
    if (!loading) {
      setOnHold(true);
    }
  }, [loading]);

  useEffect(() => {
    if (onHold || !(workItemsPair.leftWorkItem || workItemsPair.rightWorkItem)) {
      return; // Do not load data if the pair is on hold or not at least one work item provided
    }

    const body = {
      leftProjectId: leftDocument.projectId,
      pairedWorkItemsLinkRole: pairedWorkItemsLinkRole,
      pairedWorkItemsDiffer: context.state.counterpartWorkItemsDiffer,
      compareEnumsById: context.state.compareEnumsById,
      configName: searchParams.get('config'),
      configCacheBucketId: configCacheId
    };
    if (!workItemsPair.leftWorkItem && createdReportEntries) {
      let newPairData = findPair(createdReportEntries, workItemsPair.rightWorkItem.id);
      if (newPairData) {
        workItemsPair.leftWorkItem = newPairData.leftWorkItem;
      }
    }
    if (workItemsPair.leftWorkItem) {
      body.leftWorkItem = {
        id: workItemsPair.leftWorkItem.id,
        documentProjectId: leftDocument.projectId,
        documentLocationPath: leftDocument.locationPath,
        documentRevision: leftDocument.revision
      }
      if (modifiedReportEntries) {
        let modifiedPairData = findPair(modifiedReportEntries, workItemsPair.leftWorkItem.id);
        if (modifiedPairData) {
          workItemsPair.leftWorkItem = modifiedPairData.leftWorkItem;
        }
      }
      if (workItemsPair.leftWorkItem.referenced) {
        body.leftWorkItem.projectId = workItemsPair.leftWorkItem.projectId;
        body.leftWorkItem.revision = workItemsPair.leftWorkItem.revision;
      }
    }
    if (!workItemsPair.rightWorkItem && createdReportEntries) {
      let newPairData = findPair(createdReportEntries, workItemsPair.leftWorkItem.id);
      if (newPairData) {
        workItemsPair.rightWorkItem = newPairData.rightWorkItem;
      }
    }
    if (workItemsPair.rightWorkItem) {
      body.rightWorkItem = {
        id: workItemsPair.rightWorkItem.id,
        documentProjectId: rightDocument.projectId,
        documentLocationPath: rightDocument.locationPath,
        documentRevision: rightDocument.revision
      }
      if (modifiedReportEntries) {
        let modifiedPairData = findPair(modifiedReportEntries, workItemsPair.rightWorkItem.id);
        if (modifiedPairData) {
          workItemsPair.rightWorkItem = modifiedPairData.rightWorkItem;
        }
      }
      if (workItemsPair.rightWorkItem.referenced) {
        body.rightWorkItem.projectId = workItemsPair.rightWorkItem.projectId;
        body.rightWorkItem.revision = workItemsPair.rightWorkItem.revision;
      }
    }
    requestDiff(body, 0);
  }, [onHold]);

  useEffect(() => {
    const newDiffs = [];
    diffData && diffData.fieldDiffs.forEach(fieldDiff => {
      newDiffs.push({
        id: fieldDiff.id,
        name: fieldDiff.name,
        oldValue: fixImagesPath(fieldDiff.diffLeft, workItemsPair.leftWorkItem, leftDocument),
        newValue: fixImagesPath(fieldDiff.diffRight, workItemsPair.rightWorkItem, rightDocument),
        issues: fieldDiff.issues
      });
    });
    setDiffs(newDiffs);
  }, [diffData]);

  useEffect(() => {
    setLeftChapter(getChapter(workItemsPair.leftWorkItem));
  }, [workItemsPair.leftWorkItem]);

  useEffect(() => {
    setRightChapter(getChapter(workItemsPair.rightWorkItem));
  }, [workItemsPair.rightWorkItem]);

  useEffect(() => {
    if (!dataLoadedFired && (diffData || error)) {
      const diffsExist = diffData && diffData.fieldDiffs && diffData.fieldDiffs.length > 0;
      const leftWiId = workItemsPair.leftWorkItem ? workItemsPair.leftWorkItem.id : null;
      const rightWiId = workItemsPair.rightWorkItem ? workItemsPair.rightWorkItem.id : null;
      dataLoadedCallback(currentIndex, error, diffsExist, selected, leftChapter, rightChapter, leftWiId, rightWiId);
      if (diffsExist) {
        mergingContext.setPairSelected(currentIndex, workItemsPair, selected); // Initialize pair in selection registry
      } else {
        mergingContext.resetRegistryEntry(currentIndex); // Reset pair in selection registry
      }
      setDataLoadedFired(true);
    }
  }, [diffData, error]);

  useEffect(() => {
    setSelected(mergingContext.isIndexSelected(currentIndex));
  }, [mergingContext.selectionRegistry]);

  useEffect(() => {
    // selectAllTrigger of 0 value is initial value which should be ignored
    if (mergingContext.selectAllTrigger > 0 && mergingContext.selectAll !== selected && diffService.diffsExist(workItemsPair, diffs)) {
      pairSelected(mergingContext.selectAll, true);
    }
  }, [mergingContext.selectAllTrigger]);

  const pairSelected = (selected, forceAndSuppressWarnings) => {
    if (workItemsPair) {
      if (selected || unSelectionAllowedCallback(workItemsPair) || forceAndSuppressWarnings) {
        mergingContext.setPairSelected(currentIndex, workItemsPair, selected);
        if (workItemsPair.leftWorkItem && workItemsPair.rightWorkItem) {
          // In case of moved items UI contains them twice, once to show moved sign for left work item, another time - for right work item.
          // This code makes their selection/un-selection in sync
          setMirroredPairSelectedCallback(workItemsPair, selected);
        }
        if (selected && workItemsPair.leftWorkItem && workItemsPair.rightWorkItem
            && (workItemsPair.leftWorkItem.movedOutlineNumber || workItemsPair.rightWorkItem.movedOutlineNumber)) {
          // Automatically select all children items of selected heading item
          if (selectChildrenCallback(workItemsPair) && !asHeaderInDocument && !forceAndSuppressWarnings) {
            setChildrenSelectionModalVisible(true); // Additionally inform user about automatically selected inline child items, as it's not obvious on UI
          }
        }
      }
    }
  };

  const findPair = (reportEntries, workItemId) => {
    const reportEntry = reportEntries.find(entry => {
      return (entry.workItemsPair.leftWorkItem && entry.workItemsPair.leftWorkItem.id === workItemId)
          || (entry.workItemsPair.rightWorkItem && entry.workItemsPair.rightWorkItem.id === workItemId);
    });
    return reportEntry && reportEntry.workItemsPair;
  };

  const requestDiff = (body, triesCount) => {
    setLoading(true);
    setDataLoadedFired(false);
    remote.sendRequest({
      method: "POST",
      url: `/diff/workitems`,
      body: JSON.stringify(body),
      contentType: "application/json"
    }).then(response => {
      if (response.status === 200) {
        return response.json();
      } else {
        if (CODES_TO_RETRY.includes(response.status) && triesCount < 3) {
          setTimeout(() => {
            requestDiff(body, triesCount + 1);
          }, triesCount * 2000 + 1000);
          throw Promise.resolve(RETRY_MARKER); // Marker for later code that we are still trying to obtain diff from server
        } else {
          throw response.json();
        }
      }
    }).then(data =>  {
      // ----- We need to reload title/outlineNumber information in case if they were modified by merge
      if (data.leftWorkItem) {
        workItemsPair.leftWorkItem.title = data.leftWorkItem.title;
        workItemsPair.leftWorkItem.outlineNumber = data.leftWorkItem.outlineNumber;
      }
      if (data.rightWorkItem) {
        workItemsPair.rightWorkItem.title = data.rightWorkItem.title;
        workItemsPair.rightWorkItem.outlineNumber = data.rightWorkItem.outlineNumber;
      }
      // -----
      setDiffData(data);
      setLoading(false);
    }).catch(errorResponse => {
      Promise.resolve(errorResponse).then((error) => {
        if (RETRY_MARKER !== error) {
          setError(error && error.message);
          setLoading(false);
        } else {
          // Retry caught, swallow
        }
      });
    });
  };

  const getChapter = (workItem) => {
    const outlineNumberParts = workItem && workItem.outlineNumber && workItem.outlineNumber.split('-');
    return outlineNumberParts && outlineNumberParts.length > 0 && outlineNumberParts[0];
  };

  const fixImagesPath = function (diffValue, workItem, document) {
    let res = diffValue;
    if (res.includes('<img src="workitemimg:')) {
      res = res.replaceAll('<img src="workitemimg:', `<img src="/polarion/wi-attachment/${document.projectId}/${workItem && workItem.id}/`)
    }
    if (diffValue.includes('<img src="attachment:')) {
      res = res.replaceAll('<img src="attachment:', `<img src="/polarion/module-attachment/${document.projectId}/${document.spaceId}/${document.id}/`)
    }
    return res;
  };

  const expandHandler = () => {
    setExpanded(!expanded);
  };

  const getReferencedClass = (workItemsPair) => {
    return [
      workItemsPair.leftWorkItem && workItemsPair.leftWorkItem.referenced ? 'referenced-left' : '',
      workItemsPair.rightWorkItem && workItemsPair.rightWorkItem.referenced ? 'referenced-right' : ''
    ].join(' ');
  };

  const isChapterVisible = (chapter, left) => {
    const markers = left ? leftChaptersDiffMarkers : rightChaptersDiffMarkers;
    return markers.get(chapter) && markers.get(chapter).length > 0
  };

  return (
      <>
        <div className={`wi-diff ${getReferencedClass(workItemsPair)} container-fluid g-0`} style={{
          display: (workItemsPair.leftWorkItem && workItemsPair.leftWorkItem.outlineNumber) || (workItemsPair.rightWorkItem && workItemsPair.rightWorkItem.outlineNumber) ? 'block' : 'none'
        }}>
          <div style={{
            backgroundColor: asHeaderInDocument ? "#eeeeee" : "#f6f6f6",
            display: isChapterVisible(leftChapter, true) || isChapterVisible(rightChapter, false) || !context.state.hideChaptersIfNoDifference || error ? 'flex' : 'none'
          }} className="header row g-0">
            <MergeTicker workItemsPair={workItemsPair} diffs={diffs} selected={selected} pairSelectedCallback={pairSelected} />

            <WorkItemHeader workItem={workItemsPair.leftWorkItem} asHeaderInDocument={asHeaderInDocument}
                            movedOutlineNumber={workItemsPair.rightWorkItem?.movedOutlineNumber}
                            moveDirection={workItemsPair.rightWorkItem?.moveDirection}
                            side={LEFT} selected={selected}/>

            {diffs && diffs.length > 0
                && (!workItemsPair.rightWorkItem || !workItemsPair.rightWorkItem.movedOutlineNumber)
                && <FloatingButton fontAwesomeIcon={expanded ? faChevronDown : faChevronUp} clickHandler={expandHandler}/>}
            {(!diffs || diffs.length === 0) && !error && <FloatingButton fontAwesomeIcon={faEquals} disabled={true}/>}
            {(!diffs || diffs.length === 0) && error && <FloatingButton fontAwesomeIcon={faQuestion} disabled={true}/>}

            <WorkItemHeader workItem={workItemsPair.rightWorkItem} asHeaderInDocument={asHeaderInDocument}
                            movedOutlineNumber={workItemsPair.leftWorkItem?.movedOutlineNumber}
                            moveDirection={workItemsPair.leftWorkItem?.moveDirection}
                            side={RIGHT} selected={selected}/>
          </div>
          {error && <div className="wi-error">Error occurred loading diff data: <span className="error-trace">{error}</span></div>}
          {loading && <div className="loader wi-loader"></div>}
          {!loading && !(workItemsPair.rightWorkItem && workItemsPair.rightWorkItem.movedOutlineNumber)
              && !(workItemsPair.leftWorkItem && workItemsPair.leftWorkItem.externalProjectWorkItem)
              && !(workItemsPair.rightWorkItem && workItemsPair.rightWorkItem.externalProjectWorkItem) && <DiffContent diffs={diffs} expanded={expanded}/>}
        </div>
        <Modal title="Information" cancelButtonTitle="OK" visible={childrenSelectionModalVisible}
               setVisible={setChildrenSelectionModalVisible} className="modal-md">
          <p>Be aware that selected item is automatically selected for move operation together with other items which structurally are subordinate to it.</p>
        </Modal>
      </>
  );

}
