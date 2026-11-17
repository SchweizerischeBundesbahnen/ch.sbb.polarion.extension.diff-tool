import { responseError } from '@/services/responseError';
import type { SendRequest } from '@/services/useRemote';

// The PDF export of a diff, shared by the viewer's control pane and the batch export of the Documents topic.
// The PDF is the viewer page as it is rendered: its whole DOM goes to the server, and the print rules of
// globals.css shape the result.

export type PaperSize = 'A4' | 'A3';
export type Orientation = 'landscape' | 'portrait';

export interface PdfOptions {
  paperSize: PaperSize;
  orientation: Orientation;
}

/** Thrown when Polarion answers with its login page instead of the PDF. */
export class SessionExpiredError extends Error {
  constructor() {
    super('Your session has expired. Please refresh the page to log in.');
    this.name = 'SessionExpiredError';
  }
}

/** The widest an image or a table of a diff may be, so it fits into one column of the page. */
export function elementMaxWidth({ paperSize, orientation }: PdfOptions): string {
  if (paperSize === 'A4' && orientation === 'portrait') {
    return '75mm';
  }
  if (paperSize === 'A3' && orientation === 'landscape') {
    return '170mm';
  }
  return '115mm';
}

/** Adds the page-size dependent style to the inner HTML of a page, right before its `</head>`. */
export function preProcess(innerHtml: string, options: PdfOptions): string {
  const style = `
      <style>
        :root {
          --pdf-elem-max-width: ${elementMaxWidth(options)};
        }
      </style>
    `;
  const headClosing = innerHtml.indexOf('</head>');
  const index = headClosing > 0 ? headClosing : 0;
  return innerHtml.substring(0, index) + style + innerHtml.substring(index);
}

/** The HTML sent for conversion: the given page as it is rendered now. */
export function buildPdfHtml(page: Document, options: PdfOptions): string {
  return `<html lang='en'>${preProcess(page.documentElement.innerHTML, options)}</html>`;
}

/** Converts the HTML to PDF on the server. */
export async function convertHtmlToPdf(sendRequest: SendRequest, html: string, options: PdfOptions): Promise<Blob> {
  const response = await sendRequest({
    method: 'POST',
    url: `/conversion/html-to-pdf?orientation=${options.orientation}&paperSize=${options.paperSize}`,
    body: html,
    contentType: 'text/html',
  });
  if (response.headers?.get('x-com-ibm-team-repository-web-auth-msg') === 'authrequired') {
    throw new SessionExpiredError();
  }
  if (!response.ok) {
    throw await responseError(response);
  }
  return response.blob();
}

/** The window which starts a download: the top one where it can be reached, because a browser may block one started in a frame. */
export function downloadWindow(): Window {
  try {
    if (window.top && window.top !== window && window.top.document) {
      return window.top;
    }
  } catch {
    // A top window of another origin cannot be reached: download from this one.
  }
  return window;
}

/** Saves the blob as a file of the given name. */
export function downloadBlob(blob: Blob, fileName: string, target: Window = downloadWindow()): void {
  const objectUrl = URL.createObjectURL(blob);
  const anchor = target.document.createElement('a');
  anchor.href = objectUrl;
  anchor.download = fileName;
  anchor.style.display = 'none';
  target.document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  setTimeout(() => URL.revokeObjectURL(objectUrl), 100);
}
