'use client'

import ControlPane from "@/components/ControlPane";
import DocumentsDiff from "@/components/documents/DocumentsDiff";
import ExtensionInfo from "@/components/ExtensionInfo";
import * as DiffTypes from "@/DiffTypes";

export default function DocumentsPage() {
  return <>
    <div id="app-header" className="app-header">
      <div className="app-title">
        Diff/merge of Polarion Documents
      </div>
      <ExtensionInfo />
    </div>
    <ControlPane diff_type={DiffTypes.DOCUMENTS_DIFF} />
    <DocumentsDiff />
  </>;
}
