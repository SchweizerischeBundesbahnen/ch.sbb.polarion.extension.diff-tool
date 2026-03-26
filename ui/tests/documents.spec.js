import { expect } from '@playwright/test';
import { test, normalizeHtml } from './test-utils';

test.describe("page of diffing documents' WorkItems", () => {

  test.beforeEach(async ({ page, mockApi }) => {
    await mockApi.mockEndpoint({ url: '**/extension/info', fixtureFile: 'version-info.json' });
    await mockApi.mockEndpoint({ url: '**/communication/settings', fixtureFile: 'communication-settings.json' });
    await mockApi.mockEndpoint({ url: '**/settings/diff/names?scope=project/elibrary/', fixtureFile: 'configs.json' });
    await mockApi.mockEndpoint({ url: '**/diff/documents', fixtureFile: 'documents-diff.json' });
    await mockApi.mockWorkItemsDiffEndpoint();

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

    await page.waitForSelector('.header .merge-pane', { state: 'visible' }); // Wait until merge pane is visible before next steps

    const selectAllLabel = page.locator('.header .merge-pane label.select-all');
    await expect(selectAllLabel).toBeVisible();
    await expect(selectAllLabel).toHaveText("select all");

    const selectAllCheckbox = page.locator('.header .merge-pane label.select-all input[type="checkbox"]');
    await expect(selectAllCheckbox).toBeVisible();
    await expect(selectAllCheckbox).toBeChecked({ checked: false });

    const mergeButton = page.locator('.header .merge-pane .merge-button .btn');
    await expect(mergeButton).toBeVisible();
    await expect(mergeButton).toBeDisabled();
  });

  test('control pane', async ({ page }) => {
    expect(await page.locator('.control-pane.expanded').count()).toEqual(0);

    await expect(page.locator(".progress span")).toHaveText("Data loaded successfully", { timeout: 10_000 });

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

    await expect(page.locator(".control-pane .controls label[for='preserve-comments']")).toHaveText("Preserve comments");
    await expect(page.locator("#preserve-comments")).toBeChecked({checked: false});

    await expect(page.locator(".control-pane .controls label[for='paper-size']")).toHaveText("Paper size:");
    await expect(page.locator("#paper-size")).toHaveValue("A4");

    await expect(page.locator(".control-pane .controls label[for='orientation']")).toHaveText("Orientation:");
    await expect(page.locator("#orientation")).toHaveValue("landscape");

  });

  test('export to PDF', async ({ page }) => {
    expect(await page.locator('.control-pane.expanded').count()).toEqual(0);

    await page.waitForSelector('.header .merge-pane', { state: 'visible' }); // Wait until merge pane is visible before next steps

    const expandButton = page.locator('.control-pane .expand-button');
    await expect(expandButton).toBeVisible({visible: true});
    await expandButton.click();
    expect(await page.locator('.control-pane.expanded').count()).toEqual(1);

    await page.locator("#paper-size").selectOption("A3");
    await page.locator("#orientation").selectOption("portrait");

    const exportRequestPromise = page.waitForRequest(request => {
      return request.url().includes('/conversion/html-to-pdf');
    });

    const exportButton = page.getByTestId("export-button");
    await expect(exportButton).toBeVisible();
    await expect(exportButton).toBeEnabled();
    exportButton.dispatchEvent("click"); // We dispatch low level click-event here, instead of invoking Playwright's click() method to bypass button's "instability" resulted by its animation

    const exportRequest = await exportRequestPromise;
    expect(exportRequest.method()).toBe('POST');
    expect(exportRequest.url()).toContain('/conversion/html-to-pdf?orientation=portrait&paperSize=A3');
  });

  test('expand/collapse', async ({ page }) => {
    await page.waitForSelector('.header .merge-pane', { state: 'visible' }); // Wait until merge pane is visible before next steps

    const EL4977_DP11559 = page.getByTestId('EL-4977_DP-11559');

    const content = EL4977_DP11559.locator(".content");
    await expect(content).toBeVisible();

    const header = EL4977_DP11559.locator(".header");
    await expect(header).toBeVisible();

    const collapseButton = header.locator(".floating-button");
    let collapseButtonIcon = collapseButton.locator("svg");
    let collapseButtonIconHtml = await collapseButtonIcon.innerHTML();
    expect(normalizeHtml(collapseButtonIconHtml)).toBe(normalizeHtml(`
      <path fill=\"currentColor\" d=\"M201.4 406.6c12.5 12.5 32.8 12.5 45.3 0l192-192c12.5-12.5 12.5-32.8 0-45.3s-32.8-12.5-45.3 0L224 338.7 54.6 169.4c-12.5-12.5-32.8-12.5-45.3 0s-12.5 32.8 0 45.3l192 192z\"></path>
    `));

    await collapseButton.click();

    collapseButtonIcon = collapseButton.locator("svg");
    collapseButtonIconHtml = await collapseButtonIcon.innerHTML();
    expect(normalizeHtml(collapseButtonIconHtml)).toBe(normalizeHtml(`
      <path fill=\"currentColor\" d=\"M201.4 105.4c12.5-12.5 32.8-12.5 45.3 0l192 192c12.5 12.5 12.5 32.8 0 45.3s-32.8 12.5-45.3 0L224 173.3 54.6 342.6c-12.5 12.5-32.8 12.5-45.3 0s-12.5-32.8 0-45.3l192-192z\"></path>
    `));

    await expect(content).toBeVisible({ visible: false });
  });

  test('select all', async ({ page }) => {
    await page.waitForSelector('.header .merge-pane', { state: 'visible' }); // Wait until merge pane is visible before next steps

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

    // Check that merge button is enabled
    const mergeButton = page.locator('.header .merge-pane .merge-button .btn');
    await expect(mergeButton).toBeVisible();
    await expect(mergeButton).toBeEnabled();

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
    await page.waitForSelector('.header .merge-pane', { state: 'visible' }); // Wait until merge pane is visible before next steps

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

    // Check that merge button is enabled
    const mergeButton = page.locator('.header .merge-pane .merge-button .btn');
    await expect(mergeButton).toBeVisible();
    await expect(mergeButton).toBeEnabled();
  });

  test('swap documents', async ({ page, mockApi }) => {
    await page.waitForSelector('.header .merge-pane', { state: 'visible' }); // Wait until merge pane is visible before next steps

    // Verify initial state: elibrary on the left, drivepilot on the right
    await expect(page.getByTestId('LEFT-project').locator(".path-value")).toHaveText("E-Library");
    await expect(page.getByTestId('RIGHT-project').locator(".path-value")).toHaveText("Drive Pilot");
    await expect(page.getByTestId('LEFT-doc-title')).toContainText("Catalog Specification");
    await expect(page.getByTestId('RIGHT-doc-title')).toContainText("Catalog Design");

    // Mock endpoints needed after swap (drivepilot becomes source) — must be registered after initial state is verified
    await mockApi.mockEndpoint({ url: '**/settings/diff/names?scope=project/drivepilot/', fixtureFile: 'configs.json' });
    await mockApi.mockEndpoint({ url: '**/diff/documents', fixtureFile: 'reversed-documents-diff.json' });

    // Click swap button and wait for URL to change
    const swapButton = page.locator('.swap-button');
    await expect(swapButton).toBeVisible();
    await swapButton.click();

    // Wait for client-side navigation to complete
    await expect(page).toHaveURL(/sourceProjectId=drivepilot/);

    // Wait for the diff data to reload after swap
    await expect(page.getByTestId('LEFT-project').locator(".path-value")).toHaveText("Drive Pilot");
    await expect(page.getByTestId('RIGHT-project').locator(".path-value")).toHaveText("E-Library");
    await expect(page.getByTestId('LEFT-doc-title')).toContainText("Catalog Design");
    await expect(page.getByTestId('RIGHT-doc-title')).toContainText("Catalog Specification");

    // Verify URL params are swapped
    const url = new URL(page.url());
    expect(url.searchParams.get('sourceProjectId')).toBe('drivepilot');
    expect(url.searchParams.get('sourceSpaceId')).toBe('Design');
    expect(url.searchParams.get('sourceDocument')).toBe('Catalog Design');
    expect(url.searchParams.get('targetProjectId')).toBe('elibrary');
    expect(url.searchParams.get('targetSpaceId')).toBe('Specification');
    expect(url.searchParams.get('targetDocument')).toBe('Catalog Specification');
  });

  test('select single field', async ({ page }) => {
    await page.waitForSelector('.header .merge-pane', { state: 'visible' }); // Wait until merge pane is visible before next steps

    const expandButton = page.locator('.control-pane .expand-button');
    await expect(expandButton).toBeVisible({visible: true});
    await expandButton.click();
    expect(await page.locator('.control-pane.expanded').count()).toEqual(1);

    const perFieldMergeCheckbox = page.getByTestId('per-field-merge-selection');
    await perFieldMergeCheckbox.isVisible();
    await perFieldMergeCheckbox.click();

    const selectAllCheckbox = page.locator('.header .merge-pane label.select-all input[type="checkbox"]');
    await expect(selectAllCheckbox).toBeVisible();
    await expect(selectAllCheckbox).toBeChecked({ checked: false });

    let checkedFieldsCheckboxes = page.locator('.wi-diff .content .diff-wrapper .merge-ticker input[type="checkbox"]:checked');
    let checkedFieldsCheckboxesCount = await checkedFieldsCheckboxes.count();
    expect(checkedFieldsCheckboxesCount).toBe(0);

    const firstSelectionCheckbox = page.locator('.wi-diff .merge-ticker input[type="checkbox"]').nth(0);
    await firstSelectionCheckbox.isVisible();
    await firstSelectionCheckbox.click();

    // Check that "select all" is still unchecked
    await expect(selectAllCheckbox).toBeChecked({ checked: false });

    // Check that fields checkboxes of that pair are also checked
    checkedFieldsCheckboxes = page.locator('.wi-diff .content .diff-wrapper .merge-ticker input[type="checkbox"]:checked');
    checkedFieldsCheckboxesCount = await checkedFieldsCheckboxes.count();
    expect(checkedFieldsCheckboxesCount).toBe(2);

    // Then uncheck one of fields checkboxes
    const firstCheckedFieldsCheckbox = checkedFieldsCheckboxes.nth(0);
    await firstCheckedFieldsCheckbox.isVisible();
    await firstCheckedFieldsCheckbox.click();

    // Pair selection checkbox should still be checked
    await expect(firstSelectionCheckbox).toBeChecked({ checked: true });

    // And only on of fields checkboxes to stay checked
    checkedFieldsCheckboxes = page.locator('.wi-diff .content .diff-wrapper .merge-ticker input[type="checkbox"]:checked');
    checkedFieldsCheckboxesCount = await checkedFieldsCheckboxes.count();
    expect(checkedFieldsCheckboxesCount).toBe(1);

    const selectedPairs = page.locator('.wi-diff.selected');
    const selectedPairsCount = await selectedPairs.count();
    expect(selectedPairsCount).toBe(1);
    const pairHeaders = selectedPairs.nth(0).locator(".wi-header");
    for (let j = 0; j < await pairHeaders.count(); j++) {
      await expect(pairHeaders.nth(j)).toBeVisible();
      await expect(pairHeaders.nth(j)).toHaveCSS("background-color", "rgba(150, 190, 250, 0.2)");
    }

    // Check that merge button is enabled
    const mergeButton = page.locator('.header .merge-pane .merge-button .btn');
    await expect(mergeButton).toBeVisible();
    await expect(mergeButton).toBeEnabled();
  });

});
