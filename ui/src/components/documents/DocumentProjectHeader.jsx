import PathPart from "@/components/PathPart";

export default function DocumentProjectHeader({document, side}) {
  return <div className="path" data-testid={`${side}-project`}>
    <PathPart label="project" value={document.projectName}/>
  </div>;
}
