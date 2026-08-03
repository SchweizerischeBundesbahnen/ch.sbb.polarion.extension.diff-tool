import useDiffService from "@/services/useDiffService";
import {useSearchParams} from "@/router/navigation";
import {useMemo} from "react";

export default function PairMergeTicker({workItemsPair, diffs, selected, pairSelectedCallback}) {
  const diffService = useDiffService();
  const searchParams = useSearchParams();
  const branchedDocuments = useMemo(() => searchParams.get('branched') === "true", [searchParams]);

  const changeSelected = (event) => {
    event.stopPropagation();
    if (diffs && diffs.length > 0) {
      pairSelectedCallback(!selected);
    }
  };

  return (
      <div className="merge-ticker" onClick={changeSelected}>
        {diffService.diffsExist(workItemsPair, diffs, branchedDocuments) && <div className="form-check" onClick={changeSelected}>
          <input className="form-check-input" type="checkbox" checked={selected} onChange={changeSelected} />
        </div>}
      </div>
  );
}
