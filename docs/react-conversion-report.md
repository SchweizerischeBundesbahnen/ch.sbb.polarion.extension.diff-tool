# React / react-sbb-polarion conversion report

What was migrated when diff-tool's admin pages and Document Properties panels moved from JSP + hand
written vanilla JS to React on the shared [`react-sbb-polarion`](https://github.com/grigoriev/react-sbb-polarion)
library (RSP), and the decisions taken along the way. Written for whoever picks up the deferred work
below, or wonders why something here looks different from the other migrated extensions.

## Why this one was different

Every extension migrated before diff-tool (`xml-repair`, `excel-importer`, `strictdoc-exporter`,
`json-editor`, `integrity-scanner`) had *only* legacy admin pages, so the playbook's "add a third
`<ext>-app` webapp context and serve a Vite bundle at `/ui/app/`" dropped straight in.

Here `diff-tool-app` and `/ui/app/` were **already occupied** by a Next.js 16 static export - the
diff/merge viewer, ~4 700 lines across 40 components, reached by three hardcoded URLs plus `localStorage`
handoff contracts. And the two Document Properties panels need Vite **library-mode** bundles with fixed
filenames, which a Next.js static export cannot emit. So the bundler had to change either way.

**Decision: Next.js was replaced by Vite** rather than run alongside it. One npm project, one bundler,
one dependency tree. The viewer's three public URLs (`documents.html`, `collections.html`,
`workitems.html`) are byte-identical; the only file outside `ui/` that the swap touched was
`webapp/diff-tool-app/WEB-INF/web.xml`, where `/ui/app/_next/static/*` became `/ui/app/assets/*`.

## What was migrated

| Surface | From | To |
|---|---|---|
| About | `diff-tool-admin/pages/about.jsp` | RSP `About` (`src/admin/pages/AboutPage.tsx`) |
| Merge Authorization | `pages/authorization.jsp` + `js/modules/authorization.js` | `src/admin/pages/MergeAuthorizationPage.tsx` |
| Diff Configurations | `pages/configuration.jsp` + `js/modules/diff.js` (365 L) | `src/admin/pages/DiffConfigurationsPage.tsx` |
| Execution Queue | `pages/execution.jsp` + `js/modules/execution.js` (406 L) | `src/admin/pages/ExecutionQueuePage.tsx` (lazy-loaded) |
| Project Duplication | `pages/project_duplication.jsp` + `js/modules/project_duplication.js` (225 L) | `src/admin/pages/ProjectDuplicationPage.tsx` |
| Documents Comparison panel | `diff-tool/html/diff-tool.html` (104 L) + `js/modules/DiffTool.js` (327 L) | `src/formext/DiffToolPanel.tsx` |
| Documents Copy panel | `html/copy-tool.html` + `js/modules/CopyTool.js` (141 L) | `src/formext/CopyToolPanel.tsx` |
| Bundler | Next.js 16 static export | Vite 8, multi-page + a second library-mode pass |
| JS test layer | none | Vitest browser mode, 212 tests, 80% coverage gate |

`js/modules/GenericMixin.js` (the shared XHR / overlay / alert helpers behind both panels) became
`src/formext/useRemoteList.ts` + `PanelShell.tsx`.

The four vendored minified chart libraries (`chart.js`, `chartjs-adapter-date-fns`,
`chartjs-plugin-zoom`, `hammer.js` - ~350 kB checked into the repo) became npm dependencies, so they are
bundled by Vite and covered by Renovate.

## Decisions worth knowing

**Server-injected panel props, not fetches.** `BaseFormExtension` still injects the panel's data, but the
five raw-`<option>`-HTML placeholders collapsed into one `{PANEL_PROPS}` `data-props` attribute carrying
escaped JSON (`EscapeChars.forHTMLAttribute(objectMapper.writeValueAsString(props))`). Option-HTML
substitution is impossible once React owns the `<select>` children, and having the panel fetch the
projects / link roles / configuration names instead would add three REST round-trips on **every document
open** - to a pane the user may never expand - plus two endpoints that do not exist. The Java got
smaller: one Jackson call replaced five `String.replace`s, three option-template builders and a quoting
helper.

