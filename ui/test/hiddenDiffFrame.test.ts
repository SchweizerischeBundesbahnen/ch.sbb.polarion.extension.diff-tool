import { afterEach, describe, expect, it, vi } from 'vitest';
import { errorMessage, progressMessage, readyMessage } from '../src/embed/messages';
import type { ExportMessage } from '../src/embed/messages';
import { isAbortError, renderDiffInFrame } from '../src/topics/batch/hiddenDiffFrame';

// The frame loads about:blank, which shares this page's origin. The test plays the embedded viewer: it writes
// the page into the frame and posts the export messages from a script running inside the frame, so they carry
// the frame as their source, as the viewer's do.

const PDF_OPTIONS = { paperSize: 'A4', orientation: 'landscape' } as const;

const frames = () => document.querySelectorAll<HTMLIFrameElement>('iframe.batch-diff-frame');

async function loadedFrame(): Promise<HTMLIFrameElement> {
  await vi.waitFor(() => expect(frames()).toHaveLength(1));
  const frame = frames()[0];
  await vi.waitFor(() => expect(frame.contentDocument?.readyState).toBe('complete'));
  return frame;
}

/** Posts the message to the parent from a script of the frame. */
function postFromFrame(frame: HTMLIFrameElement, message: ExportMessage): void {
  const page = frame.contentDocument!;
  const script = page.createElement('script');
  script.textContent = `parent.postMessage(${JSON.stringify(message)}, location.origin === 'null' ? '*' : parent.location.origin);`;
  page.body.appendChild(script);
}

afterEach(() => {
  frames().forEach((frame) => frame.remove());
});

describe('renderDiffInFrame', () => {
  it('lays the viewer out off screen and resolves with its page once it is ready', async () => {
    const onProgress = vi.fn();
    const rendered = renderDiffInFrame('about:blank', PDF_OPTIONS, { onProgress: onProgress });
    const frame = await loadedFrame();

    expect(frame.style.left).toBe('-10000px');
    expect(frame.getAttribute('aria-hidden')).toBe('true');
    frame.contentDocument!.body.innerHTML = '<div id="diff-body">the diff</div>';
    postFromFrame(frame, progressMessage(1, 2));
    postFromFrame(frame, progressMessage(2, 2));
    postFromFrame(frame, readyMessage(true));

    const result = await rendered;
    expect(result.diffsExist).toBe(true);
    expect(result.html).toContain('<div id="diff-body">the diff</div>');
    expect(result.html).toContain('--pdf-elem-max-width: 115mm;');
    expect(onProgress).toHaveBeenNthCalledWith(1, 1, 2);
    expect(onProgress).toHaveBeenNthCalledWith(2, 2, 2);
    expect(frames()).toHaveLength(0);
  });

  it('rejects with the error of the viewer', async () => {
    const rendered = renderDiffInFrame('about:blank', PDF_OPTIONS);
    const frame = await loadedFrame();

    postFromFrame(frame, errorMessage('Document not found'));

    await expect(rendered).rejects.toThrow('Document not found');
    expect(frames()).toHaveLength(0);
  });

  it('ignores messages which do not come from its frame', async () => {
    const rendered = renderDiffInFrame('about:blank', PDF_OPTIONS);
    const frame = await loadedFrame();
    let settled = false;
    rendered.then(
      () => (settled = true),
      () => (settled = true),
    );

    window.postMessage(readyMessage(false), window.location.origin);
    window.postMessage({ type: 'ready', diffsExist: false }, window.location.origin);
    await new Promise((resolve) => setTimeout(resolve, 50));
    expect(settled).toBe(false);

    postFromFrame(frame, readyMessage(false));
    await expect(rendered).resolves.toMatchObject({ diffsExist: false });
  });

  it('stops when asked to, and removes its frame', async () => {
    const controller = new AbortController();
    const rendered = renderDiffInFrame('about:blank', PDF_OPTIONS, { signal: controller.signal });
    await loadedFrame();

    controller.abort();

    const error = await rendered.catch((caught: unknown) => caught);
    expect(isAbortError(error)).toBe(true);
    expect(frames()).toHaveLength(0);
  });

  it('starts nothing once stopped', async () => {
    const controller = new AbortController();
    controller.abort();

    const error = await renderDiffInFrame('about:blank', PDF_OPTIONS, { signal: controller.signal }).catch(
      (caught: unknown) => caught,
    );

    expect(isAbortError(error)).toBe(true);
    expect(frames()).toHaveLength(0);
  });

  it('gives up on a viewer which stays silent', async () => {
    const rendered = renderDiffInFrame('about:blank', PDF_OPTIONS, { idleTimeoutMs: 100 });

    await expect(rendered).rejects.toThrow(/did not respond/);
    expect(frames()).toHaveLength(0);
  });
});
