export default function FieldMergeTicker({fieldName, selected, changeSelectionCallback}) {

  return (
      <div className="merge-ticker" onClick={changeSelectionCallback}>
        <div className="form-check" onClick={changeSelectionCallback}>
          <input className="form-check-input" type="checkbox" checked={selected} onChange={changeSelectionCallback}
                 aria-label={`Select field ${fieldName} for merge`} />
        </div>
      </div>
  );
}
