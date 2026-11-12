import { describe, expect, it } from 'vitest';

// The PDF export renders the page with WeasyPrint, which cannot continue a flex container on the next page.
// A flex container which starts on one page and does not fit there loses its rest, and everything after it.
// So no container which holds the diff of a work item or a field may be a flex container in print.

const printDisplay = (selector: string): string | undefined =>
  [...document.styleSheets]
    .flatMap((sheet) => [...sheet.cssRules])
    .filter((rule): rule is CSSMediaRule => rule instanceof CSSMediaRule && rule.media.mediaText === 'print')
    .flatMap((media) => [...media.cssRules])
    .filter((rule): rule is CSSStyleRule => rule instanceof CSSStyleRule)
    .filter((rule) =>
      rule.selectorText
        .split(',')
        .map((s) => s.trim())
        .includes(selector),
    )
    .map((rule) => rule.style.display)
    .findLast(Boolean);

describe('Print styles of the PDF export', () => {
  it('lays out the fields of a work item as blocks, which can continue on the next page', () => {
    expect(printDisplay('.wi-diff > .content.row')).toBe('block');
  });

  it('lays out a field compared as "Fields" as a block, which can continue on the next page', () => {
    expect(printDisplay('.wi-diff.row')).toBe('block');
  });
});
