// @ts-check
import { test, expect } from '@playwright/test';

test.describe("wrong navigation", () => {

  // These two used to assert the Next.js dev server's built-in 404 page markup ("404" / "This page
  // could not be found."). That page was a framework artifact that never shipped: the static export's
  // 404.html was never registered as an <error-page> in webapp/diff-tool-app/WEB-INF/web.xml, so
  // Polarion never served it.
  //
  // What is worth asserting is that the dev server stays a multi-page app (appType: 'mpa'): the root
  // serves the admin feature router, and an unknown path resolves to nothing rather than falling back
  // to some index.html. The unknown path is checked at the HTTP level with page.request rather than
  // page.goto, because vite answers it with an empty body, which Firefox rejects outright as
  // NS_ERROR_NET_EMPTY_RESPONSE during navigation. Production 404s are the servlet's concern.
  test('serves the admin feature router at the root', async ({ page }) => {
    await page.goto('/');

    await expect(page.locator('.app.standard-admin-page')).toBeVisible();
    await expect(page.locator('.page > h1')).toHaveText('Diff Tool Administration');
  });

  test('handles wrong route', async ({ page }) => {
    const response = await page.request.get('/fake');

    expect(response.status()).toBe(404);
  });

  test('handles missing params of documents diffing', async ({ page }) => {
    await page.goto('/documents');

    await expect(page.getByTestId('app-alert-title')).toHaveText("Error occurred loading diff data!");
    await expect(page.getByTestId('app-alert-message'))
        .toHaveText("Following parameters are missing: [sourceProjectId, sourceSpaceId, sourceDocument, targetProjectId, targetSpaceId, targetDocument, linkRole]");
  });

  test('handles missing params of workitems diffing', async ({ page }) => {
    await page.goto('/workitems');

    await expect(page.getByTestId('app-alert-title')).toHaveText("Error occurred loading diff data!");
    await expect(page.getByTestId('app-alert-message'))
        .toHaveText("Following parameters are missing: [sourceProjectId, targetProjectId, linkRole, ids]");
  });

  test('handles missing params of collections diffing', async ({ page }) => {
    await page.goto('/collections');

    await expect(page.getByTestId('app-alert-title')).toHaveText("Error occurred loading diff data!");
    await expect(page.getByTestId('app-alert-message'))
        .toHaveText("Following parameters are missing: [sourceProjectId, sourceCollectionId, targetProjectId, targetCollectionId]");
  });


})
