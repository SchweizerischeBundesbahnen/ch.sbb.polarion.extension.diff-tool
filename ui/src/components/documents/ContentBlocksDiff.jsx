import DiffViewer from "@/components/diff/DiffViewer";

export default function ContentBlocksDiff({label, oldValue, newValue, selected, selectedCallback}) {

  return (
      <DiffViewer oldValue={oldValue} newValue={newValue} style={{
        position: "relative"
      }}>
        <label className="merge-ticker">
          <span className="form-check">
            <input className="form-check-input" type="checkbox" checked={selected} onChange={selectedCallback}
                   aria-label={label} />
          </span>
        </label>
      </DiffViewer>
  );
}
