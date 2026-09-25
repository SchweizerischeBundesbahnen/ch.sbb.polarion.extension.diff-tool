import { pageViolations } from '@sbb-polarion/react-sbb-polarion/testing';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup } from 'vitest-browser-react';
import CollectionsPage from '../src/pages/CollectionsPage';
import {
  SERVER_RENDERED,
  fixture,
  openControlPane,
  openDialog,
  pairDiff,
  renderViewer,
  restoreUrl,
  shoot,
} from './viewerHarness';

// Docker-only snapshots of the collections diff viewer, the collections.html entry.
//
// It pairs the documents of two collections and then hands the chosen pair to the documents viewer, so
// which of its two faces you see depends on whether that pair has a counterpart. Both are covered here,
// each from the sample data e2e/ already drives that face with:
//
//   collection-merge.json + documents-from-collection-diff.json  -> a paired document, diffed
//   collections.json                                             -> no counterpart, offering to create one
//
// Unlike the other two viewers this one renders no progress bar, which is why shoot() guards that wait
// rather than making it unconditional.

const PAIRED_URL =
  '/collections?sourceProjectId=elibrary&sourceCollectionId=1&targetProjectId=drivepilot&targetCollectionId=1' +
  '&linkRole=relates_to&config=Default&compareAs=Workitems&sourceSpaceId=Specification' +
  '&sourceDocument=Administration+Specification&targetSpaceId=Specification&targetDocument=Administration+Specification';

const UNPAIRED_URL =
  '/collections?sourceProjectId=elibrary&sourceCollectionId=1&targetProjectId=Project2&targetCollectionId=1' +
  '&linkRole=relates_to&config=Default&compareAs=Workitems&sourceSpaceId=Testing&sourceDocument=Test+Specification' +
  '&targetSpaceId=Testing&targetDocument=Test+Specification';

function renderCollections(url: string, collections: unknown, documents: unknown) {
  renderViewer(
    url,
    [
      { method: 'POST', match: /\/diff\/collections$/, json: collections },
      { method: 'POST', match: /\/diff\/documents$/, json: documents },
      { method: 'POST', match: /\/diff\/document-workitems$/, respond: pairDiff },
      { method: 'POST', match: /\/duplicate$/, json: fixture('duplicate.json') ?? {} },
    ],
    <CollectionsPage />,
  );
}

function renderPaired() {
  renderCollections(PAIRED_URL, fixture('collection-merge.json'), fixture('documents-from-collection-diff.json'));
}

async function headerLoaded() {
  await vi.waitFor(() => expect(document.querySelector('[data-testid="LEFT-collection"]')).not.toBeNull(), {
    timeout: 10000,
  });
}

/** The pair rows only render once their own diff has arrived, one request per pair. */
async function pairsLoaded() {
  await vi.waitFor(() => expect(document.querySelectorAll('.wi-diff .content').length).toBeGreaterThan(0), {
    timeout: 10000,
  });
}

async function createOffered() {
  await vi.waitFor(() => expect(document.querySelector('[data-testid="create-document-button"]')).not.toBeNull());
}

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  restoreUrl();
});

describe.skipIf(!__PIXEL_REFERENCES__)('Collections diff viewer visual', () => {
  it('a paired document, handed to the documents viewer', async () => {
    renderPaired();
    await headerLoaded();
    await pairsLoaded();
    await shoot('collections-diff');
  });

  it('configuration pane expanded, over the diff', async () => {
    renderPaired();
    await headerLoaded();
    await pairsLoaded();
    await openControlPane();
    await shoot('collections-diff-control-pane');
  });

  it('a document with no counterpart, offering to create one', async () => {
    renderCollections(UNPAIRED_URL, fixture('collections.json'), fixture('documents-from-collection.json'));
    await headerLoaded();
    await createOffered();
    await shoot('collections-diff-no-counterpart');
  });

  it('the document configuration dialog, raised over that offer', async () => {
    renderCollections(UNPAIRED_URL, fixture('collections.json'), fixture('documents-from-collection.json'));
    await headerLoaded();
    await createOffered();
    document.querySelector<HTMLButtonElement>('[data-testid="create-document-button"]')!.click();

    await openDialog('Choose document configuration');
    await shoot('collections-diff-target-configuration');
  });
});

