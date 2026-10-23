import PathPart from "@/components/PathPart";

export default function ProjectHeader({project, side}) {
  return <div className="path" data-testid={`${side}-project`}>
    <PathPart label="project" value={project.name}/>
  </div>;
}
