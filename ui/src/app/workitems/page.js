'use client'

import ControlPane from "@/components/ControlPane";
import ExtensionInfo from "@/components/ExtensionInfo";
import WorkItemsDiff from "@/components/workitems/WorkItemsDiff";
import * as DiffTypes from "@/DiffTypes";

export default function WorkItemsPage() {
  return <>
    <div id="app-header" className="app-header">
      <div className="app-title">
        Diff/merge of Polarion WorkItems
      </div>
      <ExtensionInfo />
    </div>
    <ControlPane diff_type={DiffTypes.WORK_ITEMS_DIFF} />
    <WorkItemsDiff />
  </>;
}
