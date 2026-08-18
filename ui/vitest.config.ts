import react from '@vitejs/plugin-react';
import { playwright } from '@vitest/browser-playwright';
import { defineConfig } from 'vitest/config';

// Vitest browser mode (real Chromium via Playwright), the same setup as react-sbb-polarion and the
// other migrated extensions: behavior assertions see real CSS/layout, and the visual layer
// (toMatchScreenshot) captures the real look. REST is mocked at the global fetch boundary (see
// test/mockFetch.ts), so no Polarion is needed. Reference screenshots are committed and MUST be
// generated in the pinned Playwright Docker image (npm run test:update:docker) so any dev machine and
// Linux CI produce identical pixels.
//
// This is the component/unit layer. The diff/merge viewer keeps its own end-to-end layer in e2e/
// (Playwright, 3 browsers) - see the coverage note below.

// Per-component subfolder derived from the test file name (e.g. "About.visual.test.tsx" -> "About").
const componentDir = (testFileName: string): string => testFileName.split(/[\\/]/).pop()!.split('.')[0];

// The committed reference screenshots are pixel-locked to the pinned Playwright image, so the visual
// assertions are only meaningful there. scripts/docker-test.mjs sets PIXEL_REFERENCES=1 inside the
// container; everywhere else (a developer's macOS/Windows box, a plain CI runner) the visual suites skip
// themselves rather than failing on the host's font metrics - which shift both the antialiasing and the
// rendered element height, i.e. a red run that says nothing about the code. Without this,
// `npm run test:coverage:full` was unusable off Docker, which is also what -DjsTestsNoDocker runs.
const pixelReferences = process.env.PIXEL_REFERENCES === '1';

export default defineConfig({
  define: { __PIXEL_REFERENCES__: JSON.stringify(pixelReferences) },
  plugins: [react()],
  resolve: {
    alias: { '@': new URL('./src', import.meta.url).pathname },
    dedupe: ['react', 'react-dom', 'sonner'],
  },
  // Pre-bundle these so Vite does not discover a new dependency mid-run and reload the browser page
  // (which intermittently fails a test file with "Vitest failed to find the runner"). Matters most on
  // a fresh `npm ci` in Docker where there is no warm dep-optimize cache.
  optimizeDeps: {
    include: [
      'react',
      'react-dom',
      'react-dom/client',
      'react/jsx-runtime',
      'react/jsx-dev-runtime',
      'vitest-browser-react',
      '@sbb-polarion/react-sbb-polarion',
      'sonner',
      'chart.js',
      'chartjs-adapter-date-fns',
      'chartjs-plugin-zoom',
    ],
  },
  test: {
    include: ['test/**/*.{test,spec}.{ts,tsx}'],
    setupFiles: ['./test/setup.ts'],
    // Run test files one at a time. Under high parallelism the Playwright browser provider
    // intermittently fails a worker with "Vitest failed to find the runner"; serializing the files
    // avoids that race. The suite is small and each file is fast, so the cost is minor.
    fileParallelism: false,
    browser: {
      enabled: true,
      // deviceScaleFactor: 2 -> visual-regression references are captured at 2x (sharper, finer diffs).
      // Set on the provider's contextOptions (not the instance - the provider reads it there).
      provider: playwright({ contextOptions: { deviceScaleFactor: 2 } }),
      headless: true,
      instances: [{ browser: 'chromium', viewport: { width: 1280, height: 720 } }],
      expect: {
        toMatchScreenshot: {
          resolveScreenshotPath: ({ root, arg, ext, testFileName }) =>
            `${root}/test/expected/${componentDir(testFileName)}/${arg}${ext}`,
          resolveDiffPath: ({ root, arg, ext, testFileName }) =>
            `${root}/test/__diff__/${componentDir(testFileName)}/${arg}${ext}`,
        },
      },
    },
    coverage: {
      // istanbul (source instrumented at transform time), NOT v8: in browser mode v8 intermittently
      // reports 0% depending on the dep-optimization cache. `all: false` so the istanbul
      // uncovered-files pass (which can crash in browser mode) never runs.
      provider: 'istanbul',
      reporter: ['text', 'html', 'lcov'],
      reportsDirectory: './coverage',
      all: false,
      // include: ['src/**'] is the shared gate. What deviates here is the exclude list.
      //
      // The diff/merge viewer (src/pages/**, all of src/components/** except the three shells,
      // src/services/useDiffService.js and its siblings, src/utils/**, src/useAppContext.js,
      // src/DiffTypes.js) came over unchanged from the Next.js app when the bundler was swapped, and is
      // covered end-to-end by the 11 Playwright specs in e2e/ across three browsers, CI-gated. Reaching
      // 80% branches on it with Vitest would be weeks of work that duplicates assertions e2e/ already
      // makes, and it would have blocked this gate from existing at all. So the 80% bar applies to
      // everything this migration authors, i.e. src/** minus the exclusions below.
      //
      // Deliberately an exclude list rather than the include allowlist this started as: a new file is
      // now gated unless someone takes it out explicitly, where before new authored code that nobody
      // remembered to list was silently ungated. Drop viewer entries from the list as those files are
      // converted to TypeScript.
      //
      // One consequence remains: with all:false a gated file only counts once a test imports it, so a
      // gated-but-untested file does not fail the gate. (all:true is not an option - see the istanbul
      // note above.)
      include: ['src/**'],
      exclude: [
        'src/**/*.d.ts',
        'src/**/*.css',
        'src/**/*.svg',
        // Per-page bootstrap (the equivalent of main.tsx in the sibling extensions): createRoot plus
        // nesting the shells, all of which are covered directly.
        'src/entries/**',
        'src/main.tsx',
        // Dev-only scaffolding, never opened inside Polarion (which always passes ?feature=).
        'src/admin/dev/**',
        // The viewer, gated by e2e/ instead - see the note above. AppShell, PublicShell and
        // ErrorBoundary are the exception: the admin pages and the panels mount them too, so they stay
        // under the gate and every other file in src/components/ is named here.
        'src/pages/**',
        'src/utils/**',
        'src/components/*/**',
        'src/components/AppContext.js',
        'src/components/{AppAlert,ControlPane,ErrorsOverlay,ExtensionInfo}.jsx',
        'src/components/{FloatingButton,Modal,PathPart,SearchableSelect,WorkItemHeader}.jsx',
        'src/services/{useDiffService,usePdf,useSessionRenewal}.js',
        'src/useAppContext.js',
        'src/DiffTypes.js',
      ],
      thresholds: {
        statements: 80,
        functions: 80,
        lines: 80,
        branches: 80,
      },
    },
  },
});
