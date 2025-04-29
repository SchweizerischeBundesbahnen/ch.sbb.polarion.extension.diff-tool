import { test as base } from '@playwright/test';
import path from 'path';
import fs from 'fs';

export const fixtureDataProvider = (fixtureFile) => {
  const filePath = path.join(__dirname, 'fixtures', fixtureFile);
  const fixtureData = JSON.parse(fs.readFileSync(filePath, 'utf8'));
  return JSON.stringify(fixtureData)
};

export const workItemsDiffFixtureDataProvider = (requestBody) => {
  let fixtureFile;
  if (!requestBody.leftWorkItem) {
    if (requestBody.rightWorkItem.id === "DP-11570") {
      fixtureFile = "NONE_DP-11570.json";
    }
  } else if (!requestBody.rightWorkItem) {
    if (requestBody.leftWorkItem.id === "EL-156") {
      fixtureFile = "EL-156_NONE.json";
    } else if (requestBody.leftWorkItem.id === "EL-11600") {
      fixtureFile = "EL-11600_NONE.json";
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
  } else if (requestBody.leftWorkItem.id === "EL-11575" && requestBody.rightWorkItem.id === "EL-11575") {
    fixtureFile = "EL-11575_EL-11575.json";
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
  } else if (requestBody.leftWorkItem.id === "EL-156" && requestBody.rightWorkItem.id === "DP-11599") {
    fixtureFile = "EL-156_DP-11599.json";
  }

  if (fixtureFile) {
    const filePath = path.join(__dirname, 'fixtures', fixtureFile);
    const fixtureData = JSON.parse(fs.readFileSync(filePath, 'utf8'));
    return JSON.stringify(fixtureData)
  } else {
    console.log("diff fixture not found: ", requestBody);
    return {}
  }
};

export const workItemsMergeFixtureDataProvider = (requestBody) => {
  let fixtureFile;
  if (!requestBody.pairs[0].leftWorkItem) {
    if (requestBody.pairs[0].rightWorkItem.id === "DP-11570") {
      fixtureFile = "NONE_DP-11570_merge.json";
    }
  } else if (!requestBody.pairs[0].rightWorkItem) {
    if (requestBody.pairs[0].leftWorkItem.id === "EL-156") {
      fixtureFile = "EL-156_NONE_merge.json";
    } else if (requestBody.pairs[0].leftWorkItem.id === "EL-11600") {
      fixtureFile = "EL-11600_NONE_merge.json";
    }
  } else if (requestBody.pairs[0].leftWorkItem.id === "EL-4977" && requestBody.pairs[0].rightWorkItem.id === "DP-11559" && requestBody.direction === 0) {
    fixtureFile = "EL-4977_DP-11559_merge.json";
  }  else if (requestBody.pairs[0].leftWorkItem.id === "EL-4977" && requestBody.pairs[0].rightWorkItem.id === "DP-11559" && requestBody.direction === 1) {
    fixtureFile = "EL-4977_DP-11559_conflicted_merge.json";
  } else if (requestBody.pairs[0].leftWorkItem.id === "EL-11575" && requestBody.pairs[0].rightWorkItem.id === "EL-11575" && requestBody.direction === 0) {
    fixtureFile = "EL-11575_EL-11575_merge_into_referenced.json";
  } else if (requestBody.pairs[0].leftWorkItem.id === "EL-11575" && requestBody.pairs[0].rightWorkItem.id === "EL-11575" && requestBody.direction === 1) {
    fixtureFile = "EL-11575_EL-11575_merge_from_referenced.json";
  } else if (requestBody.pairs[0].leftWorkItem.id === "EL-157" && requestBody.pairs[0].rightWorkItem.id === "DP-11562" && requestBody.direction === 0) {
    fixtureFile = "EL-157_DP-11562_merge.json";
  }

  if (fixtureFile) {
    const filePath = path.join(__dirname, 'fixtures', fixtureFile);
    const fixtureData = JSON.parse(fs.readFileSync(filePath, 'utf8'));
    return JSON.stringify(fixtureData)
  } else {
    console.log("merge fixture not found: ", requestBody);
    return {}
  }
};

// API mocking
export const test = base.extend({
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

export const normalizeHtml = (htmlString) => {
  return htmlString
      .replace(/\s+/g, ' ')
      .replace(/> </g, '><')
      .trim();
};
