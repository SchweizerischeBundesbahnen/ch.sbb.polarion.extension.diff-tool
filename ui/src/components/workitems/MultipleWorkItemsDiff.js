import {useContext, useEffect, useState} from "react";
import AppContext from "@/components/AppContext";
import {useSearchParams} from "next/navigation";
import {v4 as uuidv4} from "uuid";
import ProjectHeader from "@/components/workitems/ProjectHeader";
import Loading from "@/components/Loading";
import Error from "@/components/Error";
import useLoadingContext from "@/components/useLoadingContext";
import useDiffService from "@/services/useDiffService";
import WorkItemsDiff from "@/components/workitems/WorkItemsDiff";

const REQUIRED_PARAMS = ['sourceProjectId', 'targetProjectId', 'linkRole', 'ids'];

export default function MultipleWorkItemsDiff() {
  const context = useContext(AppContext);
  const searchParams = useSearchParams();
  const diffService = useDiffService();

  const loadingContext = useLoadingContext();

  const [configCacheId, setConfigCacheId] = useState("");
  const [workItemsData, setWorkItemsData] = useState(null);


  useEffect(() => {
    const missingParams = REQUIRED_PARAMS.filter(param => !searchParams.get(param));
    if (missingParams.length > 0) {
      loadingContext.pairsLoadingFinishedWithError(`Following parameters are missing: [${missingParams.join(", ")}]`);
      return;
    }

    const cacheId = uuidv4();
    setConfigCacheId(cacheId);

    diffService.sendMultipleWorkItemsDiffRequest(searchParams, cacheId, loadingContext)
        .then((data) => setWorkItemsData(data));
  }, [searchParams]);

  if (loadingContext.pairsLoading) return <Loading message="Loading paired WorkItems" />;

  if (loadingContext.pairsLoadingError || !workItemsData || !workItemsData.leftProjectName || !workItemsData.rightProjectName /*|| !workItemsData.pairedWorkItems*/) {
    return <Error title="Error occurred loading diff data!"
                  message={loadingContext.pairsLoadingError && !loadingContext.pairsLoadingError.includes("<html")
                      ? loadingContext.pairsLoadingError
                      : "Data wasn't loaded, please contact system administrator to diagnose the problem"} />;
  }

  return <div id="diff-body" className={`doc-diff ${context.state.controlPaneExpanded ? "control-pane-expanded" : ""}`}>
    <div className={`header container-fluid g-0 sticky-top ${context.state.headerPinned ? "pinned" : ""}`}>
      <div className="row g-0">
        <ProjectHeader projectName={workItemsData.leftProjectName} />
        <ProjectHeader projectName={workItemsData.rightProjectName} />
      </div>
    </div>
    {workItemsData.pairedWorkItems.map((pair, index) => {
      return <WorkItemsDiff key={index} workItemsPair={pair} />
    })}
  </div>;
}
