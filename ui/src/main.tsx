import { createRoot } from 'react-dom/client';
import { Toaster } from '@sbb-polarion/react-sbb-polarion';
import '@sbb-polarion/react-sbb-polarion/style.css';
import App from './App';
import './App.css';

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
