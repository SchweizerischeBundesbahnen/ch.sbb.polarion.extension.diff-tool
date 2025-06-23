import { test as base } from '@playwright/test';
import path from 'path';
import fs from 'fs';

const fixtureDataProvider = (fixtureFile) => {
  const filePath = path.join(__dirname, 'fixtures', fixtureFile);
  const fixtureData = JSON.parse(fs.readFileSync(filePath, 'utf8'));
  return JSON.stringify(fixtureData)
};

const workItemsDiffFixtureDataProvider = (requestBody) => {

  const left = requestBody.leftWorkItem ? requestBody.leftWorkItem.id : 'NONE';
  const right = requestBody.rightWorkItem ? requestBody.rightWorkItem.id : 'NONE';
  const fixtureFile = `${left}_${right}.json`;

  const filePath = path.join(__dirname, 'fixtures', fixtureFile);
  if (fs.existsSync(filePath)) {
    const fixtureData = JSON.parse(fs.readFileSync(filePath, 'utf8'));
    return JSON.stringify(fixtureData)
  } else {
    console.log(`fixture file not found: ${fixtureFile}`);
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
  } else if (requestBody.pairs[0].leftWorkItem.id === "EL-14873" && requestBody.pairs[0].rightWorkItem.id === "DP-536") {
    fixtureFile = "EL-14873_DP-536.json";
  }


  if (fixtureFile) {
    const filePath = path.join(__dirname, 'fixtures', fixtureFile);
    const fixtureData = JSON.parse(fs.readFileSync(filePath, 'utf8'));
    return JSON.stringify(fixtureData)
  } else {
    console.log("workitems merge fixture not found: ", requestBody);
    return {}
  }
};

const fieldsMergeFixtureDataProvider = (requestBody) => {
  let fixtureFile;
  if (requestBody.fieldIds.includes("version")) {
    fixtureFile = "version-field-merge.json";
  } else if (requestBody.fieldIds.includes("docRevision")) {
    fixtureFile = "docRevision-field-merge.json";
  } else if (requestBody.fieldIds.includes("docLanguage")) {
    fixtureFile = "docLanguage-field-merge.json";
  }

  if (fixtureFile) {
    const filePath = path.join(__dirname, 'fixtures', fixtureFile);
    const fixtureData = JSON.parse(fs.readFileSync(filePath, 'utf8'));
    return JSON.stringify(fixtureData)
  } else {
    console.log("field merge fixture not found: ", requestBody);
    return {}
  }
};

const contentMergeFixtureDataProvider = (requestBody) => {
  let fixtureFile;
  if (requestBody.pairs[0].leftWorkItemId === "EL-63012") {
    fixtureFile = "content-merge.json";
  } else if (requestBody.pairs[0].leftWorkItemId === "EL-63013") {
    fixtureFile = "content-merge-not-authorized.json";
  } else if (requestBody.pairs[0].leftWorkItemId === "EL-63016") {
    fixtureFile = "content-merge-aborted.json";
  }

  if (fixtureFile) {
    const filePath = path.join(__dirname, 'fixtures', fixtureFile);
    const fixtureData = JSON.parse(fs.readFileSync(filePath, 'utf8'));
    return JSON.stringify(fixtureData)
  } else {
    console.log("field merge fixture not found: ", requestBody);
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
      // Mock "/diff/documents" endpoint which emulates merge operation and returns JSON response depending on data in request body
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
      },

      // Mock "/diff/documents-fields" endpoint which emulates merge operation and returns JSON response depending on data in request body
      mockFieldsMergeEndpoint: async () => {
        await page.route("**/merge/documents-fields", route => {
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
            body: fieldsMergeFixtureDataProvider(requestBody)
          });
        });
      },

      // Mock "/diff/documents-content" endpoint which emulates merge operation and returns JSON response depending on data in request body
      mockContentMergeEndpoint: async () => {
        await page.route("**/merge/documents-content", route => {
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
            body: contentMergeFixtureDataProvider(requestBody)
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
      .replace(/changes="([^"]*)"/g, (match) => match.replace(/&gt;/g, '>').replace(/&lt;/g, '<')) // replace encoded diff HTML inside 'changes' attribute
      .replace(/\s+/g, ' ')
      .replace(/> </g, '><')
      .trim();
};
