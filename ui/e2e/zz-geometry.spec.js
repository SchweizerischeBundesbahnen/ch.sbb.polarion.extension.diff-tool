import { expect } from '@playwright/test';
import { test } from './test-utils';

// TEMPORARY: prints the geometry the runner actually renders, to settle why the expanded control pane
// intercepts the select-all click there but not in the pinned Playwright image locally. Remove before merge.
test.describe('geometry probe', () => {
  test.beforeEach(async ({ page, mockApi }) => {
    await mockApi.mockEndpoint({ url: '**/extension/info', fixtureFile: 'version-info.json' });
    await mockApi.mockEndpoint({ url: '**/communication/settings', fixtureFile: 'communication-settings.json' });
    await mockApi.mockEndpoint({ url: '**/settings/diff/names?scope=project/elibrary/', fixtureFile: 'configs.json' });
    await mockApi.mockEndpoint({ url: '**/diff/documents', fixtureFile: 'documents-diff.json' });
    await mockApi.mockWorkItemsDiffEndpoint();
    await page.goto('/documents?sourceProjectId=elibrary&sourceSpaceId=Testing&sourceDocument=Test%20Specification&targetProjectId=elibrary&targetSpaceId=Design&targetDocument=Catalog%20Design&linkRole=relates_to&config=Default');
  });

  test('probe', async ({ page }) => {
    await page.waitForSelector('.header .merge-pane', { state: 'visible' });
    await page.locator('.control-pane .expand-button').click();
    await page.waitForTimeout(800);
    const data = await page.evaluate(() => {
      const pane = document.querySelector('.control-pane.expanded');
      const input = document.querySelector('.header .merge-pane label.select-all input');
      const body = document.querySelector('#diff-body');
      const r = (el) => (el ? el.getBoundingClientRect() : null);
      const cs = (el, prop) => (el ? getComputedStyle(el)[prop] : null);
      return {
        viewport: `${window.innerWidth}x${window.innerHeight}`,
        paneBox: r(pane) && `x=${r(pane).x} w=${r(pane).width}`,
        paneWidthCss: cs(pane, 'width'),
        paneVar: getComputedStyle(document.documentElement).getPropertyValue('--control-pane-expanded-width'),
        bodyPadding: cs(body, 'paddingLeft'),
        bodyClass: body && body.className,
        inputBox: r(input) && `x=${r(input).x} w=${r(input).width}`,
        topAtCheckbox: (() => {
          const b = r(input);
          if (!b) return null;
          const el = document.elementFromPoint(b.x + b.width / 2, b.y + b.height / 2);
          return el ? `${el.tagName}.${el.className}` : null;
        })(),
      };
    });
    console.log('GEOMETRY ' + JSON.stringify(data));
    expect(true).toBe(true);
  });
});
