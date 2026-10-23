import { describe, expect, it } from 'vitest';
import useImageUtils from '../src/utils/useImageUtils';

// The unified diff holds images of both work items, so each one is resolved against its own work item.

const SOURCE = { id: 'EL-1' };
const SOURCE_DOCUMENT = { projectId: 'elibrary', spaceId: 'Specification', id: 'Spec' };
const TARGET = { id: 'DP-2' };
const TARGET_DOCUMENT = { projectId: 'drivepilot', spaceId: 'Design', id: 'Design' };

const sources = (html: string) =>
  [...new DOMParser().parseFromString(html, 'text/html').querySelectorAll('img')].map((image) =>
    image.getAttribute('src'),
  );

describe('unifiedDocumentWorkItemsDiff', () => {
  it('resolves a removed image against the source and any other image against the target', () => {
    const diffRight =
      '<span class="diff-html-removed"><img src="workitemimg:old.png"></span>' +
      '<span class="diff-html-added"><img src="workitemimg:new.png"></span>' +
      '<img src="attachment:same.png">';

    const unified = useImageUtils().unifiedDocumentWorkItemsDiff(
      '',
      diffRight,
      SOURCE,
      SOURCE_DOCUMENT,
      TARGET,
      TARGET_DOCUMENT,
    );

    expect(sources(unified)).toEqual([
      '/polarion/wi-attachment/elibrary/EL-1/old.png',
      '/polarion/wi-attachment/drivepilot/DP-2/new.png',
      '/polarion/module-attachment/drivepilot/Design/Design/same.png',
    ]);
  });

  it('takes the source diff, resolved against the source, when the target is missing', () => {
    const diffLeft = '<span class="diff-html-removed"><img src="workitemimg:old.png"></span>';

    const unified = useImageUtils().unifiedDocumentWorkItemsDiff(
      diffLeft,
      '',
      SOURCE,
      SOURCE_DOCUMENT,
      undefined,
      TARGET_DOCUMENT,
    );

    expect(sources(unified)).toEqual(['/polarion/wi-attachment/elibrary/EL-1/old.png']);
  });

  it('leaves a diff without Polarion images as it is', () => {
    const diffRight = '<span class="diff-html-added">text</span> <img src="https://example.org/a.png">';

    expect(
      useImageUtils().unifiedDocumentWorkItemsDiff('', diffRight, SOURCE, SOURCE_DOCUMENT, TARGET, TARGET_DOCUMENT),
    ).toBe(diffRight);
  });
});
