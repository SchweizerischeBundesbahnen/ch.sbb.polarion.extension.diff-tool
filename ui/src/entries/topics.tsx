import { createRoot } from 'react-dom/client';
import { Toaster } from '@grigoriev/react-sbb-polarion';
import '@grigoriev/react-sbb-polarion/style.css';
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
