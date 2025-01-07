import useRemote from "@/services/useRemote";
import {config} from "@fortawesome/fontawesome-svg-core";

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
            loadingContext.pairsLoadingFinished(getFilteredPairs(data.pairedWorkItems.slice(), searchParams));
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

  const getDocumentsFieldsDiff = (searchParams, compareEnumsById, compareOnlyMutualFields, loadingContext) => {
    const leftDocument = getDocumentFromSearchParams(searchParams, 'source');
    const rightDocument = getDocumentFromSearchParams(searchParams, 'target');

    loadingContext.fieldsDiffLoadingStarted(true); // In spite the fact that initial state is "loading", user can re-initiate loading by changing search parameters

    return new Promise((resolve, reject) => {
      remote.sendRequest({
        method: "POST",
        url: `/diff/documents-fields`,
        body: JSON.stringify({
          leftDocument: leftDocument,
          rightDocument: rightDocument,
          compareEnumsById: compareEnumsById,
          compareOnlyMutualFields: compareOnlyMutualFields
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
            loadingContext.fieldsDiffLoadingFinished();
            resolve(data);
          })
          .catch(errorResponse => {
            Promise.resolve(errorResponse).then((error) => {
              loadingContext.fieldsDiffLoadingFinishedWithError(error && error.message);
              reject(error);
            });
          });
    });
  };

  const getFilteredPairs = (pairs, searchParams) => {
    const filterHash = searchParams.get("filter");
    const filterKey = filterHash && (filterHash + "_filter");
    if (filterKey && localStorage && localStorage.getItem(filterKey)) {
      const typeKey = filterHash + "_type";

      let filter = localStorage.getItem(filterKey);
      if (filter.includes(",")) {
        filter = filter.split(",");
      } else if (filter.includes(" ")) {
        filter = filter.split(" ");
      }
      if (localStorage.getItem(typeKey) === "exclude") {
        return pairs.filter(pair => !pair.leftWorkItem || !filter.includes(pair.leftWorkItem.id));
      } else if (localStorage.getItem(typeKey) === "include") {
        return pairs.filter(pair => pair.leftWorkItem && filter.includes(pair.leftWorkItem.id))
      }
    }

    return pairs;
  };

  const sendFindWorkItemsPairsRequest = (searchParams, loadingContext) => {
    loadingContext.pairsLoadingStarted(true); // In spite the fact that initial state is "loading", user can re-initiate loading by changing search parameters

    return new Promise((resolve, reject) => {
      remote.sendRequest({
        method: "POST",
        url: `/diff/workitems-pairs`,
        body: JSON.stringify({
          leftProjectId: searchParams.get('sourceProjectId'),
          rightProjectId: searchParams.get('targetProjectId'),
          leftWorkItemIds: getWorkItemIds(searchParams),
          linkRole: searchParams.get('linkRole'),
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

  const sendCollectionsDiffRequest = (searchParams) => {
    const leftCollection = getCollectionFromSearchParams(searchParams, 'source');
    const rightCollection = getCollectionFromSearchParams(searchParams, 'target');

    return new Promise((resolve, reject) => {
      remote.sendRequest({
        method: "POST",
        url: `/diff/collections`,
        body: JSON.stringify({
          leftCollection: leftCollection,
          rightCollection: rightCollection,
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
            resolve(data);
          })
          .catch(errorResponse => {
            Promise.resolve(errorResponse).then((error) => {
              reject(error);
            });
          });
    });
  };

  const sendCreateTargetDocumentRequest = (sourceDocument, targetProjectId, targetConfiguration, linkRole) => {
    return new Promise((resolve, reject) => {
      remote.sendRequest({
        method: "POST",
        url: `/projects/${sourceDocument.projectId}/spaces/${sourceDocument.spaceId}/documents/${sourceDocument.id}/duplicate`,
        body: JSON.stringify({
          targetDocumentIdentifier: {
            projectId: targetProjectId,
            spaceId: sourceDocument.spaceId,
            name: sourceDocument.id
          },
          targetDocumentTitle: sourceDocument.title,
          configName: targetConfiguration,
          linkRoleId: linkRole,
        }),
        contentType: "application/json"
      })
          .then(response => {
            if (response.headers?.get("x-com-ibm-team-repository-web-auth-msg") === "authrequired") {
              Promise.resolve().then(() => {
                return reject("Your session has expired. Please refresh the page to log in.");
              });
            }
            if (response.ok) {
              return response.json();
            } else {
              throw response.json();
            }
          })
          .then((data) =>  {
            resolve(data);
          })
          .catch(errorResponse => {
            Promise.resolve(errorResponse).then((error) => {
              return reject(error && error.message);
            });
          });
    });
  };

  const getWorkItemIds = (searchParams) => {
    const idsHash = searchParams.get("ids");
    let idsKey = idsHash && (idsHash + "_ids");
    if (idsHash && localStorage.getItem(idsKey)) {
      return localStorage.getItem(idsKey).split(",");
    }
    return [];
  };

  const sendDocumentsMergeRequest = (searchParams, direction, configCacheId, loadingContext, mergingContext, docsData, allowReferencedWorkItemMerge) => {
    const leftDocument = getDocumentFromSearchParams(searchParams, 'source');
    const rightDocument = getDocumentFromSearchParams(searchParams, 'target');
    leftDocument.moduleXmlRevision = docsData.leftDocument.moduleXmlRevision;
    rightDocument.moduleXmlRevision = docsData.rightDocument.moduleXmlRevision;

    return new Promise((resolve, reject) => {
      remote.sendRequest({
        method: "POST",
        url: `/merge/documents`,
        body: JSON.stringify({
          leftDocument: leftDocument,
          rightDocument: rightDocument,
          direction: direction,
          linkRole: searchParams.get('linkRole'),
          configName: searchParams.get('config'),
          configCacheBucketId: configCacheId,
          pairs: filterRedundant(mergingContext.getSelectedValues()),
          allowReferencedWorkItemMerge: allowReferencedWorkItemMerge
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
            loadingContext.reload(mergingContext.getSelectedIndexes());
            resolve(data);
          })
          .catch(errorResponse => {
            Promise.resolve(errorResponse).then((error) => {
              return reject(error && error.message);
            });
          });
    });
  };

  const sendDocumentsFieldsMergeRequest = (searchParams, direction, loadingContext, mergingContext, docsData) => {
    const leftDocument = getDocumentFromSearchParams(searchParams, 'source');
    const rightDocument = getDocumentFromSearchParams(searchParams, 'target');
    leftDocument.moduleXmlRevision = docsData.leftDocument.moduleXmlRevision;
    rightDocument.moduleXmlRevision = docsData.rightDocument.moduleXmlRevision;

    return new Promise((resolve, reject) => {
      remote.sendRequest({
        method: "POST",
        url: `/merge/documents-fields`,
        body: JSON.stringify({
          leftDocument: leftDocument,
          rightDocument: rightDocument,
          direction: direction,
          fieldIds: mergingContext.getSelectedValues()
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
            resolve(data);
          })
          .catch(errorResponse => {
            Promise.resolve(errorResponse).then((error) => {
              return reject(error && error.message);
            });
          });
    });
  };

  const sendWorkItemsMergeRequest = (searchParams, direction, configCacheId, loadingContext, mergingContext) => {

    return new Promise((resolve, reject) => {
      remote.sendRequest({
        method: "POST",
        url: `/merge/workitems`,
        body: JSON.stringify({
          leftProject: {
            id: searchParams.get(`sourceProjectId`)
          },
          rightProject: {
            id: searchParams.get(`targetProjectId`)
          },
          direction: direction,
          linkRole: searchParams.get('linkRole'),
          configName: searchParams.get('config'),
          configCacheBucketId: configCacheId,
          pairs: mergingContext.getSelectedValues()
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
            loadingContext.reload(mergingContext.getSelectedIndexes());
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
      // Exclude mirrored items in case of moving items (such redundant selection is done automatically on UI for better usability)
      if (pair.leftWorkItem && pair.rightWorkItem && (pair.leftWorkItem.movedOutlineNumber || pair.rightWorkItem.outlineNumber)) {
        for (let filtered of filteredPairs) {
          if (filtered.leftWorkItem && pair.leftWorkItem.outlineNumber === filtered.leftWorkItem.outlineNumber) {
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

  const getCollectionFromSearchParams = (searchParams, prefix) => {
    return {
      projectId: searchParams.get(`${prefix}ProjectId`),
      id: searchParams.get(`${prefix}CollectionId`),
    };
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
    } else if ((workItemsPair.leftWorkItem?.referenced && !workItemsPair.rightWorkItem?.referenced) ||
        (!workItemsPair.leftWorkItem?.referenced && workItemsPair.rightWorkItem?.referenced)) {
      return true; // Work items have a referenced mismatch: one is referenced while the other is not
    } else {
      return false; // If no conditions above were met - no difference
    }
  };

  return {
    sendDocumentsDiffRequest,
    sendFindWorkItemsPairsRequest,
    sendCollectionsDiffRequest,
    sendCreateTargetDocumentRequest,
    sendDocumentsMergeRequest,
    sendWorkItemsMergeRequest,
    getDocumentsFieldsDiff,
    sendDocumentsFieldsMergeRequest,
    diffsExist
  };
}
