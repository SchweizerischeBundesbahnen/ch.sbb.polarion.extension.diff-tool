import 'bootstrap/dist/css/bootstrap.css';
import '@/styles/globals.css';

import {createRoot} from "react-dom/client";
import AppShell from "@/components/AppShell";
import PublicShell from "@/components/PublicShell";
import ErrorBoundary from "@/components/ErrorBoundary";
import WorkItemsPage from "@/pages/WorkItemsPage";

createRoot(document.getElementById('root')).render(
    <AppShell>
      <PublicShell>
        <ErrorBoundary>
          <WorkItemsPage/>
        </ErrorBoundary>
      </PublicShell>
    </AppShell>
);
