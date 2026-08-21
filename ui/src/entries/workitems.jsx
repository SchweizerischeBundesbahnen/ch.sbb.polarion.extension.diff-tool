import 'bootstrap/dist/css/bootstrap.css';
// The shared control styling, including the combobox and the --sbb-* tokens it reads: what these pages
// used to link from the generic webapp. Before globals.css, which overrides parts of it.
import '@sbb-polarion/react-sbb-polarion/style.css';
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