That also fixed a live injection bug. The old `fillParams()` wrapped each value in double quotes with no
escaping and interpolated the result into a `<link onload='...'>` attribute, so a document title
containing a quote broke out of the attribute and injected script into the Document Properties pane.

**Shadow roots for the panels.** The Document Properties pane is one page shared by several extensions'
panels, each possibly built against a different RSP version - and diff-tool contributes two of them.
Plain CSS is global by selector, so they would collide. Each panel therefore mounts into an open shadow
root (`src/formext/shadowMount.ts`) with RSP's stylesheet and the panel CSS injected inside it. That also
removed every runtime `<link>` to a Polarion-served stylesheet from the fragments, which is what lets the
dockerized test runner mount `ui/` alone. RSP's vendored `SearchableDropdown` is already shadow-aware (it
portals via `getRootNode()` and uses `composedPath()` for its outside-click check).

**`.diff-app`, not `.app`.** RSP's `PageLayout.css` claims `.app { padding: 10px; font-family: ... }`,
which would break the viewer's `.app { height: 100vh }` full-viewport layout. The viewer's shell was
renamed to `.diff-app`.

**A router shim, not react-router.** `src/router/navigation.ts` (~45 lines) reimplements
`useSearchParams`/`usePathname`/`useRouter` over `history` + `useSyncExternalStore`. The app does zero
path routing - the pathname is fixed per HTML entry and all navigation is query-string mutation - and
react-router's `useSearchParams` returns a tuple, so all 11 call sites would have had to change anyway,
for +18 kB instead of +0.4 kB.

**Remembered dropdown choices are now the app's job.** The legacy panels got "remember my last pick"
for free: the generic `SearchableDropdown` wrote every selection to a `searchable_dropdown_<select-id>`
cookie, and `DiffTool.js` / `CopyTool.js` called `restoreSelection()` on the project, link-role,
configuration and referenced-workitems dropdowns while building the view - plus `refresh()`, which
restores too, on the space / document / revision dropdowns once their options had loaded.

RSP's `createSearchableSelect` passes `rememberSelection: false`, and `SearchableSelect` drives the
selection from React's controlled value rather than calling `restoreSelection()`. That is right for the
library - a dropdown re-selecting behind React's back on every `refresh()` would fight the controlled
value - so `src/formext/rememberedSelection.ts` reimplements the persistence in the React layer, reusing
the **same cookie names** so choices remembered before the port are still found. Only user-driven changes
are stored; the cascade resets are not, which is what lets a space survive a project change when the new
project offers the same one.

**Chart.js driven imperatively, not via `react-chartjs-2`.** The existing code mutates
`chart.data.datasets[i].data` and calls `chart.update('none')` on a 3 s poll across 9 charts; a thin
`useChart` hook holding the instance in a ref preserves that exactly.

**`Feature` gained `label` and `description`.** The ten display strings moved out of `execution.js`'s
`featuresLocalization` map into the enum, because adding a `Feature` used to leave the admin page showing
a raw enum name and throwing a `TypeError`.

## Deliberate deviations from the shared playbook

**The coverage gate is scoped, not global.** `reference/ui-testing-and-gates.md` prescribes
`include: ['src/**']`. Here `coverage.include` lists the authored files explicitly - see the annotated
comment in `ui/vitest.config.ts`. The diff/merge viewer came over unchanged from the Next.js app and its
regression net is the 11 Playwright specs in `ui/e2e/` across three browsers; reaching 80% branches on it
with Vitest would be weeks of work duplicating assertions `e2e/` already makes, and it would have blocked
the gate from existing at all. Two consequences to remember: **new authored code must be added to
`coverage.include` explicitly** or it is silently ungated, and with `all: false` a listed-but-untested
file also does not fail the gate.

**`css/common.css` survives.** §9 of the playbook says delete the legacy `css/` folder. This one is
trimmed instead, down to the rules the three nav-topic JSPs and the Java-rendered widgets need - those
pages are deferred (below), and they are the only remaining consumers. It goes away with them.

## Behaviour changes

Almost all of the port is 1:1. The exceptions, each commented at its site:

- **Compare now requires a target document.** The old panel enabled the button as soon as "Enter
  manually" was clicked, which could produce a comparison URL with an empty `targetDocument`.
