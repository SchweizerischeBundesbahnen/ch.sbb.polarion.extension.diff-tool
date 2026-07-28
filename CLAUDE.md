# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Polarion ALM extension providing document and work item comparison/merging capabilities. It consists of:
- **Java backend**: JAX-RS REST API integrated into Polarion (Java 21, Maven)
- **Vite UI** (`ui/`): standalone React application (Vite 8, React 19, TypeScript for new code)

## Common Commands

### Build and Test

```bash
# Full build (includes Java backend + the Vite UI build + tests)
mvn clean package

# Build and install to local Polarion instance
# Requires POLARION_HOME environment variable to be set
mvn clean install -P install-to-local-polarion

# Run Java tests only
mvn test

# Run tests with coverage report (available in target/site/jacoco)
mvn verify

# Skip JavaScript tests during build
mvn clean package -DskipJsTests=true

# The test phase runs the Vitest suite inside the pinned Playwright Docker image. Escape hatches:
#   -DjsTestsNoDocker      run Vitest directly (no Docker on this host); the visual suites skip
#                          themselves outside the reference image, so pixels are simply not compared
#   -DskipVisualJsTests    do not even load the visual suites (only with -DjsTestsNoDocker)
#   -DinstallPlaywright    download the browser binaries (and OS deps)
#   -DjsE2eTests           additionally run the Playwright E2E suite
```

### Frontend Development (in ui/ directory)

```bash
# Install dependencies
npm install

# Development server (proxies REST/assets to VITE_BASE_URL - see .env.development.template)
npm run dev

# Development server as the E2E suite runs it (loads .env.e2e, no Polarion proxy)
npm run dev:e2e

# Build for production (vite, multi-page -> dist/app)
npm run build

# Type-check
npm run typecheck

# Run Playwright E2E tests (interactive)
npm run e2e

# Run Playwright E2E tests (headless)
npm run e2e:headless
```

The UI is a **Vite** multi-page app (not Next.js). Each Polarion entry point is its own HTML entry:
`index.html` (the admin pages, chosen by `?feature=<id>`), plus `documents.html`, `collections.html`
and `workitems.html` for the diff/merge viewer. The three viewer filenames are a public contract:
`src/formext/openDocumentsDiff.ts` and `webapp/diff-tool/js/diff-tool-widget-utils.js` open them by
literal URL, as do the Java widget renderers' inline handlers. Do not rename them.

`npm run build` is **two** Vite invocations, in this order:

1. `vite build` - the multi-page SPA above, into `dist/app` (`emptyOutDir: true`).
2. `vite build --config vite.formext.config.js` - library mode, appending the two Document Properties
   panels as fixed-name modules `dist/app/assets/{diffToolPanel,copyToolPanel}.js`
   (`emptyOutDir: false`, so it must run second). The names are fixed because the server-rendered
   fragments import them by literal URL and call a named export.

The admin pages and both Document Properties panels are built on the shared **react-sbb-polarion**
library (RSP), like the other migrated SBB Polarion extensions; the viewer predates it and does not use
it. `META-INF/hivemodule.xml` is the source of truth for which extenders point at the React app
(`.../ui/app/index.html?feature=<id>`); all five now do, and `rest-api` deliberately still points at
`/polarion/diff-tool/rest/swagger`. See `ui/README.md` for the details, including why the viewer's page
shell is `.diff-app` rather than `.app`.

Still deliberately **not** React: the three nav-topic JSPs under `webapp/diff-tool/pages/` and the four
Java widget renderers in `widgets/` (they render Polarion-native work-item tables through the platform's
own `HtmlBuilder` and query engine), plus `js/diff-tool-widget-utils.js` and `css/common.css`, which
serve exactly those pages.

Playwright browser binaries are not installed by the Maven build; run `npx playwright install` in
`ui/` once, or build with `-DskipJsTests=true`. CI needs neither: its Maven build runs the Vitest
suite in the pinned Docker image, and the E2E suite has its own per-browser job.

### Single Test Execution

```bash
# Run single Java test class
mvn test -Dtest=DiffServiceTest

# Run specific test method
mvn test -Dtest=DiffServiceTest#testDiffDocuments
```

## High-Level Architecture

### Backend-Frontend Communication

