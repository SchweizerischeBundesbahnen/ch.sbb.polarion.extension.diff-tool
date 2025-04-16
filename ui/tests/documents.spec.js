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

const workItemsMergeFixtureDataProvider = (requestBody) => {
  let fixtureFile;
  if (!requestBody.pairs[0].leftWorkItem) {
    if (requestBody.pairs[0].rightWorkItem.id === "DP-11570") {
      fixtureFile = "NONE_DP-11570_merge.json";
    }
  } else if (!requestBody.pairs[0].rightWorkItem) {
    if (requestBody.pairs[0].leftWorkItem.id === "EL-156") {
      fixtureFile = "EL-156_NONE_merge.json";
    }
  } else if (requestBody.pairs[0].leftWorkItem.id === "EL-4977" && requestBody.pairs[0].rightWorkItem.id === "DP-11559") {
    fixtureFile = "EL-4977_DP-11559_merge.json";
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
      },

      // Mock "/diff/document-workitems" endpoint which returns JSON response depending on data in request body
      mockWorkItemsMergeEndpoint: async () => {
        await page.route("**/merge/documents", route => {
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
            body: workItemsMergeFixtureDataProvider(requestBody)
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
    await mockApi.mockWorkItemsMergeEndpoint();

    await page.goto('/documents?sourceProjectId=elibrary&sourceSpaceId=Specification&sourceDocument=Catalog%20Specification&targetProjectId=drivepilot&targetSpaceId=Design&targetDocument=Catalog%20Design&linkRole=relates_to');
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
    expect(postData.leftDocument.name).toBe("Catalog Specification");
    expect(postData.leftDocument.revision).toBe(null);
    expect(postData.rightDocument.projectId).toBe("drivepilot");
    expect(postData.rightDocument.spaceId).toBe("Design");
    expect(postData.rightDocument.name).toBe("Catalog Design");
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

    const mergePane = page.locator('.header .merge-pane');
    await expect(mergePane).toBeVisible({visible: true}); // Expect merge pane is visible

    await mergePane.isVisible(); // Wait until merge pane is visible before next steps

    const selectAllLabel = page.locator('.header .merge-pane label.select-all');
    await expect(selectAllLabel).toBeVisible();
    await expect(selectAllLabel).toHaveText("select all");

    const selectAllCheckbox = page.locator('.header .merge-pane label.select-all input[type="checkbox"]');
    await expect(selectAllCheckbox).toBeVisible();
    await expect(selectAllCheckbox).toBeChecked({ checked: false });

    const mergeButtons = page.locator('.header .merge-pane .merge-button .btn');
    await expect(mergeButtons).toHaveCount(2);
    for (let i = 0; i < 2; i++) {
      await expect(mergeButtons.nth(i)).toBeVisible();
      await expect(mergeButtons.nth(i)).toBeDisabled();
    }

  });

  test('control pane', async ({ page }) => {
    expect(await page.locator('.control-pane.expanded').count()).toEqual(0);

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

    await page.locator('.header .merge-pane').isVisible(); // Wait until merge pane is visible before next steps

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

    await page.locator('.header .merge-pane').isVisible(); // Wait until merge pane is visible before next steps

    const EL183_DP11545 = page.getByTestId('EL-183_DP-11545');
    await expect(EL183_DP11545.locator(".header")).toBeVisible({ visible: false });
    await expect(EL183_DP11545.locator(".content")).toBeVisible({ visible: false });
  });

  test('EL-184 - DP-11546 pair diff', async ({ page }) => {
    await expect(page.locator('.header .merge-pane')).toBeVisible({visible: true});

    await page.locator('.header .merge-pane').isVisible(); // Wait until merge pane is visible before next steps

    const EL184_DP11546 = page.getByTestId('EL-184_DP-11546');
    await expect(EL184_DP11546.locator(".header")).toBeVisible({ visible: false });
    await expect(EL184_DP11546.locator(".content")).toBeVisible({ visible: false });
  });

  test('EL-144 - DP-11549 pair diff', async ({ page }) => {
    await expect(page.locator('.header .merge-pane')).toBeVisible({visible: true});

    await page.locator('.header .merge-pane').isVisible(); // Wait until merge pane is visible before next steps

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

    await page.locator('.header .merge-pane').isVisible(); // Wait until merge pane is visible before next steps

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

    await page.locator('.header .merge-pane').isVisible(); // Wait until merge pane is visible before next steps

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

    await page.locator('.header .merge-pane').isVisible(); // Wait until merge pane is visible before next steps

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

    await page.locator('.header .merge-pane').isVisible(); // Wait until merge pane is visible before next steps

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

    await page.locator('.header .merge-pane').isVisible(); // Wait until merge pane is visible before next steps

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

    await page.locator('.header .merge-pane').isVisible(); // Wait until merge pane is visible before next steps

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

    await page.locator('.header .merge-pane').isVisible(); // Wait until merge pane is visible before next steps

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

    const typeDiffHeader = EL156_NONE.getByTestId("Type").locator(".diff-header");
    await expect(typeDiffHeader).toBeVisible({ visible: true });
    await expect(typeDiffHeader).toHaveText("Type");

    await expect(EL156_NONE.getByTestId("Type").locator(".diff-viewer")).toBeVisible({ visible: true });

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

    const titleDiffHeader = EL156_NONE.getByTestId("Title").locator(".diff-header");
    await expect(titleDiffHeader).toBeVisible({ visible: true });
    await expect(titleDiffHeader).toHaveText("Title");

    await expect(EL156_NONE.getByTestId("Title").locator(".diff-viewer")).toBeVisible({ visible: true });

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

  test('EL-157 - DP-11562 direct moved pair', async ({ page }) => {
    await expect(page.locator('.header .merge-pane')).toBeVisible({visible: true});

    await page.locator('.header .merge-pane').isVisible(); // Wait until merge pane is visible before next steps

    const EL157_DP11562_direct = page.getByTestId('EL-157_DP-11562_direct');

    const header = EL157_DP11562_direct.locator(".header");
    await expect(header).toBeVisible({ visible: true });
    await expect(header).toHaveCSS("background-color", "rgb(246, 246, 246)");

    await expect(EL157_DP11562_direct.locator(".header .merge-ticker .form-check input[type='checkbox']")).toBeVisible({ visible: true });

    const leftBadge = EL157_DP11562_direct.locator(".header .left .badge");
    await expect(leftBadge).toBeVisible({ visible: true });
    await expect(leftBadge).toHaveText("EL-157 - User may not be told if the password or user name is wrong");

    const movedIcon = EL157_DP11562_direct.locator(".header .moved.right svg");
    await expect(movedIcon).toBeVisible({ visible: true });

    const movedIconHtml = await movedIcon.innerHTML();
    expect(normalizeHtml(movedIconHtml)).toBe(normalizeHtml(`
      <g id=\"SVGRepo_bgCarrier\" stroke-width=\"0\"></g><g id=\"SVGRepo_tracerCarrier\" stroke-linecap=\"round\" stroke-linejoin=\"round\"></g><g id=\"SVGRepo_iconCarrier\"><path d=\"M7 16L9 16C11.2091 16 13 14.2091 13 12L13 7\" stroke=\"#666666\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"></path><path d=\"M16 10L13 7L10 10\" stroke=\"#666666\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"></path></g>
    `));

    const movedLink = EL157_DP11562_direct.locator(".header .moved.right a");
    await expect(movedLink).toBeVisible({ visible: true });
    await expect(movedLink).toHaveText("moved to 2.2-2");

    await expect(EL157_DP11562_direct.locator(".content")).toBeVisible({ visible: false });
  });

  test('EL-157 - DP-11562 reverse moved pair', async ({ page }) => {
    await expect(page.locator('.header .merge-pane')).toBeVisible({visible: true});

    await page.locator('.header .merge-pane').isVisible(); // Wait until merge pane is visible before next steps

    const EL157_DP11562_reverse = page.getByTestId('EL-157_DP-11562_reverse');

    const header = EL157_DP11562_reverse.locator(".header");
    await expect(header).toBeVisible({ visible: true });
    await expect(header).toHaveCSS("background-color", "rgb(246, 246, 246)");

    await expect(EL157_DP11562_reverse.locator(".header .merge-ticker .form-check input[type='checkbox']")).toBeVisible({ visible: true });

    const movedIcon = EL157_DP11562_reverse.locator(".header .moved.left svg");
    await expect(movedIcon).toBeVisible({ visible: true });

    const movedIconHtml = await movedIcon.innerHTML();
    expect(normalizeHtml(movedIconHtml)).toBe(normalizeHtml(`
      <g id=\"SVGRepo_bgCarrier\" stroke-width=\"0\"></g><g id=\"SVGRepo_tracerCarrier\" stroke-linecap=\"round\" stroke-linejoin=\"round\"></g><g id=\"SVGRepo_iconCarrier\"><path d=\"M17 8L15 8C12.7909 8 11 9.79086 11 12L11 17\" stroke=\"#666666\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"></path><path d=\"M8 14L11 17L14 14\" stroke=\"#666666\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"></path></g>
    `));

    const movedLink = EL157_DP11562_reverse.locator(".header .moved.left a");
    await expect(movedLink).toBeVisible({ visible: true });
    await expect(movedLink).toHaveText("moved to 2.2-3");

    const rightBadge = EL157_DP11562_reverse.locator(".header .right .badge");
    await expect(rightBadge).toBeVisible({ visible: true });
    await expect(rightBadge).toHaveText("DP-11562 - User may not be told if the password or user name is wrong");

    await expect(EL157_DP11562_reverse.locator(".content")).toBeVisible({ visible: false });
  });

  test('NONE - DP-11570 pair diff', async ({ page }) => {
    await expect(page.locator('.header .merge-pane')).toBeVisible({visible: true});

    await page.locator('.header .merge-pane').isVisible(); // Wait until merge pane is visible before next steps

    const NONE_DP11570 = page.getByTestId('NONE_DP-11570');

    const header = NONE_DP11570.locator(".header");
    await expect(header).toBeVisible({ visible: true });
    await expect(header).toHaveCSS("background-color", "rgb(238, 238, 238)");

    await expect(NONE_DP11570.locator(".header .merge-ticker .form-check input[type='checkbox']")).toBeVisible({ visible: true });

    const leftHeader = NONE_DP11570.locator(".header .left");
    await expect(leftHeader).toBeVisible({ visible: true });
    await expect(leftHeader).toHaveText("–");

    const rightHeader = NONE_DP11570.locator(".header .right");
    await expect(rightHeader).toBeVisible({ visible: true });
    await expect(rightHeader).toHaveText("2.3 New paragraph DP-11570");

    const rightBadge = NONE_DP11570.locator(".header .right .badge");
    await expect(rightBadge).toBeVisible({ visible: true });
    await expect(rightBadge).toHaveText("DP-11570");

    await expect(NONE_DP11570.locator(".content")).toBeVisible({ visible: true });

    const typeDiffHeader = NONE_DP11570.getByTestId("Type").locator(".diff-header");
    await expect(typeDiffHeader).toBeVisible({ visible: true });
    await expect(typeDiffHeader).toHaveText("Type");

    await expect(NONE_DP11570.getByTestId("Type").locator(".diff-viewer")).toBeVisible({ visible: true });

    const typeDiffLeft = NONE_DP11570.getByTestId("Type").locator(".diff-viewer .diff-leaf.left");
    await expect(typeDiffLeft).toBeVisible({ visible: true });

    const typeDiffLeftHtml = await typeDiffLeft.innerHTML();
    expect(typeDiffLeftHtml).toBe("");

    const typeDiffRight = NONE_DP11570.getByTestId("Type").locator(".diff-viewer .diff-leaf.right");
    await expect(typeDiffRight).toBeVisible({ visible: true });

    const typeDiffRightHtml = await typeDiffRight.innerHTML();
    expect(normalizeHtml(typeDiffRightHtml)).toBe(normalizeHtml(`
        <span class="diff-html-removed" id="removed-diff-0" previous="first-diff" changeid="removed-diff-0" next="added-diff-0"></span>
        <span class="diff-html-added" id="added-diff-0" previous="removed-diff-0" changeid="added-diff-0" next="last-diff"><span class="polarion-JSEnumOption" title="Heading">Heading</span></span>
    `));

    const titleDiffHeader = NONE_DP11570.getByTestId("Title").locator(".diff-header");
    await expect(titleDiffHeader).toBeVisible({ visible: true });
    await expect(titleDiffHeader).toHaveText("Title");

    await expect(NONE_DP11570.getByTestId("Title").locator(".diff-viewer")).toBeVisible({ visible: true });

    const titleDiffLeft = NONE_DP11570.getByTestId("Title").locator(".diff-viewer .diff-leaf.left");
    await expect(titleDiffLeft).toBeVisible({ visible: true });

    const titleDiffLeftHtml = await titleDiffLeft.innerHTML();
    expect(titleDiffLeftHtml).toBe("");

    const titleDiffRight = NONE_DP11570.getByTestId("Title").locator(".diff-viewer .diff-leaf.right");
    await expect(titleDiffRight).toBeVisible({ visible: true });

    const titleDiffRightHtml = await titleDiffRight.innerHTML();
    expect(normalizeHtml(titleDiffRightHtml)).toBe(normalizeHtml(`
        <span class="diff-html-added" id="added-diff-0" previous="first-diff" changeid="added-diff-0" next="last-diff">New paragraph</span>
    `));

    const statusDiffHeader = NONE_DP11570.getByTestId("Status").locator(".diff-header");
    await expect(statusDiffHeader).toBeVisible({ visible: true });
    await expect(statusDiffHeader).toHaveText("Status");

    await expect(NONE_DP11570.getByTestId("Status").locator(".diff-viewer")).toBeVisible({ visible: true });

    const statusDiffLeft = NONE_DP11570.getByTestId("Status").locator(".diff-viewer .diff-leaf.left");
    await expect(statusDiffLeft).toBeVisible({ visible: true });

    const statusDiffLeftHtml = await statusDiffLeft.innerHTML();
    expect(statusDiffLeftHtml).toBe("");

    const statusDiffRight = NONE_DP11570.getByTestId("Status").locator(".diff-viewer .diff-leaf.right");
    await expect(statusDiffRight).toBeVisible({ visible: true });

    const statusDiffRightHtml = await statusDiffRight.innerHTML();
    expect(normalizeHtml(statusDiffRightHtml)).toBe(normalizeHtml(`
        <span class="diff-html-removed" id="removed-diff-0" previous="first-diff" changeid="removed-diff-0" next="added-diff-0"></span>
        <span class="diff-html-added" id="added-diff-0" previous="removed-diff-0" changeid="added-diff-0" next="last-diff"><span class="polarion-JSEnumOption" title="Open">Open</span></span>
    `));
  });

  test('EL-158 - DP-11563 direct moved pair', async ({ page }) => {
    await expect(page.locator('.header .merge-pane')).toBeVisible({visible: true});

    await page.locator('.header .merge-pane').isVisible(); // Wait until merge pane is visible before next steps

    const EL158_DP11563_direct = page.getByTestId('EL-158_DP-11563_direct');

    const header = EL158_DP11563_direct.locator(".header");
    await expect(header).toBeVisible({ visible: true });
    await expect(header).toHaveCSS("background-color", "rgb(238, 238, 238)");

    await expect(EL158_DP11563_direct.locator(".header .merge-ticker .form-check input[type='checkbox']")).toBeVisible({ visible: true });

    const leftHeader = EL158_DP11563_direct.locator(".header .left");
    await expect(leftHeader).toBeVisible({ visible: true });
    await expect(leftHeader).toHaveText("2.3 Performance Requirements EL-158");

    const leftBadge = EL158_DP11563_direct.locator(".header .left .badge");
    await expect(leftBadge).toBeVisible({ visible: true });
    await expect(leftBadge).toHaveText("EL-158");

    const movedIcon = EL158_DP11563_direct.locator(".header .moved.right svg");
    await expect(movedIcon).toBeVisible({ visible: true });

    const movedIconHtml = await movedIcon.innerHTML();
    expect(normalizeHtml(movedIconHtml)).toBe(normalizeHtml(`
      <g id=\"SVGRepo_bgCarrier\" stroke-width=\"0\"></g><g id=\"SVGRepo_tracerCarrier\" stroke-linecap=\"round\" stroke-linejoin=\"round\"></g><g id=\"SVGRepo_iconCarrier\"><path d=\"M7 8L9 8C11.2091 8 13 9.79086 13 12L13 17\" stroke=\"#666666\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"></path><path d=\"M16 14L13 17L10 14\" stroke=\"#666666\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"></path></g>
    `));

    const movedLink = EL158_DP11563_direct.locator(".header .moved.right a");
    await expect(movedLink).toBeVisible({ visible: true });
    await expect(movedLink).toHaveText("moved to 2.4");

    await expect(EL158_DP11563_direct.locator(".content")).toBeVisible({ visible: false });
  });

  test('EL-158 - DP-11563 reverse moved pair', async ({ page }) => {
    await expect(page.locator('.header .merge-pane')).toBeVisible({visible: true});

    await page.locator('.header .merge-pane').isVisible(); // Wait until merge pane is visible before next steps

    const EL158_DP11563_reverse = page.getByTestId('EL-158_DP-11563_reverse');

    const header = EL158_DP11563_reverse.locator(".header");
    await expect(header).toBeVisible({ visible: true });
    await expect(header).toHaveCSS("background-color", "rgb(238, 238, 238)");

    await expect(EL158_DP11563_reverse.locator(".header .merge-ticker .form-check input[type='checkbox']")).toBeVisible({ visible: true });

    const movedIcon = EL158_DP11563_reverse.locator(".header .moved.left svg");
    await expect(movedIcon).toBeVisible({ visible: true });

    const movedIconHtml = await movedIcon.innerHTML();
    expect(normalizeHtml(movedIconHtml)).toBe(normalizeHtml(`
      <g id=\"SVGRepo_bgCarrier\" stroke-width=\"0\"></g><g id=\"SVGRepo_tracerCarrier\" stroke-linecap=\"round\" stroke-linejoin=\"round\"></g><g id=\"SVGRepo_iconCarrier\"><path d=\"M17 16L15 16C12.7909 16 11 14.2091 11 12L11 7\" stroke=\"#666666\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"></path><path d=\"M8 10L11 7L14 10\" stroke=\"#666666\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"></path></g>
    `));

    const movedLink = EL158_DP11563_reverse.locator(".header .moved.left a");
    await expect(movedLink).toBeVisible({ visible: true });
    await expect(movedLink).toHaveText("moved to 2.3");

    const rightHeader = EL158_DP11563_reverse.locator(".header .right");
    await expect(rightHeader).toBeVisible({ visible: true });
    await expect(rightHeader).toHaveText("2.4 Performance Requirements DP-11563");

    const rightBadge = EL158_DP11563_reverse.locator(".header .right .badge");
    await expect(rightBadge).toBeVisible({ visible: true });
    await expect(rightBadge).toHaveText("DP-11563");

    await expect(EL158_DP11563_reverse.locator(".content")).toBeVisible({ visible: false });
  });

  test('EL-159 - DP-11564 direct moved pair', async ({ page }) => {
    await expect(page.locator('.header .merge-pane')).toBeVisible({visible: true});

    await page.locator('.header .merge-pane').isVisible(); // Wait until merge pane is visible before next steps

    const EL159_DP11564_direct = page.getByTestId('EL-159_DP-11564_direct');

    const header = EL159_DP11564_direct.locator(".header");
    await expect(header).toBeVisible({ visible: true });
    await expect(header).toHaveCSS("background-color", "rgb(246, 246, 246)");

    await expect(EL159_DP11564_direct.locator(".header .merge-ticker .form-check input[type='checkbox']")).toBeVisible({ visible: true });

    const leftBadge = EL159_DP11564_direct.locator(".header .left .badge");
    await expect(leftBadge).toBeVisible({ visible: true });
    await expect(leftBadge).toHaveText("EL-159 - System should meet the following acceptance criteria");

    const movedIcon = EL159_DP11564_direct.locator(".header .moved.right svg");
    await expect(movedIcon).toBeVisible({ visible: true });

    const movedIconHtml = await movedIcon.innerHTML();
    expect(normalizeHtml(movedIconHtml)).toBe(normalizeHtml(`
      <g id=\"SVGRepo_bgCarrier\" stroke-width=\"0\"></g><g id=\"SVGRepo_tracerCarrier\" stroke-linecap=\"round\" stroke-linejoin=\"round\"></g><g id=\"SVGRepo_iconCarrier\"><path d=\"M7 8L9 8C11.2091 8 13 9.79086 13 12L13 17\" stroke=\"#666666\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"></path><path d=\"M16 14L13 17L10 14\" stroke=\"#666666\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"></path></g>
    `));

    const movedLink = EL159_DP11564_direct.locator(".header .moved.right a");
    await expect(movedLink).toBeVisible({ visible: true });
    await expect(movedLink).toHaveText("moved to 2.4-1");

    await expect(EL159_DP11564_direct.locator(".content")).toBeVisible({ visible: false });
  });

  test('EL-159 - DP-11564 reverse moved pair', async ({ page }) => {
    await expect(page.locator('.header .merge-pane')).toBeVisible({visible: true});

    await page.locator('.header .merge-pane').isVisible(); // Wait until merge pane is visible before next steps

    const EL159_DP11564_reverse = page.getByTestId('EL-159_DP-11564_reverse');

    const header = EL159_DP11564_reverse.locator(".header");
    await expect(header).toBeVisible({ visible: true });
    await expect(header).toHaveCSS("background-color", "rgb(246, 246, 246)");

    await expect(EL159_DP11564_reverse.locator(".header .merge-ticker .form-check input[type='checkbox']")).toBeVisible({ visible: true });

    const movedIcon = EL159_DP11564_reverse.locator(".header .moved.left svg");
    await expect(movedIcon).toBeVisible({ visible: true });

    const movedIconHtml = await movedIcon.innerHTML();
    expect(normalizeHtml(movedIconHtml)).toBe(normalizeHtml(`
      <g id=\"SVGRepo_bgCarrier\" stroke-width=\"0\"></g><g id=\"SVGRepo_tracerCarrier\" stroke-linecap=\"round\" stroke-linejoin=\"round\"></g><g id=\"SVGRepo_iconCarrier\"><path d=\"M17 16L15 16C12.7909 16 11 14.2091 11 12L11 7\" stroke=\"#666666\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"></path><path d=\"M8 10L11 7L14 10\" stroke=\"#666666\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"></path></g>
    `));

    const movedLink = EL159_DP11564_reverse.locator(".header .moved.left a");
    await expect(movedLink).toBeVisible({ visible: true });
    await expect(movedLink).toHaveText("moved to 2.3-1");

    const rightBadge = EL159_DP11564_reverse.locator(".header .right .badge");
    await expect(rightBadge).toBeVisible({ visible: true });
    await expect(rightBadge).toHaveText("DP-11564 - System should meet the following acceptance criteria");

    await expect(EL159_DP11564_reverse.locator(".content")).toBeVisible({ visible: false });
  });

  test('expand/collapse', async ({ page }) => {
    await expect(page.locator('.header .merge-pane')).toBeVisible({visible: true});

    await page.locator('.header .merge-pane').isVisible(); // Wait until merge pane is visible before next steps

    const EL4977_DP11559 = page.getByTestId('EL-4977_DP-11559');

    const content = EL4977_DP11559.locator(".content");
    await expect(content).toBeVisible();

    const header = EL4977_DP11559.locator(".header");
    await expect(header).toBeVisible();

    const collapseButton = header.locator(".floating-button");
    let collapseButtonIcon = collapseButton.locator("svg");
    let collapseButtonIconHtml = await collapseButtonIcon.innerHTML();
    expect(normalizeHtml(collapseButtonIconHtml)).toBe(normalizeHtml(`
      <path fill=\"currentColor\" d=\"M233.4 406.6c12.5 12.5 32.8 12.5 45.3 0l192-192c12.5-12.5 12.5-32.8 0-45.3s-32.8-12.5-45.3 0L256 338.7 86.6 169.4c-12.5-12.5-32.8-12.5-45.3 0s-12.5 32.8 0 45.3l192 192z\"></path>
    `));

    await collapseButton.click();

    collapseButtonIcon = collapseButton.locator("svg");
    collapseButtonIconHtml = await collapseButtonIcon.innerHTML();
    expect(normalizeHtml(collapseButtonIconHtml)).toBe(normalizeHtml(`
      <path fill=\"currentColor\" d=\"M233.4 105.4c12.5-12.5 32.8-12.5 45.3 0l192 192c12.5 12.5 12.5 32.8 0 45.3s-32.8 12.5-45.3 0L256 173.3 86.6 342.6c-12.5 12.5-32.8 12.5-45.3 0s-12.5-32.8 0-45.3l192-192z\"></path>
    `));

    await expect(content).toBeVisible({ visible: false });
  });

  test('select all', async ({ page }) => {
    await expect(page.locator('.header .merge-pane')).toBeVisible({visible: true});

    await page.locator('.header .merge-pane').isVisible(); // Wait until merge pane is visible before next steps

    // Check first that headers of WI pairs are not visually marked as selected. Also hidden ones will be taken by locator below, but for this test this is not important
    const allPairs = page.locator('.wi-diff');
    for (let i = 0; i < await allPairs.count(); i++) {
      const pairHeaders = allPairs.nth(i).locator(".wi-header");
      for (let j = 0; j < await pairHeaders.count(); j++) {
        await expect(pairHeaders.nth(j)).not.toHaveCSS("background-color", "rgba(150, 190, 250, 0.2)");
      }
    }

    const selectAllCheckbox = page.locator('.header .merge-pane label.select-all input[type="checkbox"]');
    await selectAllCheckbox.isVisible();
    await selectAllCheckbox.click();

    // Check that when "select all" is ticked all other selection checkboxes also ticked
    const mergeTickers = page.locator('.wi-diff .merge-ticker input[type="checkbox"]');
    const mergeTickersCount = await mergeTickers.count();
    for (let i = 0; i < mergeTickersCount; i++) {
      await expect(mergeTickers.nth(i)).toBeVisible();
      await expect(mergeTickers.nth(i)).toBeChecked();
    }

    // Check that merge buttons are enabled
    const mergeButtons = page.locator('.header .merge-pane .merge-button .btn');
    await expect(mergeButtons).toHaveCount(2);
    for (let i = 0; i < 2; i++) {
      await expect(mergeButtons.nth(i)).toBeVisible();
      await expect(mergeButtons.nth(i)).toBeEnabled();
    }

    // And finally check that number of pairs visually marked as selected equal to number of selected checkboxes,
    // also check that headers of selected pairs change background
    const selectedPairs = page.locator('.wi-diff.selected');
    const selectedPairsCount = await selectedPairs.count();
    expect(selectedPairsCount).toBe(mergeTickersCount);
    for (let i = 0; i < mergeTickersCount; i++) {
      const pairHeaders = selectedPairs.nth(i).locator(".wi-header");
      for (let j = 0; j < await pairHeaders.count(); j++) {
        await expect(pairHeaders.nth(j)).toBeVisible();
        await expect(pairHeaders.nth(j)).toHaveCSS("background-color", "rgba(150, 190, 250, 0.2)");
      }
    }

    // When single checkbox is un-ticked check that "select all" is also automatically un-ticked
    await mergeTickers.nth(0).click();
    await expect(selectAllCheckbox).toBeChecked({ checked: false });
  });

  test('select single', async ({ page }) => {
    await expect(page.locator('.header .merge-pane')).toBeVisible({visible: true});

    await page.locator('.header .merge-pane').isVisible(); // Wait until merge pane is visible before next steps

    const selectAllCheckbox = page.locator('.header .merge-pane label.select-all input[type="checkbox"]');
    await expect(selectAllCheckbox).toBeVisible();
    await expect(selectAllCheckbox).toBeChecked({ checked: false });

    const firstSelectionCheckbox = page.locator('.wi-diff .merge-ticker input[type="checkbox"]').nth(0);
    await firstSelectionCheckbox.isVisible();
    await firstSelectionCheckbox.click();

    // Check that "select all" is still unchecked
    await expect(selectAllCheckbox).toBeChecked({ checked: false });

    const selectedPairs = page.locator('.wi-diff.selected');
    const selectedPairsCount = await selectedPairs.count();
    expect(selectedPairsCount).toBe(1);
    const pairHeaders = selectedPairs.nth(0).locator(".wi-header");
    for (let j = 0; j < await pairHeaders.count(); j++) {
      await expect(pairHeaders.nth(j)).toBeVisible();
      await expect(pairHeaders.nth(j)).toHaveCSS("background-color", "rgba(150, 190, 250, 0.2)");
    }

    // Check that merge buttons are enabled
    const mergeButtons = page.locator('.header .merge-pane .merge-button .btn');
    await expect(mergeButtons).toHaveCount(2);
    for (let i = 0; i < 2; i++) {
      await expect(mergeButtons.nth(i)).toBeVisible();
      await expect(mergeButtons.nth(i)).toBeEnabled();
    }
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

    const actionButton = page.getByTestId("merge-confirmation-modal-action-button");
    await expect(actionButton).toBeVisible();
    await expect(actionButton).toBeEnabled();

    await actionButton.isVisible();
    actionButton.click();

    await expect(page.getByTestId("merge-confirmation-modal")).toBeVisible({ visible: false });

    const request = await requestPromise;

    // Now you can make assertions about the request
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
      expect(postData.pairs[0].leftWorkItem.outlineNumber).toBe("2.1-3");
      expect(postData.pairs[0].leftWorkItem.externalProjectWorkItem).toBe(false);
      expect(postData.pairs[0].leftWorkItem.referenced).toBe(false);
      expect(postData.pairs[0].leftWorkItem.moveDirection).toBe(null);
      expect(postData.pairs[0].leftWorkItem.movedOutlineNumber).toBe(null);
      expect(postData.pairs[0].leftWorkItem.title).toBe("New Requirement");

      expect(postData.pairs[0].rightWorkItem.projectId).toBe("drivepilot");
      expect(postData.pairs[0].rightWorkItem.id).toBe("DP-11559");
      expect(postData.pairs[0].rightWorkItem.outlineNumber).toBe("2.1-3");
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
});
