import { lazy } from 'react';
import { DocPage, type Feature as RoutedFeature, findFeature as findIn } from '@sbb-polarion/react-sbb-polarion';
import AboutPage from './admin/pages/AboutPage';
import DiffConfigurationsPage from './admin/pages/DiffConfigurationsPage';
import MergeAuthorizationPage from './admin/pages/MergeAuthorizationPage';
import ProjectDuplicationPage from './admin/pages/ProjectDuplicationPage';
import { DOC_ORDER } from './docs/manifest';

// Lazy so Chart.js and its date adapter - by far the heaviest dependency here - land in their own chunk
// and are never downloaded by the other admin pages.
const ExecutionQueuePage = lazy(() => import('./admin/pages/ExecutionQueuePage'));

/**
 * A page of the app. Extends RSP's routed `Feature` - `id` (must equal the extender id in
 * META-INF/hivemodule.xml, which is what `?feature=` carries) and `component`, all FeatureRouter needs -
 * with what the dev Landing overview lists.
 */
export interface Feature extends RoutedFeature {
  /** Kept identical to the extender's `name`, so the two navigations read the same. */
  label: string;
  /** One line on what the page is for, shown under its link on the dev Landing overview. */
  description: string;
}

// The documentation-site articles, derived from the shared manifest (docs.config.json) rather than listed by
// hand: each renders through the single DocPage bound to its manifest entry, in the manifest's reading order.
// Adding an article is a docs.config.json item (plus its markdown and the pom render) - nothing here changes.
// They carry no hivemodule.xml menu entry of their own: they are reached from the single `documentation` node
// and the articles' cross-document links (turned into ?feature=<id> by the interceptor in App.tsx); the ids
// equal the generated html basenames.
const DOC_FEATURES: Feature[] = DOC_ORDER.map((doc) => {
  const Component = () => <DocPage doc={doc} />;
  Component.displayName = `DocPage(${doc.id})`;
  return {
    id: doc.id,
    label: doc.title,
    description: `${doc.title}, generated from ${doc.source}.`,
    component: Component,
  };
});

/**
 * The admin pages this app serves, keyed by the id Polarion passes as `?feature=`. Single source of
 * truth for both the router in App.tsx and the dev Landing overview.
 *
 * The `rest-api` extender is intentionally absent: it points straight at
 * /polarion/diff-tool/rest/swagger and is not a page of this app.
 */
export const FEATURES: Feature[] = [
  {
    id: 'about',
    label: 'About',
    description: 'Version, build and properties of the installed extension.',
    component: AboutPage,
  },
  // The documentation site, in manifest reading order (Quick Start, User Guide, Configuration, Velocity API).
  ...DOC_FEATURES,
  {
    id: 'diff-configurations',
    label: 'Diff Configurations',
    description: 'Which work item fields, link roles and statuses take part in a comparison.',
    component: DiffConfigurationsPage,
  },
  {
    id: 'execution-queue',
    label: 'Execution Queue',
    description: 'Worker and thread limits for queued operations, plus live queue statistics.',
    component: ExecutionQueuePage,
  },
  {
    id: 'merge-authorization',
    label: 'Merge Authorization',
    description: 'Which project roles may merge work items and documents.',
    component: MergeAuthorizationPage,
  },
  {
    id: 'project-duplication',
    label: 'Project Duplication',
    description: 'Duplicate a project with its documents and work items, and track the jobs.',
    component: ProjectDuplicationPage,
  },
];

export function findFeature(id: string | null): Feature | undefined {
  return findIn(FEATURES, id);
}
