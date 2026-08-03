import { fileURLToPath, URL } from 'node:url';
import react from '@vitejs/plugin-react';
import { defineConfig, loadEnv } from 'vite';

const resolvePath = (relativePath) => fileURLToPath(new URL(relativePath, import.meta.url));

/**
 * Dev-only: keep the extensionless URLs (/documents?..., /collections?..., /workitems?...) working.
 *
 * In Polarion these pages are always addressed with their .html suffix
 * (/polarion/diff-tool-app/ui/app/documents.html), which is what the built bundle serves. The
 * Playwright suite, however, drives the dev server on bare paths - as it did under `next dev`, where
 * the App Router owned /documents. Rewriting here keeps all 11 specs' navigation unchanged.
 *
 * Registered inside the configureServer hook body (not in a returned post-hook) so it runs BEFORE
 * vite's own html-serving middleware.
 */
const extensionlessHtml = () => ({
  name: 'diff-tool:extensionless-html',
  configureServer(server) {
    server.middlewares.use((req, _res, next) => {
      if (req.url) {
        const [pathname, query] = req.url.split('?');
        if (/^\/(documents|collections|workitems)$/.test(pathname)) {
          req.url = query === undefined ? `${pathname}.html` : `${pathname}.html?${query}`;
        }
      }
      next();
    });
  },
});

export default defineConfig(({ command, mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const polarionUrl = env.VITE_BASE_URL || 'http://localhost';

  const shared = {
    plugins: [react()],
    resolve: {
      alias: { '@': resolvePath('./src') },
      // The app and (from stage 4 on) the linked react-sbb-polarion package must resolve to a single
      // React instance, otherwise hooks fail with the dual-React "invalid hook call".
      // sonner for the same reason, and it fails more quietly: the pages call toast() from their own
      // import while <Toaster/> comes from react-sbb-polarion, so two instances mean the toast is queued
      // in one store and rendered from the other. Notifications just stop appearing, with nothing in the
      // console. Mirrors vitest.config.ts.
      dedupe: ['react', 'react-dom', 'sonner'],
    },
    // Multi-page: one HTML entry per Polarion entry point. The three viewer filenames are a public
    // contract - webapp/diff-tool/js/modules/DiffTool.js and js/diff-tool-widget-utils.js open them by
    // literal URL, and so do the Java widget renderers' inline handlers. Do not rename them.
    // index.html is the admin feature router, opened by the extenders in META-INF/hivemodule.xml with
    // ?feature=<id>.
    build: {
      rollupOptions: {
        input: {
          index: resolvePath('./index.html'),
          documents: resolvePath('./documents.html'),
          collections: resolvePath('./collections.html'),
          workitems: resolvePath('./workitems.html'),
        },
      },
    },
  };

  if (command === 'serve') {
    // Everything the pages load from Polarion itself: REST, the generic UI toolkit CSS/JS, the wiki
    // skin stylesheet, and the icon/font assets referenced from CSS. Replaces the
    // NEXT_PUBLIC_BASE_URL prefixing useRemote used to do, so requests stay same-origin with no CORS.
    const polarionProxy = {
      '/polarion/diff-tool-app/ui/generic': { target: polarionUrl, changeOrigin: true },
      '/polarion/diff-tool/rest': { target: polarionUrl, changeOrigin: true },
      '/polarion/diff-tool/ui': { target: polarionUrl, changeOrigin: true },
      '/polarion/wiki': { target: polarionUrl, changeOrigin: true },
      '/polarion/ria': { target: polarionUrl, changeOrigin: true },
      '/polarion/icons': { target: polarionUrl, changeOrigin: true },
    };

    return {
      ...shared,
      plugins: [...shared.plugins, extensionlessHtml()],
      // No SPA history fallback: every page is a real .html entry, and a fallback would mask a
      // genuine 404 as a blank page.
      appType: 'mpa',
      server: {
        // The Playwright config's baseURL and webServer.port both assume 3000 (vite defaults to
        // 5173). Fail loudly rather than silently drifting to another port.
        port: 3000,
        strictPort: true,
        // `--mode e2e` runs with no Polarion behind it - the Playwright suite mocks every REST call
        // at the page level - so proxying would only produce ECONNREFUSED noise. Mirrors the old
        // next.config.mjs, which returned no rewrites when PLAYWRIGHT_TESTS was set.
        proxy: mode === 'e2e' ? undefined : polarionProxy,
      },
    };
  }

  return {
    ...shared,
    // Never let a developer's personal access token reach a shipped bundle. VITE_BEARER_TOKEN is a
    // `vite dev` convenience (it switches useRemote to the token-authenticated /api endpoints so the app
    // can talk to a real Polarion without a session); Vite inlines import.meta.env.VITE_* at build time,
    // so a local .env.development.local would otherwise be baked into the bundle that
    // `mvn -P install-to-local-polarion` deploys - readable by everyone the pages are served to.
    // Forcing it undefined here keeps production on the session-authenticated /internal endpoints, which
    // is what Polarion provides anyway. Only in this branch: `serve` must keep honouring it.
    define: { 'import.meta.env.VITE_BEARER_TOKEN': 'undefined' },
    // Must match the URL space DiffToolAppUIServlet serves (webapp/diff-tool-app, servlet on /ui/*).
    base: '/polarion/diff-tool-app/ui/app/',
    build: {
      ...shared.build,
      outDir: './dist/app',
      emptyOutDir: true,
      // GenericUiServlet's extension allowlist has no .map - a sourcemap would 404. Keep this off.
      sourcemap: false,
    },
  };
});
