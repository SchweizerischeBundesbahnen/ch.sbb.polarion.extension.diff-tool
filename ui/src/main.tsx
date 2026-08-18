import { createRoot } from 'react-dom/client';
import { Toaster, configureGenericModules } from '@sbb-polarion/react-sbb-polarion';
import '@sbb-polarion/react-sbb-polarion/style.css';
import App from './App';
import './App.css';

// react-sbb-polarion lazy-loads the shared generic ES modules at runtime; point it at the copy this
// webapp context serves (GenericUiServlet resolves /ui/generic/* out of the embedded generic.app).
configureGenericModules('/polarion/diff-tool-app/ui/generic/js/modules/');

const root = document.getElementById('root');
if (root) {
  createRoot(root).render(
    <>
      <App />
      {/* Mounted once for the whole app; pages fire notifications with toast() from sonner. */}
      <Toaster />
    </>,
  );
}
