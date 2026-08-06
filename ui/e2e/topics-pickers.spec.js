// @ts-check
import { expect, test } from '@playwright/test';

// End-to-end cover for the two navigation-topic pickers (topics.html?topic=...), which replaced the
// nav-topic JSPs and the Java widget renderers. The REST layer is stubbed here rather than in
// e2e/fixtures/, since these four responses are small enough to read in place.

const PROJECTS = [
  { id: 'elibrary', name: 'E-Library' },
  { id: 'drivepilot', name: 'Drive Pilot' },
];

const LINK_ROLES = [{ id: 'relates_to', name: 'relates to', oppositeName: 'is related to' }];

const CONFIGURATIONS = [{ name: 'Default', scope: '' }];

const workItem = (id) => ({
  id: id,
  projectId: 'elibrary',
  title: `Title of ${id}`,
  type: { id: 'task', name: 'Task' },
  status: { id: 'open', name: 'Open' },
  severity: { id: 'major', name: 'Major' },
  readable: true,
});

const collection = (id, projectId) => ({
  id: id,
  projectId: projectId,
  name: `Collection ${id}`,
  authorName: 'John Doe',
  created: 1700000000000,
  updated: 1700000600000,
  readable: true,
});

const searchResult = (items, projectId) => ({
  totalCount: items.length,
  page: 1,
  lastPage: 1,
  // The backend echoes the query it ran, project restriction included
  query: `project.id:${projectId}`,
  items: items,
});

const json = (body) => ({ status: 200, contentType: 'application/json', body: JSON.stringify(body) });

async function stubRest(page) {
  await page.route('**/rest/internal/projects', (route) => route.fulfill(json(PROJECTS)));
  await page.route('**/rest/internal/projects/*/link-roles', (route) => route.fulfill(json(LINK_ROLES)));
  await page.route('**/rest/internal/settings/diff/names**', (route) => route.fulfill(json(CONFIGURATIONS)));
  await page.route('**/rest/internal/projects/elibrary/workitems/search**', (route) =>
    route.fulfill(json(searchResult([workItem('EL-1'), workItem('EL-2')], 'elibrary'))),
  );
  await page.route('**/rest/internal/projects/elibrary/collections/search**', (route) =>
    route.fulfill(json(searchResult([collection('c1', 'elibrary')], 'elibrary'))),
  );
  await page.route('**/rest/internal/projects/drivepilot/collections/search**', (route) =>
    route.fulfill(json(searchResult([collection('c2', 'drivepilot')], 'drivepilot'))),
  );
}

/**
 * Records what the page asks the browser to open, which is the contract the Compare button carries.
 *
 * Reading it off a real popup does not work across browsers: the viewer's production path has no route on the
 * dev server, and Firefox reports the popup before it has committed any URL - so `page.url()` there is still
 * `about:blank` and parsing it throws.
 */
async function captureOpenedUrls(page) {
  await page.addInitScript(() => {
    const opened = [];
    Object.defineProperty(window, 'openedUrls', { value: opened });
    // The WorkItems handoff opens a blank tab under the click and navigates it once the digest resolves, so the
    // stub answers with a tab and records where it ends up rather than the blank open itself.
    window.open = (url) => {
      if (url) {
        opened.push(String(url));
      }
      return {
        location: {
          replace: (target) => opened.push(String(target)),
        },
        close: () => {},
      };
    };
  });
}

/** The URL the page opened, resolved against it - the app builds an absolute path, not an absolute URL. */
async function openedUrl(page) {
  await page.waitForFunction(() => /** @type {any} */ (window).openedUrls.length > 0);
  const opened = await page.evaluate(() => /** @type {any} */ (window).openedUrls);
  expect(opened).toHaveLength(1);
  return new URL(opened[0], page.url());
}

test.describe('work items picker topic', () => {
  test.beforeEach(async ({ page }) => {
    await stubRest(page);
    await captureOpenedUrls(page);
  });

  test('lists the work items of the project and compares the selected ones', async ({ page }) => {
    await page.goto('/topics?topic=compare-work-items&sourceProjectId=elibrary');

    await expect(page.locator('.diff-topics .header h3')).toHaveText('Compare work items');
    await expect(page.locator('.items-table .table-content-row')).toHaveCount(2);
    await expect(page.locator('.table-counts')).toHaveText('2 items found');

    const compare = page.locator('#compare-items');
    await expect(compare).toBeDisabled();

    await page.locator('.items-table input.select-all').check();
    await expect(compare).toBeEnabled();

    // Compare opens the viewer in a new tab, as the widget's button did, at the path Polarion serves it from
    await compare.click();

    const url = await openedUrl(page);
    expect(url.pathname).toBe('/polarion/diff-tool-app/ui/app/workitems.html');
    expect(url.searchParams.get('sourceProjectId')).toBe('elibrary');
    expect(url.searchParams.get('config')).toBe('Default');
    expect(url.searchParams.get('linkRole')).toBe('relates_to');

    // ...and the selected IDs travel through localStorage under the hash the URL carries
    const ids = await page.evaluate((hash) => localStorage.getItem(`${hash}_ids`), url.searchParams.get('ids'));
    expect(ids).toBe('EL-1,EL-2');
  });

  test('keeps the applied query in its own URL, without reloading the frame', async ({ page }) => {
    await page.goto('/topics?topic=compare-work-items&sourceProjectId=elibrary');
    await expect(page.locator('.items-table .table-content-row')).toHaveCount(2);

    await page.locator('#source-query-input').fill('type:task');
    await page.getByRole('button', { name: 'Apply' }).click();

    await expect(page).toHaveURL(/sourceQuery=type%3Atask/);
    await expect(page.locator('.items-table .table-content-row')).toHaveCount(2);
  });
});

test.describe('collections picker topic', () => {
  test.beforeEach(async ({ page }) => {
    await stubRest(page);
    await captureOpenedUrls(page);
  });

  test('compares one collection from each side', async ({ page }) => {
    await page.goto('/topics?topic=compare-collections&sourceProjectId=elibrary&targetProjectId=drivepilot');

    await expect(page.locator('.diff-topics .header h3')).toHaveText('Compare Collections');
    await expect(page.locator('.columns .column')).toHaveCount(2);

    const compare = page.locator('#compare-items');
    await expect(compare).toBeDisabled();

    await page.locator('input[name="source-collection"]').first().check();
    await page.locator('input[name="target-collection"]').first().check();
    await expect(compare).toBeEnabled();

    await compare.click();

    const url = await openedUrl(page);
    expect(url.pathname).toBe('/polarion/diff-tool-app/ui/app/collections.html');
    expect(url.searchParams.get('sourceCollectionId')).toBe('c1');
    expect(url.searchParams.get('targetCollectionId')).toBe('c2');
    expect(url.searchParams.get('compareAs')).toBe('Workitems');
  });
});

test.describe('diff tool root topic', () => {
  test('offers both sub-topics', async ({ page }) => {
    await page.goto('/topics?topic=diff-tool');

    await expect(page.locator('.diff-topics .header h3')).toHaveText('Diff Tool');
    await expect(page.locator('.link-button')).toHaveText(['Compare multiple Work Items', 'Compare Collections']);
  });
});
