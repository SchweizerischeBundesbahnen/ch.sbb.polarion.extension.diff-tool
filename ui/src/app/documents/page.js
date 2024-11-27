'use client'

import ControlPane from "@/components/ControlPane";
import DocumentsDiff from "@/components/documents/DocumentsDiff";
import ExtensionInfo from "@/components/ExtensionInfo";

export default function DocumentsPage() {
  return <>
    <div id="app-header" className="app-header">
      <div className="app-title">
        Diff/merge of Polarion Documents
      </div>
      <ExtensionInfo />
    </div>
    <ControlPane/>
    <DocumentsDiff />
  </>;
}
