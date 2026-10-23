import { createRoot } from 'react-dom/client';
import { Toaster } from '@sbb-polarion/react-sbb-polarion';
import '@sbb-polarion/react-sbb-polarion/style.css';
import App from './App';
import './App.css';
import { resumePendingDoc } from './services/adminNav';

// A cross-page doc link switched the admin node and stashed the article it meant to open; restore it
// here before mounting, so we do not first render the node's default page. When it redirects to another
// article it returns true: skip the render entirely - the replace reloads the frame. When the node already
// opened that article and only the fragment differs, it sets the fragment in place and returns false, as
// a fragment-only change reloads nothing: render as usual, and the article scrolls to the fragment.
const root = document.getElementById('root');
if (root && !resumePendingDoc()) {
  createRoot(root).render(
    <>
      <App />
      {/* Mounted once for the whole app; pages fire notifications with toast() from sonner. */}
      <Toaster />
    </>,
  );
}
