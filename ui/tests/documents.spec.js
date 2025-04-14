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

const normalizeHtml = (htmlString) => {
  // This is a simplified example - you might need a more robust parser
  return htmlString
      .replace(/\s+/g, ' ')
      .replace(/> </g, '><')
      .trim();
}

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

  test('EL-183 - DP-11545 pair diff', async ({ page }) => {
    await expect(page.locator('.header .merge-pane')).toBeVisible({visible: true});

    const EL183_DP11545 = page.getByTestId('EL-183_DP-11545');
    await expect(EL183_DP11545.locator(".header")).toBeVisible({ visible: false });
    await expect(EL183_DP11545.locator(".content")).toBeVisible({ visible: false });
  });

  test('EL-184 - DP-11546 pair diff', async ({ page }) => {
    await expect(page.locator('.header .merge-pane')).toBeVisible({visible: true});

    const EL184_DP11546 = page.getByTestId('EL-184_DP-11546');
    await expect(EL184_DP11546.locator(".header")).toBeVisible({ visible: false });
    await expect(EL184_DP11546.locator(".content")).toBeVisible({ visible: false });
  });

  test('EL-144 - DP-11549 pair diff', async ({ page }) => {
    await expect(page.locator('.header .merge-pane')).toBeVisible({visible: true});

    const EL144_DP11549 = page.getByTestId('EL-144_DP-11549');

    const header = EL144_DP11549.locator(".header");
    await expect(header).toBeVisible({ visible: true });
    await expect(header).toHaveCSS("background-color", "rgb(238, 238, 238)");

    await expect(EL144_DP11549.locator(".header .merge-ticker .form-check input[type='checkbox']")).toBeVisible({ visible: false });

    const leftHeader = EL144_DP11549.locator(".header .left");
    await expect(leftHeader).toBeVisible({ visible: true });
    await expect(leftHeader).toHaveText("2 Requirements EL-144");

    const leftBadge = EL144_DP11549.locator(".header .left .badge");
    await expect(leftBadge).toBeVisible({ visible: true });
    await expect(leftBadge).toHaveText("EL-144");

    const rightHeader = EL144_DP11549.locator(".header .right");
    await expect(rightHeader).toBeVisible({ visible: true });
    await expect(rightHeader).toHaveText("2 Requirements DP-11549");

    const rightBadge = EL144_DP11549.locator(".header .right .badge");
    await expect(rightBadge).toBeVisible({ visible: true });
    await expect(rightBadge).toHaveText("DP-11549");

    await expect(EL144_DP11549.locator(".content")).toBeVisible({ visible: false });
  });

  test('EL-145 - DP-11550 pair diff', async ({ page }) => {
    await expect(page.locator('.header .merge-pane')).toBeVisible({visible: true});

    const EL145_DP11550 = page.getByTestId('EL-145_DP-11550');

    const header = EL145_DP11550.locator(".header");
    await expect(header).toBeVisible({ visible: true });
    await expect(header).toHaveCSS("background-color", "rgb(238, 238, 238)");

    await expect(EL145_DP11550.locator(".header .merge-ticker .form-check input[type='checkbox']")).toBeVisible({ visible: false });

    const leftHeader = EL145_DP11550.locator(".header .left");
    await expect(leftHeader).toBeVisible({ visible: true });
    await expect(leftHeader).toHaveText("2.1 User name and Password validation EL-145");

    const leftBadge = EL145_DP11550.locator(".header .left .badge");
    await expect(leftBadge).toBeVisible({ visible: true });
    await expect(leftBadge).toHaveText("EL-145");

    const rightHeader = EL145_DP11550.locator(".header .right");
    await expect(rightHeader).toBeVisible({ visible: true });
    await expect(rightHeader).toHaveText("2.1 User name and Password validation DP-11550");

    const rightBadge = EL145_DP11550.locator(".header .right .badge");
    await expect(rightBadge).toBeVisible({ visible: true });
    await expect(rightBadge).toHaveText("DP-11550");

    await expect(EL145_DP11550.locator(".content")).toBeVisible({ visible: false });
  });

  test('EL-101 - DP-11551 pair diff', async ({ page }) => {
    await expect(page.locator('.header .merge-pane')).toBeVisible({visible: true});

    const EL101_DP11551 = page.getByTestId('EL-101_DP-11551');

    const header = EL101_DP11551.locator(".header");
    await expect(header).toBeVisible({ visible: true });
    await expect(header).toHaveCSS("background-color", "rgb(246, 246, 246)");

    await expect(EL101_DP11551.locator(".header .merge-ticker .form-check input[type='checkbox']")).toBeVisible({ visible: false });

    const leftBadge = EL101_DP11551.locator(".header .left .badge");
    await expect(leftBadge).toBeVisible({ visible: true });
    await expect(leftBadge).toHaveText("EL-101 - User name must be validated when a new user account is created");

    const rightBadge = EL101_DP11551.locator(".header .right .badge");
    await expect(rightBadge).toBeVisible({ visible: true });
    await expect(rightBadge).toHaveText("DP-11551 - User name must be validated when a new user account is created");

    await expect(EL101_DP11551.locator(".content")).toBeVisible({ visible: false });
  });

  test('EL-146 - DP-11552 pair diff', async ({ page }) => {
    await expect(page.locator('.header .merge-pane')).toBeVisible({visible: true});

    const EL146_DP11552 = page.getByTestId('EL-146_DP-11552');

    const header = EL146_DP11552.locator(".header");
    await expect(header).toBeVisible({ visible: true });
    await expect(header).toHaveCSS("background-color", "rgb(246, 246, 246)");

    await expect(EL146_DP11552.locator(".header .merge-ticker .form-check input[type='checkbox']")).toBeVisible({ visible: false });

    const leftBadge = EL146_DP11552.locator(".header .left .badge");
    await expect(leftBadge).toBeVisible({ visible: true });
    await expect(leftBadge).toHaveText("EL-146 - User name may not contain spaces");

    const rightBadge = EL146_DP11552.locator(".header .right .badge");
    await expect(rightBadge).toBeVisible({ visible: true });
    await expect(rightBadge).toHaveText("DP-11552 - User name may not contain spaces");

    await expect(EL146_DP11552.locator(".content")).toBeVisible({ visible: false });
  });

  test('EL-4977 - DP-11559 pair diff', async ({ page }) => {
    await expect(page.locator('.header .merge-pane')).toBeVisible({visible: true});

    const EL4977_DP11559 = page.getByTestId('EL-4977_DP-11559');

    const header = EL4977_DP11559.locator(".header");
    await expect(header).toBeVisible({ visible: true });
    await expect(header).toHaveCSS("background-color", "rgb(246, 246, 246)");

    await expect(EL4977_DP11559.locator(".header .merge-ticker .form-check input[type='checkbox']")).toBeVisible({ visible: true });

    const leftBadge = EL4977_DP11559.locator(".header .left .badge");
    await expect(leftBadge).toBeVisible({ visible: true });
    await expect(leftBadge).toHaveText("EL-4977 - New Requirement");

    const rightBadge = EL4977_DP11559.locator(".header .right .badge");
    await expect(rightBadge).toBeVisible({ visible: true });
    await expect(rightBadge).toHaveText("DP-11559 - New Requirement");

    await expect(EL4977_DP11559.locator(".content")).toBeVisible({ visible: true });

    const descriptionDiffHeader = EL4977_DP11559.getByTestId("Description").locator(".diff-header");
    await expect(descriptionDiffHeader).toBeVisible({ visible: true });
    await expect(descriptionDiffHeader).toHaveText("Description");

    await expect(EL4977_DP11559.getByTestId("Description").locator(".diff-viewer")).toBeVisible({ visible: true });

    const descriptionDiffLeft = EL4977_DP11559.getByTestId("Description").locator(".diff-viewer .diff-leaf.left");
    await expect(descriptionDiffLeft).toBeVisible({ visible: true });

    const descriptionDiffLeftHtml = await descriptionDiffLeft.innerHTML();
    expect(normalizeHtml(descriptionDiffLeftHtml)).toBe(normalizeHtml(`
        <span class="diff-html-added" id="removed-diff-0" previous="first-diff" changeid="removed-diff-0" next="added-diff-0">Modified </span>
        <span class="diff-html-removed" id="added-diff-0" previous="removed-diff-0" changeid="added-diff-0" next="last-diff">Some new </span>requirement
    `));

    const descriptionDiffRight = EL4977_DP11559.getByTestId("Description").locator(".diff-viewer .diff-leaf.right");
    await expect(descriptionDiffRight).toBeVisible({ visible: true });

    const descriptionDiffRightHtml = await descriptionDiffRight.innerHTML();
    expect(normalizeHtml(descriptionDiffRightHtml)).toBe(normalizeHtml(`
        <span class="diff-html-removed" id="removed-diff-0" previous="first-diff" changeid="removed-diff-0" next="added-diff-0">Some new </span>
        <span class="diff-html-added" id="added-diff-0" previous="removed-diff-0" changeid="added-diff-0" next="last-diff">Modified </span>requirement
    `));

    const statusDiffHeader = EL4977_DP11559.getByTestId("Status").locator(".diff-header");
    await expect(statusDiffHeader).toBeVisible({ visible: true });
    await expect(statusDiffHeader).toHaveText("Status");

    await expect(EL4977_DP11559.getByTestId("Status").locator(".diff-viewer")).toBeVisible({ visible: true });

    const statusDiffLeft = EL4977_DP11559.getByTestId("Status").locator(".diff-viewer .diff-leaf.left");
    await expect(statusDiffLeft).toBeVisible({ visible: true });

    const statusDiffLeftHtml = await statusDiffLeft.innerHTML();
    expect(normalizeHtml(statusDiffLeftHtml)).toBe(normalizeHtml(`
        <span class="diff-html-added" id="removed-diff-0" previous="first-diff" changeid="removed-diff-0" next="added-diff-0"><span class="polarion-JSEnumOption" title="Draft">Draft</span></span>
        <span class="diff-html-removed" id="added-diff-0" previous="removed-diff-0" changeid="added-diff-0" next="last-diff"><span class="polarion-JSEnumOption" title="Reviewed">Reviewed</span></span>
    `));

    const statusDiffRight = EL4977_DP11559.getByTestId("Status").locator(".diff-viewer .diff-leaf.right");
    await expect(statusDiffRight).toBeVisible({ visible: true });

    const statusDiffRightHtml = await statusDiffRight.innerHTML();
    expect(normalizeHtml(statusDiffRightHtml)).toBe(normalizeHtml(`
        <span class="diff-html-removed" id="removed-diff-0" previous="first-diff" changeid="removed-diff-0" next="added-diff-0"><span class="polarion-JSEnumOption" title="Reviewed">Reviewed</span></span>
        <span class="diff-html-added" id="added-diff-0" previous="removed-diff-0" changeid="added-diff-0" next="last-diff"><span class="polarion-JSEnumOption" title="Draft">Draft</span></span>
    `));
  });

  test('EL-153 - DP-11560 pair diff', async ({ page }) => {
    await expect(page.locator('.header .merge-pane')).toBeVisible({visible: true});

    const EL153_DP11560 = page.getByTestId('EL-153_DP-11560');

    const header = EL153_DP11560.locator(".header");
    await expect(header).toBeVisible({ visible: true });
    await expect(header).toHaveCSS("background-color", "rgb(238, 238, 238)");

    await expect(EL153_DP11560.locator(".header .merge-ticker .form-check input[type='checkbox']")).toBeVisible({ visible: false });

    const leftHeader = EL153_DP11560.locator(".header .left");
    await expect(leftHeader).toBeVisible({ visible: true });
    await expect(leftHeader).toHaveText("2.2 Login Screen EL-153");

    const leftBadge = EL153_DP11560.locator(".header .left .badge");
    await expect(leftBadge).toBeVisible({ visible: true });
    await expect(leftBadge).toHaveText("EL-153");

    const rightHeader = EL153_DP11560.locator(".header .right");
    await expect(rightHeader).toBeVisible({ visible: true });
    await expect(rightHeader).toHaveText("2.2 Login Screen DP-11560");

    const rightBadge = EL153_DP11560.locator(".header .right .badge");
    await expect(rightBadge).toBeVisible({ visible: true });
    await expect(rightBadge).toHaveText("DP-11560");

    await expect(EL153_DP11560.locator(".content")).toBeVisible({ visible: false });
  });

  test('EL-11572 - DP-11571 pair diff', async ({ page }) => {
    await expect(page.locator('.header .merge-pane')).toBeVisible({visible: true});

    const EL11572_DP11571 = page.getByTestId('EL-11572_DP-11571');

    const header = EL11572_DP11571.locator(".header");
    await expect(header).toBeVisible({ visible: true });
    await expect(header).toHaveCSS("background-color", "rgb(246, 246, 246)");

    await expect(EL11572_DP11571.locator(".header .merge-ticker .form-check input[type='checkbox']")).toBeVisible({ visible: false });

    const leftBadge = EL11572_DP11571.locator(".header .left .badge");
    await expect(leftBadge).toBeVisible({ visible: true });
    await expect(leftBadge).toHaveText("EL-11572 - New Requirement");

    const rightBadge = EL11572_DP11571.locator(".header .right .badge");
    await expect(rightBadge).toBeVisible({ visible: true });
    await expect(rightBadge).toHaveText("DP-11571 - New Requirement");

    await expect(EL11572_DP11571.locator(".content")).toBeVisible({ visible: false });
  });

  test('EL-156 - NONE pair diff', async ({ page }) => {
    await expect(page.locator('.header .merge-pane')).toBeVisible({visible: true});

    const EL156_NONE = page.getByTestId('EL-156_NONE');

    const header = EL156_NONE.locator(".header");
    await expect(header).toBeVisible({ visible: true });
    await expect(header).toHaveCSS("background-color", "rgb(246, 246, 246)");

    await expect(EL156_NONE.locator(".header .merge-ticker .form-check input[type='checkbox']")).toBeVisible({ visible: true });

    const leftBadge = EL156_NONE.locator(".header .left .badge");
    await expect(leftBadge).toBeVisible({ visible: true });
    await expect(leftBadge).toHaveText("EL-156 - User must be informed on the login screen when Caps Lock is turned on");

    const rightHeader = EL156_NONE.locator(".header .right");
    await expect(rightHeader).toBeVisible({ visible: true });
    await expect(rightHeader).toHaveText("–");

    await expect(EL156_NONE.locator(".content")).toBeVisible({ visible: true });

    const descriptionDiffHeader = EL156_NONE.getByTestId("Description").locator(".diff-header");
    await expect(descriptionDiffHeader).toBeVisible({ visible: true });
    await expect(descriptionDiffHeader).toHaveText("Description");

    await expect(EL156_NONE.getByTestId("Description").locator(".diff-viewer")).toBeVisible({ visible: true });

    const descriptionDiffLeft = EL156_NONE.getByTestId("Description").locator(".diff-viewer .diff-leaf.left");
    await expect(descriptionDiffLeft).toBeVisible({ visible: true });

    const descriptionDiffLeftHtml = await descriptionDiffLeft.innerHTML();
    expect(normalizeHtml(descriptionDiffLeftHtml)).toBe(normalizeHtml(`
        <span class="diff-html-removed" id="added-diff-0" previous="first-diff" changeid="added-diff-0" next="last-diff">User must be informed on the login screen when Caps Lock is turned on</span>
    `));

    const descriptionDiffRight = EL156_NONE.getByTestId("Description").locator(".diff-viewer .diff-leaf.right");
    await expect(descriptionDiffRight).toBeVisible({ visible: true });

    const descriptionDiffRightHtml = await descriptionDiffRight.innerHTML();
    expect(descriptionDiffRightHtml).toBe("");

    const typeDiffLeft = EL156_NONE.getByTestId("Type").locator(".diff-viewer .diff-leaf.left");
    await expect(typeDiffLeft).toBeVisible({ visible: true });

    const typeDiffLeftHtml = await typeDiffLeft.innerHTML();
    expect(normalizeHtml(typeDiffLeftHtml)).toBe(normalizeHtml(`
        <span class="diff-html-added" id="removed-diff-0" previous="first-diff" changeid="removed-diff-0" next="added-diff-0"></span>
        <span class="diff-html-removed" id="added-diff-0" previous="removed-diff-0" changeid="added-diff-0" next="last-diff"><span class="polarion-JSEnumOption" title="Requirement - A nonfunctional requirement.">Requirement</span></span>
    `));

    const typeDiffRight = EL156_NONE.getByTestId("Type").locator(".diff-viewer .diff-leaf.right");
    await expect(typeDiffRight).toBeVisible({ visible: true });

    const typeDiffRightHtml = await typeDiffRight.innerHTML();
    expect(typeDiffRightHtml).toBe("");

    const titleDiffLeft = EL156_NONE.getByTestId("Title").locator(".diff-viewer .diff-leaf.left");
    await expect(titleDiffLeft).toBeVisible({ visible: true });

    const titleDiffLeftHtml = await titleDiffLeft.innerHTML();
    expect(normalizeHtml(titleDiffLeftHtml)).toBe(normalizeHtml(`
        <span class="diff-html-removed" id="added-diff-0" previous="first-diff" changeid="added-diff-0" next="last-diff">User must be informed on the login screen when Caps Lock is turned on</span>
    `));

    const titleDiffRight = EL156_NONE.getByTestId("Title").locator(".diff-viewer .diff-leaf.right");
    await expect(titleDiffRight).toBeVisible({ visible: true });

    const titleDiffRightHtml = await titleDiffRight.innerHTML();
    expect(titleDiffRightHtml).toBe("");

    const statusDiffHeader = EL156_NONE.getByTestId("Status").locator(".diff-header");
    await expect(statusDiffHeader).toBeVisible({ visible: true });
    await expect(statusDiffHeader).toHaveText("Status");

    await expect(EL156_NONE.getByTestId("Status").locator(".diff-viewer")).toBeVisible({ visible: true });

    const statusDiffLeft = EL156_NONE.getByTestId("Status").locator(".diff-viewer .diff-leaf.left");
    await expect(statusDiffLeft).toBeVisible({ visible: true });

    const statusDiffLeftHtml = await statusDiffLeft.innerHTML();
    expect(normalizeHtml(statusDiffLeftHtml)).toBe(normalizeHtml(`
        <span class="diff-html-added" id="removed-diff-0" previous="first-diff" changeid="removed-diff-0" next="added-diff-0"></span>
        <span class="diff-html-removed" id="added-diff-0" previous="removed-diff-0" changeid="added-diff-0" next="last-diff"><span class="polarion-JSEnumOption" title="Approved">Approved</span></span>
    `));

    const statusDiffRight = EL156_NONE.getByTestId("Status").locator(".diff-viewer .diff-leaf.right");
    await expect(statusDiffRight).toBeVisible({ visible: true });

    const statusDiffRightHtml = await statusDiffRight.innerHTML();
    expect(statusDiffRightHtml).toBe("");
  });

});
