import PathPart from "@/components/PathPart";

export default function ProjectHeader({projectName}) {
  return <div className="col collapsed-border">
    <div className="path">
      <PathPart label="project" value={projectName}/>
    </div>
  </div>;
}
