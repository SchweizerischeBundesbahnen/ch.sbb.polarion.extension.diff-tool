// Runs before every test file (see vitest.config.ts setupFiles).
//
// Loads the stylesheets the app actually renders with, so the browser paints components realistically
// and visual references match runtime:
//   1. Bootstrap, which the viewer's markup is built on.
//   2. this app's own globals.css (design-token bridging, layout, the restyled Bootstrap controls).
// The Polarion-served stylesheets linked from the HTML entries (presentation.css and generic's
// control-tokens.css / searchable-dropdown.css) are NOT bundled and are deliberately not loaded here -
// they are baseline chrome resolved at runtime by GenericUiServlet. globals.css carries literal
// fallbacks for the --sbb-* tokens, which is exactly what the browser sees when those links fail.
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
