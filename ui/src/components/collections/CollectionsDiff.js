import {useContext} from "react";
import AppContext from "@/components/AppContext";
import {useSearchParams} from "next/navigation";

const REQUIRED_PARAMS = ['sourceProjectId', 'sourceCollectionId', 'targetProjectId', 'targetCollectionId'];

export default function CollectionsDiff() {
  const context = useContext(AppContext);
  const searchParams = useSearchParams();

  return <div id="diff-body" className={`doc-diff ${context.state.controlPaneExpanded ? "control-pane-expanded" : ""}`}>
    <div className={`header container-fluid g-0 sticky-top ${context.state.headerPinned ? "pinned" : ""}`}>
    </div>
  </div>
}