- **Dual REST Endpoints**:
  - `/polarion/diff-tool/rest/internal/*` - session-based auth for internal Polarion UI
  - `/polarion/diff-tool/rest/api/*` - bearer token auth for external access
- **Controller Pattern**: ApiControllers wrap InternalControllers with `polarionService.callPrivileged()` for privilege escalation
- **Authentication Detection**: the UI auto-detects the mode from `VITE_BEARER_TOKEN` (see `ui/src/services/useRemote.ts`): set, it uses `/api` with a bearer header; unset, `/internal` with the Polarion session

### Core Service Architecture

**DiffService** (src/main/java/ch/sbb/polarion/extension/diff_tool/service/DiffService.java):
- Core diffing engine using DaisyDiff library
- Performs bidirectional diffing (left-to-right AND right-to-left with reversed styles) for better visual results
- Implements "moved items" detection by creating surrogate pairs to show items in both original and new positions
- Uses DiffLifecycleHandlers pipeline for HTML preprocessing/postprocessing:
  - IdsHandler: Removes/replaces element IDs to avoid false diffs
  - LinksHandler: Normalizes work item links
  - ImageHandler: Handles embedded images
  - EnumReplaceHandler: Optionally compares enums by ID vs name
  - OuterWrapperHandler: Wraps content to fix DaisyDiff edge cases
- Caches work items per document via DocumentWorkItemsCache for performance

**MergeService** (src/main/java/ch/sbb/polarion/extension/diff_tool/service/MergeService.java):
- Handles all merge operations (documents, work items, fields, content)
- Multi-pass transactional approach:
  1. Create/delete work items
  2. Update fields and move items within document
  3. Handle referenced work items moved out
  4. Re-insert items deleted from affected external documents
- Rich text transformation: replaces work item links with paired counterparts
- Uses Polarion's DLE (Document Level Editor) APIs for document manipulation
- Implements revision conflict detection before merging

**PolarionService** (src/main/java/ch/sbb/polarion/extension/diff_tool/service/PolarionService.java):
- Facade over Polarion APIs (ITrackerService, IProjectService, ISecurityService, etc.)
- Manages work item pairing logic using link roles with parallel processing (ExecutorService with 2 threads)
- Authorization checks via AuthorizationSettings (role-based per project)
- Document work items caching by user subject

**ExecutionQueueService** (src/main/java/ch/sbb/polarion/extension/diff_tool/service/ExecutionQueueService.java):
- Queues heavy operations across 9 configurable workers to prevent Polarion server overload
- Worker 0 = bypass queue (direct execution)
- Includes queue size limits and CPU load monitoring

### Settings Management

**NamedSettings Pattern** (from ch.sbb.polarion.extension.generic framework):
- DiffSettings, AuthorizationSettings, ExecutionQueueSettings extend GenericNamedSettings
- Stored per-scope (global/project/space) in Polarion
- Registered in DiffToolRestApplication constructor via NamedSettingsRegistry

**DiffSettings**:
- Defines DiffModel with:
  - `diffFields`: which work item fields to compare/merge
  - `hyperlinkRoles`, `linkedWorkItemRoles`: which links to preserve during merge
  - `statusesToIgnore`: work items with these statuses excluded from diff

**AuthorizationSettings**:
- Controls which Polarion roles can perform merge operations per project
- Checked via PolarionService.userAuthorizedForMerge()

**ExecutionQueueSettings**:
- Maps features (DIFF, MERGE, etc.) to worker queues (1-9)
- Configurable queue sizes and thread counts

### Key Utilities

**DiffToolUtils** (src/main/java/ch/sbb/polarion/extension/diff_tool/util/DiffToolUtils.java):
- Wrapper for DaisyDiff HTML comparison
- Style application (converts CSS classes to inline styles for PDF export)
- Surrogate tag wrapping/unwrapping to fix DaisyDiff edge cases

**DocumentWorkItemsCache** (src/main/java/ch/sbb/polarion/extension/diff_tool/util/DocumentWorkItemsCache.java):
- Singleton cache keying by document+revision+user subject
- Critical for performance when comparing documents with many work items

**CommentUtils** (src/main/java/ch/sbb/polarion/extension/diff_tool/util/CommentUtils.java):
- Extracts/removes/appends Polarion comments in rich text fields
- Used when preserveComments=true to maintain target comments during merge

