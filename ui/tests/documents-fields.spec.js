import { expect } from '@playwright/test';
import { test, normalizeHtml } from './test-utils';

test.describe("diffing and merging of documents' fields", () => {

  test.beforeEach(async ({page, mockApi}) => {
    await mockApi.mockEndpoint({url: '**/extension/info', fixtureFile: 'version-info.json'});
    await mockApi.mockEndpoint({url: '**/communication/settings', fixtureFile: 'communication-settings.json'});
    await mockApi.mockEndpoint({url: '**/settings/diff/names?scope=project/elibrary/', fixtureFile: 'configs.json'});
    await mockApi.mockEndpoint({url: '**/diff/documents-fields', fixtureFile: 'documents-fields.json'});
    await mockApi.mockFieldsMergeEndpoint();

    await page.goto('/documents?sourceProjectId=elibrary&sourceSpaceId=Specification&sourceDocument=Catalog%20Specification&targetProjectId=drivepilot&targetSpaceId=Design&targetDocument=Catalog%20Design&linkRole=relates_to&compareAs=Fields');
  });

  test('version field diff', async ({ page }) => {
    await page.waitForSelector('.header .merge-pane', { state: 'visible' }); // Wait until merge pane is visible before next steps

    const versionFieldDiff = page.getByTestId('version-field-diff');

    await expect(versionFieldDiff.locator(".merge-ticker .form-check input[type='checkbox']")).toBeVisible();

    const header = versionFieldDiff.locator(".diff-header");
    await expect(header).toBeVisible();
    await expect(header).toHaveText('Version');

    const diffViewer = versionFieldDiff.locator(".diff-viewer");
    await expect(diffViewer).toBeVisible();

    const diffLeft = diffViewer.locator(".left");
    await expect(diffLeft).toBeVisible();
    const diffLeftHtml = await diffLeft.innerHTML();
    expect(normalizeHtml(diffLeftHtml)).toBe(normalizeHtml(`
        <span class="diff-html-added" id="removed-diff-0" previous="first-diff" changeid="removed-diff-0" next="added-diff-0">2</span>
        <span class="diff-html-removed" id="added-diff-0" previous="removed-diff-0" changeid="added-diff-0" next="last-diff">1</span>.0
    `));

    const diffRight = diffViewer.locator(".right");
    await expect(diffRight).toBeVisible();
    const diffRightHtml = await diffRight.innerHTML();
    expect(normalizeHtml(diffRightHtml)).toBe(normalizeHtml(`
        <span class="diff-html-removed" id="removed-diff-0" previous="first-diff" changeid="removed-diff-0" next="added-diff-0">1</span>
        <span class="diff-html-added" id="added-diff-0" previous="removed-diff-0" changeid="added-diff-0" next="last-diff">2</span>.0
    `));
  });

  test('docType field diff', async ({ page }) => {
    await page.waitForSelector('.header .merge-pane', { state: 'visible' }); // Wait until merge pane is visible before next steps

    const docDocTypeFieldDiff = page.getByTestId('docType-field-diff');
    await expect(docDocTypeFieldDiff.locator(".diff-header")).toBeVisible({ visible: false });
    await expect(docDocTypeFieldDiff.locator(".diff-viewer")).toBeVisible({ visible: false });
  });

  test('docRevision field diff', async ({ page }) => {
    await page.waitForSelector('.header .merge-pane', { state: 'visible' }); // Wait until merge pane is visible before next steps

    const docDocTypeFieldDiff = page.getByTestId('docRevision-field-diff');

    await expect(docDocTypeFieldDiff.locator(".merge-ticker .form-check input[type='checkbox']")).toBeVisible();

    const header = docDocTypeFieldDiff.locator(".diff-header");
    await expect(header).toBeVisible();
    await expect(header).toHaveText('Document Revision');

    const diffViewer = docDocTypeFieldDiff.locator(".diff-viewer");
    await expect(diffViewer).toBeVisible();

    const diffLeft = diffViewer.locator(".left");
    await expect(diffLeft).toBeVisible();
    const diffLeftHtml = await diffLeft.innerHTML();
    expect(normalizeHtml(diffLeftHtml)).toBe(normalizeHtml(`
        <span class="diff-html-added" id="removed-diff-0" previous="first-diff" changeid="removed-diff-0" next="last-diff">183</span>
    `));

    const diffRight = diffViewer.locator(".right");
    await expect(diffRight).toBeVisible();
    const diffRightHtml = await diffRight.innerHTML();
    expect(normalizeHtml(diffRightHtml)).toBe(normalizeHtml(`
        <span class="diff-html-added" id="added-diff-0" previous="first-diff" changeid="added-diff-0" next="last-diff">183</span>
    `));
  });

  test('docLanguage field diff', async ({ page }) => {
    await page.waitForSelector('.header .merge-pane', { state: 'visible' }); // Wait until merge pane is visible before next steps

    const docDocTypeFieldDiff = page.getByTestId('docLanguage-field-diff');

    await expect(docDocTypeFieldDiff.locator(".merge-ticker .form-check input[type='checkbox']")).toBeVisible();

    const header = docDocTypeFieldDiff.locator(".diff-header");
    await expect(header).toBeVisible();
    await expect(header).toHaveText('Document Language');

    const diffViewer = docDocTypeFieldDiff.locator(".diff-viewer");
    await expect(diffViewer).toBeVisible();

    const diffLeft = diffViewer.locator(".left");
    await expect(diffLeft).toBeVisible();
    const diffLeftHtml = await diffLeft.innerHTML();
    expect(normalizeHtml(diffLeftHtml)).toBe(normalizeHtml(`
        <span class="diff-html-added" id="removed-diff-0" previous="first-diff" changeid="removed-diff-0" next="added-diff-0"></span>
        <span class="diff-html-removed" id="added-diff-0" previous="removed-diff-0" changeid="added-diff-0" next="last-diff"><span class="polarion-JSEnumOption" title="Deutsch">Deutsch</span></span>
    `));

    const diffRight = diffViewer.locator(".right");
    await expect(diffRight).toBeVisible();
    const diffRightHtml = await diffRight.innerHTML();
    expect(normalizeHtml(diffRightHtml)).toBe(normalizeHtml(`
        <span class="diff-html-removed" id="removed-diff-0" previous="first-diff" changeid="removed-diff-0" next="added-diff-0"><span class="polarion-JSEnumOption" title="Deutsch">Deutsch</span></span>
        <span class="diff-html-added" id="added-diff-0" previous="removed-diff-0" changeid="added-diff-0" next="last-diff"></span>
    `));
  });

  test('merge canceled', async ({ page }) => {
    let requestWasMade = false;

    // Set up route monitoring
    await page.route('**/merge/documents-fields', route => {
      requestWasMade = true;
      route.continue();
    });

    await page.waitForSelector('.header .merge-pane', { state: 'visible' }); // Wait until merge pane is visible before next steps

    await expect(page.getByTestId("merge-confirmation-modal")).toBeVisible({ visible: false });

    const selectionCheckbox = page.getByTestId('version-field-diff').locator('.merge-ticker input[type="checkbox"]');
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

  test('merge changes', async ({ page }) => {
    const requestPromise = page.waitForRequest(request => {
      return request.url().includes('/merge/documents-fields');
    });

    await page.waitForSelector('.header .merge-pane', { state: 'visible' }); // Wait until merge pane is visible before next steps

    await expect(page.getByTestId("merge-confirmation-modal")).toBeVisible({ visible: false });
    await expect(page.getByTestId("merge-result-modal")).toBeVisible({ visible: false });

    const selectionCheckbox = page.getByTestId('version-field-diff').locator('.merge-ticker input[type="checkbox"]');
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
    expect(request.url()).toContain('/merge/documents-fields');

    if (request.method() === 'POST') {
      const postData = JSON.parse(request.postData() || '{}');
      expect(postData.direction).toBe(0);
      expect(postData.fieldIds).toStrictEqual(["version"]);

      expect(postData.leftDocument.projectId).toBe("elibrary");
      expect(postData.leftDocument.spaceId).toBe("Specification");
      expect(postData.leftDocument.name).toBe("Catalog Specification");

      expect(postData.rightDocument.projectId).toBe("drivepilot");
      expect(postData.rightDocument.spaceId).toBe("Design");
      expect(postData.rightDocument.name).toBe("Catalog Design");

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
      await expect(mergeResultModalList).toHaveText("1 items were copied.");

      const mergeResultModalLogLink = page.getByTestId("see-full-log");
      await expect(mergeResultModalLogLink).toBeVisible();
      await expect(mergeResultModalLogLink).toHaveText("See full log");

      const mergeResultModalLog = mergeResultModal.locator("pre");
      await expect(mergeResultModalLog).toBeVisible({visible: false});

      await mergeResultModalLogLink.click();

      await expect(mergeResultModalLog).toBeVisible();
      await expect(mergeResultModalLog).toHaveText("2025-05-06 11:57:49.469: 'COPIED' -- field ID 'version' -- value successfully copied");

      const cancelButton = page.getByTestId("merge-result-modal-cancel-button");
      await expect(cancelButton).toBeVisible();
      await expect(cancelButton).toBeEnabled();
      await cancelButton.click();

      await expect(mergeResultModal).toBeVisible({ visible: false});
    }
  });

  test('merge not authorized', async ({ page }) => {
    const requestPromise = page.waitForRequest(request => {
      return request.url().includes('/merge/documents-fields');
    });

    await page.waitForSelector('.header .merge-pane', { state: 'visible' }); // Wait until merge pane is visible before next steps

    await expect(page.getByTestId("merge-confirmation-modal")).toBeVisible({ visible: false });
    await expect(page.getByTestId("merge-result-modal")).toBeVisible({ visible: false });

    const selectionCheckbox = page.getByTestId('docRevision-field-diff').locator('.merge-ticker input[type="checkbox"]');
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
    expect(request.url()).toContain('/merge/documents-fields');

    if (request.method() === 'POST') {
      const postData = JSON.parse(request.postData() || '{}');
      expect(postData.direction).toBe(1);
      expect(postData.fieldIds).toStrictEqual(["docRevision"]);

      expect(postData.leftDocument.projectId).toBe("elibrary");
      expect(postData.leftDocument.spaceId).toBe("Specification");
      expect(postData.leftDocument.name).toBe("Catalog Specification");

      expect(postData.rightDocument.projectId).toBe("drivepilot");
      expect(postData.rightDocument.spaceId).toBe("Design");
      expect(postData.rightDocument.name).toBe("Catalog Design");

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

  test('merge aborted because of changes in target document', async ({ page }) => {
    const requestPromise = page.waitForRequest(request => {
      return request.url().includes('/merge/documents-fields');
    });

    await page.waitForSelector('.header .merge-pane', { state: 'visible' }); // Wait until merge pane is visible before next steps

    await expect(page.getByTestId("merge-confirmation-modal")).toBeVisible({ visible: false });
    await expect(page.getByTestId("merge-result-modal")).toBeVisible({ visible: false });

    const selectionCheckbox = page.getByTestId('docLanguage-field-diff').locator('.merge-ticker input[type="checkbox"]');
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
    expect(request.url()).toContain('/merge/documents-fields');

    if (request.method() === 'POST') {
      const postData = JSON.parse(request.postData() || '{}');
      expect(postData.direction).toBe(0);
      expect(postData.fieldIds).toStrictEqual(["docLanguage"]);

      expect(postData.leftDocument.projectId).toBe("elibrary");
      expect(postData.leftDocument.spaceId).toBe("Specification");
      expect(postData.leftDocument.name).toBe("Catalog Specification");

      expect(postData.rightDocument.projectId).toBe("drivepilot");
      expect(postData.rightDocument.spaceId).toBe("Design");
      expect(postData.rightDocument.name).toBe("Catalog Design");

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
