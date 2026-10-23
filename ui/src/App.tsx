import { Suspense } from 'react';
import {
  DocLinkInterceptor,
  type DocSearchRecord,
  DocsProvider,
  FeatureRouter,
  buildDocsConfig,
} from '@sbb-polarion/react-sbb-polarion';
import Landing from './admin/dev/Landing';
import { DOC_ORDER } from './docs/manifest';
import searchIndex from './docs/search-index.json';
import { SOURCE_BASE_URL } from './docs/source';
import { FEATURES } from './features';
import { switchToFeatureNode } from './services/adminNav';

/**
 * Documentation-site configuration for the shared components (@sbb-polarion/react-sbb-polarion): the article
 * manifest, the build-generated search index, and the admin-shell sync a cross-document link triggers. The
 * help articles cross-link with plain relative markdown links so they work as-is on GitHub; the build leaves
 * them relative, and DocLinkInterceptor resolves a click at runtime - a `.md` source through `mdLinkMap` to a
 * `?feature=` switch, and any other relative link (docs/openapi.json) to `sourceBaseUrl` on GitHub.
 * `onDocLinkNavigate` prefers switching the admin shell's node (so Polarion's breadcrumb and left menu
 * follow); it returns false when it cannot, and the interceptor then falls back to a plain in-frame navigation.
 */
const docsConfig = buildDocsConfig({
  docs: DOC_ORDER,
  searchIndex: searchIndex as DocSearchRecord[],
  sourceBaseUrl: SOURCE_BASE_URL,
  // The README is not a doc-site article but renders as the About page, so a relative `README.md` link in an
  // article switches to it. buildDocsConfig folds this together with each article's own source (from the
  // manifest) into the mdLinkMap the interceptor uses to turn relative `.md` cross-links into feature switches.
  extraMdLinks: {
    'README.md': 'about',
  },
  // The breadcrumb keeps its defaults: "Documentation", linking to the first article of the manifest (Quick
  // Start), which is also the page the documentation admin node opens.
  onDocLinkNavigate: switchToFeatureNode,
});

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
  return (
    <DocsProvider config={docsConfig}>
      {/* Delegates cross-document help-article links to in-app feature navigation (see docsConfig). */}
      <DocLinkInterceptor>
        <div className="app standard-admin-page">
          {/* Execution Queue is lazily loaded so Chart.js stays out of the other pages' chunk. */}
          <Suspense fallback={<div className="page-loading">Loading...</div>}>
            <FeatureRouter features={FEATURES} fallback={Landing} />
          </Suspense>
        </div>
      </DocLinkInterceptor>
    </DocsProvider>
  );
}
