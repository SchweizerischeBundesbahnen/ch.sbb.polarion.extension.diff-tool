import {DIFF_SIDES, DiffLeaf} from "@/components/diff/DiffLeaf";

export default function ContentBlocksDiff({oldValue, newValue, selected, selectedCallback}) {

  return (
      <div className="diff-viewer" style={{
        position: "relative"
      }}>
        <div className="merge-ticker" onClick={selectedCallback}>
          <div className="form-check">
            <input className="form-check-input" type="checkbox" checked={selected} onChange={selectedCallback} />
          </div>
        </div>
        <DiffLeaf htmlDiff={oldValue} diffSide={DIFF_SIDES.LEFT}/>
        <DiffLeaf htmlDiff={newValue} diffSide={DIFF_SIDES.RIGHT}/>
      </div>
  );
}
