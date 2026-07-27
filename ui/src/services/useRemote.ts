const REST_PATH = '/polarion/diff-tool/rest';

export interface SendRequestOptions {
  method: string;
  url: string;
  body?: BodyInit | null;
  contentType?: string;
}

/** The single REST seam of the app. Structurally compatible with react-sbb-polarion's SendRequest. */
export type SendRequest = (options: SendRequestOptions) => Promise<Response>;

export default function useRemote(): { sendRequest: SendRequest } {
  const sendRequest: SendRequest = ({ method, url, body, contentType }) => {
    const headers: Record<string, string> = {};
    if (contentType) {
      headers['Content-Type'] = contentType;
    }
    const bearerToken = import.meta.env.VITE_BEARER_TOKEN;
    if (bearerToken) {
      headers['Authorization'] = `Bearer ${bearerToken}`;
    }

    const apiPath = bearerToken ? '/api' : '/internal';

    // Always same-origin: inside Polarion the app is served from the same host, and in `vite dev` the
    // dev-server proxy forwards /polarion/diff-tool/rest to VITE_BASE_URL. That replaces the old
    // NEXT_PUBLIC_BASE_URL prefixing, which needed CORS.
    return fetch(`${REST_PATH}${apiPath}${url}`, {
      method: method,
      mode: 'cors', // no-cors, *cors, same-origin
      cache: 'no-cache', // *default, no-cache, reload, force-cache, only-if-cached
      headers: headers,
      body: body,
    }).catch(() => {
      // fetch() rejects on a network error; surface it as an ordinary response instead so every
      // caller's handleResponse path works unchanged and the UI can show the "Be sure Polarion is
      // started" alert.
      //
      // NOTE: the JS version built this init as { status: 503, 'Content-Type': 'application/json' },
      // where Content-Type is not a ResponseInit field and was therefore silently dropped - the
      // synthesised response carried no content type. Nesting it under `headers` fixes that; the 503
      // status and the body are unchanged.
      return new Response(
        JSON.stringify({
          message:
            'Network error occurred when attempting to fetch a resource. Be sure Polarion is started and accessible.',
        }),
        { status: 503, headers: { 'Content-Type': 'application/json' } },
      );
    });
  };

  return { sendRequest: sendRequest };
}
