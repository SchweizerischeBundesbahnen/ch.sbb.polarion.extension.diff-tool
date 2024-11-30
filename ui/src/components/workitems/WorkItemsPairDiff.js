import MergeTicker from "@/components/documents/workitems/MergeTicker";
import {LEFT, RIGHT, WorkItemHeader} from "@/components/documents/workitems/WorkItemHeader";
import {useContext, useEffect, useState} from "react";
import useImageUtils from "@/utils/useImageUtils";
import DiffContent from "@/components/documents/workitems/DiffContent";
import useRemote from "@/services/useRemote";
import AppContext from "@/components/AppContext";
import FloatingButton from "@/components/FloatingButton";
import {faChevronDown, faChevronUp, faEquals, faQuestion} from "@fortawesome/free-solid-svg-icons";

export default function WorkItemsPairDiff({ workItemsPair, leftProject, rightProject, pairedWorkItemsLinkRole, configName, configCacheId, dataLoadedCallback,
                                            currentIndex, loadingContext, mergingContext, createdReportEntries, modifiedReportEntries}) {
  const CODES_TO_RETRY = [500, 502, 503, 504]; // 500 - Internal Server Error, 502 - Bad Gateway, 503 - Service Unavailable, 504 - Gateway Timeout
  const RETRY_MARKER = "RETRY";

  const context = useContext(AppContext);
  const remote = useRemote();
  const imageUtils = useImageUtils();

  const [expanded, setExpanded] = useState(true);
  const [onHold, setOnHold] = useState(true); // By default, all pairs are on hold, not to start loading data for all of them at once
  const [loading, setLoading] = useState(true); // By default, the component is considered in "loading" state, even if it's in fact "on hold"
  const [error, setError] = useState(null);
  const [diffData, setDiffData] = useState(null);
  const [diffs, setDiffs] = useState([]);
  const [dataLoadedFired, setDataLoadedFired] = useState(false);
  const [selected, setSelected] = useState(mergingContext.isIndexSelected(currentIndex));

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
      leftProjectId: leftProject.id,
      pairedWorkItemsLinkRole: pairedWorkItemsLinkRole,
      pairedWorkItemsDiffer: context.state.counterpartWorkItemsDiffer,
      compareEnumsById: context.state.compareEnumsById,
      configName: configName,
      configCacheBucketId: configCacheId
    };
    if (workItemsPair.leftWorkItem) {
      body.leftWorkItem = {
        id: workItemsPair.leftWorkItem.id,
        projectId: leftProject.id
      }
      if (modifiedReportEntries) {
        let modifiedPairData = findPair(modifiedReportEntries, workItemsPair.leftWorkItem.id);
        if (modifiedPairData) {
          workItemsPair.leftWorkItem = modifiedPairData.leftWorkItem;
        }
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
        projectId: rightProject.id
      }
      if (modifiedReportEntries) {
        let modifiedPairData = findPair(modifiedReportEntries, workItemsPair.rightWorkItem.id);
        if (modifiedPairData) {
          workItemsPair.rightWorkItem = modifiedPairData.rightWorkItem;
        }
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
        oldValue: imageUtils.fixImagesPathInDetachedWorkItem(fieldDiff.diffLeft, workItemsPair.leftWorkItem),
        newValue: imageUtils.fixImagesPathInDetachedWorkItem(fieldDiff.diffRight, workItemsPair.rightWorkItem),
        issues: fieldDiff.issues
      });
    });
    setDiffs(newDiffs);
  }, [diffData]);

  useEffect(() => {
    if (!dataLoadedFired && (diffData || error)) {
      const diffsExist = diffData && diffData.fieldDiffs && diffData.fieldDiffs.length > 0;
      dataLoadedCallback(currentIndex, error, diffsExist);
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

  const pairSelected = (selected, force) => {
    if (workItemsPair) {
      if (selected || force) {
        mergingContext.setPairSelected(currentIndex, workItemsPair, selected);
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

  const expandHandler = () => {
    setExpanded(!expanded);
  };

  const requestDiff = (body, triesCount) => {
    setLoading(true);
    setDataLoadedFired(false);
    remote.sendRequest({
      method: "POST",
      url: `/diff/detached-workitems`,
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
      }
      if (data.rightWorkItem) {
        workItemsPair.rightWorkItem.title = data.rightWorkItem.title;
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

  return (
      <>
        <div className={`wi-diff container-fluid g-0`}>
          <div style={{
            backgroundColor: "#f6f6f6",
            display: 'flex'
          }} className="header row g-0">
            <MergeTicker workItemsPair={workItemsPair} diffs={{}} selected={false} pairSelectedCallback={() => {}} />
            <WorkItemHeader workItem={workItemsPair.leftWorkItem} side={LEFT} selected={false}/>

            {diffs && diffs.length > 0
                && (!workItemsPair.rightWorkItem || !workItemsPair.rightWorkItem.movedOutlineNumber)
                && <FloatingButton fontAwesomeIcon={expanded ? faChevronDown : faChevronUp} clickHandler={expandHandler}/>}
            {(!diffs || diffs.length === 0) && !error && <FloatingButton fontAwesomeIcon={faEquals} disabled={true}/>}
            {(!diffs || diffs.length === 0) && error && <FloatingButton fontAwesomeIcon={faQuestion} disabled={true}/>}

            <WorkItemHeader workItem={workItemsPair.rightWorkItem} side={RIGHT} selected={false}/>
          </div>
          {error && <div className="wi-error">Error occurred loading diff data: <span className="error-trace">{error}</span></div>}
          {loading && <div className="loader wi-loader"></div>}
          {!loading && <DiffContent diffs={diffs} expanded={expanded}/>}
        </div>
      </>
  )
}
