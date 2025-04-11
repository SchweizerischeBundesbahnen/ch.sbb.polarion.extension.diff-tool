import { test as base, expect } from '@playwright/test';
import path from 'path';
import fs from 'fs';

const fixtureDataProvider = (fixtureFile) => {
  const filePath = path.join(__dirname, 'fixtures', fixtureFile);
  const fixtureData = JSON.parse(fs.readFileSync(filePath, 'utf8'));
  return JSON.stringify(fixtureData)
};

const workItemsDiffFixtureDataProvider = (requestBody) => {
  let fixtureFile;
  if (!requestBody.leftWorkItem) {
    if (requestBody.rightWorkItem.id === "DP-11570") {
      fixtureFile = "NONE_DP-11570.json";
    }
  } else if (!requestBody.rightWorkItem) {
    if (requestBody.leftWorkItem.id === "EL-156") {
      fixtureFile = "EL-156_NONE.json";
    }
  } else if (requestBody.leftWorkItem.id === "EL-183" && requestBody.rightWorkItem.id === "DP-11545") {
    fixtureFile = "EL-183_DP-11545.json";
  } else if (requestBody.leftWorkItem.id === "EL-184" && requestBody.rightWorkItem.id === "DP-11546") {
    fixtureFile = "EL-184_DP-11546.json";
  } else if (requestBody.leftWorkItem.id === "EL-144" && requestBody.rightWorkItem.id === "DP-11549") {
    fixtureFile = "EL-144_DP-11549.json";
  } else if (requestBody.leftWorkItem.id === "EL-145" && requestBody.rightWorkItem.id === "DP-11550") {
    fixtureFile = "EL-145_DP-11550.json";
  } else if (requestBody.leftWorkItem.id === "EL-101" && requestBody.rightWorkItem.id === "DP-11551") {
    fixtureFile = "EL-101_DP-11551.json";
  } else if (requestBody.leftWorkItem.id === "EL-146" && requestBody.rightWorkItem.id === "DP-11552") {
    fixtureFile = "EL-146_DP-11552.json";
  } else if (requestBody.leftWorkItem.id === "EL-4977" && requestBody.rightWorkItem.id === "DP-11559") {
    fixtureFile = "EL-4977_DP-11559.json";
  } else if (requestBody.leftWorkItem.id === "EL-153" && requestBody.rightWorkItem.id === "DP-11560") {
    fixtureFile = "EL-153_DP-11560.json";
  } else if (requestBody.leftWorkItem.id === "EL-11572" && requestBody.rightWorkItem.id === "DP-11571") {
    fixtureFile = "EL-11572_DP-11571.json";
  } else if (requestBody.leftWorkItem.id === "EL-157" && requestBody.rightWorkItem.id === "DP-11562") {
    fixtureFile = "EL-157_DP-11562.json";
  } else if (requestBody.leftWorkItem.id === "EL-158" && requestBody.rightWorkItem.id === "DP-11563") {
    fixtureFile = "EL-158_DP-11563.json";
  } else if (requestBody.leftWorkItem.id === "EL-159" && requestBody.rightWorkItem.id === "DP-11564") {
    fixtureFile = "EL-159_DP-11564.json";
  }

  if (fixtureFile) {
    const filePath = path.join(__dirname, 'fixtures', fixtureFile);
    const fixtureData = JSON.parse(fs.readFileSync(filePath, 'utf8'));
    return JSON.stringify(fixtureData)
  } else {
    console.log("fixture not found: ", requestBody);
    return {}
  }
};

// API mocking
const test = base.extend({
  mockApi: async ({ page }, use) => {
    const mockApi = {
      // Mock any API endpoint with certain JSON response in general
      mockEndpoint: async ({ url, fixtureFile }) => {
        const responseData = fixtureDataProvider(fixtureFile);

        await page.route(url, route => {
          route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: fixtureDataProvider(fixtureFile)
          });
        });

        return responseData;
      },

      // Mock "/diff/document-workitems" endpoint which returns JSON response depending on data in request body
      mockWorkItemsDiffEndpoint: async () => {
        await page.route("**/diff/document-workitems", route => {
          const request = route.request();
          let requestBody;
          try {
            requestBody = request.postDataJSON();
          } catch (e) {
            requestBody = {};
          }

          route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: workItemsDiffFixtureDataProvider(requestBody)
          });
        });
      }
    };

    // Use the fixture in the test
    await use(mockApi);
  },
});

test.describe("page of diffing documents' WorkItems", () => {

  test.beforeEach(async ({ page, mockApi }) => {
    await mockApi.mockEndpoint({ url: '**/extension/info', fixtureFile: 'version-info.json' });
    await mockApi.mockEndpoint({ url: '**/communication/settings', fixtureFile: 'communication-settings.json' });
    await mockApi.mockEndpoint({ url: '**/settings/diff/names?scope=project/elibrary/', fixtureFile: 'configs.json' });
    await mockApi.mockEndpoint({ url: '**/diff/documents', fixtureFile: 'documents-diff.json' });
    await mockApi.mockWorkItemsDiffEndpoint();

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
    if (postData.configName) {
      expect(postData.configName).toBe("Default");
    } else {
      expect(postData.configName).toBe(null);
    }

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

    await expect(page.getByTestId('LEFT-doc-title')).toHaveText("Catalog SpecificationHEAD (rev. 6457)");

    await expect(page.getByTestId('RIGHT-doc-title')).toHaveText("Catalog DesignHEAD (rev. 6457)");

    await expect(page.locator('.header .merge-pane')).toBeVisible({visible: true});
  });

  test('control pane', async ({ page }) => {
    expect(await page.locator('.control-pane.expanded').count()).toEqual(0);
    await expect(page.locator('.control-pane .expand-button')).toBeVisible({visible: false});

    await expect(page.locator(".progress span")).toHaveText("Data loaded successfully");

    const expandButton = page.locator('.control-pane .expand-button');
    await expect(expandButton).toBeVisible({visible: true});
    if (await expandButton.isVisible()) {
      await expandButton.click();
      expect(await page.locator('.control-pane.expanded').count()).toEqual(1);
    }

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

  test('export to PDF', async ({ page }) => {
    expect(await page.locator('.control-pane.expanded').count()).toEqual(0);
    await expect(page.locator('.control-pane .expand-button')).toBeVisible({visible: false});

    await expect(page.locator(".progress span")).toHaveText("Data loaded successfully");

    const expandButton = page.locator('.control-pane .expand-button');
    await expect(expandButton).toBeVisible({visible: true});
    if (await expandButton.isVisible()) {
      await expandButton.click();
      expect(await page.locator('.control-pane.expanded').count()).toEqual(1);
    }

    page.locator("#paper-size").selectOption("A3");
    page.locator("#orientation").selectOption("portrait");

    const exportRequestPromise = page.waitForRequest(request => {
      return request.url().includes('/conversion/html-to-pdf');
    });

    page.getByTestId("export-button").click();

    const exportRequest = await exportRequestPromise;
    expect(exportRequest.method()).toBe('POST');
    expect(exportRequest.url().includes('/conversion/html-to-pdf?orientation=portrait&paperSize=A3')).toBeTruthy();
  });

});
