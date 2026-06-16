'use client'

import {useSearchParams} from "next/navigation";
import ControlPane from "@/components/ControlPane";
import DocumentsDiff from "@/components/documents/DocumentsDiff";
import DocumentsFieldsDiff from "@/components/documents/DocumentsFieldsDiff";
import DocumentsContentDiff from "@/components/documents/DocumentsContentDiff";
import ExtensionInfo from "@/components/ExtensionInfo";
import * as DiffTypes from "@/DiffTypes";
import {useMemo, useState} from "react";

export default function DocumentsPage() {
  const searchParams = useSearchParams();

  const compareAsWorkItems = useMemo(() => !searchParams.get('compareAs') || searchParams.get('compareAs') === 'Workitems', [searchParams]);
  const compareAsFields = useMemo(() => searchParams.get('compareAs') === 'Fields', [searchParams]);
  const compareAsContent = useMemo(() => searchParams.get('compareAs') === 'Content', [searchParams]);
  const branchedDocuments = useMemo(() => searchParams.get('branched') === "true", [searchParams]);

  return <>
    <div id="app-header" className="app-header">
      <div className="app-title">
        Diff/merge of Polarion {branchedDocuments ? "Branched " : ""}Documents ({compareAsWorkItems ? "WorkItems" : (compareAsFields ? "Fields" : "Content")})
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
