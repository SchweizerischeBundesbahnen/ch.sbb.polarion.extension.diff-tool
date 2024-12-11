'use client'

import ControlPane from "@/components/ControlPane";
import DocumentsDiff from "@/components/documents/DocumentsDiff";
import ExtensionInfo from "@/components/ExtensionInfo";
import * as DiffTypes from "@/DiffTypes";
import {COLLECTIONS_DIFF} from "@/DiffTypes";
import CollectionsDiff from "@/components/collections/CollectionsDiff";

export default function CollectionsPage() {
  return <>
    <div id="app-header" className="app-header">
      <div className="app-title">
        Diff/merge of Polarion Collections
      </div>
      <ExtensionInfo />
    </div>
    <ControlPane diff_type={DiffTypes.COLLECTIONS_DIFF} />
    <CollectionsDiff />
  </>;
}
