import {useState} from "react";

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
  const [dataLoaded, setDataLoaded] = useState(false);
  const [diffsExist, setDiffsExist] = useState(false);
  const [selectedItemsCount, setSelectedItemsCount] = useState(0);

  return { headerPinned,
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
    dataLoaded,
    setDataLoaded,
    diffsExist,
    setDiffsExist,
    selectedItemsCount,
    setSelectedItemsCount
  };
}
