import {usePathname, useRouter, useSearchParams} from "next/navigation";

const SWAP_MAPPING = {
  sourceProjectId: 'targetProjectId',
  sourceSpaceId: 'targetSpaceId',
  sourceDocument: 'targetDocument',
  sourceRevision: 'targetRevision',
  targetProjectId: 'sourceProjectId',
  targetSpaceId: 'sourceSpaceId',
  targetDocument: 'sourceDocument',
  targetRevision: 'sourceRevision',
};

export default function useSwapDocuments() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();

  return () => {
    const params = new URLSearchParams();
    for (const [key, value] of searchParams.entries()) {
      const swappedKey = SWAP_MAPPING[key] || key;
      params.append(swappedKey, value);
    }
    router.push(pathname + '?' + params.toString());
  };
}
