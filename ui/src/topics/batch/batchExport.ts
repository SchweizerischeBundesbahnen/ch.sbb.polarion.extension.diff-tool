import { embedParams } from '../../embed/exportMode';
import type { EmbedViewOptions } from '../../embed/exportMode';
import { buildDocumentsDiffUrl } from '../../formext/openDocumentsDiff';
import type { PdfOptions } from '../../services/pdfExport';

// The model of a batch PDF export of document diffs: which document pairs, compared how, exported how.

/** The viewer page, relative to the topics page. */
const VIEWER_PAGE = 'documents.html';

/** A document of a project. */
export interface DocumentRef {
  projectId: string;
  spaceId: string;
  spaceTitle?: string | null;
  name: string;
  title?: string | null;
}

/** One pair of the batch: the source document compared with the target one. An empty revision is HEAD. */
export interface DocumentPair {
  source: DocumentRef;
  target: DocumentRef;
  sourceRevision: string;
  targetRevision: string;
}

export type CompareAs = 'Workitems' | 'Fields' | 'Content';
export type DiffView = 'side-by-side' | 'unified';

/** What applies to every pair of the batch. */
/**
 * Whether a comparison pairs the work items of the two documents, which takes a link role unless both sides are
 * the same document. The content comparison does too: the text between work items is anchored to them.
 */
export const needsLinkRole = (compareAs: CompareAs): boolean => compareAs !== 'Fields';

export interface BatchSettings {
  linkRole: string;
  config: string;
  compareAs: CompareAs;
  diffView: DiffView;
  pdf: PdfOptions;
  view: EmbedViewOptions;
}

/** Whether both sides of the pair are the same document, which is compared without a link role. */
export function isSameDocument(pair: DocumentPair): boolean {
  return (
    pair.source.projectId === pair.target.projectId &&
    pair.source.spaceId === pair.target.spaceId &&
    pair.source.name === pair.target.name
  );
}

/**
 * The URL of the viewer embedded for the export of one pair. Relative: the viewer is a sibling of the topics
 * page, under the app path Polarion serves and under the dev server root alike.
 */
export function viewerUrl(pair: DocumentPair, settings: BatchSettings, now: number = Date.now()): string {
  const base = buildDocumentsDiffUrl(
    {
      sourceProjectId: pair.source.projectId,
      sourceSpaceId: pair.source.spaceId,
      sourceDocument: pair.source.name,
      sourceRevision: pair.sourceRevision,
      targetProjectId: pair.target.projectId,
      targetSpaceId: pair.target.spaceId,
      targetDocument: pair.target.name,
      targetRevision: pair.targetRevision,
      linkRole: settings.linkRole,
      config: settings.config,
      branched: false,
    },
    now,
  );
  const query = base.split('?')[1];
  const params = new URLSearchParams(query);
  params.set('compareAs', settings.compareAs);
  if (settings.diffView === 'unified') {
    params.set('diffView', 'unified');
  }
  Object.entries(embedParams(settings.view)).forEach(([name, value]) => params.set(name, value));
  return `${VIEWER_PAGE}?${params.toString()}`;
}

/** Characters which a file name may not contain on one of the common systems; control characters neither. */
const UNSAFE_FILE_NAME_CHARACTERS = /[\\/:*?"<>|]+/g;

const withoutControlCharacters = (value: string) =>
  Array.from(value, (character) => (character.charCodeAt(0) < 32 ? '_' : character)).join('');

const fileNamePart = (value: string) =>
  withoutControlCharacters(value).replace(UNSAFE_FILE_NAME_CHARACTERS, '_').trim();

const documentPart = (document: DocumentRef, revision: string, withProject: boolean) =>
  [withProject ? document.projectId : null, document.spaceId, document.name, revision ? `r${revision}` : null]
    .filter((part): part is string => !!part)
    .map(fileNamePart)
    .join('_');

/** The PDF file name of a pair: the source document, then the target document with its project. */
export function pdfFileName(pair: DocumentPair): string {
  return `${documentPart(pair.source, pair.sourceRevision, false)}__${documentPart(pair.target, pair.targetRevision, true)}.pdf`;
}

/** The file names of the pairs, each one unique in the batch: a repeated name gets a number. */
export function uniqueFileNames(pairs: DocumentPair[]): string[] {
  const used = new Map<string, number>();
  return pairs.map((pair) => {
    const name = pdfFileName(pair);
    const count = (used.get(name) ?? 0) + 1;
    used.set(name, count);
    return count === 1 ? name : name.replace(/\.pdf$/, ` (${count}).pdf`);
  });
}

/** How a document reads in the progress dialog. */
export function documentLabel(document: DocumentRef, revision: string, withProject: boolean): string {
  const parts = [withProject ? document.projectId : null, document.spaceId, document.title || document.name].filter(
    (part): part is string => !!part,
  );
  return parts.join(' / ') + (revision ? ` (rev. ${revision})` : '');
}
