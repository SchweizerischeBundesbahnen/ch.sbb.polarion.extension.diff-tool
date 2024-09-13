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
          if (pair.leftWorkItem.outlineNumber.startsWith(filtered.leftWorkItem.outlineNumber)) {
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

  return { sendDocumentsDiffRequest, sendMergeRequest };
}