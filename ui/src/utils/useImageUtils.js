const WORK_ITEM_IMAGE = "workitemimg:";
const DOCUMENT_IMAGE = "attachment:";

const replaceImagePaths = function (diffValue, paths) {
  let res = diffValue;
  for (const [prefix, path] of paths) {
    if (res.includes(`<img src="${prefix}`)) {
      res = res.replaceAll(`<img src="${prefix}`, `<img src="${path}`);
    }
  }
  return res;
};

export default function useImageUtils() {

  const documentWorkItemPaths = function (workItem, document) {
    return [
      [WORK_ITEM_IMAGE, `/polarion/wi-attachment/${document.projectId}/${workItem && workItem.id}/`],
      [DOCUMENT_IMAGE, `/polarion/module-attachment/${document.projectId}/${document.spaceId}/${document.id}/`]
    ];
  };

  const detachedWorkItemPaths = function (workItem) {
    return [
      [WORK_ITEM_IMAGE, `/polarion/wi-attachment/${workItem && workItem.projectId}/${workItem && workItem.id}/`],
      // TODO: fix document reference or combine with method above!!
      [DOCUMENT_IMAGE, `/polarion/module-attachment/${workItem && workItem.projectId}/${document.spaceId}/${document.id}/`]
    ];
  };

  const fixImagesPathInDocumentWorkItem = function (diffValue, workItem, document) {
    return replaceImagePaths(diffValue, documentWorkItemPaths(workItem, document));
  };

  const fixImagesPathInDetachedWorkItem = function (diffValue, workItem) {
    return replaceImagePaths(diffValue, detachedWorkItemPaths(workItem));
  };

  /**
   * The unified diff holds images of both work items: a removed image is the source's, any other one the
   * target's. So each image is resolved against the work item it belongs to.
   */
  const fixImagesPathInUnifiedDiff = function (diffValue, sourcePaths, targetPaths) {
    if (![WORK_ITEM_IMAGE, DOCUMENT_IMAGE].some(prefix => diffValue.includes(`<img src="${prefix}`))) {
      return diffValue;
    }
    const template = window.document.createElement("template");
    template.innerHTML = diffValue;
    for (const image of template.content.querySelectorAll("img[src]")) {
      const paths = image.closest(".diff-html-removed") ? sourcePaths : targetPaths;
      const src = image.getAttribute("src");
      const match = paths.find(([prefix]) => src.startsWith(prefix));
      if (match) {
        image.setAttribute("src", match[1] + src.substring(match[0].length));
      }
    }
    return template.innerHTML;
  };

  const unifiedDocumentWorkItemsDiff = function (diffLeft, diffRight, leftWorkItem, leftDocument, rightWorkItem, rightDocument) {
    return diffRight
        ? fixImagesPathInUnifiedDiff(diffRight, documentWorkItemPaths(leftWorkItem, leftDocument), documentWorkItemPaths(rightWorkItem, rightDocument))
        : fixImagesPathInDocumentWorkItem(diffLeft, leftWorkItem, leftDocument);
  };

  const unifiedDetachedWorkItemsDiff = function (diffLeft, diffRight, leftWorkItem, rightWorkItem) {
    return diffRight
        ? fixImagesPathInUnifiedDiff(diffRight, detachedWorkItemPaths(leftWorkItem), detachedWorkItemPaths(rightWorkItem))
        : fixImagesPathInDetachedWorkItem(diffLeft, leftWorkItem);
  };

  return { fixImagesPathInDocumentWorkItem, fixImagesPathInDetachedWorkItem, unifiedDocumentWorkItemsDiff, unifiedDetachedWorkItemsDiff };
}
