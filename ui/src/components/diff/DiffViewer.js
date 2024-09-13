import {DIFF_SIDES, DiffLeaf} from "@/components/diff/DiffLeaf";

export default function DiffViewer({oldValue, newValue}) {
  return (
      <div className="diff-viewer">
        <DiffLeaf htmlDiff={oldValue} diffSide={DIFF_SIDES.LEFT} />
        <DiffLeaf htmlDiff={newValue} diffSide={DIFF_SIDES.RIGHT} />
      </div>
  );
}