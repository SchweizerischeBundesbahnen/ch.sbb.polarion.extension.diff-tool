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
// react-sbb-polarion's style.css joins this list when the admin pages land.
import '@testing-library/jest-dom/vitest';
import 'bootstrap/dist/css/bootstrap.css';
import '../src/styles/globals.css';
