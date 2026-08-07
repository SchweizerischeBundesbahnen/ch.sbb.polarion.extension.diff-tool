import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import { subTopicHref } from '../src/topics/DiffToolHomePage';
import TopicsApp from '../src/topics/TopicsApp';
import { COMPARE_COLLECTIONS, COMPARE_WORK_ITEMS, DIFF_TOOL, TOPICS, findTopic } from '../src/topics/topics';
import { installFetchMock } from './mockFetch';

// The ?topic= router behind topics.html, and the root topic that replaced diff-tool.jsp.

const origUrl = window.location.pathname + window.location.search;

const openTopic = (topic: string) => window.history.replaceState({}, '', `?topic=${topic}&sourceProjectId=elibrary`);

beforeEach(() => {
  installFetchMock([
    { method: 'GET', match: /\/projects$/, json: [] },
    { method: 'GET', match: /link-roles$/, json: [] },
    { method: 'GET', match: /\/settings\/diff\/names/, json: [] },
    { method: 'GET', match: /\/search/, json: { totalCount: 0, page: 1, lastPage: 1, query: '', items: [] } },
  ]);
});

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
  window.history.replaceState({}, '', origUrl);
});

describe('topic registry', () => {
  it('carries the three navigation node ids of the Java side', () => {
    expect(TOPICS.map((topic) => topic.id)).toEqual([DIFF_TOOL, COMPARE_WORK_ITEMS, COMPARE_COLLECTIONS]);
  });

  it('finds a topic by id and nothing else', () => {
    expect(findTopic(COMPARE_WORK_ITEMS)?.title).toBe('Multiple Work Items');
    expect(findTopic('nonexistent')).toBeUndefined();
    expect(findTopic(null)).toBeUndefined();
  });
});

describe('TopicsApp', () => {
  it('renders the work items picker for its topic id', async () => {
    openTopic(COMPARE_WORK_ITEMS);
    render(<TopicsApp />);

    await vi.waitFor(() => expect(document.querySelector('.header h3')!.textContent).toBe('Compare work items'));
    expect(document.querySelector('.diff-topics')).not.toBeNull();
  });

  it('renders the collections picker for its topic id', async () => {
    openTopic(COMPARE_COLLECTIONS);
    render(<TopicsApp />);

    await vi.waitFor(() => expect(document.querySelector('.header h3')!.textContent).toBe('Compare Collections'));
  });

  it('falls back to the root topic for an unknown one', async () => {
    openTopic('nonexistent');
    render(<TopicsApp />);

    await vi.waitFor(() => expect(document.querySelector('.header h3')!.textContent).toBe('Diff Tool'));
  });

  it('falls back to the root topic when none is given', async () => {
    window.history.replaceState({}, '', '?sourceProjectId=elibrary');
    render(<TopicsApp />);

    await vi.waitFor(() => expect(document.querySelector('.header h3')!.textContent).toBe('Diff Tool'));
  });

  it('offers both sub-topics on the root topic', async () => {
    openTopic(DIFF_TOOL);
    render(<TopicsApp />);

    await vi.waitFor(() => expect(document.querySelectorAll('.link-button').length).toBe(2));
    const links = Array.from(document.querySelectorAll<HTMLButtonElement>('.link-button'));
    expect(links.map((link) => link.textContent)).toEqual(['Compare multiple Work Items', 'Compare Collections']);
  });

  it('points a sub-topic link at the topic path plus the node id', () => {
    // The click itself navigates the Polarion shell frame, which a test cannot exercise: window.top is the
    // test page here, and it is not redefinable.
    expect(subTopicHref('https://polarion/#/project/elibrary/diff-tool', COMPARE_WORK_ITEMS)).toBe(
      'https://polarion/#/project/elibrary/diff-tool/compare-work-items',
    );
  });
});
