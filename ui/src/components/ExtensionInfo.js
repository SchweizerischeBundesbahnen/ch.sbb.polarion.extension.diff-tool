import {useEffect, useState} from "react";
import useRemote from "@/services/useRemote";

export default function ExtensionInfo() {
  const remote = useRemote();

  const [extensionInfo, setExtensionInfo] = useState("");

  useEffect(() => {
    if (remote) {
      remote.sendRequest({
        method: "GET",
        url: `/extension/info/`,
        contentType: "application/json"
      }).then(response => {
        if (response.ok) {
          return response.json();
        } else {
          throw response.json();
        }
      }).then(data => {
        setExtensionInfo(`v${data?.version?.bundleVersion} | ${data?.version?.bundleBuildTimestamp}`);
      }).catch(errorResponse => {
        Promise.resolve(errorResponse).then((error) => alert("Error occurred loading extension info" + (error && error.message ? ": " + error.message : "")));
      });
    }
  }, []);

  return (
      <div className="extension-info">
        {extensionInfo}
      </div>
  );
}
