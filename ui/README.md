# diff-tool UI

The React front end of the Polarion diff-tool extension: a [Vite](https://vite.dev/) **multi-page**
app, one HTML entry per Polarion entry point.

| Entry | Served at | Opened by |
|---|---|---|
| `documents.html` | `/polarion/diff-tool-app/ui/app/documents.html` | `webapp/diff-tool/js/modules/DiffTool.js` |
| `collections.html` | `/polarion/diff-tool-app/ui/app/collections.html` | `webapp/diff-tool/js/diff-tool-widget-utils.js` |
| `workitems.html` | `/polarion/diff-tool-app/ui/app/workitems.html` | `webapp/diff-tool/js/diff-tool-widget-utils.js` |

Those three filenames are a **public contract** - the vanilla-JS callers above and the Java widget
renderers' inline handlers open them by literal URL, and pass extra state through `localStorage`. Do
not rename them.

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
| `npm run build` | production build to `dist/app` (copied into the extension jar by Maven) |
| `npm run typecheck` | `tsc --noEmit` |
| `npm run e2e` | Playwright E2E suite (interactive) |
| `npm run e2e:headless` | Playwright E2E suite (list reporter) |

Playwright browser binaries are **not** installed by the Maven build - run
`npx playwright install` once, or build with `-DskipJsTests=true`.

## Layout

```
documents.html collections.html workitems.html   HTML entries
src/entries/     one module per HTML entry: mounts the React tree
src/pages/       the page component behind each entry
src/components/  the diff/merge UI
src/services/    REST access (useRemote), diff/merge orchestration, PDF export
src/router/      navigation.ts - the useSearchParams/usePathname/useRouter shim
src/styles/      globals.css
e2e/             Playwright specs + JSON fixtures
```

New code is written in TypeScript. The diff/merge viewer is still plain JS (`allowJs` is on,
`checkJs` is off) and is covered end-to-end by the Playwright suite in `e2e/`; files come under type
checking as they are converted.
