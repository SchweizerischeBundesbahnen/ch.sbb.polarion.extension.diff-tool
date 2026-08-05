import { createRoot } from 'react-dom/client';
import { Toaster, configureGenericModules } from '@grigoriev/react-sbb-polarion';
import '@grigoriev/react-sbb-polarion/style.css';
import TopicsApp from '../topics/TopicsApp';
import '../topics/topics.css';

// Same as the admin bundle: react-sbb-polarion lazy-loads the shared generic ES modules at runtime (the
// breadcrumb bridge among them), so point it at the copy this webapp context serves.
configureGenericModules('/polarion/diff-tool-app/ui/generic/js/modules/');

const root = document.getElementById('root');
if (root) {
  createRoot(root).render(
    <>
      <TopicsApp />
      <Toaster />
    </>,
  );
}
