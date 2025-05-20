import { normalizeHtml, test } from "./test-utils";
import { expect } from "@playwright/test";


test.describe("page of diffing Collections c", () => {

  test.beforeEach(async ({page, mockApi}) => {
    await mockApi.mockEndpoint({url: '**/extension/info', fixtureFile: 'version-info.json'});
    await mockApi.mockEndpoint({url: '**/communication/settings', fixtureFile: 'communication-settings.json'});
    await mockApi.mockEndpoint({url: '**/settings/diff/names?scope=project/elibrary/', fixtureFile: 'configs.json'});
    await mockApi.mockEndpoint({url: '**/diff/collections', fixtureFile: 'collection-merge.json'});
    await mockApi.mockEndpoint({url: '**/diff/documents', fixtureFile: 'documents-from-collection-merge.json', method: 'POST'});
    await mockApi.mockWorkItemsDiffEndpoint();


    await page.goto('/collections?sourceProjectId=elibrary&sourceCollectionId=1&targetProjectId=drivepilot&targetCollectionId=1&linkRole=relates_to&config=Default&compareAs=Workitems&sourceSpaceId=Specification&sourceDocument=Administration+Specification&targetSpaceId=Specification&targetDocument=Administration+Specification');
  });

  test('page header', async ({ page }) => {
    await expect(page.locator('.app-header .app-title')).toHaveText("Diff/merge of Polarion Collections");
    await expect(page.locator('.app-header .extension-info')).toHaveText("v5.1.2 | 2025-03-17 14:48");
    await page.screenshot({ path: 'page-header.png', fullPage: true });

    await expect(page.getByTestId('LEFT-collection').locator(".path-label:has-text('project')")).toHaveText("project:");
    await expect(page.getByTestId('LEFT-collection').locator(".path-label:has-text('collection:')")).toHaveText("collection:");

    await expect(page.getByTestId('LEFT-collection').locator(".path-value").nth(0)).toHaveText("E-Library");
    await expect(page.getByTestId('LEFT-collection').locator(".path-value").nth(1)).toHaveText("Specification v1.0");

    await expect(page.getByTestId('RIGHT-collection').locator(".path-label:has-text('project')")).toHaveText("project:");
    await expect(page.getByTestId('RIGHT-collection').locator(".path-label:has-text('collection:')")).toHaveText("collection:");

    await expect(page.getByTestId('RIGHT-collection').locator(".path-value").nth(0)).toHaveText("Drive Pilot");
    await expect(page.getByTestId('RIGHT-collection').locator(".path-value").nth(1)).toHaveText("System Specification v.1");

    await expect(page.getByTestId('LEFT-space').locator(".path-label")).toHaveText("space:");
    await expect(page.getByTestId('LEFT-space').locator(".path-value")).toHaveText("Specification");

    await expect(page.getByTestId('RIGHT-space').locator(".path-label")).toHaveText("space:");
    await expect(page.getByTestId('RIGHT-space').locator(".path-value")).toHaveText("Specification");

    await expect(page.getByTestId('LEFT-doc-title')).toHaveText("Administration SpecificationHEAD (rev. 9491)");
    await expect(page.getByTestId('RIGHT-doc-title')).toHaveText("Administration SpecificationHEAD (rev. 9491)");
  });

  test('verify merge functionality and cancel merge operation', async ({ page }) => {
    let requestWasMade = false;

    // Set up route monitoring
    await page.route('**/merge/documents', route => {
      requestWasMade = true;
      route.continue();
    });

    await page.waitForSelector('.header .merge-pane', { state: 'visible' }); // Wait until merge pane is visible before next steps

    const EL144_DP11549 = page.getByTestId('EL-14873_DP-536');

    const header = EL144_DP11549.locator(".header");
    await expect(header).toBeVisible();

    await expect(EL144_DP11549.locator(".header .merge-ticker .form-check input[type='checkbox']")).toBeVisible({ visible: true });
    await EL144_DP11549.locator(".header .merge-ticker .form-check input[type='checkbox']").check();

    const mergeButton = page.locator('.header .merge-pane .merge-button .btn').nth(0);
    await expect(mergeButton).toBeEnabled();
    await mergeButton.isEnabled();
    await mergeButton.click();

    await expect(page.getByTestId("merge-confirmation-modal")).toBeVisible();

    const cancelButton = page.getByTestId("merge-confirmation-modal-cancel-button");
    await expect(cancelButton).toBeVisible();
    await expect(cancelButton).toBeEnabled();

    await cancelButton.isVisible();
    cancelButton.click();

    await expect(page.getByTestId("merge-confirmation-modal")).toBeVisible({ visible: false });

    await page.waitForTimeout(500);

    expect(requestWasMade).toBe(false);
  });
})
