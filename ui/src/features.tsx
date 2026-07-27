import { type ComponentType, lazy } from 'react';
import AboutPage from './admin/pages/AboutPage';
import DiffConfigurationsPage from './admin/pages/DiffConfigurationsPage';
import MergeAuthorizationPage from './admin/pages/MergeAuthorizationPage';

// Lazy so Chart.js and its date adapter - by far the heaviest dependency here - land in their own chunk
// and are never downloaded by the other admin pages.
const ExecutionQueuePage = lazy(() => import('./admin/pages/ExecutionQueuePage'));

export interface Feature {
  /** Must equal the extender id in META-INF/hivemodule.xml, which is what `?feature=` carries. */
  id: string;
  label: string;
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
  { id: 'about', label: 'About', component: AboutPage },
  { id: 'diff-configurations', label: 'Diff Configurations', component: DiffConfigurationsPage },
  { id: 'execution-queue', label: 'Execution Queue', component: ExecutionQueuePage },
  { id: 'merge-authorization', label: 'Merge Authorization', component: MergeAuthorizationPage },
];

export function findFeature(id: string | null): Feature | undefined {
  return FEATURES.find((feature) => feature.id === id);
}
