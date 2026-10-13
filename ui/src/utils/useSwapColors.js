import {usePathname, useRouter, useSearchParams} from "@/router/navigation";

const SWAP_COLORS_PARAM = 'swapColors';

/**
 * Whether the diff colors are swapped (added in red, removed in green), kept in the query string.
 * A view-only param: the diff components load their data via useDataSearchParams, so toggling it does not reload the diff.
 */
export default function useSwapColors() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();

  const swapColors = searchParams.get(SWAP_COLORS_PARAM) === 'true';
  const setSwapColors = (value) => {
    const params = new URLSearchParams(searchParams);
    if (value) {
      params.set(SWAP_COLORS_PARAM, 'true');
    } else {
      params.delete(SWAP_COLORS_PARAM);
    }
    router.replace(pathname + '?' + params.toString());
  };

  return [swapColors, setSwapColors];
}
