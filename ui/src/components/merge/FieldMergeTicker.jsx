export default function FieldMergeTicker({fieldName, selected, changeSelectionCallback}) {

  return (
      <label className="merge-ticker">
        <span className="form-check">
          <input className="form-check-input" type="checkbox" checked={selected} onChange={changeSelectionCallback}
                 aria-label={`Select field ${fieldName} for merge`} />
        </span>
      </label>
  );
}
