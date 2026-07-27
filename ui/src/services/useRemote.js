const REST_PATH = '/polarion/diff-tool/rest';

export default function useRemote() {

  const sendRequest = ({method, url, body, contentType}) => {
    const headers = {};
    if (contentType) {
      headers["Content-Type"] = contentType;
    }
    const bearerToken = import.meta.env.VITE_BEARER_TOKEN;
    if (bearerToken) {
      headers["Authorization"] = `Bearer ${bearerToken}`;
    }

    const apiPath = bearerToken ? "/api" : "/internal";

    // Always same-origin: inside Polarion the app is served from the same host, and in `vite dev` the
    // dev-server proxy forwards /polarion/diff-tool/rest to VITE_BASE_URL. That replaces the old
    // NEXT_PUBLIC_BASE_URL prefixing, which needed CORS.
    return fetch(`${REST_PATH}${apiPath}${url}`, {
      method: method,
      mode: "cors", // no-cors, *cors, same-origin
      cache: "no-cache", // *default, no-cache, reload, force-cache, only-if-cached
      headers: headers,
      body: body,
    }).catch(() => {
      // window's fetch() method will throw an exception in case of network errors, we are gracefully handling this exception here, simulating "service unavailable" response from server
      const headers = {
        "status": 503,
        "Content-Type": "application/json"
      };
      const errorResponse = new Response(JSON.stringify({ message: "Network error occurred when attempting to fetch a resource. Be sure Polarion is started and accessible." }), headers);
      return Promise.resolve(errorResponse);
    });
  }

  return {sendRequest};
}
