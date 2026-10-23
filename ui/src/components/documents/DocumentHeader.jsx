import PathPart from "@/components/PathPart";

export function DocumentSpaceHeader({document, side}) {
  return <div className="path" data-testid={`${side}-space`}>
    <PathPart label="space" value={document.spaceName}/>
  </div>;
}

export function DocumentTitleHeader({document, side}) {
  return <div className="doc-title" data-testid={`${side}-doc-title`}>
    {document.title}
    <span className="doc-revision">
      {document.revision && <span>rev. <span>{document.revision}</span></span>}
      {!document.revision && document.headRevision && <span>HEAD (rev. <span>{document.headRevision}</span>)</span>}
    </span>
  </div>;
}
