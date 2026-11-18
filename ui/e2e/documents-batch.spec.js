import { expect } from '@playwright/test';
import { test } from './test-utils';

// End-to-end cover of the batch PDF export of the Documents topic: the page renders each selected pair in the
// documents viewer, embedded in a hidden frame, converts what the viewer shows to PDF and downloads one file
// per pair. The REST layer of the page and of the frames is stubbed; the viewer is the real one.

const PROJECTS = [
  { id: 'elibrary', name: 'E-Library' },
  { id: 'drivepilot', name: 'Drive Pilot' },
];

const LINK_ROLES = [{ id: 'relates_to', name: 'relates to', oppositeName: 'is related to' }];

const CONFIGURATIONS = [{ name: 'Default', scope: '' }];

const document_ = (spaceId, id) => ({
  id: id,
  projectId: 'elibrary',
  spaceId: spaceId,
  spaceTitle: spaceId,
  title: id,
  type: { id: 'req', name: 'Requirements' },
  status: { id: 'draft', name: 'Draft' },
  readable: true,
});

const DOCUMENTS = {
  totalCount: 2,
  page: 1,
  lastPage: 1,
  query: 'project.id:elibrary',
  items: [document_('Design', 'Catalog Design'), document_('Specification', 'Catalog Specification')],
};

const json = (body) => ({ status: 200, contentType: 'application/json', body: JSON.stringify(body) });

const TOPIC_URL = '/topics?topic=documents-diff-to-pdf&sourceProjectId=elibrary&targetProjectId=drivepilot&linkRole=relates_to';

