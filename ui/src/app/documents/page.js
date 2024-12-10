'use client'

import {useSearchParams} from "next/navigation";
import ControlPane from "@/components/ControlPane";
import DocumentsDiff from "@/components/documents/DocumentsDiff";
import DocumentsFieldsDiff from "@/components/documents/DocumentsFieldsDiff";
import ExtensionInfo from "@/components/ExtensionInfo";
import * as DiffTypes from "@/DiffTypes";

export default function DocumentsPage() {
  const searchParams = useSearchParams();

  const isCompareAsWorkitems = () => {
    return searchParams.get('compareAs') === 'Workitems';
  }

  return <>
    <div id="app-header" className="app-header">
      <div className="app-title">
        Diff/merge of Polarion Documents
      </div>
      <ExtensionInfo />
    </div>
    <ControlPane diff_type={isCompareAsWorkitems() ? DiffTypes.DOCUMENTS_DIFF : DiffTypes.DOCUMENTS_FIELDS_DIFF}/>
    {isCompareAsWorkitems() && <DocumentsDiff/>}
    {!isCompareAsWorkitems() && <DocumentsFieldsDiff/>}
  </>;
}
