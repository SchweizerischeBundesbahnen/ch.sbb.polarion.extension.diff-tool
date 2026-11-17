import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  SessionExpiredError,
  buildPdfHtml,
  convertHtmlToPdf,
  downloadBlob,
  downloadWindow,
  elementMaxWidth,
  preProcess,
} from '../src/services/pdfExport';
import type { SendRequest } from '../src/services/useRemote';

afterEach(() => {
  vi.restoreAllMocks();
});

describe('elementMaxWidth', () => {
  it('is narrowest on A4 portrait and widest on A3 landscape', () => {
    expect(elementMaxWidth({ paperSize: 'A4', orientation: 'portrait' })).toBe('75mm');
    expect(elementMaxWidth({ paperSize: 'A3', orientation: 'landscape' })).toBe('170mm');
    expect(elementMaxWidth({ paperSize: 'A4', orientation: 'landscape' })).toBe('115mm');
    expect(elementMaxWidth({ paperSize: 'A3', orientation: 'portrait' })).toBe('115mm');
  });
});

describe('preProcess', () => {
  it('puts the style right before the closing head tag', () => {
    const html = preProcess('<head><title>t</title></head><body>b</body>', {
      paperSize: 'A4',
      orientation: 'portrait',
    });

    expect(html).toMatch(
      /<title>t<\/title>\s*<style>[^<]*--pdf-elem-max-width: 75mm;[^<]*<\/style>\s*<\/head><body>b<\/body>/,
    );
  });

  it('puts the style first when there is no head', () => {
    const html = preProcess('<body>b</body>', { paperSize: 'A3', orientation: 'landscape' });

    expect(html.trimStart().startsWith('<style>')).toBe(true);
    expect(html.endsWith('<body>b</body>')).toBe(true);
  });
});

describe('buildPdfHtml', () => {
  it('wraps the rendered page into an html element', () => {
    const page = document.implementation.createHTMLDocument('page');
    page.body.innerHTML = '<div id="diff-body">diff</div>';

    const html = buildPdfHtml(page, { paperSize: 'A4', orientation: 'landscape' });

    expect(html.startsWith("<html lang='en'><head>")).toBe(true);
    expect(html).toContain('--pdf-elem-max-width: 115mm;');
    expect(html).toContain('<div id="diff-body">diff</div>');
    expect(html.endsWith('</body></html>')).toBe(true);
  });
});

describe('convertHtmlToPdf', () => {
  const OPTIONS = { paperSize: 'A3', orientation: 'portrait' } as const;

  it('posts the HTML with the page options and returns the PDF', async () => {
    const pdf = new Blob(['%PDF'], { type: 'application/pdf' });
    const sendRequest = vi.fn<SendRequest>().mockResolvedValue(new Response(pdf, { status: 200 }));

    const result = await convertHtmlToPdf(sendRequest, '<html></html>', OPTIONS);

    expect(await result.text()).toBe('%PDF');
    expect(sendRequest).toHaveBeenCalledWith({
      method: 'POST',
      url: '/conversion/html-to-pdf?orientation=portrait&paperSize=A3',
      body: '<html></html>',
      contentType: 'text/html',
    });
  });

  it('reports an expired session', async () => {
    const loginPage = new Response('<html>login</html>', {
      status: 200,
      headers: { 'x-com-ibm-team-repository-web-auth-msg': 'authrequired' },
    });
    const sendRequest = vi.fn<SendRequest>().mockResolvedValue(loginPage);

    await expect(convertHtmlToPdf(sendRequest, '<html></html>', OPTIONS)).rejects.toBeInstanceOf(SessionExpiredError);
  });

  it('reports the message of the server', async () => {
    const failure = new Response(
      JSON.stringify({ message: 'No ch.sbb.polarion.extension.pdf-exporter extension found.' }),
      {
        status: 500,
      },
    );
    const sendRequest = vi.fn<SendRequest>().mockResolvedValue(failure);

    await expect(convertHtmlToPdf(sendRequest, '<html></html>', OPTIONS)).rejects.toThrow(
      'No ch.sbb.polarion.extension.pdf-exporter extension found.',
    );
  });
});

describe('downloadWindow', () => {
  it('is the top window when this page runs in a frame of the same origin', () => {
    // The test page itself runs in a frame of the Vitest runner.
    expect(downloadWindow()).toBe(window.top);
  });
});

describe('downloadBlob', () => {
  it('clicks a download link with the file name and revokes its URL afterwards', () => {
    vi.useFakeTimers();
    try {
      const createObjectURL = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:pdf');
      const revokeObjectURL = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined);
      const clicked: HTMLAnchorElement[] = [];
      vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (this: HTMLAnchorElement) {
        clicked.push(this);
      });
      const blob = new Blob(['%PDF']);

      downloadBlob(blob, 'Space_Doc.pdf', window);

      expect(createObjectURL).toHaveBeenCalledWith(blob);
      expect(clicked).toHaveLength(1);
      expect(clicked[0].download).toBe('Space_Doc.pdf');
      expect(clicked[0].href).toBe('blob:pdf');
      expect(clicked[0].isConnected).toBe(false);
      expect(revokeObjectURL).not.toHaveBeenCalled();
      vi.advanceTimersByTime(100);
      expect(revokeObjectURL).toHaveBeenCalledWith('blob:pdf');
    } finally {
      vi.useRealTimers();
    }
  });
});
