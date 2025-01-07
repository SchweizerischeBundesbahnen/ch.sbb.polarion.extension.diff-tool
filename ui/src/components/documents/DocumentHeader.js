import PathPart from "@/components/PathPart";

export default function DocumentHeader({document}) {
  return <div className="col collapsed-border">
    <div className="path">
      <PathPart label="space" value={document.spaceName}/>
    </div>
    <div className="doc-title">
      {document.title}
      <span style={{paddingLeft: "1.2em", fontSize: ".65em", fontWeight: "300"}}>
        {document.revision && <span>rev. <span>{document.revision}</span></span>}
        {!document.revision && document.headRevision && <span>HEAD (rev. <span>{document.headRevision}</span>)</span>}
      </span>
    </div>
  </div>;
}
