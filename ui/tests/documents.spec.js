import { test as base, expect } from '@playwright/test';
import path from 'path';
import fs from 'fs';

// Define a fixture for API mocking
const test = base.extend({
  // Create a new "mockApi" fixture
  mockApi: async ({ page }, use) => {
    // Mock API helper function
    const mockApi = {
      mockEndpoint: async ({ url, fixtureFile }) => {
        const filePath = path.join(__dirname, 'fixtures', fixtureFile);
        const responseData = JSON.parse(fs.readFileSync(filePath, 'utf8'));

        await page.route(url, route => {
          route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify(responseData)
          });
        });

        return responseData;
      }
    };

    // Use the fixture in the test
    await use(mockApi);
  },
});

test.describe('diffing workitems of documents', () => {

  test.beforeEach(async ({ page, mockApi }) => {
    await mockApi.mockEndpoint({ url: '**/extension/info', fixtureFile: 'version-info.json' });
    await mockApi.mockEndpoint({ url: '**/communication/settings', fixtureFile: 'communication-settings.json' });
    await mockApi.mockEndpoint({ url: '**/settings/diff/names?scope=project/elibrary/', fixtureFile: 'configs.json' });

    const documentsDiff = JSON.parse(fs.readFileSync(path.join(__dirname, 'fixtures', 'documents-diff.json'), 'utf8'));
    await page.route('**/diff/documents', route => route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(documentsDiff)
    }));

    await page.goto('/documents?sourceProjectId=elibrary&sourceSpaceId=Specification&sourceDocument=Product%20Specification&targetProjectId=drivepilot&targetSpaceId=Design&targetDocument=Product%20Design&linkRole=relates_to');
  });

  test('documents diff request', async ({ page }) => {
    const diffPostRequestPromise = page.waitForRequest(request => {
      return request.url().includes('/diff/documents') && request.method() === "POST";
    });

    // Wait for the request and assert request data
    const diffPostRequest = await diffPostRequestPromise;
    const postData = JSON.parse(diffPostRequest.postData() || '{}');
    expect(postData.leftDocument.projectId).toBe("elibrary");
    expect(postData.leftDocument.spaceId).toBe("Specification");
    expect(postData.leftDocument.name).toBe("Product Specification");
    expect(postData.leftDocument.revision).toBe(null);
    expect(postData.rightDocument.projectId).toBe("drivepilot");
    expect(postData.rightDocument.spaceId).toBe("Design");
    expect(postData.rightDocument.name).toBe("Product Design");
    expect(postData.rightDocument.revision).toBe(null);
    expect(postData.linkRole).toBe("relates_to");
    expect(postData.configName).toBe(null);
  });

  test('page header', async ({ page }) => {
    await expect(page.locator('.app-header .app-title')).toHaveText("Diff/merge of Polarion Documents (WorkItems)");
    await expect(page.locator('.app-header .extension-info')).toHaveText("v5.1.2 | 2025-03-17 14:48");

    await expect(page.getByTestId('LEFT-project').locator(".path-label")).toHaveText("project:");
    await expect(page.getByTestId('LEFT-project').locator(".path-value")).toHaveText("E-Library");

    await expect(page.getByTestId('RIGHT-project').locator(".path-label")).toHaveText("project:");
    await expect(page.getByTestId('RIGHT-project').locator(".path-value")).toHaveText("Drive Pilot");

    await expect(page.getByTestId('LEFT-space').locator(".path-label")).toHaveText("space:");
    await expect(page.getByTestId('LEFT-space').locator(".path-value")).toHaveText("Specification");

    await expect(page.getByTestId('RIGHT-space').locator(".path-label")).toHaveText("space:");
    await expect(page.getByTestId('RIGHT-space').locator(".path-value")).toHaveText("Design");

    await expect(page.getByTestId('LEFT-doc-title')).toHaveText("Product SpecificationHEAD (rev. 6437)");

    await expect(page.getByTestId('RIGHT-doc-title')).toHaveText("Product DesignHEAD (rev. 6437)");
  });
});
