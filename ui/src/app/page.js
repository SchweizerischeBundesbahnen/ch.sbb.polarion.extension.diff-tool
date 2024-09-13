'use client'

import {Suspense, useContext} from 'react';
import DocumentsDiff from "@/components/documents/DocumentsDiff";
import Loading from "@/components/Loading";
import ControlPane from "@/components/ControlPane";
import AppContext from "@/components/AppContext";

export default function IndexPage() {
  const context = useContext(AppContext);
  return (
      <main className="app">
        <div id="app-header" className="app-header">
          Diff/merge of Polarion Live Documents
          <div className="extension-info">
           {context.state.extensionInfo}
          </div>
        </div>
        <Suspense fallback={<Loading/>}>
          <ControlPane/>
          <DocumentsDiff/>
        </Suspense>
      </main>
  );
}
