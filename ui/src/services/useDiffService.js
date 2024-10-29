import useRemote from "@/services/useRemote";

export default function useDiffService() {
  const remote = useRemote();

  const sendDocumentsDiffRequest = (searchParams, configCacheId, loadingContext) => {
    const leftDocument = getDocumentFromSearchParams(searchParams, 'source');
    const rightDocument = getDocumentFromSearchParams(searchParams, 'target');

    loadingContext.pairsLoadingStarted(true); // In spite the fact that initial state is "loading", user can re-initiate loading by changing search parameters

    return new Promise((resolve, reject) => {
      remote.sendRequest({
        method: "POST",
        url: `/diff/documents`,
        body: JSON.stringify({
          leftDocument: leftDocument,
          rightDocument: rightDocument,
          linkRole: searchParams.get('linkRole'),
          configName: searchParams.get('config'),
          configCacheBucketId: configCacheId
        }),
        contentType: "application/json"
      })
          .then(response => {
            if (response.ok) {
              return response.json();
            } else {
              throw response.json();
            }
          })
          .then(data =>  {
            loadingContext.pairsLoadingFinished(data.pairedWorkItems.slice());
            resolve(data);
          })
          .catch(errorResponse => {
            Promise.resolve(errorResponse).then((error) => {
              loadingContext.pairsLoadingFinishedWithError(error && error.message);
              reject(error);
            });
          });
    });
  };

  const sendMergeRequest = (searchParams, direction, configCacheId, loadingContext, mergingContext, docsData) => {
    const leftDocument = getDocumentFromSearchParams(searchParams, 'source');
    const rightDocument = getDocumentFromSearchParams(searchParams, 'target');
    leftDocument.moduleXmlRevision = docsData.leftDocument.moduleXmlRevision;
    rightDocument.moduleXmlRevision = docsData.rightDocument.moduleXmlRevision;

    return new Promise((resolve, reject) => {
      remote.sendRequest({
        method: "POST",
        url: `/merge/workitems`,
        body: JSON.stringify({
          leftDocument: leftDocument,
          rightDocument: rightDocument,
          direction: direction,
          linkRole: searchParams.get('linkRole'),
          configName: searchParams.get('config'),
          configCacheBucketId: configCacheId,
          pairs: filterRedundant(Array.from(mergingContext.selectionRegistry.values()))
        }),
        contentType: "application/json"
      })
          .then(response => {
            if (response.headers?.get("x-com-ibm-team-repository-web-auth-msg") === "authrequired") {
              Promise.resolve().then(() => {
                return reject("Your session has expired. Please refresh the page to log in. Note that any unsaved changes will be lost.");
              });
            }
            if (response.ok) {
              return response.json();
            } else {
              throw response.json();
            }
          })
          .then((data) =>  {
            loadingContext.reload(Array.from(mergingContext.selectionRegistry.keys()));
            resolve(data);
          })
          .catch(errorResponse => {
            Promise.resolve(errorResponse).then((error) => {
              return reject(error && error.message);
            });
          });
    });
  };

  const filterRedundant = (selectedPairs) => {
    const filteredPairs = [];
    selectedPairs.forEach(pair => {
      let redundant = false;
      // Exclude mirrored and child items in case of moving items (such redundant selection is done automatically on UI for better usability)
      if (pair.leftWorkItem && pair.rightWorkItem && (pair.leftWorkItem.movedOutlineNumber || pair.rightWorkItem.outlineNumber)) {
        for (let filtered of filteredPairs) {
          if (filtered.leftWorkItem && pair.leftWorkItem.outlineNumber.startsWith(filtered.leftWorkItem.outlineNumber)) {
            redundant = true;
            break;
          }
        }
      }
      if (!redundant) {
        filteredPairs.push(pair);
      }
    });
    return filteredPairs;
  };

  const getDocumentFromSearchParams = (searchParams, prefix) => {
    return {
      projectId: searchParams.get(`${prefix}ProjectId`),
      spaceId: searchParams.get(`${prefix}SpaceId`),
      name: searchParams.get(`${prefix}Document`),
      revision: searchParams.get(`${prefix}Revision`)
    };
  };

  const diffsExist = (workItemsPair, diffs) => {
    if (diffs && (diffs.length > 1 || (diffs.length === 1 && diffs[0].id !== 'outlineNumber'))) {
      return true; // Content differs. Outline number difference shouldn't be taken into account, cause this means moved item, not content difference
    } else if (workItemsPair.leftWorkItem?.movedOutlineNumber || workItemsPair.rightWorkItem?.movedOutlineNumber) {
      return true; // Work item was moved, so there's a difference
    } else if (!workItemsPair.leftWorkItem || !workItemsPair.rightWorkItem) {
      return true; // Work item was deleted/created, so there's a difference
    } else {
      return false; // If no conditions above were met - no difference
    }
  };

  return { sendDocumentsDiffRequest, sendMergeRequest, diffsExist };
}
