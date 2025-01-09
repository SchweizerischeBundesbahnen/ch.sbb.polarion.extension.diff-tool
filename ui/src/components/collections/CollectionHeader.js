import PathPart from "@/components/PathPart";

export default function CollectionHeader({collection}) {
  return <div className="col collapsed-border">
    <div className="path">
      <PathPart label="project" value={collection.projectName}/>
      <PathPart label="collection" value={collection.name}/>
    </div>
  </div>;
}
