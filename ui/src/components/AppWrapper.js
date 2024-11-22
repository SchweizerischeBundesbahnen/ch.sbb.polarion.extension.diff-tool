import {useSearchParams} from "next/navigation";
import Documents from "@/components/Documents";
import WorkItems from "@/components/WorkItems";
import Error from "@/components/Error";
import useRemote from "@/services/useRemote";
import {useEffect, useState} from "react";

export default function AppWrapper() {
  const searchParams = useSearchParams();
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

  if (searchParams.get("type") === "documents") {
    return <Documents extensionInfo={extensionInfo} />
  } else if (searchParams.get("type") === "workitems") {
    return <WorkItems extensionInfo={extensionInfo} />
  } else {
    return <Error title="Unknown diff type" />
  }
}
