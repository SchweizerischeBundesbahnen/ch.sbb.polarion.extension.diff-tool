import PathPart from "@/components/PathPart";

export default function DocumentProjectHeader({document}) {
  return <div className="col collapsed-border">
    <div className="path">
      <PathPart label="project" value={document.projectName}/>
    </div>
  </div>;
}