// Not Docker-only, unlike the visual suite above: an accessibility scan compares no pixels.
describe('Collections diff viewer, document configuration dialog', () => {
  const escape = (target: Element) =>
    target.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true, cancelable: true }));
  const modalShown = () =>
    getComputedStyle(document.querySelector<HTMLElement>('[data-testid="target-configuration-modal"]')!).display !==
    'none';

  // The dialog handles Escape from inside itself, so it has to take the focus from the button that opened it,
  // and it gives the focus back on close, so the next Tab continues from that button.
  it('takes the focus when it opens, so a first Escape closes it, and gives it back to its opener', async () => {
    renderCollections(UNPAIRED_URL, fixture('collections.json'), fixture('documents-from-collection.json'));
    await headerLoaded();
    await createOffered();
    const opener = document.querySelector<HTMLButtonElement>('[data-testid="create-document-button"]')!;
    opener.focus();
    opener.click();
    await openDialog('Choose document configuration');
    const modal = document.querySelector('[data-testid="target-configuration-modal"]')!;
    await vi.waitFor(() => expect(modal.contains(document.activeElement)).toBe(true));

    escape(document.activeElement!);
    await vi.waitFor(() => expect(modalShown()).toBe(false));
    expect(document.activeElement).toBe(opener);
  });

  // The dropdown consumes the Escape that closes its list (preventDefault) but lets it bubble, so the dialog
  // must skip a handled Escape: otherwise one key press closes the list and the dialog together.
  it('closes an open configuration list on the first Escape and the dialog only on the second', async () => {
    renderCollections(UNPAIRED_URL, fixture('collections.json'), fixture('documents-from-collection.json'));
    await headerLoaded();
    await createOffered();
    document.querySelector<HTMLButtonElement>('[data-testid="create-document-button"]')!.click();
    await openDialog('Choose document configuration');
    const trigger = await vi.waitFor(() => {
      const found = document.querySelector<HTMLElement>('#target-configuration + .searchable-dropdown .sd-trigger');
      expect(found).not.toBeNull();
      return found!;
    });
    trigger.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true }));
    await vi.waitFor(() => expect(trigger.closest('.searchable-dropdown')!.classList.contains('open')).toBe(true));

    escape(trigger);
    await vi.waitFor(() => expect(trigger.closest('.searchable-dropdown')!.classList.contains('open')).toBe(false));
    expect(modalShown()).toBe(true);

    escape(trigger);
    await vi.waitFor(() => expect(modalShown()).toBe(false));
  });
});

describe('Collections diff viewer, accessibility', () => {
  it('has no WCAG A/AA violations with a paired document shown', async () => {
    renderPaired();
    await headerLoaded();
    await pairsLoaded();
    expect(await pageViolations({ exclude: SERVER_RENDERED })).toEqual([]);
  });

  it('has no WCAG A/AA violations while offering to create a missing counterpart', async () => {
    renderCollections(UNPAIRED_URL, fixture('collections.json'), fixture('documents-from-collection.json'));
    await headerLoaded();
    await createOffered();
    expect(await pageViolations({ exclude: SERVER_RENDERED })).toEqual([]);
  });

  it('has no WCAG A/AA violations with the document configuration dialog open', async () => {
    renderCollections(UNPAIRED_URL, fixture('collections.json'), fixture('documents-from-collection.json'));
    await headerLoaded();
    await createOffered();
    document.querySelector<HTMLButtonElement>('[data-testid="create-document-button"]')!.click();
    await openDialog('Choose document configuration');
    expect(await pageViolations({ exclude: SERVER_RENDERED })).toEqual([]);
  });
});
