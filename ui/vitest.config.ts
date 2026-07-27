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

export default defineConfig({
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
      '@grigoriev/react-sbb-polarion',
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
      // DELIBERATE DEVIATION from the shared gate, which uses include: ['src/**'].
      //
      // The diff/merge viewer (src/components/**, src/pages/**, src/services/useDiffService.js,
      // src/utils/**, src/useAppContext.js) came over unchanged from the Next.js app when the bundler
      // was swapped, and is covered end-to-end by the 11 Playwright specs in e2e/ across three
      // browsers, CI-gated. Reaching 80% branches on it with Vitest would be weeks of work that
      // duplicates assertions e2e/ already makes, and it would have blocked this gate from existing at
      // all. So the 80% bar applies to everything this migration authors, listed explicitly below.
      //
      // Two consequences to keep in mind:
      //  - Files are added to this list as they are written, not discovered. New authored code that is
      //    not listed here is silently ungated.
      //  - With all:false a listed file only counts once a test imports it, so a listed-but-untested
      //    file also does not fail the gate. (all:true is not an option - see the istanbul note above.)
      // Move viewer files under the gate as they are converted to TypeScript.
      include: [
        'src/router/**',
        'src/services/useRemote.ts',
        'src/components/AppShell.jsx',
        'src/components/PublicShell.jsx',
        'src/components/ErrorBoundary.jsx',
        'src/App.tsx',
        'src/features.tsx',
        'src/admin/**',
        'src/services/useSettings.ts',
        // Landing as the remaining surfaces are ported:
        'src/formext/**',
      ],
      // src/entries/** is the per-page bootstrap (the equivalent of main.tsx in the sibling
      // extensions): it only calls createRoot and nests the shells, all of which are covered directly.
      exclude: [
        'src/**/*.d.ts',
        'src/**/*.css',
        // Per-page bootstrap (the equivalent of main.tsx in the sibling extensions): createRoot plus
        // nesting the shells, all of which are covered directly.
        'src/entries/**',
        'src/main.tsx',
        // Dev-only scaffolding, never opened inside Polarion (which always passes ?feature=).
        'src/admin/dev/**',
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
