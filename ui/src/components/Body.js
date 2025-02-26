'use client'

import {useEffect, useState} from "react";
import {Inter} from "next/font/google";
import AppContext from "./AppContext";
import useSessionRenewal from '../services/useSessionRenewal';

const inter = Inter({ subsets: ["latin"] });

export default function Body({ children }) {
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

  const handleScroll = event => {
    setHeaderPinned(event.currentTarget.scrollTop > 60);
  };

  useEffect(() => {
    setControlPaneAccessible(dataLoaded);
    if (dataLoaded) {
      setTimeout(() => setHideChaptersIfNoDifference(true), 200);
    }
  }, [dataLoaded]);

  useSessionRenewal();

  return <AppContext.Provider
      value={{
        state: {
          headerPinned: headerPinned,
          controlPaneAccessible: controlPaneAccessible,
          setControlPaneAccessible: setControlPaneAccessible,
          controlPaneExpanded: controlPaneExpanded,
          setControlPaneExpanded: setControlPaneExpanded,
          pairedDocuments: pairedDocuments,
          setPairedDocuments: setPairedDocuments,
          showOutlineNumbersDiff: showOutlineNumbersDiff,
          setShowOutlineNumbersDiff: setShowOutlineNumbersDiff,
          counterpartWorkItemsDiffer: counterpartWorkItemsDiffer,
          setCounterpartWorkItemsDiffer: setCounterpartWorkItemsDiffer,
          compareOnlyMutualFields: compareOnlyMutualFields,
          setCompareOnlyMutualFields: setCompareOnlyMutualFields,
          compareEnumsById: compareEnumsById,
          setCompareEnumsById: setCompareEnumsById,
          allowReferencedWorkItemMerge: allowReferencedWorkItemMerge,
          setAllowReferencedWorkItemMerge: setAllowReferencedWorkItemMerge,
          hideChaptersIfNoDifference: hideChaptersIfNoDifference,
          setHideChaptersIfNoDifference: setHideChaptersIfNoDifference,
          dataLoaded: dataLoaded,
          setDataLoaded: setDataLoaded,
          diffsExist: diffsExist,
          setDiffsExist: setDiffsExist,
          selectedItemsCount: selectedItemsCount,
          setSelectedItemsCount: setSelectedItemsCount
        }
      }}
  >
    <body className={inter.className} onScroll={handleScroll}>{children}</body>
  </AppContext.Provider>
}
