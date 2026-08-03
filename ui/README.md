# diff-tool UI

The React front end of the Polarion diff-tool extension: a [Vite](https://vite.dev/) **multi-page**
app, one HTML entry per Polarion entry point.

| Entry | Served at | Opened by |
|---|---|---|
| `index.html` | `/polarion/diff-tool-app/ui/app/index.html?feature=<id>` | the admin extenders in `META-INF/hivemodule.xml` |
| `documents.html` | `/polarion/diff-tool-app/ui/app/documents.html` | `src/formext/openDocumentsDiff.ts` |
| `collections.html` | `/polarion/diff-tool-app/ui/app/collections.html` | `webapp/diff-tool/js/diff-tool-widget-utils.js` |
| `workitems.html` | `/polarion/diff-tool-app/ui/app/workitems.html` | `webapp/diff-tool/js/diff-tool-widget-utils.js` |

Plus a second, library-mode build for the two Document Properties side panels:

| Module | Served at | Imported by |
|---|---|---|
| `assets/diffToolPanel.js` | `/polarion/diff-tool-app/ui/app/assets/diffToolPanel.js` | `webapp/diff-tool/html/diff-tool.html` |
| `assets/copyToolPanel.js` | `/polarion/diff-tool-app/ui/app/assets/copyToolPanel.js` | `webapp/diff-tool/html/copy-tool.html` |

The three viewer filenames are a **public contract** - the callers above, `diff-tool-widget-utils.js`
and the Java widget renderers' inline handlers open them by literal URL, and pass extra state through
`localStorage`. Do not rename them.

## Three surfaces

**Admin pages** (`index.html`) are built on the shared
[`react-sbb-polarion`](https://github.com/grigoriev/react-sbb-polarion) library (RSP), like every other
migrated SBB Polarion extension: `PageLayout`, `About`, `ConfigurationButtons`, `RevisionsTable`,
`Toaster` and the generic control CSS all come from it. The page is chosen by `?feature=<id>`, where the
ids match the extender ids in `META-INF/hivemodule.xml` - see `src/features.tsx`. Root classes are
`<body class="sbb-ui">` plus `<div class="app standard-admin-page">`; both are a cross-extension
contract, not a local choice.

**The diff/merge viewer** (the other three entries) predates RSP and does not use it. Its page shell is
`.diff-app`, deliberately not `.app`, because RSP claims `.app` for the admin shell. Vite emits CSS per
entry, so the two never share a stylesheet at runtime - but they do share the document under Vitest.

**The Document Properties panels** (`src/formext/`) are not part of the SPA at all. They are built by
`vite.formext.config.js` in **library mode**, which is what guarantees the fixed filenames and the
preserved named exports (`mountDiffToolPanel` / `mountCopyToolPanel`) the server-rendered fragments call.
`emptyOutDir: false`, so this build must run *after* the SPA build - hence the two-step `npm run build`.

Each panel mounts into a **shadow root** on the fragment's div (`src/formext/shadowMount.ts`). The
Document Properties pane is one page shared by several extensions' panels - and by both of these - so
plain global CSS would collide. RSP's stylesheet and `src/formext/diff-tool.css` are injected *inside*
the shadow (via `?inline`), which also means nothing has to be `<link>`ed from a Polarion-served URL and
the panels can be tested with `ui/` alone.

Their data is **server-injected**, not fetched: `BaseFormExtension` puts the source document's identity,
the projects, link roles, configuration names and referenced-workitem behaviours into a single
`data-props` attribute as HTML-escaped JSON, which `src/formext/panelProps.ts` parses. The fragment is
rendered on every document open, so fetching those lists would cost three round-trips on a pane the user
may never expand, and two of them have no endpoint.

`src/formext/openDocumentsDiff.ts` holds the handoff to `documents.html` - query string plus the
`<uuid>_additionalParams` localStorage entry. It is a verbatim port and must not drift; the viewer's
reading half of it (and of the widget's `?ids=`) is pinned by `test/widgetHandoff.test.tsx`.

## Getting started

```bash
npm install
npm run dev
```

To drive the app against a real Polarion, copy `.env.development.template` to
`.env.development.local` and fill in your user token. `VITE_BASE_URL` is only the dev-server proxy
target (see `vite.config.js`) - the app itself always issues same-origin requests. With
`VITE_BEARER_TOKEN` set, `useRemote` targets the token-authenticated `/rest/api` endpoints instead of
the session-authenticated `/rest/internal` ones.

Then open one of the pages with the parameters it expects, e.g.
<http://localhost:3000/documents?sourceProjectId=...>. Bare paths without the `.html` suffix work in
dev too (see the `extensionlessHtml` plugin in `vite.config.js`).

## Scripts

| Script | |
|---|---|
| `npm run dev` | dev server on port 3000, Polarion requests proxied to `VITE_BASE_URL` |
| `npm run dev:e2e` | dev server as the E2E suite runs it: loads `.env.e2e`, no proxy |
| `npm run build` | production build to `dist/app`, both Vite passes (copied into the extension jar by Maven) |
| `npm run typecheck` | `tsc --noEmit` |
| `npm run lint` / `lint:fix` | ESLint |
| `npm run format` / `format:check` | Prettier (`**/*.{ts,tsx,css,html}`) |
| `npm run e2e` | Playwright E2E suite (interactive) |
| `npm run e2e:headless` | Playwright E2E suite (list reporter) |

Maven runs **both** JS suites in the `test` phase - Vitest (dockerized, with the coverage gate) and then
the Playwright E2E suite, as it did before the React migration. The browser binaries are not downloaded
by the build, so run `npx playwright install` once in `ui/` (or build with `-DinstallPlaywright`, or
`-DskipJsE2eTests=true` to leave the suite out).

## Testing

Two layers, deliberately:

- **`test/`** - Vitest in **browser mode** (real Chromium via Playwright), the same setup as
  `react-sbb-polarion` and the other migrated extensions. Components render against real CSS and
  layout; REST is mocked at the global `fetch` boundary (`test/mockFetch.ts`), so no Polarion is
  needed. Visual references live in `test/expected/<Component>/` and **must** be generated inside the
  pinned Playwright Docker image (`npm run test:update:docker`) so any dev machine and Linux CI
  produce identical pixels.
- **`e2e/`** - the Playwright end-to-end suite for the diff/merge viewer: 11 specs across
  chromium/firefox/webkit, driving the real dev server with every REST call stubbed from
  `e2e/fixtures/`.

| Script | |
|---|---|
| `npm run test` | Vitest, host browser; the visual suites skip themselves outside the reference image |
| `npm run test:watch` | Vitest in watch mode |
| `npm run test:docker` | Vitest in the pinned Playwright image (authoritative for visuals) |
| `npm run test:update:docker` | regenerate visual references (Docker only) |
| `npm run test:coverage` | behaviour-only coverage + the 80% gate |
| `npm run test:coverage:full` | full suite coverage |
| `npm run test:coverage:docker` | **the canonical run** - the full suite + the gate in the image, what Maven's `test` phase invokes |

**Pixel references are Docker-only.** The committed screenshots are locked to the pinned Playwright
image, so `scripts/docker-test.mjs` marks that container with `PIXEL_REFERENCES=1`, `vitest.config.ts`
turns it into the `__PIXEL_REFERENCES__` constant, and every visual suite is
`describe.skipIf(!__PIXEL_REFERENCES__)`. Off Docker they skip instead of failing on the host's font
metrics - which shift both the antialiasing and the rendered element height, i.e. a red run that says
nothing about the code. Inside the image every screenshot is still enforced.

**Coverage gate.** 80% on statements/branches/functions/lines over `src/**` minus the annotated
`coverage.exclude` list in `vitest.config.ts`. The diff/merge viewer is deliberately on that list: it
came over unchanged from the Next.js app and its regression net is `e2e/`. **A new file is gated unless
someone excludes it there**, and viewer entries leave the list as those files are converted to
TypeScript.

Maven runs the Vitest suite (dockerized) in the `test` phase. Useful flags:

| Flag | |
|---|---|
| `-DskipJsTests=true` | skip both JS suites. CI does NOT skip Vitest: it runs it in the Maven build |
| `-DskipJsE2eTests=true` | skip only the Playwright suite - what CI's Maven build passes, since its `e2e` job runs it per browser |
| `-DjsTestsNoDocker` | run Vitest directly instead of in the image |
| `-DskipVisualJsTests` | keep behaviour tests, drop the pixel comparisons |
| `-DinstallPlaywright` | download the browser binaries (and OS deps) |

## Layout

```
documents.html collections.html workitems.html   HTML entries
src/entries/     one module per HTML entry: mounts the React tree
src/pages/       the page component behind each entry
src/components/  the diff/merge UI
src/services/    REST access (useRemote), diff/merge orchestration, PDF export
src/router/      navigation.ts - the useSearchParams/usePathname/useRouter shim
src/admin/       the RSP admin pages (pages/, components/, dev/ scaffolding)
src/formext/     the two Document Properties panels + their library-mode entry points
src/features.tsx the ?feature=<id> registry, ids matching hivemodule.xml
src/styles/      globals.css
test/            Vitest component + visual tests
e2e/             Playwright specs + JSON fixtures
```

New code is written in TypeScript. The diff/merge viewer is still plain JS (`allowJs` is on,
`checkJs` is off) and is covered end-to-end by the Playwright suite in `e2e/`; files come under type
checking as they are converted.