- **The configuration select starts on the first configuration** in both panels, as the server already
  marked it `selected`. The legacy `restoreSelection()` cleared that again, so the panel could open with
  none chosen and send `&config=`.
- **Project Duplication went onto `useRemote()`.** It used raw `fetch` with no auth handling, so
  `VITE_BEARER_TOKEN` dev mode did not work against it. Its `escapeHtml()` + `innerHTML` string
  templating became JSX, removing a class of injection risk.
- **A production race was fixed on the way past.** `FeatureExecutionTask` now decrements the execution
  counter in a `finally` on the thread that incremented it, instead of on the submitting thread
  (`8207cb3`). This was surfaced by a flaky `ExecutionQueueMonitorTest`, not by the migration itself.

## Webapp contexts

The extension declares **two** contexts in `plugin.xml`, down from three:

- `webapp/diff-tool` - the REST API, the three nav-topic JSPs and the two Document Properties fragments.
- `webapp/diff-tool-app` - the Vite bundle, the two form-extension panel modules, the generated
  `about.html`, and the administration-menu icons.

`webapp/diff-tool-admin` is gone. Once the admin pages became React its only remaining job was serving
those icons, so they moved to the app context and every `iconUrl` in `hivemodule.xml`,
`DiffToolNavigationExtender.getIconUrl()` and `diff-tool.jsp`'s breadcrumb bootstrap followed.
`app-icon.svg` went with it - its only consumer was the deleted `about.jsp`, and RSP's `About` now gets
the icon from `ui/src/assets/`. This needs the generic parent at **15.9.0 or later**: that is the version
whose `ExtensionInfoInternalController` looks for `about.html` under `<ext>-app/html` before falling back
to `<ext>-admin/html`.

The whole UI build is inherited from the parent's `vite-ui` profile, which activates on the presence of
`ui/package.json` - roughly 230 lines of node/npm, `npm ci`, `npm run build`, the dockerized Vitest run
and the bundle copy that this pom used to repeat. What stays local is the `js-e2e-tests` profile (only
diff-tool has a Playwright suite) and the purge of the copy target before each build, which the shared
profile does not do.

## Contracts that must not drift

- `documents.html`, `collections.html`, `workitems.html` - opened by literal URL from
  `src/formext/openDocumentsDiff.ts`, `webapp/diff-tool/js/diff-tool-widget-utils.js` and the Java widget
  renderers' inline handlers.
- `assets/diffToolPanel.js`, `assets/copyToolPanel.js` and their named exports - imported by literal URL
  from the two fragments. This is why the second Vite build is library mode.
- `<uuid>_additionalParams` and `<sha1>_ids` in `localStorage` - the panel and the table widget write
  them, the viewer reads them. `ui/test/widgetHandoff.test.tsx` pins the reading half of both.
- The `data-props` field names - shared between `BaseFormExtension.PanelProps` and
  `ui/src/formext/panelProps.ts`. `BaseFormExtensionTest` pins the fragment side.

## Deferred, with reasons

**The three nav-topic JSPs and the four Java widget renderers.**
`pages/{work-items-diff-widget,collections-diff-widget,items-table-widget}.jsp` are scriptlet wrappers
around `DiffWidgetRenderer` (350 L) and friends, which use Polarion's *internal*
`RichPageWidgetRenderingContextImpl` / `HtmlBuilder` / `WidgetResourcesServlet` to render Polarion-native
work-item tables with the platform's own query engine, paging and icons. Migrating means reimplementing
all of that plus the Lucene query surface behind new REST endpoints: 2-3 weeks, a large regression
surface, and zero user-visible benefit - the tables look native because they *are* native. The three
nav-topic pages host those widgets, so they are blocked on it. Sequencing if it is ever taken on:
(a) JSON endpoints for the widget data, (b) a `?feature=work-items-picker` page, (c) re-point
`MultipleWorkItemsNode` / `CollectionsNode` / `DiffToolNavigationExtender.getPageUrl`.

`js/diff-tool-widget-utils.js` and the trimmed `css/common.css` stay for exactly those pages. The DOM
their inline `onclick`/`onload` handlers walk is produced by Java, not React, so there is no seam to
migrate across.

