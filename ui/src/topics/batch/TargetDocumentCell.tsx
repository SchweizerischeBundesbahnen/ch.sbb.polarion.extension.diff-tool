import { useState } from 'react';
import { faPen, faXmark } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { SearchableSelect } from '@sbb-polarion/react-sbb-polarion';
import type { SearchDocument } from '../types';
import TitleWithId from './TitleWithId';
import type { DocumentRef } from './batchExport';

/** The target of a pair: the document of the same space and name, or the one the user picked instead. */
export type TargetResolution =
  | { state: 'pending' }
  | { state: 'found'; document: DocumentRef; overridden: boolean }
  | { state: 'missing'; message: string };

interface Props {
  sourceName: string;
  target: TargetResolution;
  /** The documents of the target project, loaded once a user wants to pick one. */
  candidates: SearchDocument[] | null;
  candidatesLoading: boolean;
  onEdit: () => void;
  onPick: (document: SearchDocument | null) => void;
}

export const documentKey = (spaceId: string, name: string) => `${spaceId}/${name}`;

/**
 * The target document cell of a selected row: the counterpart found in the target project, or a note that
 * there is none, with a button to pick another document instead.
 */
export default function TargetDocumentCell({
  sourceName,
  target,
  candidates,
  candidatesLoading,
  onEdit,
  onPick,
}: Readonly<Props>) {
  const [editing, setEditing] = useState(false);

  if (editing) {
    const options = (candidates ?? [])
      .filter((candidate) => candidate.readable)
      .map((candidate) => ({
        id: documentKey(candidate.spaceId ?? '', candidate.id),
        // Titles only: the picker is for choosing, the cell shows the IDs of the choice
        name: `${candidate.spaceTitle || candidate.spaceId} / ${candidate.title || candidate.id}`,
      }));
    const value = target.state === 'found' ? documentKey(target.document.spaceId, target.document.name) : '';
    return (
      <span className="target-cell">
        <SearchableSelect
          value={value}
          onChange={(key) => {
            onPick(
              (candidates ?? []).find((candidate) => documentKey(candidate.spaceId ?? '', candidate.id) === key) ??
                null,
            );
            setEditing(false);
          }}
          options={options}
          loading={candidatesLoading}
          ariaLabel={`Target document of ${sourceName}`}
          placeholder="Select document..."
        />
        <button
          type="button"
          className="icon-button"
          title="Cancel"
          aria-label="Cancel"
          onClick={() => setEditing(false)}
        >
          <FontAwesomeIcon icon={faXmark} />
        </button>
      </span>
    );
  }

  const edit = () => {
    onEdit();
    setEditing(true);
  };

  return (
    <span className="target-cell">
      {target.state === 'pending' ? <span className="unselected">Looking up...</span> : null}
      {target.state === 'found' ? (
        <TitleWithId
          title={`${target.document.spaceTitle || target.document.spaceId} / ${target.document.title || target.document.name}`}
          id={`${target.document.spaceId} / ${target.document.name}`}
        />
      ) : null}
      {target.state === 'missing' ? <span className="not-found">{target.message}</span> : null}
      <button
        type="button"
        className="icon-button"
        title="Pick another target document"
        aria-label={`Pick another target document for ${sourceName}`}
        onClick={edit}
      >
        <FontAwesomeIcon icon={faPen} />
      </button>
      {target.state === 'found' && target.overridden ? (
        <button
          type="button"
          className="icon-button"
          title="Use the document of the same space and name"
          aria-label={`Reset the target document of ${sourceName}`}
          onClick={() => onPick(null)}
        >
          <FontAwesomeIcon icon={faXmark} />
        </button>
      ) : null}
    </span>
  );
}
