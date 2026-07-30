import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { buildDocumentsDiffUrl, openDocumentsDiff } from '../src/formext/openDocumentsDiff';
import type { DiffRequest } from '../src/formext/openDocumentsDiff';

// The handoff contract between the Document Properties panel and the viewer page: the query string, and
// the `<uuid>_additionalParams` localStorage entry the viewer reads alongside it. Both were ported
// verbatim from DiffTool.showDiffResult(), so these assertions are spelled out literally rather than
// derived - a refactor that changes them has broken the viewer.

const NOW = 1_780_000_000_000;

const REQUEST: DiffRequest = {
  sourceProjectId: 'elibrary',
  sourceSpaceId: 'specification',
  sourceDocument: 'Product Specification',
  sourceRevision: '',
  targetProjectId: 'drivepilot',
  targetSpaceId: 'design',
  targetDocument: 'Design Spec',
  targetRevision: '',
  linkRole: '',
  config: 'Default',
  branched: false,
};

const paramsKeys = () => Object.keys(localStorage).filter((key) => key.endsWith('_additionalParams'));

const storedParams = () => JSON.parse(localStorage.getItem(paramsKeys()[0])!) as Record<string, unknown>;

beforeEach(() => localStorage.clear());

afterEach(() => {
  localStorage.clear();
  vi.restoreAllMocks();
});

describe('buildDocumentsDiffUrl', () => {
  it('builds the minimal URL, omitting the parameters that are empty', () => {
    const url = buildDocumentsDiffUrl(REQUEST, NOW);

    const [path, query] = url.split('?');
    expect(path).toBe('/polarion/diff-tool-app/ui/app/documents.html');
    // Order matters as little as the names do, but pinning the whole thing catches an accidental rename.
    // Values are percent-encoded (spaces as `+`, which URLSearchParams.get() decodes on the viewer side);
    // the parameter names and their order are the part of the contract that must not move.
    expect(query.replace(/&additionalParams=.*/, '')).toBe(
      'sourceProjectId=elibrary&sourceSpaceId=specification&sourceDocument=Product+Specification' +
        '&targetProjectId=drivepilot&targetSpaceId=design&targetDocument=Design+Spec' +
        '&config=Default&compareAs=Workitems&branched=false',
    );
    expect(url).not.toContain('linkRole=');
    expect(url).not.toContain('sourceRevision=');
    expect(url).not.toContain('targetRevision=');
  });

  it('encodes values that would otherwise break the query, and the viewer reads them back', () => {
    // The legacy code concatenated these raw, so `&` started a new parameter and `#` a fragment: the
    // viewer then compared the wrong document, or none. Asserted through URLSearchParams because that is
    // exactly how the viewer reads them (getDocumentFromSearchParams in services/useDiffService.js).
    const url = buildDocumentsDiffUrl(
      {
        ...REQUEST,
        sourceDocument: 'R&D Spec',
        targetDocument: 'Design #2',
        targetSpaceId: 'a/b',
        config: '100% Strict & Fast',
        linkRole: 'relates&to',
      },
      NOW,
    );

    const query = new URLSearchParams(url.split('?')[1]);
    expect(query.get('sourceDocument')).toBe('R&D Spec');
    expect(query.get('targetDocument')).toBe('Design #2');
    expect(query.get('targetSpaceId')).toBe('a/b');
    expect(query.get('config')).toBe('100% Strict & Fast');
    expect(query.get('linkRole')).toBe('relates&to');
    // Nothing leaked into a stray parameter or a fragment.
    expect(Array.from(query.keys()).filter((key) => key === 'sourceDocument')).toHaveLength(1);
    expect(url).not.toContain('#');
  });

  it('appends the link role and both revisions when they are set', () => {
    const url = buildDocumentsDiffUrl(
      { ...REQUEST, linkRole: 'relates_to', sourceRevision: '100', targetRevision: '200', branched: true },
      NOW,
    );

    expect(url).toContain('&branched=true');
    expect(url).toContain('&linkRole=relates_to');
    expect(url).toContain('&sourceRevision=100');
    expect(url).toContain('&targetRevision=200');
  });

  it('stashes the additional parameters under the uuid it put in the URL', () => {
    const url = buildDocumentsDiffUrl(REQUEST, NOW);

    const hash = new URLSearchParams(url.split('?')[1]).get('additionalParams')!;
    expect(hash).toMatch(/^[0-9a-f-]{36}$/);
    expect(JSON.parse(localStorage.getItem(`${hash}_additionalParams`)!)).toEqual({
      individualFieldsSelection: true,
      ts: NOW,
    });
  });

  it('includes the work items filter when one was given', () => {
    buildDocumentsDiffUrl({ ...REQUEST, filter: { value: 'EL-1, EL-2', type: 'include' } }, NOW);

    expect(storedParams().filter).toEqual({ value: 'EL-1, EL-2', type: 'include' });
  });

  it('drops handoff entries older than 24 hours and keeps fresh ones', () => {
    localStorage.setItem('stale_additionalParams', JSON.stringify({ ts: NOW - 25 * 60 * 60 * 1000 }));
    localStorage.setItem('fresh_additionalParams', JSON.stringify({ ts: NOW - 60 * 1000 }));

    buildDocumentsDiffUrl(REQUEST, NOW);

    expect(localStorage.getItem('stale_additionalParams')).toBeNull();
    expect(localStorage.getItem('fresh_additionalParams')).not.toBeNull();
  });

  it('drops handoff entries that are unparseable or carry no timestamp', () => {
    localStorage.setItem('broken_additionalParams', 'not json');
    localStorage.setItem('untimed_additionalParams', JSON.stringify({ individualFieldsSelection: true }));
    localStorage.setItem('null_additionalParams', 'null');

    buildDocumentsDiffUrl(REQUEST, NOW);

    expect(localStorage.getItem('broken_additionalParams')).toBeNull();
    expect(localStorage.getItem('untimed_additionalParams')).toBeNull();
    expect(localStorage.getItem('null_additionalParams')).toBeNull();
  });

  it('purges the keys an older version of the handoff wrote, and nothing else', () => {
    localStorage.setItem('abc_filter', 'EL-1');
    localStorage.setItem('abc_type', 'include');
    localStorage.setItem('unrelated', 'keep me');

    buildDocumentsDiffUrl(REQUEST, NOW);

    expect(localStorage.getItem('abc_filter')).toBeNull();
    expect(localStorage.getItem('abc_type')).toBeNull();
    expect(localStorage.getItem('unrelated')).toBe('keep me');
  });
});

describe('openDocumentsDiff', () => {
  it('opens the comparison in a new tab', () => {
    const open = vi.spyOn(window, 'open').mockReturnValue(null);

    openDocumentsDiff(REQUEST);

    expect(open).toHaveBeenCalledTimes(1);
    const [url, target] = open.mock.calls[0];
    expect(String(url)).toContain('/polarion/diff-tool-app/ui/app/documents.html?');
    expect(target).toBe('_blank');
  });
});
