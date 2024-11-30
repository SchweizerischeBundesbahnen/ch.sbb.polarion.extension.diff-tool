import PathPart from "@/components/PathPart";

export default function ProjectHeader({project}) {
  return <div className="col collapsed-border">
    <div className="path">
      <PathPart label="project" value={project.name}/>
    </div>
  </div>;
}
