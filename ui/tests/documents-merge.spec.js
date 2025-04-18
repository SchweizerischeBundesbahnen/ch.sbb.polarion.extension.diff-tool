import { expect } from '@playwright/test';
import { test } from './test-utils';

test.describe("page of diffing documents' WorkItems", () => {

  test.beforeEach(async ({ page, mockApi }) => {
    await mockApi.mockEndpoint({ url: '**/extension/info', fixtureFile: 'version-info.json' });
    await mockApi.mockEndpoint({ url: '**/communication/settings', fixtureFile: 'communication-settings.json' });
    await mockApi.mockEndpoint({ url: '**/settings/diff/names?scope=project/elibrary/', fixtureFile: 'configs.json' });
    await mockApi.mockEndpoint({ url: '**/diff/documents', fixtureFile: 'documents-diff.json' });
    await mockApi.mockWorkItemsDiffEndpoint();
    await mockApi.mockWorkItemsMergeEndpoint();

    await page.goto('/documents?sourceProjectId=elibrary&sourceSpaceId=Specification&sourceDocument=Catalog%20Specification&targetProjectId=drivepilot&targetSpaceId=Design&targetDocument=Catalog%20Design&linkRole=relates_to');
  });

  test('merge canceled', async ({ page }) => {
    let requestWasMade = false;

    // Set up route monitoring
    await page.route('**/merge/documents', route => {
      requestWasMade = true;
      route.continue();
    });

    await expect(page.locator('.header .merge-pane')).toBeVisible({visible: true});

    await page.locator('.header .merge-pane').isVisible(); // Wait until merge pane is visible before next steps

    await expect(page.getByTestId("merge-confirmation-modal")).toBeVisible({ visible: false });

    const selectionCheckbox = page.getByTestId('EL-4977_DP-11559').locator('.merge-ticker input[type="checkbox"]').nth(0);
    await selectionCheckbox.isVisible();
    await selectionCheckbox.click();

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

    // Assert the request was not made
    expect(requestWasMade).toBe(false);
  });

  test('merge changes', async ({ page }) => {
    const requestPromise = page.waitForRequest(request => {
      return request.url().includes('/merge/documents');
    });

    await expect(page.locator('.header .merge-pane')).toBeVisible({visible: true});

    await page.locator('.header .merge-pane').isVisible(); // Wait until merge pane is visible before next steps

    await expect(page.getByTestId("merge-confirmation-modal")).toBeVisible({ visible: false });
    await expect(page.getByTestId("merge-result-modal")).toBeVisible({ visible: false });

    const selectionCheckbox = page.getByTestId('EL-4977_DP-11559').locator('.merge-ticker input[type="checkbox"]').nth(0);
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
      expect(postData.leftDocument.name).toBe("Catalog Specification");

      expect(postData.rightDocument.projectId).toBe("drivepilot");
      expect(postData.rightDocument.spaceId).toBe("Design");
      expect(postData.rightDocument.name).toBe("Catalog Design");

      expect(postData.pairs[0].leftWorkItem.projectId).toBe("elibrary");
      expect(postData.pairs[0].leftWorkItem.id).toBe("EL-4977");
      expect(postData.pairs[0].leftWorkItem.outlineNumber).toBe("2.1-2");
      expect(postData.pairs[0].leftWorkItem.externalProjectWorkItem).toBe(false);
      expect(postData.pairs[0].leftWorkItem.referenced).toBe(false);
      expect(postData.pairs[0].leftWorkItem.moveDirection).toBe(null);
      expect(postData.pairs[0].leftWorkItem.movedOutlineNumber).toBe(null);
      expect(postData.pairs[0].leftWorkItem.title).toBe("New Requirement");

      expect(postData.pairs[0].rightWorkItem.projectId).toBe("drivepilot");
      expect(postData.pairs[0].rightWorkItem.id).toBe("DP-11559");
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
      await expect(mergeResultModalList).toHaveText("Content of 1 items were modified.");

      const mergeResultModalLogLink = page.getByTestId("see-full-log");
      await expect(mergeResultModalLogLink).toBeVisible();
      await expect(mergeResultModalLogLink).toHaveText("See full log");

      const mergeResultModalLog = mergeResultModal.locator("pre");
      await expect(mergeResultModalLog).toBeVisible({visible: false});

      await mergeResultModalLogLink.click();

      await expect(mergeResultModalLog).toBeVisible();
      await expect(mergeResultModalLog).toHaveText("2025-04-16 15:38:22.215: 'MODIFIED' -- left WI 'EL-4977', right WI 'DP-11559' -- workitem 'DP-11559' modified with info from 'EL-4977'");

      const cancelButton = page.getByTestId("merge-result-modal-cancel-button");
      await expect(cancelButton).toBeVisible();
      await expect(cancelButton).toBeEnabled();
      await cancelButton.click();

      await expect(mergeResultModal).toBeVisible({ visible: false});
    }
  });

  test('created by merge', async ({ page }) => {
    const requestPromise = page.waitForRequest(request => {
      return request.url().includes('/merge/documents');
    });

    await expect(page.locator('.header .merge-pane')).toBeVisible({visible: true});

    await page.locator('.header .merge-pane').isVisible(); // Wait until merge pane is visible before next steps

    await expect(page.getByTestId("merge-confirmation-modal")).toBeVisible({ visible: false });
    await expect(page.getByTestId("merge-result-modal")).toBeVisible({ visible: false });

    const selectionCheckbox = page.getByTestId('EL-156_NONE').locator('.merge-ticker input[type="checkbox"]').nth(0);
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
    const structuralChangesWarning = page.getByTestId("merge-confirmation-structural-changes-warning");
    await expect(structuralChangesWarning).toBeVisible();
    await expect(structuralChangesWarning).toHaveCSS("font-weight", "700");
    await expect(structuralChangesWarning).toHaveText("Be aware that you have selected items to be merged which will lead to document's structural changes. After merge is finished, diffs view will automatically be reloaded to actualize documents structure.");

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
      expect(postData.leftDocument.name).toBe("Catalog Specification");

      expect(postData.rightDocument.projectId).toBe("drivepilot");
      expect(postData.rightDocument.spaceId).toBe("Design");
      expect(postData.rightDocument.name).toBe("Catalog Design");

      expect(postData.pairs[0].leftWorkItem.projectId).toBe("elibrary");
      expect(postData.pairs[0].leftWorkItem.id).toBe("EL-156");
      expect(postData.pairs[0].leftWorkItem.outlineNumber).toBe("2.2-2");
      expect(postData.pairs[0].leftWorkItem.externalProjectWorkItem).toBe(false);
      expect(postData.pairs[0].leftWorkItem.referenced).toBe(false);
      expect(postData.pairs[0].leftWorkItem.moveDirection).toBe(null);
      expect(postData.pairs[0].leftWorkItem.movedOutlineNumber).toBe(null);
      expect(postData.pairs[0].leftWorkItem.title).toBe("User must be informed on the login screen when Caps Lock is turned on");

      expect(postData.pairs[0].rightWorkItem).toBe(null);

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
      await expect(mergeResultModalList).toHaveText("1 items were created.");

      const mergeResultModalLogLink = page.getByTestId("see-full-log");
      await expect(mergeResultModalLogLink).toBeVisible();
      await expect(mergeResultModalLogLink).toHaveText("See full log");

      const mergeResultModalLog = mergeResultModal.locator("pre");
      await expect(mergeResultModalLog).toBeVisible({visible: false});

      await mergeResultModalLogLink.click();

      await expect(mergeResultModalLog).toBeVisible();
      await expect(mergeResultModalLog).toHaveText("2025-04-17 06:22:43.241: 'CREATED' -- left WI 'EL-156', right WI 'DP-11599' -- workitem 'DP-11599' moved from 'Specification / Catalog Specification'");

      const cancelButton = page.getByTestId("merge-result-modal-cancel-button");
      await expect(cancelButton).toBeVisible();
      await expect(cancelButton).toBeEnabled();
      await cancelButton.click();

      await expect(mergeResultModal).toBeVisible({ visible: false });
    }
  });

  test('creation failed', async ({ page }) => {
    const requestPromise = page.waitForRequest(request => {
      return request.url().includes('/merge/documents');
    });

    await expect(page.locator('.header .merge-pane')).toBeVisible({visible: true});

    await page.locator('.header .merge-pane').isVisible(); // Wait until merge pane is visible before next steps

    await expect(page.getByTestId("merge-confirmation-modal")).toBeVisible({ visible: false });
    await expect(page.getByTestId("merge-result-modal")).toBeVisible({ visible: false });

    const selectionCheckbox = page.getByTestId('EL-11600_NONE').locator('.merge-ticker input[type="checkbox"]').nth(0);
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
    const structuralChangesWarning = page.getByTestId("merge-confirmation-structural-changes-warning");
    await expect(structuralChangesWarning).toBeVisible();
    await expect(structuralChangesWarning).toHaveCSS("font-weight", "700");
    await expect(structuralChangesWarning).toHaveText("Be aware that you have selected items to be merged which will lead to document's structural changes. After merge is finished, diffs view will automatically be reloaded to actualize documents structure.");

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
      expect(postData.leftDocument.name).toBe("Catalog Specification");

      expect(postData.rightDocument.projectId).toBe("drivepilot");
      expect(postData.rightDocument.spaceId).toBe("Design");
      expect(postData.rightDocument.name).toBe("Catalog Design");

      expect(postData.pairs[0].leftWorkItem.projectId).toBe("elibrary");
      expect(postData.pairs[0].leftWorkItem.id).toBe("EL-11600");
      expect(postData.pairs[0].leftWorkItem.outlineNumber).toBe("2.1-4");
      expect(postData.pairs[0].leftWorkItem.externalProjectWorkItem).toBe(false);
      expect(postData.pairs[0].leftWorkItem.referenced).toBe(true);
      expect(postData.pairs[0].leftWorkItem.moveDirection).toBe(null);
      expect(postData.pairs[0].leftWorkItem.movedOutlineNumber).toBe(null);
      expect(postData.pairs[0].leftWorkItem.title).toBe("Very important requirement");

      expect(postData.pairs[0].rightWorkItem).toBe(null);

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
      await expect(mergeResultModalList).toHaveText("1 items were not merged because work items referenced in source document do not have counterparts in target project.");

      const mergeResultModalLogLink = page.getByTestId("see-full-log");
      await expect(mergeResultModalLogLink).toBeVisible();
      await expect(mergeResultModalLogLink).toHaveText("See full log");

      const mergeResultModalLog = mergeResultModal.locator("pre");
      await expect(mergeResultModalLog).toBeVisible({visible: false});

      await mergeResultModalLogLink.click();

      await expect(mergeResultModalLog).toBeVisible();
      await expect(mergeResultModalLog).toHaveText("2025-04-18 13:30:48.940: 'CREATION_FAILED' -- left WI 'EL-11600', right WI 'null' -- new workitem based on source workitem 'EL-11600' NOT created");

      const cancelButton = page.getByTestId("merge-result-modal-cancel-button");
      await expect(cancelButton).toBeVisible();
      await expect(cancelButton).toBeEnabled();
      await cancelButton.click();

      await expect(mergeResultModal).toBeVisible({ visible: false });
    }
  });

  test('deleted by merge', async ({ page }) => {
    const requestPromise = page.waitForRequest(request => {
      return request.url().includes('/merge/documents');
    });

    await expect(page.locator('.header .merge-pane')).toBeVisible({visible: true});

    await page.locator('.header .merge-pane').isVisible(); // Wait until merge pane is visible before next steps

    await expect(page.getByTestId("merge-confirmation-modal")).toBeVisible({ visible: false });
    await expect(page.getByTestId("merge-result-modal")).toBeVisible({ visible: false });

    const selectionCheckbox = page.getByTestId('NONE_DP-11570').locator('.merge-ticker input[type="checkbox"]').nth(0);
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
    const structuralChangesWarning = page.getByTestId("merge-confirmation-structural-changes-warning");
    await expect(structuralChangesWarning).toBeVisible();
    await expect(structuralChangesWarning).toHaveCSS("font-weight", "700");
    await expect(structuralChangesWarning).toHaveText("Be aware that you have selected items to be merged which will lead to document's structural changes. After merge is finished, diffs view will automatically be reloaded to actualize documents structure.");

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
      expect(postData.leftDocument.name).toBe("Catalog Specification");

      expect(postData.rightDocument.projectId).toBe("drivepilot");
      expect(postData.rightDocument.spaceId).toBe("Design");
      expect(postData.rightDocument.name).toBe("Catalog Design");

      expect(postData.pairs[0].leftWorkItem).toBe(null);

      expect(postData.pairs[0].rightWorkItem.projectId).toBe("drivepilot");
      expect(postData.pairs[0].rightWorkItem.id).toBe("DP-11570");
      expect(postData.pairs[0].rightWorkItem.outlineNumber).toBe("2.3");
      expect(postData.pairs[0].rightWorkItem.externalProjectWorkItem).toBe(false);
      expect(postData.pairs[0].rightWorkItem.referenced).toBe(false);
      expect(postData.pairs[0].rightWorkItem.moveDirection).toBe(null);
      expect(postData.pairs[0].rightWorkItem.movedOutlineNumber).toBe(null);
      expect(postData.pairs[0].rightWorkItem.title).toBe("New paragraph");

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
      await expect(mergeResultModalList).toHaveText("1 items were deleted.");

      const mergeResultModalLogLink = page.getByTestId("see-full-log");
      await expect(mergeResultModalLogLink).toBeVisible();
      await expect(mergeResultModalLogLink).toHaveText("See full log");

      const mergeResultModalLog = mergeResultModal.locator("pre");
      await expect(mergeResultModalLog).toBeVisible({visible: false});

      await mergeResultModalLogLink.click();

      await expect(mergeResultModalLog).toBeVisible();
      await expect(mergeResultModalLog).toHaveText("2025-04-17 06:22:43.241: 'DELETED' -- left WI 'null', right WI 'DP-11570' -- workitem 'DP-11570' deleted in target document");

      const cancelButton = page.getByTestId("merge-result-modal-cancel-button");
      await expect(cancelButton).toBeVisible();
      await expect(cancelButton).toBeEnabled();
      await cancelButton.click();

      await expect(mergeResultModal).toBeVisible({ visible: false });
    }
  });

  test('merge into referenced item prohibited', async ({ page }) => {
    const requestPromise = page.waitForRequest(request => {
      return request.url().includes('/merge/documents');
    });

    await expect(page.locator('.header .merge-pane')).toBeVisible({visible: true});

    await page.locator('.header .merge-pane').isVisible(); // Wait until merge pane is visible before next steps

    await expect(page.getByTestId("merge-confirmation-modal")).toBeVisible({ visible: false });
    await expect(page.getByTestId("merge-result-modal")).toBeVisible({ visible: false });

    const selectionCheckbox = page.getByTestId('EL-11575_EL-11575').locator('.merge-ticker input[type="checkbox"]').nth(0);
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
      expect(postData.leftDocument.name).toBe("Catalog Specification");

      expect(postData.rightDocument.projectId).toBe("drivepilot");
      expect(postData.rightDocument.spaceId).toBe("Design");
      expect(postData.rightDocument.name).toBe("Catalog Design");

      expect(postData.pairs[0].leftWorkItem.projectId).toBe("elibrary");
      expect(postData.pairs[0].leftWorkItem.id).toBe("EL-11575");
      expect(postData.pairs[0].leftWorkItem.outlineNumber).toBe("2.1-3");
      expect(postData.pairs[0].leftWorkItem.externalProjectWorkItem).toBe(false);
      expect(postData.pairs[0].leftWorkItem.referenced).toBe(false);
      expect(postData.pairs[0].leftWorkItem.moveDirection).toBe(null);
      expect(postData.pairs[0].leftWorkItem.movedOutlineNumber).toBe(null);
      expect(postData.pairs[0].leftWorkItem.title).toBe("Another one requirement");

      expect(postData.pairs[0].rightWorkItem.projectId).toBe("elibrary");
      expect(postData.pairs[0].rightWorkItem.id).toBe("EL-11575");
      expect(postData.pairs[0].rightWorkItem.outlineNumber).toBe("2.1-3");
      expect(postData.pairs[0].rightWorkItem.externalProjectWorkItem).toBe(false);
      expect(postData.pairs[0].rightWorkItem.referenced).toBe(true);
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
      await expect(mergeResultModalLog).toHaveText("2025-04-18 12:48:11.184: 'PROHIBITED' -- left WI 'EL-11575', right WI 'EL-11575' -- can't merge into referenced workitem 'EL-11575' in target document 'Design / Catalog Design'");

      const cancelButton = page.getByTestId("merge-result-modal-cancel-button");
      await expect(cancelButton).toBeVisible();
      await expect(cancelButton).toBeEnabled();
      await cancelButton.click();

      await expect(mergeResultModal).toBeVisible({ visible: false});
    }
  });

  test('merge from referenced item prohibited', async ({ page }) => {
    const requestPromise = page.waitForRequest(request => {
      return request.url().includes('/merge/documents');
    });

    await expect(page.locator('.header .merge-pane')).toBeVisible({visible: true});

    await page.locator('.header .merge-pane').isVisible(); // Wait until merge pane is visible before next steps

    await expect(page.getByTestId("merge-confirmation-modal")).toBeVisible({ visible: false });
    await expect(page.getByTestId("merge-result-modal")).toBeVisible({ visible: false });

    const selectionCheckbox = page.getByTestId('EL-11575_EL-11575').locator('.merge-ticker input[type="checkbox"]').nth(0);
    await selectionCheckbox.isVisible();
    await selectionCheckbox.click();

    const mergeButton = page.locator('.header .merge-pane .merge-button .btn').nth(1);
    await expect(mergeButton).toBeEnabled();
    await mergeButton.isEnabled();
    await mergeButton.click();

    await expect(page.getByTestId("merge-confirmation-modal")).toBeVisible();
    const mergeConfirmation = page.getByTestId("merge-confirmation");
    await expect(mergeConfirmation).toBeVisible();
    await expect(mergeConfirmation).toHaveText("Are you sure you want to merge selected items from target to source document?");

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
      expect(postData.direction).toBe(1);
      expect(postData.linkRole).toBe("relates_to");

      expect(postData.leftDocument.projectId).toBe("elibrary");
      expect(postData.leftDocument.spaceId).toBe("Specification");
      expect(postData.leftDocument.name).toBe("Catalog Specification");

      expect(postData.rightDocument.projectId).toBe("drivepilot");
      expect(postData.rightDocument.spaceId).toBe("Design");
      expect(postData.rightDocument.name).toBe("Catalog Design");

      expect(postData.pairs[0].leftWorkItem.projectId).toBe("elibrary");
      expect(postData.pairs[0].leftWorkItem.id).toBe("EL-11575");
      expect(postData.pairs[0].leftWorkItem.outlineNumber).toBe("2.1-3");
      expect(postData.pairs[0].leftWorkItem.externalProjectWorkItem).toBe(false);
      expect(postData.pairs[0].leftWorkItem.referenced).toBe(false);
      expect(postData.pairs[0].leftWorkItem.moveDirection).toBe(null);
      expect(postData.pairs[0].leftWorkItem.movedOutlineNumber).toBe(null);
      expect(postData.pairs[0].leftWorkItem.title).toBe("Another one requirement");

      expect(postData.pairs[0].rightWorkItem.projectId).toBe("elibrary");
      expect(postData.pairs[0].rightWorkItem.id).toBe("EL-11575");
      expect(postData.pairs[0].rightWorkItem.outlineNumber).toBe("2.1-3");
      expect(postData.pairs[0].rightWorkItem.externalProjectWorkItem).toBe(false);
      expect(postData.pairs[0].rightWorkItem.referenced).toBe(true);
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

    await expect(page.locator('.header .merge-pane')).toBeVisible({visible: true});

    await page.locator('.header .merge-pane').isVisible(); // Wait until merge pane is visible before next steps

    await expect(page.getByTestId("merge-confirmation-modal")).toBeVisible({ visible: false });
    await expect(page.getByTestId("merge-result-modal")).toBeVisible({ visible: false });

    const selectionCheckbox = page.getByTestId('EL-4977_DP-11559').locator('.merge-ticker input[type="checkbox"]').nth(0);
    await selectionCheckbox.isVisible();
    await selectionCheckbox.click();

    const mergeButton = page.locator('.header .merge-pane .merge-button .btn').nth(1);
    await expect(mergeButton).toBeEnabled();
    await mergeButton.isEnabled();
    await mergeButton.click();

    await expect(page.getByTestId("merge-confirmation-modal")).toBeVisible();
    const mergeConfirmation = page.getByTestId("merge-confirmation");
    await expect(mergeConfirmation).toBeVisible();
    await expect(mergeConfirmation).toHaveText("Are you sure you want to merge selected items from target to source document?");

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
      expect(postData.direction).toBe(1);
      expect(postData.linkRole).toBe("relates_to");

      expect(postData.leftDocument.projectId).toBe("elibrary");
      expect(postData.leftDocument.spaceId).toBe("Specification");
      expect(postData.leftDocument.name).toBe("Catalog Specification");

      expect(postData.rightDocument.projectId).toBe("drivepilot");
      expect(postData.rightDocument.spaceId).toBe("Design");
      expect(postData.rightDocument.name).toBe("Catalog Design");

      expect(postData.pairs[0].leftWorkItem.projectId).toBe("elibrary");
      expect(postData.pairs[0].leftWorkItem.id).toBe("EL-4977");
      expect(postData.pairs[0].leftWorkItem.outlineNumber).toBe("2.1-2");
      expect(postData.pairs[0].leftWorkItem.externalProjectWorkItem).toBe(false);
      expect(postData.pairs[0].leftWorkItem.referenced).toBe(false);
      expect(postData.pairs[0].leftWorkItem.moveDirection).toBe(null);
      expect(postData.pairs[0].leftWorkItem.movedOutlineNumber).toBe(null);
      expect(postData.pairs[0].leftWorkItem.title).toBe("New Requirement");

      expect(postData.pairs[0].rightWorkItem.projectId).toBe("drivepilot");
      expect(postData.pairs[0].rightWorkItem.id).toBe("DP-11559");
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
      await expect(mergeResultModalLog).toHaveText("2025-04-18 13:02:30.818: 'CONFLICTED' -- left WI 'EL-4977', right WI 'DP-11559' -- merge is not allowed: target workitem 'EL-4977' revision '5976' has been already changed to '6462'");

      const cancelButton = page.getByTestId("merge-result-modal-cancel-button");
      await expect(cancelButton).toBeVisible();
      await expect(cancelButton).toBeEnabled();
      await cancelButton.click();

      await expect(mergeResultModal).toBeVisible({ visible: false});
    }
  });

});
