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
    const hashId = new URLSearchParams(window.location.search).get("individualFieldsSelection");
    if (!hashId) {
      return true;
    }
    const stored = localStorage.getItem(hashId + "_individualFieldsSelection");
    return stored === null ? true : stored === "true";
  });
  const [dataLoaded, setDataLoaded] = useState(false);
  const [diffsExist, setDiffsExist] = useState(false);
  const [selectedItemsCount, setSelectedItemsCount] = useState(0);

  useEffect(() => {
    if (typeof window === "undefined") {
      return;
    }
    const hashId = new URLSearchParams(window.location.search).get("individualFieldsSelection");
    if (hashId) {
      localStorage.setItem(hashId + "_individualFieldsSelection", String(individualFieldsSelection));
    }
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