test.describe('documents batch PDF export topic', () => {
  /** @type {string[]} */
  let convertedHtml;

  test.beforeEach(async ({ page, mockApi }) => {
    convertedHtml = [];
    await page.route('**/rest/internal/projects', (route) => route.fulfill(json(PROJECTS)));
    await page.route('**/rest/internal/projects/*/link-roles', (route) => route.fulfill(json(LINK_ROLES)));
    await page.route('**/rest/internal/settings/diff/names**', (route) => route.fulfill(json(CONFIGURATIONS)));
    await page.route('**/rest/internal/projects/elibrary/documents/search**', (route) => route.fulfill(json(DOCUMENTS)));
    await page.route('**/rest/internal/projects/drivepilot/documents/resolve', (route) => {
      const asked = route.request().postDataJSON();
      return route.fulfill(json(asked.map((document) => ({ ...document, found: true, spaceTitle: document.spaceId, title: document.name }))));
    });
    await page.route('**/rest/internal/projects/*/spaces/*/documents/*/revisions', (route) => route.fulfill(json([])));

    // What the embedded viewer asks for
    await mockApi.mockEndpoint({ url: '**/extension/info', fixtureFile: 'version-info.json' });
    await mockApi.mockEndpoint({ url: '**/communication/settings', fixtureFile: 'communication-settings.json' });
    await mockApi.mockEndpoint({ url: '**/diff/documents', fixtureFile: 'documents-diff.json' });
    await mockApi.mockWorkItemsDiffEndpoint();

    await page.route('**/conversion/html-to-pdf**', (route) => {
      convertedHtml.push(route.request().postData() ?? '');
      return route.fulfill({ status: 200, contentType: 'application/pdf', body: '%PDF-1.4 batch' });
    });
  });

  test('exports the diff of each selected document to a PDF of its own', async ({ page }) => {
    /** @type {string[]} */
    const downloads = [];
    page.on('download', (download) => downloads.push(download.suggestedFilename()));

    await page.goto(TOPIC_URL);
    await expect(page.locator('.items-table .table-content-row')).toHaveCount(2);
    await expect(page.locator('.target-cell').first()).toContainText('Design / Catalog Design');

    await page.locator('.items-table input.select-all').check();
    const exportButton = page.locator('#compare-items');
    await expect(exportButton).toHaveText('Export 2 diffs to PDF');
    await exportButton.click();

    const dialog = page.locator('.batch-export-progress');
    await expect(dialog).toBeVisible();
    await expect(dialog.locator('.batch-export-row')).toHaveCount(2);
    await expect(dialog.locator('.result')).toHaveText('Export successfully finished', { timeout: 30_000 });
    await expect(dialog.locator('.batch-export-row.finished')).toHaveCount(2);

    expect(convertedHtml).toHaveLength(2);
    // The PDF is the viewer as rendered: its diff body, without its control pane
    expect(convertedHtml[0]).toContain('id="diff-body"');
    expect(convertedHtml[0]).toContain('--pdf-elem-max-width');
    expect(convertedHtml[0]).not.toContain('class="control-pane');
    await expect.poll(() => downloads.length).toBe(2);
    // WebKit on Linux saves a download with its spaces replaced by underscores, so they are compared as equal.
    // The exact names the page asks for are checked in test/batchExport.test.ts.
    const sanitized = (/** @type {string} */ name) => name.replaceAll(' ', '_');
    expect(downloads.map(sanitized)).toEqual(
      [
        'Design_Catalog Design__drivepilot_Design_Catalog Design.pdf',
        'Specification_Catalog Specification__drivepilot_Specification_Catalog Specification.pdf',
      ].map(sanitized),
    );
    // No frame is left behind
    await expect(page.locator('iframe.batch-diff-frame')).toHaveCount(0);

    await page.locator('.rsp-modal-footer .sbb-btn--secondary').click();
    await expect(dialog).toHaveCount(0);
  });

  test('reports a pair which failed and goes on with the next one', async ({ page }) => {
    let conversions = 0;
    await page.route('**/conversion/html-to-pdf**', (route) => {
      conversions++;
      return conversions === 1
        ? route.fulfill({ status: 500, contentType: 'application/json', body: JSON.stringify({ message: 'WeasyPrint is down' }) })
        : route.fulfill({ status: 200, contentType: 'application/pdf', body: '%PDF-1.4 batch' });
    });

    await page.goto(TOPIC_URL);
    await expect(page.locator('.target-cell').first()).toContainText('Design / Catalog Design');
    await page.locator('.items-table input.select-all').check();
    await page.locator('#compare-items').click();

    const dialog = page.locator('.batch-export-progress');
    await expect(dialog.locator('.result')).toHaveText('Export finished with errors', { timeout: 30_000 });
    await expect(dialog.locator('.batch-export-row').nth(0)).toHaveClass(/error/);
    await expect(dialog.locator('.batch-export-row').nth(0)).toContainText('WeasyPrint is down');
    await expect(dialog.locator('.batch-export-row').nth(1)).toHaveClass(/finished/);
  });

  test('stops the run', async ({ page }) => {
    // The first conversion never answers until the run is stopped
    /** @type {(() => void) | undefined} */
    let release;
    await page.route('**/conversion/html-to-pdf**', async (route) => {
      await new Promise((resolve) => (release = () => resolve(undefined)));
      await route.fulfill({ status: 200, contentType: 'application/pdf', body: '%PDF-1.4 batch' });
    });
    /** @type {string[]} */
    const downloads = [];
    page.on('download', (download) => downloads.push(download.suggestedFilename()));

    await page.goto(TOPIC_URL);
    await expect(page.locator('.target-cell').first()).toContainText('Design / Catalog Design');
    await page.locator('.items-table input.select-all').check();
    await page.locator('#compare-items').click();

    const dialog = page.locator('.batch-export-progress');
    await expect(dialog.locator('.batch-export-row.converting')).toHaveCount(1, { timeout: 30_000 });
    await page.locator('.rsp-modal-footer .sbb-btn--primary').click();
    release?.();

    await expect(dialog.locator('.result')).toHaveText('Export interrupted by user');
    await expect(dialog.locator('.batch-export-row.interrupted')).toHaveCount(2);
    expect(downloads).toEqual([]);
  });
});
