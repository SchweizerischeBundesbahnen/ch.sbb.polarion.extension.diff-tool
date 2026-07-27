import { Suspense } from 'react';
import Landing from './admin/dev/Landing';
import { findFeature } from './features';

/**
 * Feature router for the admin pages: one bundle, one index.html, the page chosen by `?feature=<id>`
 * (see features.tsx and the extender URLs in META-INF/hivemodule.xml). An unknown or missing feature
 * falls back to the dev Landing overview.
 *
 * The two root classes are the shared contract across every RSP extension and must not diverge:
 * `.app` carries the page padding and font from react-sbb-polarion's PageLayout.css, and
 * `.standard-admin-page` is what scopes the styled admin checkbox - `.sbb-ui` alone (on <body>)
 * defines the --sbb-* tokens but not that styling.
 */
export default function App() {
  const feature = findFeature(new URLSearchParams(window.location.search).get('feature'));
  const Page = feature?.component ?? Landing;

  return (
    <div className="app standard-admin-page">
      {/* Execution Queue is lazily loaded so Chart.js stays out of the other pages' chunk. */}
      <Suspense fallback={<div className="page-loading">Loading...</div>}>
        <Page />
      </Suspense>
    </div>
  );
}
