import type { ComponentType } from 'react';
import CollectionsPickerPage from './CollectionsPickerPage';
import DiffToolHomePage from './DiffToolHomePage';
import WorkItemsPickerPage from './WorkItemsPickerPage';

/**
 * The ids Polarion passes as `?topic=`. They MUST equal the navigation node ids on the Java side
 * (DiffToolNavigationExtender.DIFF_TOOL, MultipleWorkItemsNode.NODE_ID, CollectionsNode.NODE_ID), which is
 * what those nodes' getPageUrl() puts into the URL - and what DiffToolHomePage appends to the topic path.
 */
export const DIFF_TOOL = 'diff-tool';
export const COMPARE_WORK_ITEMS = 'compare-work-items';
export const COMPARE_COLLECTIONS = 'compare-collections';

export interface Topic {
  id: string;
  /** Breadcrumb label, kept identical to the navigation node's label. */
  title: string;
  /** Parent label of a sub-topic, as the legacy breadcrumb bootstrap passed it. */
  parent?: string;
  /** Polarion-served breadcrumb icon, the same URL the navigation node returns. */
  icon: string;
  component: ComponentType;
}

/**
 * The three Diff Tool navigation topics this page serves, keyed by the id `?topic=` carries. Single source of
 * truth for the router in TopicsApp and for the breadcrumb.
 */
export const TOPICS: Topic[] = [
  {
    id: DIFF_TOOL,
    title: 'Diff Tool',
    icon: '/polarion/diff-tool-app/ui/images/menu/30x30/_parent.svg',
    component: DiffToolHomePage,
  },
  {
    id: COMPARE_WORK_ITEMS,
    title: 'Multiple Work Items',
    parent: 'Diff Tool',
    icon: '/polarion/ria/images/topicIconsSmall/workItems.svg',
    component: WorkItemsPickerPage,
  },
  {
    id: COMPARE_COLLECTIONS,
    title: 'Collections',
    parent: 'Diff Tool',
    icon: '/polarion/ria/images/topicIconsSmall/collectionsTopic.svg',
    component: CollectionsPickerPage,
  },
];

export function findTopic(id: string | null): Topic | undefined {
  return TOPICS.find((topic) => topic.id === id);
}
