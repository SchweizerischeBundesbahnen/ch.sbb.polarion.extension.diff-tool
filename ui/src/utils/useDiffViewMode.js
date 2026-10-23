import {usePathname, useRouter, useSearchParams} from "@/router/navigation";
import * as DiffViewModes from "@/DiffViewModes";

export const DIFF_VIEW_PARAM = 'diffView';

/**
 * The diff view (side by side or unified), kept in the query string, so the reload after a merge keeps it.
 * A view-only param: the diff components load their data via useDataSearchParams, so switching it does not reload the diff.
 * Side by side, the default, is left out of the query string.
 */
export default function useDiffViewMode() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();

  const diffViewMode = searchParams.get(DIFF_VIEW_PARAM) === DiffViewModes.UNIFIED ? DiffViewModes.UNIFIED : DiffViewModes.SIDE_BY_SIDE;
  const setDiffViewMode = (value) => {
    const params = new URLSearchParams(searchParams);
    if (value === DiffViewModes.UNIFIED) {
      params.set(DIFF_VIEW_PARAM, DiffViewModes.UNIFIED);
    } else {
      params.delete(DIFF_VIEW_PARAM);
    }
    router.replace(pathname + '?' + params.toString());
  };

  return [diffViewMode, setDiffViewMode];
}
