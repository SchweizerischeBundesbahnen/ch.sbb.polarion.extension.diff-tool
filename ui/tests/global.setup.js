import { test as setup } from '@playwright/test';

/*
 * Warm up the Next.js dev server routes before the real tests run.
 *
 * In dev mode Next.js compiles each route on its first request. Under CI
 * (2 parallel workers × 3 browsers against a single `npm run dev` server) the
 * first test to hit a heavy route - e.g. `/documents` - can lose the race
 * against the per-test timeout while the route is still compiling, which showed
 * up as flaky WebKit timeouts waiting for `.header .merge-pane` to become
 * visible. Visiting each route once here pays that compilation cost up front so
 * the actual tests always run against an already-compiled server.
 *
 * This project is a dependency of the browser projects (see playwright.config.js),
 * and the webServer is guaranteed to be up before it runs.
 */
const routesToWarmUp = ['/documents', '/collections', '/workitems'];

for (const route of routesToWarmUp) {
  setup(`warm up ${route}`, async ({ page }) => {
    // Override the suite's per-test timeout (60s CI / 30s local) so the warmup
    // can actually absorb a slow cold route compile up to the goto budget below.
    setup.setTimeout(120_000);
    try {
      // `load` ensures the client bundle is fetched (and thus compiled), not just the HTML shell.
      await page.goto(route, { waitUntil: 'load', timeout: 120_000 });
    } catch {
      // App-level errors (missing query params / unmocked APIs) are irrelevant here -
      // route compilation, the thing we want to warm, has already happened by this point.
    }
  });
}
