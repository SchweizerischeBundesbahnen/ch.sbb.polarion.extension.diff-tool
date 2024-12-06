export default function useImageUtils() {

  const fixImagesPathInDocumentWorkItem = function (diffValue, workItem, document) {
    let res = diffValue;
    if (res.includes('<img src="workitemimg:')) {
      res = res.replaceAll('<img src="workitemimg:', `<img src="/polarion/wi-attachment/${document.projectId}/${workItem && workItem.id}/`)
    }
    if (res.includes('<img src="attachment:')) {
      res = res.replaceAll('<img src="attachment:', `<img src="/polarion/module-attachment/${document.projectId}/${document.spaceId}/${document.id}/`)
    }
    return res;
  };

  const fixImagesPathInDetachedWorkItem = function (diffValue, workItem) {
    let res = diffValue;
    if (res.includes('<img src="workitemimg:')) {
      res = res.replaceAll('<img src="workitemimg:', `<img src="/polarion/wi-attachment/${workItem && workItem.projectId}/${workItem && workItem.id}/`)
    }
    if (res.includes('<img src="attachment:')) { // TODO: fix document reference or combine with method above!!
      res = res.replaceAll('<img src="attachment:', `<img src="/polarion/module-attachment/${workItem && workItem.projectId}/${document.spaceId}/${document.id}/`)
    }
    return res;
  };

  return { fixImagesPathInDocumentWorkItem, fixImagesPathInDetachedWorkItem };
}
