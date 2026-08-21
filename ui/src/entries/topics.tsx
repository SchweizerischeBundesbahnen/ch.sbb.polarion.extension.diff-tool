import { createRoot } from 'react-dom/client';
import { Toaster } from '@sbb-polarion/react-sbb-polarion';
import '@sbb-polarion/react-sbb-polarion/style.css';
import TopicsApp from '../topics/TopicsApp';
import '../topics/topics.css';

const root = document.getElementById('root');
if (root) {
  createRoot(root).render(
    <>
      <TopicsApp />
      <Toaster />
    </>,
  );
}
