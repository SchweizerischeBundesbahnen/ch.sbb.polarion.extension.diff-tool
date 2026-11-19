import { afterEach, describe, expect, it } from 'vitest';
import { DEFAULT_EXPORT_OPTIONS } from '../src/topics/batch/ExportOptionsPanel';
import {
  documentLabel,
  isSameDocument,
  pdfFileName,
  uniqueFileNames,
  viewerUrl,
} from '../src/topics/batch/batchExport';
import type { BatchSettings, DocumentPair } from '../src/topics/batch/batchExport';

const PAIR: DocumentPair = {
  source: { projectId: 'elibrary', spaceId: 'Specification', name: 'Catalog Specification', title: 'Catalog' },
  target: { projectId: 'drivepilot', spaceId: 'Design', name: 'Catalog Design', title: null },
  sourceRevision: '',
  targetRevision: '',
};

const SETTINGS: BatchSettings = { ...DEFAULT_EXPORT_OPTIONS, linkRole: 'relates_to', config: 'Default' };

afterEach(() => localStorage.clear());

describe('viewerUrl', () => {
  it('opens the documents viewer embedded, with the comparison and the view options of the batch', () => {
    const [path, query] = viewerUrl(PAIR, SETTINGS).split('?');
    const params = new URLSearchParams(query);

    expect(path).toBe('documents.html');
    expect(params.get('sourceProjectId')).toBe('elibrary');
    expect(params.get('sourceSpaceId')).toBe('Specification');
    expect(params.get('sourceDocument')).toBe('Catalog Specification');
    expect(params.get('targetProjectId')).toBe('drivepilot');
    expect(params.get('targetDocument')).toBe('Catalog Design');
    expect(params.get('linkRole')).toBe('relates_to');
    expect(params.get('config')).toBe('Default');
    expect(params.get('compareAs')).toBe('Workitems');
    expect(params.get('embed')).toBe('pdf-export');
    expect(params.get('hideChaptersIfNoDifference')).toBe('true');
    expect(params.get('compareOnlyMutualFields')).toBe('true');
    expect(params.has('diffView')).toBe(false);
    expect(params.has('sourceRevision')).toBe(false);
  });

  it('carries the revisions, the kind of comparison and the unified view', () => {
    const pair = { ...PAIR, sourceRevision: '1234', targetRevision: '987' };
    const settings: BatchSettings = { ...SETTINGS, compareAs: 'Fields', diffView: 'unified' };

    const params = new URLSearchParams(viewerUrl(pair, settings).split('?')[1]);

    expect(params.get('sourceRevision')).toBe('1234');
    expect(params.get('targetRevision')).toBe('987');
    expect(params.get('compareAs')).toBe('Fields');
    expect(params.get('diffView')).toBe('unified');
  });
});

describe('isSameDocument', () => {
  it('is true only for the same project, space and name', () => {
    const same = { ...PAIR, target: { ...PAIR.source, title: 'other' }, targetRevision: '5' };

    expect(isSameDocument(same)).toBe(true);
    expect(isSameDocument(PAIR)).toBe(false);
  });
});

describe('pdfFileName', () => {
  it('names the source document, then the target document with its project', () => {
    expect(pdfFileName(PAIR)).toBe('Specification_Catalog Specification__drivepilot_Design_Catalog Design.pdf');
  });

  it('names the revisions', () => {
    const pair = { ...PAIR, sourceRevision: '12', targetRevision: '34' };

    expect(pdfFileName(pair)).toBe('Specification_Catalog Specification_r12__drivepilot_Design_Catalog Design_r34.pdf');
  });

  it('replaces characters a file name cannot hold', () => {
    const pair = {
      ...PAIR,
      source: { ...PAIR.source, spaceId: 'a/b', name: 'x:y*?"<>|z\u0007' },
    };

    expect(pdfFileName(pair).startsWith('a_b_x_y_z_')).toBe(true);
  });
});

describe('uniqueFileNames', () => {
  it('numbers a repeated name', () => {
    expect(uniqueFileNames([PAIR, PAIR, PAIR])).toEqual([
      'Specification_Catalog Specification__drivepilot_Design_Catalog Design.pdf',
      'Specification_Catalog Specification__drivepilot_Design_Catalog Design (2).pdf',
      'Specification_Catalog Specification__drivepilot_Design_Catalog Design (3).pdf',
    ]);
  });
});

describe('documentLabel', () => {
  it('shows the title, or the name of a document without one, and its revision', () => {
    expect(documentLabel(PAIR.source, '', false)).toBe('Specification / Catalog');
    expect(documentLabel(PAIR.target, '42', true)).toBe('drivepilot / Design / Catalog Design (rev. 42)');
  });
});
