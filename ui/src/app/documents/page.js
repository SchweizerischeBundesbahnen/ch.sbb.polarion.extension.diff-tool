'use client'

import {useSearchParams} from "next/navigation";
import ControlPane from "@/components/ControlPane";
import DocumentsDiff from "@/components/documents/DocumentsDiff";
import DocumentsFieldsDiff from "@/components/documents/DocumentsFieldsDiff";
import DocumentsContentDiff from "@/components/documents/DocumentsContentDiff";
import ExtensionInfo from "@/components/ExtensionInfo";
import * as DiffTypes from "@/DiffTypes";
import {useState} from "react";

export default function DocumentsPage() {
  const searchParams = useSearchParams();

  const [compareAsWorkItems] = useState(!searchParams.get('compareAs') || searchParams.get('compareAs') === 'Workitems');
  const [compareAsFields] = useState(searchParams.get('compareAs') === 'Fields');
  const [compareAsContent] = useState(searchParams.get('compareAs') === 'Content');

  return <>
    <div id="app-header" className="app-header">
      <div className="app-title">
        Diff/merge of Polarion Documents ({compareAsWorkItems ? "WorkItems" : (compareAsFields ? "Fields" : "Content")})
      </div>
      <ExtensionInfo />
    </div>
    <ControlPane diff_type={compareAsWorkItems
        ? DiffTypes.DOCUMENTS_DIFF
        : (compareAsFields ? DiffTypes.DOCUMENTS_FIELDS_DIFF : DiffTypes.DOCUMENTS_CONTENT_DIFF)}/>
    {compareAsWorkItems && <DocumentsDiff/>}
    {compareAsFields && <DocumentsFieldsDiff/>}
    {compareAsContent && <DocumentsContentDiff />}
  </>;
}
