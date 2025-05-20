import { normalizeHtml, test } from "./test-utils";
import { expect } from "@playwright/test";


test.describe("page of diffing Collections c", () => {

  test.beforeEach(async ({page, mockApi}) => {
    await mockApi.mockEndpoint({url: '**/extension/info', fixtureFile: 'version-info.json'});
    await mockApi.mockEndpoint({url: '**/communication/settings', fixtureFile: 'communication-settings.json'});
    await mockApi.mockEndpoint({url: '**/settings/diff/names?scope=project/elibrary/', fixtureFile: 'configs.json'});
    await mockApi.mockEndpoint({url: '**/diff/collections', fixtureFile: 'collection-merge.json'});
    await mockApi.mockEndpoint({url: '**/diff/documents', fixtureFile: 'documents-from-collection-diff.json', method: 'POST'});
    await mockApi.mockEndpoint({url: '**/merge/documents', fixtureFile: 'documents-from-collection-merge.json', method: 'POST'});

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

  test('cancel merge operation', async ({ page }) => {
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

  test('accept merge operation', async ({ page }) => {
    const requestPromise = page.waitForRequest(request => {
      return request.url().includes('/merge/documents');
    });

    await page.waitForSelector('.header .merge-pane', { state: 'visible' }); // Wait until merge pane is visible before next steps

    await expect(page.getByTestId("merge-confirmation-modal")).toBeVisible({ visible: false });
    await expect(page.getByTestId("merge-result-modal")).toBeVisible({ visible: false });

    const selectionCheckbox = page.getByTestId('EL-14873_DP-536').locator('.merge-ticker input[type="checkbox"]').nth(0);
    await selectionCheckbox.isVisible();
    await selectionCheckbox.click();

    const mergeButton = page.locator('.header .merge-pane .merge-button .btn').nth(0);
    await expect(mergeButton).toBeEnabled();
    await mergeButton.isEnabled();
    await mergeButton.click();

    await expect(page.getByTestId("merge-confirmation-modal")).toBeVisible();
    const mergeConfirmation = page.getByTestId("merge-confirmation");
    await expect(mergeConfirmation).toBeVisible();
    await expect(mergeConfirmation).toHaveText("Are you sure you want to merge selected items from source to target document?");

    const actionButton = page.getByTestId("merge-confirmation-modal-action-button");
    await expect(actionButton).toBeVisible();
    await expect(actionButton).toBeEnabled();

    await actionButton.isVisible();
    actionButton.click();

    await expect(page.getByTestId("merge-confirmation-modal")).toBeVisible({ visible: false });

    const request = await requestPromise;

    expect(request.method()).toBe('POST');
    expect(request.url()).toContain('/merge/documents');

    if (request.method() === 'POST') {
      const postData = JSON.parse(request.postData() || '{}');
      expect(postData.allowReferencedWorkItemMerge).toBe(false);
      expect(postData.configName).toBe("Default");
      expect(postData.direction).toBe(0);
      expect(postData.linkRole).toBe("relates_to");

      expect(postData.leftDocument.projectId).toBe("elibrary");
      expect(postData.leftDocument.spaceId).toBe("Specification");
      expect(postData.leftDocument.name).toBe("Administration Specification");

      expect(postData.rightDocument.projectId).toBe("drivepilot");
      expect(postData.rightDocument.spaceId).toBe("Specification");
      expect(postData.rightDocument.name).toBe("Administration Specification");

      expect(postData.pairs[0].leftWorkItem.projectId).toBe("elibrary");
      expect(postData.pairs[0].leftWorkItem.id).toBe("EL-14873");
      expect(postData.pairs[0].leftWorkItem.outlineNumber).toBe("2.1-1");
      expect(postData.pairs[0].leftWorkItem.externalProjectWorkItem).toBe(false);
      expect(postData.pairs[0].leftWorkItem.referenced).toBe(false);
      expect(postData.pairs[0].leftWorkItem.moveDirection).toBe(null);
      expect(postData.pairs[0].leftWorkItem.movedOutlineNumber).toBe(null);
      expect(postData.pairs[0].leftWorkItem.title).toBe("User can be assigned into multiple user roles");

      expect(postData.pairs[0].rightWorkItem.projectId).toBe("drivepilot");
      expect(postData.pairs[0].rightWorkItem.id).toBe("DP-536");
      expect(postData.pairs[0].rightWorkItem.outlineNumber).toBe("2.1-1");
      expect(postData.pairs[0].rightWorkItem.externalProjectWorkItem).toBe(false);
      expect(postData.pairs[0].rightWorkItem.referenced).toBe(false);
      expect(postData.pairs[0].rightWorkItem.moveDirection).toBe(null);
      expect(postData.pairs[0].rightWorkItem.movedOutlineNumber).toBe(null);
      expect(postData.pairs[0].rightWorkItem.title).toBe("User can be assigned into multiple user roles");

      await page.screenshot({ path: 'page-header.png', fullPage: true });

      const mergeResultModal = page.getByTestId("merge-result-modal");
      await expect(mergeResultModal).toBeVisible();

      const mergeResultModalTitle = mergeResultModal.locator(".modal-title");
      await expect(mergeResultModalTitle).toBeVisible();
      await expect(mergeResultModalTitle).toHaveText("Merge Report");

      const mergeResultModalMessage = page.getByTestId("merge-completed-with-result");
      await expect(mergeResultModalMessage).toBeVisible();
      await expect(mergeResultModalMessage).toHaveText("Merge operation completed with following result:");

      const mergeResultModalList = mergeResultModal.locator("ul");
      await expect(mergeResultModalList).toBeVisible();
      await expect(mergeResultModalList).toHaveText("Content of 1 items were modified.");

      const mergeResultModalLogLink = page.getByTestId("see-full-log");
      await expect(mergeResultModalLogLink).toBeVisible();
      await expect(mergeResultModalLogLink).toHaveText("See full log");

      const mergeResultModalLog = mergeResultModal.locator("pre");
      await expect(mergeResultModalLog).toBeVisible({visible: false});

      await mergeResultModalLogLink.click();

      await expect(mergeResultModalLog).toBeVisible();
      await expect(mergeResultModalLog).toHaveText("2025-05-20 08:17:41.743: 'MODIFIED' -- left WI 'EL-14873', right WI 'DP-537' -- workitem 'DP-537' modified with info from 'EL-14874'");

      const cancelButton = page.getByTestId("merge-result-modal-cancel-button");
      await expect(cancelButton).toBeVisible();
      await expect(cancelButton).toBeEnabled();
      await cancelButton.click();

      await expect(mergeResultModal).toBeVisible({ visible: false});
    }
  });
})
