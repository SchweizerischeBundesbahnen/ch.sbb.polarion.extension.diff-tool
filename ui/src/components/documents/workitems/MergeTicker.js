export default function MergeTicker({selected, pairSelectedCallback, diffs, moved, createdOrDeleted}) {
  const changeSelected = (event) => {
    event.stopPropagation();
    if (diffs && diffs.length > 0) {
      pairSelectedCallback(!selected);
    }
  };

  return (
      <div style={{}} className="merge-ticker" onClick={changeSelected}>
        {((diffs && (diffs.length > 1 || (diffs.length === 1 && diffs[0].id !== 'outlineNumber'))) || moved || createdOrDeleted) && <div className="form-check" onClick={changeSelected}>
          <input className="form-check-input" type="checkbox" checked={selected} id="flexCheckChecked" onChange={changeSelected} />
        </div>}
      </div>
  );
}