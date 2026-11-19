// The messages an embedded viewer posts to the page which embeds it. Both sides use this module, so the
// contract has one definition. Messages go only to a page of the same origin, and are accepted only from one.

export const EXPORT_MESSAGE_SOURCE = 'diff-tool-export';

export type ExportMessage =
  /** Work items diff: `loaded` of `total` work item pairs are diffed. */
  | { source: typeof EXPORT_MESSAGE_SOURCE; type: 'progress'; loaded: number; total: number }
  /** The diff is rendered completely and can be exported. */
  | { source: typeof EXPORT_MESSAGE_SOURCE; type: 'ready'; diffsExist: boolean }
  /** The diff could not be loaded. */
  | { source: typeof EXPORT_MESSAGE_SOURCE; type: 'error'; message: string };

export const progressMessage = (loaded: number, total: number): ExportMessage => ({
  source: EXPORT_MESSAGE_SOURCE,
  type: 'progress',
  loaded: loaded,
  total: total,
});

export const readyMessage = (diffsExist: boolean): ExportMessage => ({
  source: EXPORT_MESSAGE_SOURCE,
  type: 'ready',
  diffsExist: diffsExist,
});

export const errorMessage = (message: string): ExportMessage => ({
  source: EXPORT_MESSAGE_SOURCE,
  type: 'error',
  message: message,
});

/** Whether the data of a message event is an export message. */
export function isExportMessage(data: unknown): data is ExportMessage {
  if (!data || typeof data !== 'object') {
    return false;
  }
  const message = data as Record<string, unknown>;
  if (message.source !== EXPORT_MESSAGE_SOURCE) {
    return false;
  }
  switch (message.type) {
    case 'progress':
      return typeof message.loaded === 'number' && typeof message.total === 'number';
    case 'ready':
      return typeof message.diffsExist === 'boolean';
    case 'error':
      return typeof message.message === 'string';
    default:
      return false;
  }
}

/** Posts the message to the embedding page. Does nothing when this page is not embedded. */
export function postToParent(message: ExportMessage, target: Window = window.parent): void {
  if (target && target !== window) {
    target.postMessage(message, window.location.origin);
  }
}
