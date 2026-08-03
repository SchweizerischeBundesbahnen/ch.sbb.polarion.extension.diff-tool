import { type ComponentType, lazy } from 'react';
import AboutPage from './admin/pages/AboutPage';
import DiffConfigurationsPage from './admin/pages/DiffConfigurationsPage';
import MergeAuthorizationPage from './admin/pages/MergeAuthorizationPage';
import ProjectDuplicationPage from './admin/pages/ProjectDuplicationPage';

// Lazy so Chart.js and its date adapter - by far the heaviest dependency here - land in their own chunk
// and are never downloaded by the other admin pages.
const ExecutionQueuePage = lazy(() => import('./admin/pages/ExecutionQueuePage'));

export interface Feature {
  /** Must equal the extender id in META-INF/hivemodule.xml, which is what `?feature=` carries. */
  id: string;
  /** Kept identical to the extender's `name`, so the two navigations read the same. */
  label: string;
  /** One line on what the page is for, shown under its link on the dev Landing overview. */
  description: string;
  component: ComponentType;
}

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
  return FEATURES.find((feature) => feature.id === id);
}
