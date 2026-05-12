import {useEffect, useState} from "react";

export function useAppContext(){
  const [headerPinned, setHeaderPinned] = useState(false);
  const [controlPaneAccessible, setControlPaneAccessible] = useState(false);
  const [controlPaneExpanded, setControlPaneExpanded] = useState(false);
  const [pairedDocuments, setPairedDocuments] = useState([]);
  const [showOutlineNumbersDiff, setShowOutlineNumbersDiff] = useState(false);
  const [counterpartWorkItemsDiffer, setCounterpartWorkItemsDiffer] = useState(false);
  const [compareOnlyMutualFields, setCompareOnlyMutualFields] = useState(true);
  const [compareEnumsById, setCompareEnumsById] = useState(false);
  const [allowReferencedWorkItemMerge, setAllowReferencedWorkItemMerge] = useState(false);
  const [hideChaptersIfNoDifference, setHideChaptersIfNoDifference] = useState(true);
  const [preserveComments, setPreserveComments] = useState(false);
  const [copyMissingDocumentAttachments, setCopyMissingDocumentAttachments] = useState(true);
  const [updateAttachmentReferences, setUpdateAttachmentReferences] = useState(true);
  const [individualFieldsSelection, setIndividualFieldsSelection] = useState(() => {
    if (typeof window === "undefined") {
      return true;
    }
    const hashId = new URLSearchParams(window.location.search).get("additionalParams");
    if (!hashId) {
      return true;
    }
    const raw = localStorage.getItem(hashId + "_additionalParams");
    if (!raw) {
      return true;
    }
    try {
      const parsed = JSON.parse(raw);
      return typeof parsed.individualFieldsSelection === "boolean" ? parsed.individualFieldsSelection : true;
    } catch (e) {
      return true;
    }
  });
  const [dataLoaded, setDataLoaded] = useState(false);
  const [diffsExist, setDiffsExist] = useState(false);
  const [selectedItemsCount, setSelectedItemsCount] = useState(0);

  useEffect(() => {
    if (typeof window === "undefined") {
      return;
    }
    const hashId = new URLSearchParams(window.location.search).get("additionalParams");
    if (!hashId) {
      return;
    }
    const key = hashId + "_additionalParams";
    let current = {};
    try {
      const raw = localStorage.getItem(key);
      if (raw) {
        const parsed = JSON.parse(raw);
        if (parsed && typeof parsed === "object") {
          current = parsed;
        }
      }
    } catch (e) {
      current = {};
    }
    current.individualFieldsSelection = individualFieldsSelection;
    current.ts = Date.now();
    localStorage.setItem(key, JSON.stringify(current));
  }, [individualFieldsSelection]);

  return { headerPinned,
    setHeaderPinned,
    controlPaneAccessible,
    setControlPaneAccessible,
    controlPaneExpanded,
    setControlPaneExpanded,
    pairedDocuments,
    setPairedDocuments,
    showOutlineNumbersDiff,
    setShowOutlineNumbersDiff,
    counterpartWorkItemsDiffer,
    setCounterpartWorkItemsDiffer,
    compareOnlyMutualFields,
    setCompareOnlyMutualFields,
    compareEnumsById,
    setCompareEnumsById,
    allowReferencedWorkItemMerge,
    setAllowReferencedWorkItemMerge,
    hideChaptersIfNoDifference,
    setHideChaptersIfNoDifference,
    preserveComments,
    setPreserveComments,
    copyMissingDocumentAttachments,
    setCopyMissingDocumentAttachments,
    updateAttachmentReferences,
    setUpdateAttachmentReferences,
    individualFieldsSelection,
    setIndividualFieldsSelection,
    dataLoaded,
    setDataLoaded,
    diffsExist,
    setDiffsExist,
    selectedItemsCount,
    setSelectedItemsCount
  };
}
