const REST_PATH = '/polarion/diff-tool/rest';

export default function useRemote() {

  const sendRequest = ({method, url, body, contentType}) => {
    const headers = {};
    if (contentType) {
      headers["Content-Type"] = contentType;
    }
    if (process.env.NEXT_PUBLIC_BEARER_TOKEN) {
      headers["Authorization"] = `Bearer ${process.env.NEXT_PUBLIC_BEARER_TOKEN}`;
    }

    const apiPath = process.env.NEXT_PUBLIC_BEARER_TOKEN ? "/api" : "/internal";

    return fetch(`${process.env.NEXT_PUBLIC_BASE_URL || ""}${REST_PATH}${apiPath}${url}`, {
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
