import { copyFileSync, readFileSync } from 'node:fs';
import { createRequire } from 'node:module';
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
        if (/^\/(documents|collections|workitems|topics)$/.test(pathname)) {
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

// react-sbb-polarion's BreadcrumbInjector loads breadcrumb-bridge.js from next to the running page. It
// runs in the Polarion shell window rather than in this app's frame, so it stays a classic script and
// cannot be bundled - it is copied next to the built entries instead. See "Shell scripts" in the
// library's README.
function copyRspShellScripts() {
  return {
    name: 'copy-rsp-shell-scripts',
    // `vite dev` serves nothing out of the build output, so without this the same request 404s and the
    // breadcrumb just never appears - silently, since the injector treats the shell as optional chrome.
    configureServer(server) {
      const require = createRequire(import.meta.url);
      server.middlewares.use('/breadcrumb-bridge.js', (_req, res) => {
        res.setHeader('Content-Type', 'text/javascript');
        res.end(readFileSync(require.resolve('@sbb-polarion/react-sbb-polarion/breadcrumb-bridge.js')));
      });
    },
    writeBundle(options) {
      const require = createRequire(import.meta.url);
      copyFileSync(
        require.resolve('@sbb-polarion/react-sbb-polarion/breadcrumb-bridge.js'),
        `${options.dir}/breadcrumb-bridge.js`,
      );
    },
  };
}

  const shared = {
    plugins: [react(), copyRspShellScripts()],
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
    // contract - src/formext/openDocumentsDiff.ts and src/topics/open{WorkItems,Collections}Diff.ts open
    // them by literal URL. Do not rename them.
    // index.html is the admin feature router, opened by the extenders in META-INF/hivemodule.xml with
    // ?feature=<id>; topics.html is the same arrangement for the three navigation topics, opened with
    // ?topic=<id> by the getPageUrl() of the nodes in ch.sbb.polarion.extension.diff_tool.navigation.
    build: {
      rollupOptions: {
        input: {
          index: resolvePath('./index.html'),
          topics: resolvePath('./topics.html'),
          documents: resolvePath('./documents.html'),
          collections: resolvePath('./collections.html'),
          workitems: resolvePath('./workitems.html'),
        },
      },
    },
  };

  if (command === 'serve') {
    // Everything the pages load from Polarion itself: REST, the wiki
    // skin stylesheet, and the icon/font assets referenced from CSS. Replaces the
    // NEXT_PUBLIC_BASE_URL prefixing useRemote used to do, so requests stay same-origin with no CORS.
    const polarionProxy = {
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
