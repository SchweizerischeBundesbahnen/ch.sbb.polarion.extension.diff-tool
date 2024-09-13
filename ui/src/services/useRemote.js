const REST_PATH = '/polarion/diff-tool/rest';

export default function useRemote() {

  const sendRequest = ({method, url, body, contentType}) => {
    const headers = {
      "Content-Type": contentType
    };
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
    });
  }

  return { sendRequest };
}
