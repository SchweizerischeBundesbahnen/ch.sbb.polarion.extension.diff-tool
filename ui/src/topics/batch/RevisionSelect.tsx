import { useMemo } from 'react';
import { SearchableSelect } from '@sbb-polarion/react-sbb-polarion';
import useRemoteList from '../../formext/useRemoteList';
import type { DocumentRef } from './batchExport';

interface RevisionInfo {
  name: string;
  baselineName?: string | null;
}

const encode = (segment: string) => encodeURIComponent(segment);

interface Props {
  document: DocumentRef;
  value: string;
  onChange: (value: string) => void;
  ariaLabel: string;
}

/**
 * The revision of one document of a pair: HEAD, or one of its revisions, named by a baseline when it has one.
 * Rendered only for a selected row, so the table asks for the revisions of the documents it exports alone.
 */
export default function RevisionSelect({ document, value, onChange, ariaLabel }: Readonly<Props>) {
  const revisions = useRemoteList<RevisionInfo>({
    url: `/projects/${encode(document.projectId)}/spaces/${encode(document.spaceId)}/documents/${encode(document.name)}/revisions`,
    progressMessage: 'Loading revisions',
    errorMessage: 'Error occurred loading revisions',
  });

  const options = useMemo(
    () => [
      { id: '', name: 'HEAD' },
      ...revisions.items.map((revision) => ({
        id: revision.name,
        name: revision.baselineName ? `${revision.name} | ${revision.baselineName}` : revision.name,
      })),
    ],
    [revisions.items],
  );

  return (
    <SearchableSelect
      value={value}
      onChange={onChange}
      options={options}
      loading={!!revisions.loading}
      ariaLabel={ariaLabel}
      placeholder="HEAD"
    />
  );
}