**DiffModelCachedResource** (src/main/java/ch/sbb/polarion/extension/diff_tool/util/DiffModelCachedResource.java):
- Caches DiffModel configuration per project/config name
- DiffModel defines which fields to diff/merge, link roles, statuses to ignore

**OutlineNumberComparator** (src/main/java/ch/sbb/polarion/extension/diff_tool/util/OutlineNumberComparator.java):
- Custom comparator for document outline numbers (e.g., "1.2.3-1" < "1.2.3-10")
- Handles complex numbering schemes with multiple segments and suffixes

## Important Technical Details

### Bidirectional Diffing Strategy

The extension performs diffs in BOTH directions (A→B and B→A) with style reversal:
- This solves visual asymmetry issues in DaisyDiff
- Produces more intuitive results for users comparing documents
- ReverseStylesHandler swaps add/remove CSS classes for reverse direction

### "Moved Items" Detection

When work items move positions within a document:
- Creates surrogate pairs showing the item in BOTH old and new positions
- One marked as "removed from" (old position), one as "added to" (new position)
- Allows users to see the movement clearly in the diff view

### Rich Text Link Transformation During Merge

When merging rich text fields containing work item links:
- Links to work items are replaced with links to their paired counterparts in target project
- Preserves semantic meaning across project boundaries
- Example: Link to "ProjectA-WI-123" becomes link to "ProjectB-WI-456" if they're paired via link role

### Document Structure Manipulation

- Uses Polarion's `IModule.IStructureNode` API for document tree operations
- Handles complex parent-child relationships during move operations
- Implements workaround for Polarion bug when moving items from position 0

### Performance Considerations

- **Parallel Processing**: Work item pairing uses thread pool (default 2 threads)
- **Caching**: DocumentWorkItemsCache and DiffModelCachedResource reduce repeated API calls
- **Queue Management**: ExecutionQueueService prevents server overload during heavy operations
- **Configurable Chunk Size**: `ch.sbb.polarion.extension.diff-tool.chunk.size` property controls parallel REST requests (default: 2)

### Testing Polarion Extensions

