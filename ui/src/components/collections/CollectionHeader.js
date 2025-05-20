import PathPart from "@/components/PathPart";

export default function CollectionHeader({collection, side}) {
  return <div className="col collapsed-border">
    <div className="path" data-testid={`${side}-collection`}>
      <PathPart label="project" value={collection.projectName}/>
      <PathPart label="collection" value={collection.name}/>
    </div>
  </div>;
}
