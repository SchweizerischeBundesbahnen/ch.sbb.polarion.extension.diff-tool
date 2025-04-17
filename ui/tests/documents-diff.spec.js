import { expect } from '@playwright/test';
import { test, normalizeHtml } from './test-utils';

test.describe("diffing of documents work items' WorkItems", () => {

  test.beforeEach(async ({ page, mockApi }) => {
    await mockApi.mockEndpoint({ url: '**/extension/info', fixtureFile: 'version-info.json' });
    await mockApi.mockEndpoint({ url: '**/communication/settings', fixtureFile: 'communication-settings.json' });
    await mockApi.mockEndpoint({ url: '**/settings/diff/names?scope=project/elibrary/', fixtureFile: 'configs.json' });
    await mockApi.mockEndpoint({ url: '**/diff/documents', fixtureFile: 'documents-diff.json' });
    await mockApi.mockWorkItemsDiffEndpoint();

    await page.goto('/documents?sourceProjectId=elibrary&sourceSpaceId=Specification&sourceDocument=Catalog%20Specification&targetProjectId=drivepilot&targetSpaceId=Design&targetDocument=Catalog%20Design&linkRole=relates_to');
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

});