- Unit tests use mocked Polarion services (ITrackerService, IProjectService, etc.)
- Integration tests require Polarion dependencies extracted via [polarion-artifacts-deployer](https://github.com/SchweizerischeBundesbahnen/polarion-artifacts-deployer)
- Frontend has two layers: Vitest browser mode (real Chromium via Playwright) for components and
  visual regression, and Playwright specs in `ui/e2e/` for the viewer end-to-end. Only the Vitest layer
  runs in the Maven `test` phase; see `ui/README.md`

### Deployment and Installation

After building (`mvn clean package`):
1. Copy `target/ch.sbb.polarion.extension.diff-tool-<version>.jar` to `<polarion_home>/polarion/extensions/ch.sbb.polarion.extension.diff-tool/eclipse/plugins/`
2. Clear `<polarion_home>/data/workspace/.config` folder
3. Restart Polarion service

For development, use: `mvn clean install -P install-to-local-polarion` with `POLARION_HOME` environment variable set.

## Configuration in Polarion

### Enable in Project

1. Document Properties Sidebar:
   ```xml
   <extension id="diff-tool" label="Documents Comparison" />
   <extension id="copy-tool" label="Documents Copy" />
   ```

2. Navigation Topics:
   ```xml
   <topic id="diff-tool"/>
   ```

### Performance Tuning

In `polarion.properties`:
```properties
ch.sbb.polarion.extension.diff-tool.chunk.size=2
```
Increase for faster processing, but may overload server.

## Code Quality

- **SonarCloud Integration**: Quality gate must pass before merging
- **Pre-commit Hooks**: Configured in `.pre-commit-config.yaml` (run automatically via maven plugin)
- **Coverage Reports**: Generated by JaCoCo (Java) and Vitest/istanbul (JavaScript, 80% gate - see the
  scoping note in `ui/vitest.config.ts`)
- **Code Smells**: Actively monitored and fixed (see recent commits)

## Related Extensions

- **PDF-Exporter** (v9+): Used for HTML to PDF conversion in ConversionService
- **Generic Extension Framework** (v15.9.0, from the parent POM): Provides base classes for settings, REST
  controllers, servlet integration, and - via its `vite-ui` profile, activated by the presence of
  `ui/package.json` - the whole React/Vite build

## Important Notes

- All contributors must have an active Polarion license (see CONTRIBUTING.md)
- Extension only supports Polarion 2512+ (as of version 5.0.0)
- REST API documented in `docs/openapi.json`
- Velocity context functions available: `$diffTool.diffText()`, `$diffTool.diffHtml()`, `$diffTool.diffWorkItems()`

## GitHub PR Code Reviews (Automated Workflow)

**Philosophy**: Reviews should be **terse, actionable, and problem-focused**. No praise, no analysis of unchanged code.

**When reviewing PRs via the automated workflow:**
- ONLY review lines changed in the PR diff
- ONLY report actual problems (bugs, security issues, breaking changes, missing tests)
- Use terse format: `[file:line] Problem - Fix: Solution`
- If no issues found, say "No issues found." and stop
- Do NOT: praise code quality, review unchanged code, suggest optional improvements, analyze performance if not changed

**Review categories:**
- 🔴 **Critical**: Bugs, security vulnerabilities, breaking changes
- 🟡 **Important**: Missing tests for new functionality, significant issues

### Skip Reviews For (Automated Tools Handle These)

**The following are already checked by automated tools - DO NOT comment on them:**

**Code Formatting & Style** (handled by Maven plugins + pre-commit hooks):
- Java code formatting (Spotless/google-java-format)
- Import organization
- Indentation and whitespace
- Line length
- Code style consistency

**Static Analysis** (handled by Maven plugins):
- SonarCloud integration for code quality
- SpotBugs for bug detection
- Checkstyle for style violations
- PMD for code quality issues

**Testing** (handled by Maven + Surefire/Failsafe):
- Test execution and coverage
- JUnit test configuration
- JavaScript test execution (Vitest, dockerized)
- Integration test setup

**Security & Compliance** (handled by pre-commit hooks):
- Sensitive data leak detection (gitleaks)
- Internal URL/email exposure
- UE numbers and DEV ticket numbers
- Secret scanning

**YAML & Documentation** (handled by pre-commit hooks):
- YAML formatting (yamlfix)
- GitHub Actions validation (actionlint, zizmor)
- Commit message format (commitizen)

**Don't suggest these common patterns (already established in codebase):**
- Using Lombok annotations
- JAX-RS patterns for REST endpoints
- OSGi bundle structure
- Polarion extension patterns from parent POM
- Maven plugin configurations already in use
- JavaScript testing with Vitest + react-sbb-polarion test helpers
- Shadow-root mounting for form-extension panels (`ui/src/formext/shadowMount.ts`)

### Project-Specific Review Focus

**DO focus on:**

1. **Security**:
    - Proper input validation in REST endpoints
    - SQL injection in queries (especially in settings/repository access)
    - XSS vulnerabilities in HTML processing
    - Path traversal in file operations
    - Secrets exposure in logs or error messages
    - CORS configuration if changed

2. **Polarion Integration**:
    - Correct OSGi service registration/unregistration
    - Proper transaction handling with Polarion API
    - Resource cleanup (sessions, connections)
    - Settings inheritance and overrides
    - Widget integration patterns

3. **PDF Export Logic**:
    - HTML transformation pipeline correctness
    - WeasyPrint service error handling
    - Image and table sizing calculations
    - CSS handling and internalization
    - Link resolution (internal vs external)

4. **Async Job Processing**:
    - Proper job lifecycle management
    - Timeout handling
    - Job cleanup and resource management
    - Concurrent job execution safety

5. **Breaking Changes**:
    - REST API endpoint changes
    - Settings structure changes
    - JavaScript API changes
    - OpenAPI specification updates
    - Backward compatibility with existing configurations

6. **Resource Management**:
    - Temporary file cleanup
    - Memory leaks in long-running jobs
    - Database connection handling
    - OSGi service lifecycle

7. **Multi-language Support**:
    - Proper localization handling (XLIFF)
    - Character encoding in exports
    - Velocity template rendering

8. **JavaScript Components**:
    - DOM manipulation safety
    - Event handler cleanup
    - API request error handling
    - Browser compatibility (if UI changes)
