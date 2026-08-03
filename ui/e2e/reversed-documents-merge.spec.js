import { expect } from '@playwright/test';
import { test } from './test-utils';

test.describe("page of diffing reversed documents' WorkItems", () => {

  test.beforeEach(async ({ page, mockApi }) => {
    await mockApi.mockEndpoint({ url: '**/extension/info', fixtureFile: 'version-info.json' });
    await mockApi.mockEndpoint({ url: '**/communication/settings', fixtureFile: 'communication-settings.json' });
    await mockApi.mockEndpoint({ url: '**/settings/diff/names?scope=project/drivepilot/', fixtureFile: 'configs.json' });
    await mockApi.mockEndpoint({ url: '**/diff/documents', fixtureFile: 'reversed-documents-diff.json' });
    await mockApi.mockWorkItemsDiffEndpoint();
    await mockApi.mockWorkItemsMergeEndpoint();

    await page.goto('/documents?sourceProjectId=drivepilot&sourceSpaceId=Design&sourceDocument=Catalog%20Design&targetProjectId=elibrary&targetSpaceId=Specification&targetDocument=Catalog%20Specification&linkRole=relates_to');
  });

  test('merge from referenced item prohibited', async ({ page }) => {
    const requestPromise = page.waitForRequest(request => {
      return request.url().includes('/merge/documents');
    });

    await page.waitForSelector('.header .merge-pane', { state: 'visible' }); // Wait until merge pane is visible before next steps

    await expect(page.getByTestId("merge-confirmation-modal")).toBeVisible({ visible: false });
    await expect(page.getByTestId("merge-result-modal")).toBeVisible({ visible: false });

    const selectionCheckbox = page.getByTestId('EL-11575_EL-11575').locator('.merge-ticker input[type="checkbox"]').nth(0);
    await selectionCheckbox.isVisible();
    await selectionCheckbox.click();

    const mergeButton = page.locator('.header .merge-pane .merge-button .btn');
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
    await actionButton.click();

    await expect(page.getByTestId("merge-confirmation-modal")).toBeVisible({ visible: false });

    const request = await requestPromise;

    expect(request.method()).toBe('POST');
    expect(request.url()).toContain('/merge/documents');

    if (request.method() === 'POST') {
      const postData = JSON.parse(request.postData() || '{}');
      expect(postData.allowReferencedWorkItemMerge).toBe(false);
      expect(postData.configName).toBe("Default");
      expect(postData.mergeDirection).toBe(0);
      expect(postData.linkRole).toBe("relates_to");

      expect(postData.leftDocument.projectId).toBe("drivepilot");
      expect(postData.leftDocument.spaceId).toBe("Design");
      expect(postData.leftDocument.name).toBe("Catalog Design");

      expect(postData.rightDocument.projectId).toBe("elibrary");
      expect(postData.rightDocument.spaceId).toBe("Specification");
      expect(postData.rightDocument.name).toBe("Catalog Specification");

      expect(postData.pairs[0].leftWorkItem.projectId).toBe("elibrary");
      expect(postData.pairs[0].leftWorkItem.id).toBe("EL-11575");
      expect(postData.pairs[0].leftWorkItem.outlineNumber).toBe("2.1-3");
      expect(postData.pairs[0].leftWorkItem.externalProjectWorkItem).toBe(false);
      expect(postData.pairs[0].leftWorkItem.referenced).toBe(true);
      expect(postData.pairs[0].leftWorkItem.moveDirection).toBe(null);
      expect(postData.pairs[0].leftWorkItem.movedOutlineNumber).toBe(null);
      expect(postData.pairs[0].leftWorkItem.title).toBe("Another one requirement");

      expect(postData.pairs[0].rightWorkItem.projectId).toBe("elibrary");
      expect(postData.pairs[0].rightWorkItem.id).toBe("EL-11575");
      expect(postData.pairs[0].rightWorkItem.outlineNumber).toBe("2.1-3");
      expect(postData.pairs[0].rightWorkItem.externalProjectWorkItem).toBe(false);
      expect(postData.pairs[0].rightWorkItem.referenced).toBe(false);
      expect(postData.pairs[0].rightWorkItem.moveDirection).toBe(null);
      expect(postData.pairs[0].rightWorkItem.movedOutlineNumber).toBe(null);
      expect(postData.pairs[0].rightWorkItem.title).toBe("Another one requirement");

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
      await expect(mergeResultModalList).toHaveText("1 items were not merged because such operation is logically prohibited.");

      const mergeResultModalLogLink = page.getByTestId("see-full-log");
      await expect(mergeResultModalLogLink).toBeVisible();
      await expect(mergeResultModalLogLink).toHaveText("See full log");

      const mergeResultModalLog = mergeResultModal.locator("pre");
      await expect(mergeResultModalLog).toBeVisible({visible: false});

      await mergeResultModalLogLink.click();

      await expect(mergeResultModalLog).toBeVisible();
      await expect(mergeResultModalLog).toHaveText("2025-04-18 12:56:16.904: 'PROHIBITED' -- left WI 'EL-11575', right WI 'EL-11575' -- don't allow merging referenced work item into included one");

      const cancelButton = page.getByTestId("merge-result-modal-cancel-button");
      await expect(cancelButton).toBeVisible();
      await expect(cancelButton).toBeEnabled();
      await cancelButton.click();

      await expect(mergeResultModal).toBeVisible({ visible: false});
    }
  });

  test('conflicted merge', async ({ page }) => {
    const requestPromise = page.waitForRequest(request => {
      return request.url().includes('/merge/documents');
    });

    await page.waitForSelector('.header .merge-pane', { state: 'visible' }); // Wait until merge pane is visible before next steps

    await expect(page.getByTestId("merge-confirmation-modal")).toBeVisible({ visible: false });
    await expect(page.getByTestId("merge-result-modal")).toBeVisible({ visible: false });

    const selectionCheckbox = page.getByTestId('DP-11559_EL-4977').locator('.merge-ticker input[type="checkbox"]').nth(0);
    await selectionCheckbox.isVisible();
    await selectionCheckbox.click();

    const mergeButton = page.locator('.header .merge-pane .merge-button .btn');
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
    await actionButton.click();

    await expect(page.getByTestId("merge-confirmation-modal")).toBeVisible({ visible: false });

    const request = await requestPromise;

    expect(request.method()).toBe('POST');
    expect(request.url()).toContain('/merge/documents');

    if (request.method() === 'POST') {
      const postData = JSON.parse(request.postData() || '{}');
      expect(postData.allowReferencedWorkItemMerge).toBe(false);
      expect(postData.configName).toBe("Default");
      expect(postData.mergeDirection).toBe(0);
      expect(postData.linkRole).toBe("relates_to");

      expect(postData.leftDocument.projectId).toBe("drivepilot");
      expect(postData.leftDocument.spaceId).toBe("Design");
      expect(postData.leftDocument.name).toBe("Catalog Design");

      expect(postData.rightDocument.projectId).toBe("elibrary");
      expect(postData.rightDocument.spaceId).toBe("Specification");
      expect(postData.rightDocument.name).toBe("Catalog Specification");

      expect(postData.pairs[0].leftWorkItem.projectId).toBe("drivepilot");
      expect(postData.pairs[0].leftWorkItem.id).toBe("DP-11559");
      expect(postData.pairs[0].leftWorkItem.outlineNumber).toBe("2.1-2");
      expect(postData.pairs[0].leftWorkItem.externalProjectWorkItem).toBe(false);
      expect(postData.pairs[0].leftWorkItem.referenced).toBe(false);
      expect(postData.pairs[0].leftWorkItem.moveDirection).toBe(null);
      expect(postData.pairs[0].leftWorkItem.movedOutlineNumber).toBe(null);
      expect(postData.pairs[0].leftWorkItem.title).toBe("New Requirement");

      expect(postData.pairs[0].rightWorkItem.projectId).toBe("elibrary");
      expect(postData.pairs[0].rightWorkItem.id).toBe("EL-4977");
      expect(postData.pairs[0].rightWorkItem.outlineNumber).toBe("2.1-2");
      expect(postData.pairs[0].rightWorkItem.externalProjectWorkItem).toBe(false);
      expect(postData.pairs[0].rightWorkItem.referenced).toBe(false);
      expect(postData.pairs[0].rightWorkItem.moveDirection).toBe(null);
      expect(postData.pairs[0].rightWorkItem.movedOutlineNumber).toBe(null);
      expect(postData.pairs[0].rightWorkItem.title).toBe("New Requirement");

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
      await expect(mergeResultModalList).toHaveText("1 items were not merged because of concurrent modifications.");

      const mergeResultModalLogLink = page.getByTestId("see-full-log");
      await expect(mergeResultModalLogLink).toBeVisible();
      await expect(mergeResultModalLogLink).toHaveText("See full log");

      const mergeResultModalLog = mergeResultModal.locator("pre");
      await expect(mergeResultModalLog).toBeVisible({visible: false});

      await mergeResultModalLogLink.click();

      await expect(mergeResultModalLog).toBeVisible();
      await expect(mergeResultModalLog).toHaveText("2025-04-18 13:02:30.818: 'CONFLICTED' -- left WI 'DP-11559', right WI 'EL-4977' -- merge is not allowed: target workitem 'EL-4977' revision '5976' has been already changed to '6462'");

      const cancelButton = page.getByTestId("merge-result-modal-cancel-button");
      await expect(cancelButton).toBeVisible();
      await expect(cancelButton).toBeEnabled();
      await cancelButton.click();

      await expect(mergeResultModal).toBeVisible({ visible: false});
    }
  });


});
