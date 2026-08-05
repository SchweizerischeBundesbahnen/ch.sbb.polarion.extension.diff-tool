import { afterEach, describe, expect, it, vi } from 'vitest';
import { buildCollectionsDiffUrl, openCollectionsDiff } from '../src/topics/openCollectionsDiff';
import { buildWorkItemsDiffUrl, digestMessage, openWorkItemsDiff } from '../src/topics/openWorkItemsDiff';

// The writing half of the two handoffs from the navigation topics to the viewer, ported from the deleted
// webapp/diff-tool/js/diff-tool-widget-utils.js. The reading half - that the viewer picks
// localStorage["<sha1>_ids"] up again - is pinned by widgetHandoff.test.tsx.

const WORK_ITEMS_REQUEST = {
  sourceProjectId: 'elibrary',
  targetProjectId: 'drivepilot',
  linkRole: 'relates_to',
  config: 'Default',
  ids: ['EL-1', 'EL-2'],
};

const COLLECTIONS_REQUEST = {
  sourceProjectId: 'elibrary',
  sourceCollectionId: 'c1',
  targetProjectId: 'drivepilot',
  targetCollectionId: 'c2',
  linkRole: 'relates_to',
  config: 'Default',
};

afterEach(() => {
  localStorage.clear();
  vi.restoreAllMocks();
});

describe('openWorkItemsDiff', () => {
  it('hashes the selected IDs the way the legacy widget did', async () => {
    // sha1("EL-1,EL-2"), so an entry written by the old JS is still found by the new code
    expect(await digestMessage('EL-1,EL-2')).toBe('f7882cd0db39f495e883885db3d41971f8dc6fa8');
  });

  it('builds the viewer URL with the legacy parameters and stashes the IDs', async () => {
    const url = await buildWorkItemsDiffUrl(WORK_ITEMS_REQUEST);

    const hash = await digestMessage('EL-1,EL-2');
    expect(url).toBe(
      '/polarion/diff-tool-app/ui/app/workitems.html' +
        `?sourceProjectId=elibrary&targetProjectId=drivepilot&linkRole=relates_to&config=Default&ids=${hash}`,
    );
    expect(localStorage.getItem(`${hash}_ids`)).toBe('EL-1,EL-2');
  });

  it('percent-encodes values that would otherwise break the query', async () => {
    const url = await buildWorkItemsDiffUrl({ ...WORK_ITEMS_REQUEST, config: 'A&B' });

    expect(url).toContain('config=A%26B');
  });

  it('opens the comparison in a new tab', async () => {
    const open = vi.spyOn(window, 'open').mockReturnValue(null);

    await openWorkItemsDiff(WORK_ITEMS_REQUEST);

    expect(open).toHaveBeenCalledWith(expect.stringContaining('/workitems.html?sourceProjectId=elibrary'), '_blank');
  });
});

describe('openCollectionsDiff', () => {
  it('builds the viewer URL with the legacy parameters, compareAs included', () => {
    expect(buildCollectionsDiffUrl(COLLECTIONS_REQUEST)).toBe(
      '/polarion/diff-tool-app/ui/app/collections.html' +
        '?sourceProjectId=elibrary&sourceCollectionId=c1&targetProjectId=drivepilot&targetCollectionId=c2' +
        '&linkRole=relates_to&config=Default&compareAs=Workitems',
    );
  });

  it('opens the comparison in a new tab', () => {
    const open = vi.spyOn(window, 'open').mockReturnValue(null);

    openCollectionsDiff(COLLECTIONS_REQUEST);

    expect(open).toHaveBeenCalledWith(expect.stringContaining('/collections.html?sourceProjectId=elibrary'), '_blank');
  });
});
