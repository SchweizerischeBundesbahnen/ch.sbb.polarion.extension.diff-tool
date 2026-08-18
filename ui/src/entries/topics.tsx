import { createRoot } from 'react-dom/client';
import { Toaster, configureGenericModules } from '@sbb-polarion/react-sbb-polarion';
import '@sbb-polarion/react-sbb-polarion/style.css';
import TopicsApp from '../topics/TopicsApp';
import { GENERIC_MODULES_BASE } from '../topics/genericModules';
import '../topics/topics.css';

// Same as the admin bundle: react-sbb-polarion lazy-loads the shared generic ES modules at runtime, so point
// it at the copy this webapp context serves. src/topics/BreadcrumbTopic.tsx loads the breadcrumb bridge from
// the same place.
configureGenericModules(GENERIC_MODULES_BASE);

const root = document.getElementById('root');
if (root) {
  createRoot(root).render(
    <>
      <TopicsApp />
      <Toaster />
    </>,
  );
}
