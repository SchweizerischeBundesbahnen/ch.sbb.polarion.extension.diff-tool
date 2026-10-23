import { expect } from '@playwright/test';
import { test, pickOption } from './test-utils';

// The unified view end to end: the switch in the configuration pane, a merge started from it, and the
// links between the two positions of a moved work item.

test.describe("unified view of diffing documents' WorkItems", () => {

  test.beforeEach(async ({ page, mockApi }) => {
    await mockApi.mockEndpoint({ url: '**/extension/info', fixtureFile: 'version-info.json' });
    await mockApi.mockEndpoint({ url: '**/communication/settings', fixtureFile: 'communication-settings.json' });
    await mockApi.mockEndpoint({ url: '**/settings/diff/names?scope=project/elibrary/', fixtureFile: 'configs.json' });
    await mockApi.mockEndpoint({ url: '**/diff/documents', fixtureFile: 'documents-diff.json' });
    await mockApi.mockWorkItemsDiffEndpoint();
    await mockApi.mockWorkItemsMergeEndpoint();

    await page.goto('/documents?sourceProjectId=elibrary&sourceSpaceId=Specification&sourceDocument=Catalog%20Specification&targetProjectId=drivepilot&targetSpaceId=Design&targetDocument=Catalog%20Design&linkRole=relates_to');
    await page.waitForSelector('.header .merge-pane', { state: 'visible' }); // Wait until merge pane is visible before next steps
  });

  const switchView = async (page, label) => {
    await page.locator('.control-pane .expand-button').click();
    await expect(page.locator('.control-pane.expanded')).toHaveCount(1);
    await pickOption(page, 'diff-view', label);
    await page.locator('.control-pane .expand-button').click();
    await expect(page.locator('.control-pane.expanded')).toHaveCount(0);
  };

  const mergeSelected = async (page) => {
    const requestPromise = page.waitForRequest(request => request.url().includes('/merge/documents'));
    const mergeButton = page.locator('.header .merge-pane .merge-button .btn');
    await expect(mergeButton).toBeEnabled();
    await mergeButton.click();
    await expect(page.getByTestId('merge-confirmation-modal')).toBeVisible();
    await page.getByTestId('merge-confirmation-modal-action-button').click();
    return JSON.parse((await requestPromise).postData() || '{}');
  };

  test('switches to unified and back from the configuration pane', async ({ page }) => {
    const pair = page.getByTestId('EL-4977_DP-11559');
    await expect(pair.getByTestId('Description').locator('.diff-leaf')).toHaveCount(2);

    await switchView(page, 'Unified');

    await expect(page.locator('#diff-body')).toHaveClass(/diff-view-unified/);
    await expect(pair.getByTestId('Description').locator('.diff-leaf.unified')).toHaveCount(1);
    await expect(pair.getByTestId('Description').locator('.diff-leaf.left, .diff-leaf.right')).toHaveCount(0);
    await expect(pair.locator('.header .wi-header')).toHaveCount(1);
    await expect(pair.getByLabel('Source WorkItem EL-4977')).toBeVisible();
    await expect(pair.getByLabel('Target WorkItem DP-11559')).toBeVisible();

    await switchView(page, 'Side by side');

    await expect(page.locator('#diff-body')).toHaveClass(/diff-view-side-by-side/);
    await expect(pair.getByTestId('Description').locator('.diff-leaf')).toHaveCount(2);
    await expect(pair.locator('.header .wi-header')).toHaveCount(2);
  });

  test('keeps the unified view in the URL, so a reload keeps it', async ({ page }) => {
    await switchView(page, 'Unified');
    await expect(page).toHaveURL(/diffView=unified/);

    // A merge with structural changes reloads the page: the same happens here.
    await page.reload();
    await page.waitForSelector('.header .merge-pane', { state: 'visible' });

    await expect(page.locator('#diff-body')).toHaveClass(/diff-view-unified/);
    await expect(page.getByTestId('EL-4977_DP-11559').getByTestId('Description').locator('.diff-leaf.unified')).toHaveCount(1);

    await switchView(page, 'Side by side');
    await expect(page).not.toHaveURL(/diffView/);
  });

  test('merges from the unified view what it merges side by side', async ({ page }) => {
    await switchView(page, 'Unified');

    await page.getByTestId('EL-4977_DP-11559').locator('.header .merge-ticker input[type="checkbox"]').click();
    await expect(page.getByTestId('selection-count')).toHaveText('1 selected');

    const postData = await mergeSelected(page);

    expect(postData.mergeDirection).toBe(0);
    expect(postData.pairs).toHaveLength(1);
    expect(postData.pairs[0].leftWorkItem.id).toBe('EL-4977');
    expect(postData.pairs[0].rightWorkItem.id).toBe('DP-11559');
    await expect(page.getByTestId('merge-result-modal')).toBeVisible();
  });

  test('merges a moved WorkItem once, selected at its source position', async ({ page }) => {
    await switchView(page, 'Unified');

    await page.getByTestId('EL-157_DP-11562_direct').locator('.header .merge-ticker input[type="checkbox"]').click();
    await expect(page.getByTestId('EL-157_DP-11562_reverse')).toHaveClass(/selected/);
    await expect(page.getByTestId('EL-157_DP-11562_direct')).toHaveClass(/selected/);
    await expect(page.getByTestId('selection-count')).toHaveText('1 selected');

    const postData = await mergeSelected(page);

    const moved = postData.pairs.filter(pair => pair.leftWorkItem?.id === 'EL-157');
    expect(moved).toHaveLength(1);
    expect(moved[0].rightWorkItem.id).toBe('DP-11562');
  });

  test('links both positions of a moved WorkItem', async ({ page }) => {
    await switchView(page, 'Unified');

    const target = page.getByTestId('EL-158_DP-11563_reverse');
    const source = page.getByTestId('EL-158_DP-11563_direct');
    await expect(target.locator('.wi-header')).toHaveClass(/moved-placeholder/);

    await source.locator('a.wi-tag.moved', { hasText: 'Moved to 2.4' }).click();
    await expect(page).toHaveURL(/#right2\.4$/);
    await expect(target.locator('.wi-header')).toBeInViewport();

    await target.locator('a.wi-tag.moved', { hasText: 'Moved from 2.3' }).click();
    await expect(page).toHaveURL(/#left2\.3$/);
    await expect(source.locator('.wi-header')).toBeInViewport();
  });
});
