import { normalizeHtml, test } from "./test-utils";
import { expect } from "@playwright/test";


test.describe("page of diffing Collections c", () => {

  test.beforeEach(async ({page, mockApi}) => {
    await mockApi.mockEndpoint({url: '**/extension/info', fixtureFile: 'version-info.json'});
    await mockApi.mockEndpoint({url: '**/communication/settings', fixtureFile: 'communication-settings.json'});
    await mockApi.mockEndpoint({url: '**/settings/diff/names?scope=project/elibrary/', fixtureFile: 'configs.json'});
    await mockApi.mockEndpoint({url: '**/diff/collections', fixtureFile: 'collections.json'});
    await mockApi.mockEndpoint({url: '**/diff/documents', fixtureFile: 'documents-from-collection.json', method: 'POST'});
    await mockApi.mockEndpoint({url: '**/duplicate', fixtureFile: 'duplicate.json', method: 'POST'});

    await page.goto('/collections?sourceProjectId=elibrary&sourceCollectionId=1&targetProjectId=Project2&targetCollectionId=1&linkRole=relates_to&config=Default&compareAs=Workitems&sourceSpaceId=Testing&sourceDocument=Test+Specification&targetSpaceId=Testing&targetDocument=Test+Specification');
  });

  test('collections pairs request', async ({page}) => {
    const diffPostRequestPromise = page.waitForRequest(request => {
      return request.url().includes('/diff/collections') && request.method() === "POST";
    });

    const diffPostRequest = await diffPostRequestPromise;
    const postData = JSON.parse(diffPostRequest.postData() || '{}');

    expect(postData.leftCollection.projectId).toBe("elibrary");
    expect(postData.rightCollection.projectId).toBe("Project2");
  });

  test('page header', async ({ page }) => {
    // Verify the main title and extension info
    await expect(page.locator('.app-header .app-title')).toHaveText("Diff/merge of Polarion Collections");
    await expect(page.locator('.app-header .extension-info')).toHaveText("v5.1.2 | 2025-03-17 14:48");

    // Verify the left collection details
    await expect(page.getByTestId('LEFT-collection').locator(".path-label:has-text('project')")).toHaveText("project:");
    await expect(page.getByTestId('LEFT-collection').locator(".path-label:has-text('collection:')")).toHaveText("collection:");

    await expect(page.getByTestId('LEFT-collection').locator(".path-value").nth(0)).toHaveText("E-Library");
    await expect(page.getByTestId('LEFT-collection').locator(".path-value").nth(1)).toHaveText("Specification v1.0");


    // Verify the right collection details
    await expect(page.getByTestId('RIGHT-collection').locator(".path-label:has-text('project')")).toHaveText("project:");
    await expect(page.getByTestId('RIGHT-collection').locator(".path-label:has-text('collection:')")).toHaveText("collection:");

    await expect(page.getByTestId('RIGHT-collection').locator(".path-value").nth(0)).toHaveText("Drive Pilot");
    await expect(page.getByTestId('RIGHT-collection').locator(".path-value").nth(1)).toHaveText("Software Specification v.1");

    await expect(page.getByTestId('LEFT-space').locator(".path-label")).toHaveText("space:");
    await expect(page.getByTestId('LEFT-space').locator(".path-value")).toHaveText("Testing");


    const createDocumentButton = page.getByTestId('create-document-button');
    await expect(createDocumentButton).toBeVisible();
  });

  test('click create document button', async ({ page }) => {
    // Wait for the button to be visible
    const createDocumentButton = page.getByTestId('create-document-button');
    await expect(createDocumentButton).toBeVisible();

    // Click the button
    await createDocumentButton.click();

    // Verify the expected behavior (e.g., modal becomes visible)
    const targetConfigurationModal = page.getByTestId('target-configuration-modal');
    await expect(targetConfigurationModal).toBeVisible();

    // Verify the presence of the "Cancel" button
    const cancelButton = targetConfigurationModal.locator('button', { hasText: 'Cancel' });
    await expect(cancelButton).toBeVisible();

    // Verify the presence of the "Create Document" button
    const createButton = targetConfigurationModal.locator('button', { hasText: 'Create Document' });
    await expect(createButton).toBeVisible();

    // Click the "Create Document" button
    await createButton.click();

    // Verify the modal is no longer visible
    await expect(targetConfigurationModal).not.toBeVisible();

    // Adjust this part based on your implementation
    await page.screenshot({ path: 'page-header.png', fullPage: true });
    const successAlert = page.getByTestId('app-alert-title')
    await expect(successAlert).toBeVisible();
  });

  test('click cancel button in create document modal', async ({ page }) => {
    // Wait for the button to be visible
    const createDocumentButton = page.getByTestId('create-document-button');
    await expect(createDocumentButton).toBeVisible();

    // Click the button
    await createDocumentButton.click();

    // Verify the expected behavior (e.g., modal becomes visible)
    const targetConfigurationModal = page.getByTestId('target-configuration-modal');
    await expect(targetConfigurationModal).toBeVisible();

    // Click the "Cancel" button
    const cancelButton = targetConfigurationModal.locator('button', { hasText: 'Cancel' });
    await cancelButton.click();

    // Verify the modal is no longer visible
    await expect(targetConfigurationModal).not.toBeVisible();
  });

  test('control pane', async ({ page }) => {
    expect(await page.locator('.control-pane.expanded').count()).toEqual(0);

    const expandButton = page.locator('.control-pane .expand-button');
    await expect(expandButton).toBeVisible({visible: true});
    if (await expandButton.isVisible()) {
      await expandButton.click();
      expect(await page.locator('.control-pane.expanded').count()).toEqual(1);
    }

    await expect(page.locator(".control-pane .controls label[for='source-document']")).toHaveText("Source documents:")
    await expect(page.locator("#source-document")).toHaveValue("Testing/Test Specification");

    await expect(page.locator(".control-pane .controls label[for='target-type']")).toHaveText("Compare as:");
    await expect(page.locator("#target-type")).toHaveValue("Workitems");

    await expect(page.locator(".control-pane .controls label[for='configuration']")).toHaveText("Configuration:");
    await expect(page.locator("#configuration")).toHaveValue("Default");

    await expect(page.locator(".control-pane .controls label[for='outline-numbers']")).toHaveText("Show difference in outline numbers");
    await expect(page.locator("#outline-numbers")).toBeChecked({checked: false});

    await expect(page.locator(".control-pane .controls label[for='counterparts-differ']")).toHaveText("Counterpart WorkItems differ");
    await expect(page.locator("#counterparts-differ")).toBeChecked({checked: false});

    await expect(page.locator(".control-pane .controls label[for='compare-enums-by-id']")).toHaveText("Compare enums by ID");
    await expect(page.locator("#compare-enums-by-id")).toBeChecked({checked: false});

    await expect(page.locator(".control-pane .controls label[for='allow-reference-wi-merge']")).toHaveText("Allow reference work item merge");
    await expect(page.locator("#allow-reference-wi-merge")).toBeChecked({checked: false});

    await expect(page.locator(".control-pane .controls label[for='hide-chapters']")).toHaveText("Hide chapters if no difference");
    await expect(page.locator("#hide-chapters")).toBeChecked({checked: true});

    await expect(page.locator(".control-pane .controls label[for='paper-size']")).toHaveText("Paper size:");
    await expect(page.locator("#paper-size")).toHaveValue("A4");

    await expect(page.locator(".control-pane .controls label[for='orientation']")).toHaveText("Orientation:");
    await expect(page.locator("#orientation")).toHaveValue("landscape");
  });

  test('control pane change source document', async ({ page }) => {
    expect(await page.locator('.control-pane.expanded').count()).toEqual(0);

    const expandButton = page.locator('.control-pane .expand-button');
    await expect(expandButton).toBeVisible({visible: true});
    if (await expandButton.isVisible()) {
      await expandButton.click();
      expect(await page.locator('.control-pane.expanded').count()).toEqual(1);
    }

    await expect(page.locator(".control-pane .controls label[for='source-document']")).toHaveText("Source documents:")
    await expect(page.locator("#source-document")).toHaveValue("Testing/Test Specification");


    // Select a different source document
    const sourceDocumentSelect = page.locator("#source-document");
    await sourceDocumentSelect.selectOption("Specification/Administration Specification");

    // Verify the header updates with the new source document
    const header = page.getByTestId('LEFT-doc-title');
    await expect(header).toHaveText("Test SpecificationHEAD (rev. 9487)");
  });
})
