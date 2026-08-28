// Runs before every test file (see vitest.config.ts setupFiles).
//
// Loads the stylesheets the app actually renders with, so the browser paints components realistically
// and visual references match runtime:
//   1. Bootstrap, which the viewer's markup is built on.
//   2. this app's own globals.css (design-token bridging, layout, the restyled Bootstrap controls).
// The one Polarion-served stylesheet the HTML entries still link, presentation.css, is NOT bundled and
// is deliberately not loaded here - it is baseline chrome resolved at runtime. The --sbb-* tokens no
// longer depend on such a link: react-sbb-polarion's bundled style.css carries them, and item 3 below
// loads it, so the browser sees the same token values here as at runtime.
//
//   3. react-sbb-polarion's bundled control CSS (tokens + buttons/inputs/checkboxes/alerts + its own
//      component styles), the same import main.tsx uses, plus this app's App.css for the admin pages.
//
// Loading the viewer's globals.css and RSP's stylesheet into one document is only safe because the
// viewer's page shell is `.diff-app`, not `.app`: RSP claims `.app` for the admin shell.
import '@sbb-polarion/react-sbb-polarion/style.css';
import '@testing-library/jest-dom/vitest';
import 'bootstrap/dist/css/bootstrap.css';
import '../src/App.css';
import '../src/styles/globals.css';
//   4. the navigation topics' own stylesheet, which src/entries/topics.tsx imports. Safe alongside the
//      other two: every rule is scoped under .diff-topics.
import '../src/topics/topics.css';

// Chromium decides per layer how to rasterize text, and the decision depends on the compositing of the
// page as a whole - which differs between "this file ran on its own" and "this file ran after that one".
// The result is the same glyphs at the same coordinates with a different gamma, and a reference that
// agrees with the runs that had the same files ahead of it and with no others. Asking for grayscale
// explicitly takes the decision away from the compositor.
//
// Test-only, and the references are regenerated with it so they and the runs agree.
const textRendering = document.createElement('style');
textRendering.textContent = '*, *::before, *::after { -webkit-font-smoothing: antialiased !important; }';
document.head.appendChild(textRendering);
