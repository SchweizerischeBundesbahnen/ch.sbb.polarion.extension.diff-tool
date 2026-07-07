import 'bootstrap/dist/css/bootstrap.css';
import "./globals.css";
import {Suspense} from 'react';
import Body from "@/components/Body";
import Loading from "@/components/loading/Loading";

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <head>
        <link rel="stylesheet" href="/polarion/wiki/skins/sidecar/presentation.css" />
        {/* Shared 2606 control design tokens from generic (served by GenericUiServlet from the
            embedded generic.app). globals.css restyles the Bootstrap controls to the Polarion look
            via var(--sbb-*); a restyle then happens once in generic. Literal fallbacks keep the
            look if this file fails to load (e.g. vite/next dev outside Polarion). */}
        <link rel="stylesheet" href="/polarion/diff-tool-app/ui/generic/css/control-tokens.css" />
        {/* The shared SearchableDropdown component (wrapped around the native <select>s in
            SearchableSelect) renders with these classes; load them so the combos match every
            other extension's combobox exactly. */}
        <link rel="stylesheet" href="/polarion/diff-tool-app/ui/generic/css/searchable-dropdown.css" />
      </head>
      <Body>
        <main className="app">
          <Suspense fallback={<Loading/>}>
            {children}
          </Suspense>
        </main>
      </Body>
    </html>
);
}
