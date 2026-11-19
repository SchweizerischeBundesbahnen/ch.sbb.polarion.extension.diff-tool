import { afterEach, describe, expect, it, vi } from 'vitest';
import { embedParams, isPdfExportEmbed, readEmbedViewOptions } from '../src/embed/exportMode';
import {
  EXPORT_MESSAGE_SOURCE,
  errorMessage,
  isExportMessage,
  postToParent,
  progressMessage,
  readyMessage,
} from '../src/embed/messages';
import { loadingError } from '../src/embed/useEmbedReporter';

afterEach(() => {
  vi.restoreAllMocks();
});

describe('embedded export mode', () => {
  it('is on only for embed=pdf-export', () => {
    expect(isPdfExportEmbed('?embed=pdf-export&sourceProjectId=p')).toBe(true);
    expect(isPdfExportEmbed('?embed=other')).toBe(false);
    expect(isPdfExportEmbed('?sourceProjectId=p')).toBe(false);
  });

  it('reads the view options which the embedding page writes', () => {
    const search =
      '?' +
      new URLSearchParams(
        embedParams({
          hideChaptersIfNoDifference: false,
          showOutlineNumbersDiff: true,
          compareEnumsById: true,
          counterpartWorkItemsDiffer: false,
          compareOnlyMutualFields: false,
        }),
      ).toString();

    expect(readEmbedViewOptions(search)).toEqual({
      hideChaptersIfNoDifference: false,
      showOutlineNumbersDiff: true,
      compareEnumsById: true,
      counterpartWorkItemsDiffer: false,
      compareOnlyMutualFields: false,
    });
  });

  it('leaves out options which are not given or not boolean', () => {
    expect(embedParams({ showOutlineNumbersDiff: undefined })).toEqual({ embed: 'pdf-export' });
    expect(readEmbedViewOptions('?embed=pdf-export&showOutlineNumbersDiff=yes')).toEqual({});
  });

  it('reads no options outside the embedded mode, so the viewer keeps its defaults', () => {
    expect(readEmbedViewOptions('?showOutlineNumbersDiff=true')).toEqual({});
  });
});

describe('export messages', () => {
  it('accepts the messages it builds', () => {
    expect(isExportMessage(progressMessage(3, 10))).toBe(true);
    expect(isExportMessage(readyMessage(false))).toBe(true);
    expect(isExportMessage(errorMessage('failed'))).toBe(true);
  });

  it('rejects anything else', () => {
    expect(isExportMessage(null)).toBe(false);
    expect(isExportMessage('ready')).toBe(false);
    expect(isExportMessage({ type: 'ready', diffsExist: true })).toBe(false);
    expect(isExportMessage({ source: EXPORT_MESSAGE_SOURCE, type: 'ready' })).toBe(false);
    expect(isExportMessage({ source: EXPORT_MESSAGE_SOURCE, type: 'progress', loaded: '1', total: 2 })).toBe(false);
    expect(isExportMessage({ source: EXPORT_MESSAGE_SOURCE, type: 'unknown' })).toBe(false);
  });

  it('posts to the embedding page of the same origin only', () => {
    const postMessage = vi.fn();
    const target = { postMessage: postMessage } as unknown as Window;
    const message = readyMessage(true);

    postToParent(message, target);

    expect(postMessage).toHaveBeenCalledWith(message, window.location.origin);
  });

  it('posts nothing when the page is not embedded', () => {
    const postMessage = vi.spyOn(window, 'postMessage');

    postToParent(readyMessage(true), window);

    expect(postMessage).not.toHaveBeenCalled();
  });
});

describe('loadingError', () => {
  it('names the error of each kind of diff', () => {
    expect(loadingError({ pairsLoadingError: 'pairs failed' })).toBe('pairs failed');
    expect(loadingError({ fieldsDiffLoadingError: 'fields failed' })).toBe('fields failed');
    expect(loadingError({ contentDiffLoadingError: 'content failed' })).toBe('content failed');
    expect(loadingError({ diffsLoadingErrors: true })).toBe('Failed to load the diff of some work items.');
  });

  it('is empty while the loading goes well', () => {
    expect(loadingError({ pairsLoading: false, diffsLoadingErrors: false, pairsLoadingError: null })).toBeNull();
  });
});
