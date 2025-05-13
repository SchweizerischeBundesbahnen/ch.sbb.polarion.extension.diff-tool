import { expect } from '@playwright/test';
import { test, normalizeHtml } from './test-utils';

test.describe("diffing of documents' contents", () => {

  test.beforeEach(async ({ page, mockApi }) => {
    await mockApi.mockEndpoint({ url: '**/extension/info', fixtureFile: 'version-info.json' });
    await mockApi.mockEndpoint({ url: '**/communication/settings', fixtureFile: 'communication-settings.json' });
    await mockApi.mockEndpoint({ url: '**/settings/diff/names?scope=project/elibrary/', fixtureFile: 'configs.json' });
    await mockApi.mockEndpoint({ url: '**/diff/documents-content', fixtureFile: 'documents-content.json' });
    await mockApi.mockContentMergeEndpoint();

    await page.goto('/documents?sourceProjectId=elibrary&sourceSpaceId=Specification&sourceDocument=Building%20Specification%20v2&targetProjectId=drivepilot&targetSpaceId=Specification&targetDocument=Building%20Specification%20v3&linkRole=relates_to&config=Default&compareAs=Content');
  });

  test('EL-63010 - DP-1152 anchors diff', async ({ page }) => {
    await page.waitForSelector('.header .merge-pane', { state: 'visible' }); // Wait until merge pane is visible before next steps

    const invisibleChange = page.getByTestId('EL-63010_DP-1152');
    await expect(invisibleChange.locator(".header")).toBeVisible(); // despite the item doesn't have a change, its title still must be displayed
    await expect(invisibleChange.locator(".diff-viewer")).toBeVisible({ visible: false });
  });

  test('EL-63012 - DP-1154 anchors diff', async ({ page }) => {
    await page.waitForSelector('.header .merge-pane', { state: 'visible' }); // Wait until merge pane is visible before next steps

    const visibleChange = page.getByTestId('EL-63012_DP-1154');
    await expect(visibleChange.locator(".header")).toBeVisible();
    await expect(visibleChange.locator(".diff-viewer .merge-ticker")).toBeVisible();
    await expect(visibleChange.locator(".diff-viewer .merge-ticker .form-check input[type='checkbox']")).toBeVisible();

    const diffLeft = visibleChange.locator(".diff-viewer .diff-leaf.left");
    await expect(diffLeft).toBeVisible();
    const diffLeftHtml = await diffLeft.innerHTML();
    expect(normalizeHtml(diffLeftHtml)).toBe(normalizeHtml(`
        <div class=\"polarion-dle-pdf\"><p id=\"polarion_3\">The contractor shall <span class=\"diff-html-added\" id=\"removed-diff-0\" previous=\"first-diff\" changeid=\"removed-diff-0\" next=\"added-diff-0\">be responsible for the full execution of the project, including the supply of all </span><span class=\"diff-html-removed\" id=\"added-diff-0\" previous=\"removed-diff-0\" changeid=\"added-diff-0\" next=\"removed-diff-1\">provide all labor, </span>materials, <span class=\"diff-html-added\" id=\"removed-diff-1\" previous=\"added-diff-0\" changeid=\"removed-diff-1\" next=\"added-diff-1\">coordination of subcontractors</span><span class=\"diff-html-removed\" id=\"added-diff-1\" previous=\"removed-diff-1\" changeid=\"added-diff-1\" next=\"removed-diff-2\">tools</span>, <span class=\"diff-html-added\" id=\"removed-diff-2\" previous=\"added-diff-1\" changeid=\"removed-diff-2\" next=\"added-diff-2\">health and safety compliance</span><span class=\"diff-html-removed\" id=\"added-diff-2\" previous=\"removed-diff-2\" changeid=\"added-diff-2\" next=\"removed-diff-3\">equipment</span>, and <span class=\"diff-html-added\" id=\"removed-diff-3\" previous=\"added-diff-2\" changeid=\"removed-diff-3\" next=\"added-diff-3\">completion of works to a defect-free standard</span><span class=\"diff-html-removed\" id=\"added-diff-3\" previous=\"removed-diff-3\" changeid=\"added-diff-3\" next=\"removed-diff-4\">supervision required to complete the building as specified</span>. This <span class=\"diff-html-added\" id=\"removed-diff-4\" previous=\"added-diff-3\" changeid=\"removed-diff-4\" next=\"added-diff-4\">encompasses </span><span class=\"diff-html-removed\" id=\"added-diff-4\" previous=\"removed-diff-4\" changeid=\"added-diff-4\" next=\"removed-diff-5\">includes </span>site <span class=\"diff-html-added\" id=\"removed-diff-5\" previous=\"added-diff-4\" changeid=\"removed-diff-5\" next=\"added-diff-5\">clearance</span><span class=\"diff-html-removed\" id=\"added-diff-5\" previous=\"removed-diff-5\" changeid=\"added-diff-5\" next=\"removed-diff-6\">preparation</span>, <span class=\"diff-html-added\" id=\"removed-diff-6\" previous=\"added-diff-5\" changeid=\"removed-diff-6\" next=\"added-diff-6\">foundation works</span><span class=\"diff-html-removed\" id=\"added-diff-6\" previous=\"removed-diff-6\" changeid=\"added-diff-6\" next=\"removed-diff-7\">structural work</span>, <span class=\"diff-html-added\" id=\"removed-diff-7\" previous=\"added-diff-6\" changeid=\"removed-diff-7\" next=\"added-diff-7\">core construction</span><span class=\"diff-html-removed\" id=\"added-diff-7\" previous=\"removed-diff-7\" changeid=\"added-diff-7\" next=\"removed-diff-8\">architectural finishes</span>, MEP <span class=\"diff-html-added\" id=\"removed-diff-8\" previous=\"added-diff-7\" changeid=\"removed-diff-8\" next=\"added-diff-8\">installations</span><span class=\"diff-html-removed\" id=\"added-diff-8\" previous=\"removed-diff-8\" changeid=\"added-diff-8\" next=\"removed-diff-9\">systems</span>, <span class=\"diff-html-added\" id=\"removed-diff-9\" previous=\"added-diff-8\" changeid=\"removed-diff-9\" next=\"removed-diff-10\">interior fit-out, </span>and <span class=\"diff-html-added\" id=\"removed-diff-10\" previous=\"removed-diff-9\" changeid=\"removed-diff-10\" next=\"added-diff-9\">landscape </span><span class=\"diff-html-removed\" id=\"added-diff-9\" previous=\"removed-diff-10\" changeid=\"added-diff-9\" next=\"last-diff\">external </span>works.</p></div>
    `));

    const diffRight = visibleChange.locator(".diff-viewer .diff-leaf.right");
    await expect(diffRight).toBeVisible();
    const diffRightHtml = await diffRight.innerHTML();
    expect(normalizeHtml(diffRightHtml)).toBe(normalizeHtml(`
        <div class=\"polarion-dle-pdf\"><p id=\"polarion_3\">The contractor shall <span class=\"diff-html-removed\" id=\"removed-diff-0\" previous=\"first-diff\" changeid=\"removed-diff-0\" next=\"added-diff-0\">provide </span><span class=\"diff-html-added\" id=\"added-diff-0\" previous=\"removed-diff-0\" changeid=\"added-diff-0\" next=\"removed-diff-1\">be responsible for the full execution of the project, including the supply of </span>all <span class=\"diff-html-removed\" id=\"removed-diff-1\" previous=\"added-diff-0\" changeid=\"removed-diff-1\" next=\"removed-diff-2\">labor, </span>materials, <span class=\"diff-html-removed\" id=\"removed-diff-2\" previous=\"removed-diff-1\" changeid=\"removed-diff-2\" next=\"added-diff-1\">tools</span><span class=\"diff-html-added\" id=\"added-diff-1\" previous=\"removed-diff-2\" changeid=\"added-diff-1\" next=\"removed-diff-3\">coordination of subcontractors</span>, <span class=\"diff-html-removed\" id=\"removed-diff-3\" previous=\"added-diff-1\" changeid=\"removed-diff-3\" next=\"added-diff-2\">equipment</span><span class=\"diff-html-added\" id=\"added-diff-2\" previous=\"removed-diff-3\" changeid=\"added-diff-2\" next=\"removed-diff-4\">health and safety compliance</span>, and <span class=\"diff-html-removed\" id=\"removed-diff-4\" previous=\"added-diff-2\" changeid=\"removed-diff-4\" next=\"added-diff-3\">supervision required to complete the building as specified</span><span class=\"diff-html-added\" id=\"added-diff-3\" previous=\"removed-diff-4\" changeid=\"added-diff-3\" next=\"removed-diff-5\">completion of works to a defect-free standard</span>. This <span class=\"diff-html-removed\" id=\"removed-diff-5\" previous=\"added-diff-3\" changeid=\"removed-diff-5\" next=\"added-diff-4\">includes </span><span class=\"diff-html-added\" id=\"added-diff-4\" previous=\"removed-diff-5\" changeid=\"added-diff-4\" next=\"removed-diff-6\">encompasses </span>site <span class=\"diff-html-removed\" id=\"removed-diff-6\" previous=\"added-diff-4\" changeid=\"removed-diff-6\" next=\"added-diff-5\">preparation</span><span class=\"diff-html-added\" id=\"added-diff-5\" previous=\"removed-diff-6\" changeid=\"added-diff-5\" next=\"removed-diff-7\">clearance</span>, <span class=\"diff-html-removed\" id=\"removed-diff-7\" previous=\"added-diff-5\" changeid=\"removed-diff-7\" next=\"added-diff-6\">structural work</span><span class=\"diff-html-added\" id=\"added-diff-6\" previous=\"removed-diff-7\" changeid=\"added-diff-6\" next=\"removed-diff-8\">foundation works</span>, <span class=\"diff-html-removed\" id=\"removed-diff-8\" previous=\"added-diff-6\" changeid=\"removed-diff-8\" next=\"added-diff-7\">architectural finishes</span><span class=\"diff-html-added\" id=\"added-diff-7\" previous=\"removed-diff-8\" changeid=\"added-diff-7\" next=\"removed-diff-9\">core construction</span>, MEP <span class=\"diff-html-removed\" id=\"removed-diff-9\" previous=\"added-diff-7\" changeid=\"removed-diff-9\" next=\"added-diff-8\">systems</span><span class=\"diff-html-added\" id=\"added-diff-8\" previous=\"removed-diff-9\" changeid=\"added-diff-8\" next=\"added-diff-9\">installations</span>, <span class=\"diff-html-added\" id=\"added-diff-9\" previous=\"added-diff-8\" changeid=\"added-diff-9\" next=\"removed-diff-10\">interior fit-out, </span>and <span class=\"diff-html-removed\" id=\"removed-diff-10\" previous=\"added-diff-9\" changeid=\"removed-diff-10\" next=\"added-diff-10\">external </span><span class=\"diff-html-added\" id=\"added-diff-10\" previous=\"removed-diff-10\" changeid=\"added-diff-10\" next=\"last-diff\">landscape </span>works.</p></div>
    `));
  });

  test('EL-63024 - NONE anchors diff', async ({ page }) => {
    await page.waitForSelector('.header .merge-pane', { state: 'visible' }); // Wait until merge pane is visible before next steps

    const visibleChange = page.getByTestId('EL-63024_NONE');
    await expect(visibleChange.locator(".header")).toBeVisible();
    await expect(visibleChange.locator(".diff-viewer .merge-ticker .form-check input[type='checkbox']")).toBeVisible();
    await expect(visibleChange.locator(".diff-viewer .merge-ticker")).toBeVisible();

    const headerLeft = visibleChange.locator(".header .wi-header.left");
    await expect(headerLeft).toBeVisible();
    const headerLeftHtml = await headerLeft.innerHTML();
    expect(normalizeHtml(headerLeftHtml)).toContain("4.2 Electrical Installation");

    const diffLeft = visibleChange.locator(".diff-viewer .diff-leaf.left");
    await expect(diffLeft).toBeVisible();
    const diffLeftHtml = await diffLeft.innerHTML();
    expect(normalizeHtml(diffLeftHtml)).toBe(normalizeHtml(`
        <div class=\"polarion-dle-pdf\"><p id=\"polarion_15\"><span class=\"diff-html-removed\" id=\"added-diff-0\" previous=\"first-diff\" changeid=\"added-diff-0\" next=\"last-diff\">All wiring shall be in accordance with IEC 60364 standards. Lighting systems shall be LED-based with daylight sensors and motion detectors in common areas to reduce energy consumption.</span></p></div>
    `));

    const headerRight = visibleChange.locator(".header .wi-header.right");
    await expect(headerRight).toBeVisible();
    const headerRightHtml = await headerRight.innerHTML();
    expect(normalizeHtml(headerRightHtml)).toBe(normalizeHtml(`<span style=\"font-size: 0.75em;\">–</span>`));

    const diffRight = visibleChange.locator(".diff-viewer .diff-leaf.right");
    await expect(diffRight).toBeVisible();
    const diffRightHtml = await diffRight.innerHTML();
    expect(normalizeHtml(diffRightHtml)).toBe(normalizeHtml(``));
  });

  test('content merge canceled', async ({ page }) => {
    let requestWasMade = false;

    // Set up route monitoring
    await page.route('**/merge/documents-content', route => {
      requestWasMade = true;
      route.continue();
    });

    await page.waitForSelector('.header .merge-pane', { state: 'visible' }); // Wait until merge pane is visible before next steps

    await expect(page.getByTestId("merge-confirmation-modal")).toBeVisible({ visible: false });

    const selectionCheckbox = page.getByTestId('EL-63012_DP-1154').locator('.merge-ticker input[type="checkbox"]');
    await selectionCheckbox.isVisible();
    await selectionCheckbox.click();

    const mergeButton = page.locator('.header .merge-pane .merge-button .btn').nth(0);
    await expect(mergeButton).toBeEnabled();
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

  test('content merge changes', async ({ page }) => {
    const requestPromise = page.waitForRequest(request => {
      return request.url().includes('/merge/documents-content');
    });

    await page.waitForSelector('.header .merge-pane', { state: 'visible' }); // Wait until merge pane is visible before next steps

    await expect(page.getByTestId("merge-confirmation-modal")).toBeVisible({ visible: false });
    await expect(page.getByTestId("merge-result-modal")).toBeVisible({ visible: false });

    const selectionCheckbox = page.getByTestId('EL-63012_DP-1154').locator('.merge-ticker input[type="checkbox"]');
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
    expect(request.url()).toContain('/merge/documents-content');

    if (request.method() === 'POST') {
      const postData = JSON.parse(request.postData() || '{}');
      expect(postData.direction).toBe(0);
      expect(postData.pairs).toStrictEqual([{leftWorkItemId: "EL-63012", rightWorkItemId: "DP-1154", contentPosition: "BELOW"}]);

      expect(postData.leftDocument.projectId).toBe("elibrary");
      expect(postData.leftDocument.spaceId).toBe("Specification");
      expect(postData.leftDocument.name).toBe("Building Specification v2");

      expect(postData.rightDocument.projectId).toBe("drivepilot");
      expect(postData.rightDocument.spaceId).toBe("Specification");
      expect(postData.rightDocument.name).toBe("Building Specification v3");

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
      await expect(mergeResultModalLog).toHaveText("2025-05-13 14:29:54.586: 'MODIFIED' -- left WI 'EL-63012', right WI 'DP-1154' -- content below workitem 'DP-1154' modified with content below workitem 'EL-63012'");

      const cancelButton = page.getByTestId("merge-result-modal-cancel-button");
      await expect(cancelButton).toBeVisible();
      await expect(cancelButton).toBeEnabled();
      await cancelButton.click();

      await expect(mergeResultModal).toBeVisible({ visible: false});
    }
  });

  test('content merge not authorized', async ({ page }) => {
    const requestPromise = page.waitForRequest(request => {
      return request.url().includes('/merge/documents-content');
    });

    await page.waitForSelector('.header .merge-pane', { state: 'visible' }); // Wait until merge pane is visible before next steps

    await expect(page.getByTestId("merge-confirmation-modal")).toBeVisible({ visible: false });
    await expect(page.getByTestId("merge-result-modal")).toBeVisible({ visible: false });

    const selectionCheckbox = page.getByTestId('EL-63013_DP-1155').locator('.merge-ticker input[type="checkbox"]');
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
    expect(request.url()).toContain('/merge/documents-content');

    if (request.method() === 'POST') {
      const postData = JSON.parse(request.postData() || '{}');
      expect(postData.direction).toBe(1);
      expect(postData.pairs).toStrictEqual([{leftWorkItemId: "EL-63013", rightWorkItemId: "DP-1155", contentPosition: "BELOW"}]);

      expect(postData.leftDocument.projectId).toBe("elibrary");
      expect(postData.leftDocument.spaceId).toBe("Specification");
      expect(postData.leftDocument.name).toBe("Building Specification v2");

      expect(postData.rightDocument.projectId).toBe("drivepilot");
      expect(postData.rightDocument.spaceId).toBe("Specification");
      expect(postData.rightDocument.name).toBe("Building Specification v3");

      const mergeResultModal = page.getByTestId("merge-result-modal");
      await expect(mergeResultModal).toBeVisible();

      const mergeResultModalTitle = mergeResultModal.locator(".modal-title");
      await expect(mergeResultModalTitle).toBeVisible();
      await expect(mergeResultModalTitle).toHaveText("Merge Report");

      const mergeResultModalMessage = page.getByTestId("merge-not-authorized");
      await expect(mergeResultModalMessage).toBeVisible();
      await expect(mergeResultModalMessage).toHaveText("You are not authorized to execute such merge request.");

      const cancelButton = page.getByTestId("merge-result-modal-cancel-button");
      await expect(cancelButton).toBeVisible();
      await expect(cancelButton).toBeEnabled();
      await cancelButton.click();

      await expect(mergeResultModal).toBeVisible({ visible: false});
    }
  });

  test('content merge aborted because of changes in target document', async ({ page }) => {
    const requestPromise = page.waitForRequest(request => {
      return request.url().includes('/merge/documents-content');
    });

    await page.waitForSelector('.header .merge-pane', { state: 'visible' }); // Wait until merge pane is visible before next steps

    await expect(page.getByTestId("merge-confirmation-modal")).toBeVisible({ visible: false });
    await expect(page.getByTestId("merge-result-modal")).toBeVisible({ visible: false });

    const selectionCheckbox = page.getByTestId('EL-63016_DP-1158').locator('.merge-ticker input[type="checkbox"]');
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
    expect(request.url()).toContain('/merge/documents-content');

    if (request.method() === 'POST') {
      const postData = JSON.parse(request.postData() || '{}');
      expect(postData.direction).toBe(0);
      expect(postData.pairs).toStrictEqual([{leftWorkItemId: "EL-63016", rightWorkItemId: "DP-1158", contentPosition: "BELOW"}]);

      expect(postData.leftDocument.projectId).toBe("elibrary");
      expect(postData.leftDocument.spaceId).toBe("Specification");
      expect(postData.leftDocument.name).toBe("Building Specification v2");

      expect(postData.rightDocument.projectId).toBe("drivepilot");
      expect(postData.rightDocument.spaceId).toBe("Specification");
      expect(postData.rightDocument.name).toBe("Building Specification v3");

      const mergeResultModal = page.getByTestId("merge-result-modal");
      await expect(mergeResultModal).toBeVisible();

      const mergeResultModalTitle = mergeResultModal.locator(".modal-title");
      await expect(mergeResultModalTitle).toBeVisible();
      await expect(mergeResultModalTitle).toHaveText("Merge Report");

      const mergeAborted = page.getByTestId("merge-aborted");
      await expect(mergeAborted).toBeVisible();
      await expect(mergeAborted).toHaveText("Merge was aborted because some structural changes were done in target document meanwhile.");

      const cancelButton = page.getByTestId("merge-result-modal-cancel-button");
      await expect(cancelButton).toBeVisible();
      await expect(cancelButton).toBeEnabled();
      await cancelButton.click();

      await expect(mergeResultModal).toBeVisible({ visible: false});
    }
  });

});
