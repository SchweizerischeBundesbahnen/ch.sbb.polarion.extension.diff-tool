import {useEffect, useState} from "react";
import useRemote from "@/services/useRemote";
import {responseError} from "@/services/responseError";

export default function ExtensionInfo() {
  const remote = useRemote();

  const [extensionInfo, setExtensionInfo] = useState("");

  useEffect(() => {
    if (remote) {
      remote.sendRequest({
        method: "GET",
        url: `/extension/info`,
        contentType: "application/json"
      }).then(async response => {
        if (!response.ok) {
          throw await responseError(response);
        }
        return response.json();
      }).then(data => {
        setExtensionInfo(`v${data?.version?.bundleVersion} | ${data?.version?.bundleBuildTimestamp}`);
      }).catch(error => {
        console.log("Error occurred loading extension info" + (error && error.message ? ": " + error.message : ""));
      });
    }
  }, []);

  return (
      <div className="extension-info">
        {extensionInfo}
      </div>
  );
}
