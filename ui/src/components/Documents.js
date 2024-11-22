import ControlPane from "@/components/ControlPane";
import DocumentsDiff from "@/components/documents/DocumentsDiff";

export default function Documents({ extensionInfo }) {
  return (
      <>
        <div id="app-header" className="app-header">
          Diff/merge of Polarion Live Documents
          <div className="extension-info">
            {extensionInfo}
          </div>
        </div>
        <ControlPane/>
        <DocumentsDiff/>
      </>
  );
}