**The JS→TS sweep of the viewer** (~10 d). Do it file by file, keeping `e2e/` green, moving each file
into `coverage.include` as it converts.

## What has since moved into the library

Two things this conversion built locally have been promoted into react-sbb-polarion and are now imported
rather than owned here. Both were predicted as promotion candidates; the entries are kept so the next
extension does not rebuild them.

**The multi-select combobox.** `src/admin/components/MultiSearchableSelect.tsx` wrapped
`createSearchableSelect(el, { multiselect: true })` because RSP's `SearchableSelect` was single-select
only. RSP 0.1.0 makes `multiple` a prop and gives `SelectOption` an `iconURL`, so the three Diff
Configurations combos import it directly. No reference screenshot moved on the swap.

**The whole Merge Authorization page.** RSP 0.0.11 added `AuthorizationSettings` - the roles of the
current scope as checkboxes over one named setting, with the standard toolbar and the revision table -
because three extensions had written it out. `MergeAuthorizationPage.tsx` is now a thin wrapper supplying
the title, the setting name and the Quick Help; `RoleCheckboxGroup.tsx` and its CSS are gone. The roles
come from generic's own `/roles` endpoint (15.10.0), which `DiffToolRestApplication` registers, replacing
this extension's `RolesUtils`, `RolesModel` and `/roles` controller method - the generic implementation
resolves the scope identically. Two things stay local: the sort that gives the checkboxes a stable order
(`ISecurityService` returns unordered collections and neither the endpoint nor the component sorts), and
`.authorization-page h2` in `App.css`, since RSP leaves those group headings for the app to size.

The page lost its "saved by a different version of the extension" banner with the move, deliberately: a
role setting is two lists of role names, so no schema can go stale, and because the timestamp is stamped
at save time the banner appeared after every plugin upgrade and could only be dismissed by saving again.

**`/api/roles` is documented, and that needs generic >= 15.10.1.** Like every other generic endpoint the
extension exposes (`/api/version`, `/api/readme`, ...), it reaches `docs/openapi.json` because its package
is listed in the swagger plugin's `resourcePackages` *and* its `/internal` twin carries swagger's
`@Hidden` - which is how the spec stays public-surface-only, with none of diff-tool's own internal
controllers in it either. Swagger inherits the `@GET`/`@Path`/`@Operation` method annotations and the
class `@Tag` from that hidden superclass, so the `/api` half is fully described on its own.

15.10.0 shipped `RolesInternalController` without the `@Hidden` its siblings have, which published
`/internal/roles` alongside `/api/roles` and - since `RolesApiController` overrides `getRoles` - made
swagger break the resulting operationId tie by renaming one to `getRoles_1`. Fixed in 15.10.1. Downgrading
the parent below that would quietly bring both back.

## Verification

Per stage: `mvn -s .mvn/settings.xml clean verify` (766 Java tests, 212 Vitest tests, both Vite builds)
plus the CI `e2e` matrix across chromium/firefox/webkit. In `ui/`: `npx tsc --noEmit`, `npm run lint`,
`npm run format:check`, `npm run test:coverage:docker`.

Visual references are **Docker-only** - generate them with `npm run test:update:docker`, and never
overwrite one to make a check pass.

**Validated in a real Polarion** (`mvn clean install -P install-to-local-polarion`, clear
`<polarion_home>/data/workspace/.config`, restart): the five admin pages - each extender opens at the
right scope, and Save / Cancel / Default / Revisions + revert work.

**Still to validate in a real Polarion** - the two Document Properties panels. Vitest cannot assess
these, so check by hand before releasing:

1. Both panels render inside their shadow roots with the correct font (13 px Segoe UI, not a serif
   fallback - that is the symptom of the `.sbb-ui` base rule not applying).
2. The dropdown popups are not clipped by Polarion's `overflow: hidden` field wrapper, and they open
   *inside* the shadow root rather than at the top of the page.
3. Compare opens `documents.html` with the same parameters and `localStorage` state as before.
4. Create Document produces a working link to the new document.
5. Neither panel disturbs another extension's panel in the same pane (the point of the shadow root).

The Execution Queue charts' pan/zoom feel also needs a real, loaded queue to judge; Vitest only asserts
that the canvases paint.
