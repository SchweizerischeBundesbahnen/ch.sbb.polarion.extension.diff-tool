import useDiffService from "@/services/useDiffService";

export default function MergeTicker({workItemsPair, diffs, selected, pairSelectedCallback}) {
  const diffService = useDiffService();

  const changeSelected = (event) => {
    event.stopPropagation();
    if (diffs && diffs.length > 0) {
      pairSelectedCallback(!selected);
    }
  };

  return (
      <div className="merge-ticker" onClick={changeSelected}>
        {diffService.diffsExist(workItemsPair, diffs) && <div className="form-check" onClick={changeSelected}>
          <input className="form-check-input" type="checkbox" checked={selected} onChange={changeSelected} />
        </div>}
      </div>
  );
}
