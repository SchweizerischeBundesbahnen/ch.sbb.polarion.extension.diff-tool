// @ts-check
import { test, expect } from '@playwright/test';

test.describe("wrong navigation", () => {

  test('handles root route', async ({ page }) => {
    await page.goto('http://127.0.0.1:3000/');

    await expect(page.locator("h1")).toHaveText("404");
    await expect(page.locator("h2")).toHaveText("This page could not be found.");
  });

  test('handles wrong route', async ({ page }) => {
    await page.goto('http://127.0.0.1:3000/fake');

    await expect(page.locator("h1")).toHaveText("404");
    await expect(page.locator("h2")).toHaveText("This page could not be found.");
  });

  test('handles missing params of documents diffing', async ({ page }) => {
    await page.goto('http://127.0.0.1:3000/documents');

    await expect(page.locator("h2")).toHaveText("Error occurred loading diff data!");
    await expect(page.locator("p").filter({ hasText: "Following parameters are missing:" }))
        .toHaveText("Following parameters are missing: [sourceProjectId, sourceSpaceId, sourceDocument, targetProjectId, targetSpaceId, targetDocument, linkRole]");
  });

  test('handles missing params of workitems diffing', async ({ page }) => {
    await page.goto('http://127.0.0.1:3000/workitems');

    await expect(page.locator("h2")).toHaveText("Error occurred loading diff data!");
    await expect(page.locator("p").filter({ hasText: "Following parameters are missing:" }))
        .toHaveText("Following parameters are missing: [sourceProjectId, targetProjectId, linkRole, ids]");
  });

  test('handles missing params of collections diffing', async ({ page }) => {
    await page.goto('http://127.0.0.1:3000/collections');

    await expect(page.locator("h2")).toHaveText("Error occurred loading diff data!");
    await expect(page.locator("p").filter({ hasText: "Following parameters are missing:" }))
        .toHaveText("Following parameters are missing: [sourceProjectId, sourceCollectionId, targetProjectId, targetCollectionId]");
  });


})
