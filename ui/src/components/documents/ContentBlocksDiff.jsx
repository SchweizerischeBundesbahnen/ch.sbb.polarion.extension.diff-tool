import {DIFF_SIDES, DiffLeaf} from "@/components/diff/DiffLeaf";

export default function ContentBlocksDiff({label, oldValue, newValue, selected, selectedCallback}) {

  return (
      <div className="diff-viewer" style={{
        position: "relative"
      }}>
        <label className="merge-ticker">
          <span className="form-check">
            <input className="form-check-input" type="checkbox" checked={selected} onChange={selectedCallback}
                   aria-label={label} />
          </span>
        </label>
        <DiffLeaf htmlDiff={oldValue} diffSide={DIFF_SIDES.LEFT}/>
        <DiffLeaf htmlDiff={newValue} diffSide={DIFF_SIDES.RIGHT}/>
      </div>
  );
}
