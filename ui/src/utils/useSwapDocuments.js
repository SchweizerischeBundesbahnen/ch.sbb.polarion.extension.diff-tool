import {usePathname, useRouter, useSearchParams} from "next/navigation";

const SWAP_MAPPING = {
  sourceProjectId: 'targetProjectId',
  sourceSpaceId: 'targetSpaceId',
  sourceDocument: 'targetDocument',
  targetProjectId: 'sourceProjectId',
  targetSpaceId: 'sourceSpaceId',
  targetDocument: 'sourceDocument',
};

export default function useSwapDocuments() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();

  return () => {
    const params = [];
    for (const [key, value] of searchParams.entries()) {
      const swappedKey = SWAP_MAPPING[key] || key;
      params.push(`${swappedKey}=${value}`);
    }
    router.push(pathname + '?' + params.join('&'));
  };
}
