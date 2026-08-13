import 'bootstrap/dist/css/bootstrap.css';
import '@/styles/globals.css';

import {createRoot} from "react-dom/client";
import AppShell from "@/components/AppShell";
import PublicShell from "@/components/PublicShell";
import ErrorBoundary from "@/components/ErrorBoundary";
import DocumentsPage from "@/pages/DocumentsPage";

// Nesting mirrors the Next.js tree this replaces: root layout (context provider) > page frame with
// its Suspense fallback > the route segment's error boundary > the page.
createRoot(document.getElementById('root')).render(
    <AppShell>
      <PublicShell>
        <ErrorBoundary>
          <DocumentsPage/>
        </ErrorBoundary>
      </PublicShell>
    </AppShell>
);
