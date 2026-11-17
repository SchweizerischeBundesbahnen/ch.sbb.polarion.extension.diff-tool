import { isExportMessage } from '../../embed/messages';
import { buildPdfHtml } from '../../services/pdfExport';
import type { PdfOptions } from '../../services/pdfExport';

// Renders the diff of one document pair in a hidden frame: the documents viewer itself, embedded for a PDF
// export (embed=pdf-export), so the PDF is exactly what the viewer shows. The frame reports its loading with
// postMessage; once it is ready, its page is taken as the HTML of the PDF and the frame is removed.

/** How long a frame may stay silent before its diff is given up. A pair reports progress per work item. */
export const FRAME_IDLE_TIMEOUT_MS = 10 * 60 * 1000;

/** The width the viewer is laid out at, the width of a usual browser window. */
const FRAME_WIDTH = 1400;
const FRAME_HEIGHT = 900;

export interface RenderedDiff {
  html: string;
  diffsExist: boolean;
}

export interface RenderOptions {
  onProgress?: (loaded: number, total: number) => void;
  signal?: AbortSignal;
  idleTimeoutMs?: number;
  /** Where the frame is attached. */
  container?: HTMLElement;
}

/** The rejection of a render which was stopped. */
export function abortError(): DOMException {
  return new DOMException('The export was stopped', 'AbortError');
}

export function isAbortError(error: unknown): boolean {
  return error instanceof DOMException && error.name === 'AbortError';
}

function hiddenFrame(url: string): HTMLIFrameElement {
  const frame = document.createElement('iframe');
  frame.className = 'batch-diff-frame';
  frame.title = 'Diff being exported to PDF';
  frame.setAttribute('aria-hidden', 'true');
  frame.tabIndex = -1;
  // Off screen rather than display: none, so the viewer is laid out and renders as it does for a user
  Object.assign(frame.style, {
    position: 'fixed',
    left: '-10000px',
    top: '0',
    width: `${FRAME_WIDTH}px`,
    height: `${FRAME_HEIGHT}px`,
    border: '0',
    visibility: 'hidden',
  });
  frame.src = url;
  return frame;
}

/** Renders the diff of the viewer URL in a hidden frame and resolves with the HTML of its PDF. */
export function renderDiffInFrame(
  url: string,
  pdfOptions: PdfOptions,
  options: RenderOptions = {},
): Promise<RenderedDiff> {
  const { onProgress, signal, idleTimeoutMs = FRAME_IDLE_TIMEOUT_MS, container = document.body } = options;

  return new Promise<RenderedDiff>((resolve, reject) => {
    if (signal?.aborted) {
      reject(abortError());
      return;
    }

    const frame = hiddenFrame(url);
    let idleTimer: ReturnType<typeof setTimeout> | undefined;

    const finish = (error: unknown, result?: RenderedDiff) => {
      window.removeEventListener('message', onMessage);
      signal?.removeEventListener('abort', onAbort);
      clearTimeout(idleTimer);
      frame.remove();
      if (result) {
        resolve(result);
      } else {
        reject(error);
      }
    };

    const restartIdleTimer = () => {
      clearTimeout(idleTimer);
      idleTimer = setTimeout(
        () => finish(new Error(`The diff did not respond for ${Math.round(idleTimeoutMs / 60000)} minutes`)),
        idleTimeoutMs,
      );
    };

    function onAbort() {
      finish(abortError());
    }

    function onMessage(event: MessageEvent) {
      // Only the frame of this render, of this origin, and only its export messages
      if (
        event.source !== frame.contentWindow ||
        event.origin !== window.location.origin ||
        !isExportMessage(event.data)
      ) {
        return;
      }
      const message = event.data;
      restartIdleTimer();
      if (message.type === 'progress') {
        onProgress?.(message.loaded, message.total);
      } else if (message.type === 'error') {
        finish(new Error(message.message));
      } else {
        try {
          const page = frame.contentDocument;
          if (!page) {
            throw new Error('The rendered diff cannot be read');
          }
          finish(null, { html: buildPdfHtml(page, pdfOptions), diffsExist: message.diffsExist });
        } catch (error) {
          finish(error);
        }
      }
    }

    window.addEventListener('message', onMessage);
    signal?.addEventListener('abort', onAbort, { once: true });
    restartIdleTimer();
    container.appendChild(frame);
  });
}
