import {useEffect} from "react";
import AppContext from "./AppContext";
import useSessionRenewal from '../services/useSessionRenewal';
import {useAppContext} from "@/useAppContext";

export default function AppShell({ children }) {
  const appContext = useAppContext();

  // Ported as-is from the Next.js <body onScroll={...}> handler. It reads document.body.scrollTop,
  // which is always 0 in standards mode (documentElement is the scrolling element), so headerPinned
  // never flips - and the resulting "pinned" class has no CSS rule anywhere and no test asserting
  // it. Kept faithful on purpose: making it work would be a behaviour change smuggled into a
  // bundler swap. Fix or drop it deliberately, separately.
  useEffect(() => {
    const handleScroll = () => appContext.setHeaderPinned(document.body.scrollTop > 60);
    document.body.addEventListener('scroll', handleScroll);
    return () => document.body.removeEventListener('scroll', handleScroll);
  }, []);

  useEffect(() => {
    appContext.setControlPaneAccessible(appContext.dataLoaded);
    if (appContext.dataLoaded) {
      setTimeout(() => appContext.setHideChaptersIfNoDifference(true), 200);
    }
  }, [appContext.dataLoaded]);

  useSessionRenewal();

  return <AppContext.Provider value={{ state: { ...appContext } }}>{children}</AppContext.Provider>
}
