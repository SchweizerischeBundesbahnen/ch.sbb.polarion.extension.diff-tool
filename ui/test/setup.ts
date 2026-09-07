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

// The visual suites gate themselves on `__PIXEL_REFERENCES__` (see vitest.config.ts), and the gate reads
// `!__PIXEL_REFERENCES__`. A `define` that substitutes the string "false" rather than the boolean makes
// that expression false whatever the flag says, so every visual suite runs outside the pinned Playwright
// image and fails on the host's font metrics. Vitest 5 changed the substitution once already, so assert
// the type here instead of trusting it.
if (typeof __PIXEL_REFERENCES__ !== 'boolean') {
  throw new Error(
    `__PIXEL_REFERENCES__ must be a boolean, got ${typeof __PIXEL_REFERENCES__}. ` +
      'Check the `define` in vitest.config.ts.',
  );
}

// Transitions and animations are off for every capture. A screenshot taken mid-fade is a reference that
// only sometimes reproduces, and the durations are react-sbb-polarion's, which can change them without
// this repository noticing. Killing them removes the race instead of outrunning it with a sleep.
//
// Grayscale antialiasing is NOT pinned here: `-webkit-font-smoothing` is implemented only on macOS in
// Blink, so on the Linux container the rule parses and is ignored - a reference captured with it is
// byte-identical to one captured without. `--disable-lcd-text` in vitest.config.ts is the platform
// independent way to ask for the same thing.
const STILLNESS = '*, *::before, *::after { transition: none !important; animation: none !important; }';

const stillness = document.createElement('style');
stillness.textContent = STILLNESS;
document.head.appendChild(stillness);

// A shadow root sees none of the document's rules, and both Document Properties panels are mounted in one
// (src/formext/shadowMount.ts). Their controls are react-sbb-polarion's, which transitions `border-color`
// and `box-shadow` over 150ms, so a value read or captured right after a class changed is the value it is
// fading FROM. That is what this plants in every shadow root as it is created.
//
// An adopted stylesheet rather than a `<style>` child, because `mountInShadow` calls `replaceChildren()`
// on the root it is given - which would sweep a child straight out again - and because adopted sheets are
// applied after the root's own, so nothing in the root can outrank this.
const stillnessSheet = new CSSStyleSheet();
stillnessSheet.replaceSync(STILLNESS);
const attachShadow = Element.prototype.attachShadow;
Element.prototype.attachShadow = function attachStillShadow(init: ShadowRootInit): ShadowRoot {
  const root = attachShadow.call(this, init);
  root.adoptedStyleSheets = [...root.adoptedStyleSheets, stillnessSheet];
  return root;
};
