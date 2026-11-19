import {useEffect, useState} from "react";
import useDiffViewMode from "@/utils/useDiffViewMode";
import {isPdfExportEmbed, readEmbedViewOptions} from "@/embed/exportMode";

export function useAppContext(){
  // Embedded for a PDF export, the page which embeds the viewer chooses the view options. Elsewhere this is empty.
  const [embedOptions] = useState(() => readEmbedViewOptions());
  const [headerPinned, setHeaderPinned] = useState(false);
  const [controlPaneAccessible, setControlPaneAccessible] = useState(false);
  // A PDF is exported from the open control pane, and the print styles of the diff body depend on that.
  // Embedded, the viewer has no control pane, so it starts as if the pane were open: its PDF looks the same.
  const [controlPaneExpanded, setControlPaneExpanded] = useState(() => isPdfExportEmbed());
  const [pairedDocuments, setPairedDocuments] = useState([]);
  // In the query string rather than here, so the reload after a merge keeps it.
  const [diffViewMode, setDiffViewMode] = useDiffViewMode();
  const [showOutlineNumbersDiff, setShowOutlineNumbersDiff] = useState(embedOptions.showOutlineNumbersDiff ?? false);
  const [syncStructure, setSyncStructure] = useState(true);
  const [counterpartWorkItemsDiffer, setCounterpartWorkItemsDiffer] = useState(embedOptions.counterpartWorkItemsDiffer ?? false);
  const [compareOnlyMutualFields, setCompareOnlyMutualFields] = useState(embedOptions.compareOnlyMutualFields ?? true);
  const [compareEnumsById, setCompareEnumsById] = useState(embedOptions.compareEnumsById ?? false);
  const [allowReferencedWorkItemMerge, setAllowReferencedWorkItemMerge] = useState(false);
  const [hideChaptersIfNoDifference, setHideChaptersIfNoDifference] = useState(embedOptions.hideChaptersIfNoDifference ?? true);
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
    } catch {
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
    } catch {
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
    diffViewMode,
    setDiffViewMode,
    showOutlineNumbersDiff,
    setShowOutlineNumbersDiff,
    syncStructure,
    setSyncStructure,
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
