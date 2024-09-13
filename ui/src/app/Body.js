'use client'

import {useEffect, useState} from "react";
import {Inter} from "next/font/google";
import AppContext from "../components/AppContext";

const inter = Inter({ subsets: ["latin"] });

export default function Body({ children }) {
  const [headerPinned, setHeaderPinned] = useState(false);
  const [controlPaneAccessible, setControlPaneAccessible] = useState(false);
  const [controlPaneExpanded, setControlPaneExpanded] = useState(false);
  const [showOutlineNumbersDiff, setShowOutlineNumbersDiff] = useState(false);
  const [counterpartWorkItemsDiffer, setCounterpartWorkItemsDiffer] = useState(false);
  const [hideChaptersIfNoDifference, setHideChaptersIfNoDifference] = useState(true);
  const [dataLoaded, setDataLoaded] = useState(false);
  const [extensionInfo, setExtensionInfo] = useState(false);

  const handleScroll = event => {
    setHeaderPinned(event.currentTarget.scrollTop > 60);
  };

  useEffect(() => {
    setControlPaneAccessible(dataLoaded);
    if (dataLoaded) {
      setTimeout(() => setHideChaptersIfNoDifference(true), 200);
    }
  }, [dataLoaded]);

  return <AppContext.Provider
      value={{
        state: {
          headerPinned: headerPinned,
          controlPaneAccessible: controlPaneAccessible,
          setControlPaneAccessible: setControlPaneAccessible,
          controlPaneExpanded: controlPaneExpanded,
          setControlPaneExpanded: setControlPaneExpanded,
          showOutlineNumbersDiff: showOutlineNumbersDiff,
          setShowOutlineNumbersDiff: setShowOutlineNumbersDiff,
          counterpartWorkItemsDiffer: counterpartWorkItemsDiffer,
          setCounterpartWorkItemsDiffer: setCounterpartWorkItemsDiffer,
          hideChaptersIfNoDifference: hideChaptersIfNoDifference,
          setHideChaptersIfNoDifference: setHideChaptersIfNoDifference,
          dataLoaded: dataLoaded,
          setDataLoaded: setDataLoaded,
          extensionInfo: extensionInfo,
          setExtensionInfo: setExtensionInfo
        }
      }}
  >
    <body className={inter.className} onScroll={handleScroll}>{children}</body>
  </AppContext.Provider>
}