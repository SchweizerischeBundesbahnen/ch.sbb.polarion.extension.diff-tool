'use client'

import {useEffect} from "react";
import {Inter} from "next/font/google";
import AppContext from "./AppContext";
import useSessionRenewal from '../services/useSessionRenewal';
import {useAppContext} from "@/useAppContext";

const inter = Inter({ subsets: ["latin"] });

export default function Body({ children }) {
  const appContext = useAppContext();

  const handleScroll = event => {
    appContext.setHeaderPinned(event.currentTarget.scrollTop > 60);
  };

  useEffect(() => {
    appContext.setControlPaneAccessible(appContext.dataLoaded);
    if (appContext.dataLoaded) {
      setTimeout(() => appContext.setHideChaptersIfNoDifference(true), 200);
    }
  }, [appContext.dataLoaded]);

  useSessionRenewal();

  return <AppContext.Provider value={{ state: { ...appContext } }}>
    <body className={inter.className} onScroll={handleScroll}>{children}</body>
  </AppContext.Provider>
}
