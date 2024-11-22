export default function WorkItems({ extensionInfo }) {
  return (
      <>
        <div id="app-header" className="app-header">
          Diff/merge of Polarion WorkItems
          <div className="extension-info">
            {extensionInfo}
          </div>
        </div>
      </>
  );
}
