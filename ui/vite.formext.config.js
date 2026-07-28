import react from '@vitejs/plugin-react';
import { fileURLToPath } from 'node:url';
import { defineConfig } from 'vite';

/**
 * Separate build for the two Document Properties form-extension bundles. Intentionally NOT part of the
 * multi-page SPA build: the server-rendered fragments (webapp/diff-tool/html/diff-tool.html and
 * copy-tool.html) import these by a fixed URL and call a named export, so they must be stable-name ES
 * modules rather than hashed SPA assets. Library mode with two entries guarantees both the fixed names
 * and the preserved exports; React is shared between them via one chunk, which matters because both
 * panels are rendered into the same Document Properties pane.
 *
 * Output: dist/app/assets/{diffToolPanel,copyToolPanel}.js (+ a shared chunk), appended next to the SPA
 * bundle (emptyOutDir: false), so the SPA build - which empties dist/app - MUST run first. See the
 * `build` script in package.json. Served under /polarion/diff-tool-app/ui/app/assets/.
 */
export default defineConfig({
  plugins: [react()],
  // Same public base as the SPA build. Small assets (the compare icon) inline as data URIs, but an asset
  // that ever exceeds the inline limit would be emitted with an absolute URL - which has to be the path
  // Polarion serves, not `/`.
  base: '/polarion/diff-tool-app/ui/app/',
  // Keep a single React instance in the bundle even though react-sbb-polarion ships its own copy.
  resolve: { dedupe: ['react', 'react-dom'] },
  // Force React's production build. Library mode does not substitute process.env.NODE_ENV, so without
  // this React's larger development build (with its runtime warnings) would ship to Polarion.
  define: {
    'process.env.NODE_ENV': JSON.stringify('production'),
  },
  build: {
    outDir: './dist/app/assets',
    emptyOutDir: false,
    // Minifier left at Vite 8's default (oxc), the same as the SPA build. Do not set `minify: 'esbuild'`
    // here: since Vite moved to Rolldown, esbuild is no longer bundled and asking for it fails the build
    // with "Failed to load transformWithEsbuild".
    // GenericUiServlet serves no .map files, so a source map would only 404.
    sourcemap: false,
    lib: {
      // React is bundled in rather than externalized: the fragments load these modules standalone, with
      // no import map and no other script on the page providing React.
      entry: {
        diffToolPanel: fileURLToPath(new URL('./src/formext/mountDiffToolPanel.tsx', import.meta.url)),
        copyToolPanel: fileURLToPath(new URL('./src/formext/mountCopyToolPanel.tsx', import.meta.url)),
      },
      formats: ['es'],
      fileName: (_format, entryName) => `${entryName}.js`,
    },
  },
});
