import { test, expect } from '@playwright/test';


// We do not mock REST API requests here, that's why they fail, and we are checking that UI correctly reflects this
test.describe('data fetch failing', () => {

  test.beforeEach(async ({ page }) => {
    await page.route('**/diff/**', async (route) => {
      // Emulate network error
      return route.abort();
    });
  });

  test('documents data fetch fails', async ({ page }) => {
    await page.goto('/documents?sourceProjectId=project1&sourceSpaceId=space1&sourceDocument=document1&targetProjectId=project2&targetSpaceId=space2&targetDocument=document2&linkRole=relates_to');

    await expect(page.locator('.app-header .app-title')).toHaveText("Diff/merge of Polarion Documents (WorkItems)");
    await expect(page.getByTestId('app-alert-title')).toHaveText("Error occurred loading diff data!");
    await expect(page.getByTestId('app-alert-message')).toHaveText("Network error occurred when attempting to fetch a resource. Be sure Polarion is started and accessible.");
  });

  test('documents content data fetch fails', async ({ page }) => {
    await page.goto('/documents?sourceProjectId=project1&sourceSpaceId=space1&sourceDocument=document1&targetProjectId=project2&targetSpaceId=space2&targetDocument=document2&linkRole=relates_to&compareAs=Content');

    await expect(page.locator('.app-header .app-title')).toHaveText("Diff/merge of Polarion Documents (Content)");
    await expect(page.getByTestId('app-alert-title')).toHaveText("Error occurred loading diff data!");
    await expect(page.getByTestId('app-alert-message')).toHaveText("Network error occurred when attempting to fetch a resource. Be sure Polarion is started and accessible.");
  });

  test('documents field data fetch fails', async ({ page }) => {
    await page.goto('/documents?sourceProjectId=project1&sourceSpaceId=space1&sourceDocument=document1&targetProjectId=project2&targetSpaceId=space2&targetDocument=document2&linkRole=relates_to&compareAs=Fields');

    await expect(page.locator('.app-header .app-title')).toHaveText("Diff/merge of Polarion Documents (Fields)");
    await expect(page.getByTestId('app-alert-title')).toHaveText("Error occurred loading diff data!");
    await expect(page.getByTestId('app-alert-message')).toHaveText("Network error occurred when attempting to fetch a resource. Be sure Polarion is started and accessible.");
  });

  test('workitems data fetch fails', async ({ page }) => {
    await page.goto('/workitems?sourceProjectId=project1&targetProjectId=project2&linkRole=relates_to&ids=ID1,ID2');

    await expect(page.locator('.app-header .app-title')).toHaveText("Diff/merge of Polarion WorkItems");
    await expect(page.getByTestId('app-alert-title')).toHaveText("Error occurred loading diff data!");
    await expect(page.getByTestId('app-alert-message')).toHaveText("Network error occurred when attempting to fetch a resource. Be sure Polarion is started and accessible.");
  });

  test('collections data fetch fails', async ({ page }) => {
    await page.goto('/collections?sourceProjectId=project1&sourceCollectionId=collection1&targetProjectId=project2&targetCollectionId=collection2');

    await expect(page.locator('.app-header .app-title')).toHaveText("Diff/merge of Polarion Collections");
    await expect(page.getByTestId('app-alert-title')).toHaveText("Error occurred loading diff data!");
    await expect(page.getByTestId('app-alert-message')).toHaveText("Network error occurred when attempting to fetch a resource. Be sure Polarion is started and accessible.");
  });

});
