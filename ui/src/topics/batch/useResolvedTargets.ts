import { useEffect, useRef, useState } from 'react';
import { responseError } from '../../services/responseError';
import { sendRequest } from '../../services/useRemote';
import type { ResolvedDocument } from '../types';
import { documentKey } from './TargetDocumentCell';

export interface ResolvedTargets {
  /** What is known so far, keyed by `space/name`. A document still being looked up is missing. */
  resolved: Map<string, ResolvedDocument>;
  error: string | null;
}

/**
 * Looks up whether the documents of the given spaces and names exist in the target project, so each source
 * document can be paired with its counterpart there. Asks once for all documents it does not know yet, and
 * keeps what it learned until the target project changes.
 */
export default function useResolvedTargets(
  targetProjectId: string,
  documents: { spaceId: string; name: string }[],
): ResolvedTargets {
  const [resolved, setResolved] = useState<Map<string, ResolvedDocument>>(new Map());
  const [error, setError] = useState<string | null>(null);
  const requested = useRef<Set<string>>(new Set());
  const project = useRef(targetProjectId);

  if (project.current !== targetProjectId) {
    project.current = targetProjectId;
    requested.current = new Set();
  }

  useEffect(() => {
    setResolved(new Map());
    setError(null);
  }, [targetProjectId]);

  const unknown = documents.filter((document) => !requested.current.has(documentKey(document.spaceId, document.name)));
  const unknownKey = unknown.map((document) => documentKey(document.spaceId, document.name)).join('\n');

  useEffect(() => {
    if (!targetProjectId || unknown.length === 0) {
      return undefined;
    }
    unknown.forEach((document) => requested.current.add(documentKey(document.spaceId, document.name)));
    let cancelled = false;

    (async () => {
      try {
        const response = await sendRequest({
          method: 'POST',
          url: `/projects/${encodeURIComponent(targetProjectId)}/documents/resolve`,
          body: JSON.stringify(unknown.map((document) => ({ spaceId: document.spaceId, name: document.name }))),
          contentType: 'application/json',
        });
        if (!response.ok) {
          throw await responseError(response);
        }
        const found = (await response.json()) as ResolvedDocument[];
        if (!cancelled) {
          setResolved((previous) => {
            const next = new Map(previous);
            found.forEach((document) => next.set(documentKey(document.spaceId, document.name), document));
            return next;
          });
        }
      } catch (lookupError) {
        if (!cancelled) {
          // Asked again on the next change, rather than never
          unknown.forEach((document) => requested.current.delete(documentKey(document.spaceId, document.name)));
          setError(
            `Error occurred looking up target documents${lookupError instanceof Error ? `: ${lookupError.message}` : ''}`,
          );
        }
      }
    })();

    return () => {
      cancelled = true;
    };
    // `unknownKey` names exactly the documents of `unknown`
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [targetProjectId, unknownKey]);

  return { resolved: resolved, error: error };
}
