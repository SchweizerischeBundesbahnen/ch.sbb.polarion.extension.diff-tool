import {useContext, useEffect, useState} from "react";
import AppContext from "@/components/AppContext";
import {useSearchParams} from "next/navigation";

const PAUSED = 0;
const READY = 1;
const FINISHED = 2;

export default function useLoadingContext() {
  const appContext = useContext(AppContext);
  const searchParams = useSearchParams();

  const [chunkSize] = useState(searchParams.get('chunkSize') || 10);
  const [pairsLoading, setPairsLoading] = useState(true); // We consider initial state as pairs loading even prior sending factual request, otherwise view will be undefined
  const [pairsLoadingError, setPairsLoadingError] = useState(null);
  const [pairs, setPairs] = useState([]);
  const [pairsCount, setPairsCount] = useState(0);
  const [loadingRegistry, setLoadingRegistry] = useState(new Map());
  const [loadedDiffs, setLoadedDiffs] = useState(0);
  const [diffsLoadingProgress, setDiffsLoadingProgress] = useState(0);
  const [diffsLoadingErrors, setDiffsLoadingErrors] = useState(false);
  const [reloadMarker, setReloadMarker] = useState(0);

  useEffect(() => {
    // In case of loading errors we stop further data portions loading that's why consider process as finished (100%)
    const progress = pairsCount > 0 ? (diffsLoadingErrors ? 100 : Math.min(100, Math.floor((loadedDiffs * 100) / pairsCount))) : 0;
    setDiffsLoadingProgress(progress);
    appContext.state.setDataLoaded(!diffsLoadingErrors && progress === 100);
  }, [pairsCount, loadedDiffs, diffsLoadingErrors]);

  const pairLoadingAllowed = (pairIndex) => {
    return !diffsLoadingErrors && loadingRegistry.get(pairIndex) === READY; // Release data loading if the pair is marked as ready for diffing, but not in case if loading errors happen for other pairs
  };

  const pairsLoadingStarted = () => {
    setPairsLoading(true);
  };

  const pairsLoadingFinished = (pairs) => {
    setPairsLoading(false);
    setLoadedDiffs(0);
    setDiffsLoadingErrors(false);

    const pairsCount = pairs ? pairs.length : 0;
    setPairsCount(pairsCount);

    const loadingRegistry = new Map();
    for (let i = 0; i < pairsCount; i++) {
      loadingRegistry.set(i, i < chunkSize ? READY : PAUSED);
    }
    setLoadingRegistry(loadingRegistry);

    setPairs(pairs);
  };

  const pairsLoadingFinishedWithError = (error) => {
    setPairsLoading(false);
    setPairsLoadingError(error);
  };

  const diffLoadingFinished = (index, error) => {
    setDiffsLoadingErrors(priorErrors => priorErrors || error);
    setLoadedDiffs(priorCount => priorCount + 1);

    loadingRegistry.set(index, FINISHED);
    for (let i = 1; i <= pairsCount; i++) {
      if (loadingRegistry.get(i) === PAUSED) {
        loadingRegistry.set(i, READY);
      }
    }
  };

  const resetDiffsLoadingState = (pairs) => {
    setPairs([]);
    appContext.state.setDataLoaded(false);
    appContext.state.setControlPaneExpanded(false);

    setTimeout(() => pairsLoadingFinished(pairs), 100); // Re-initialize state based on passed pairs
  };

  const reload = (indexes) => {
    for (let i = 0; i < indexes.length; i++) {
      loadingRegistry.set(indexes[i], i < chunkSize ? READY : PAUSED);
    }
    setLoadedDiffs(pairs.length - indexes.length);
    setReloadMarker(n => n + 1);
  }

  return { pairsLoading, pairsLoadingError, pairs, pairsCount, loadedDiffs, diffsLoadingProgress, diffsLoadingErrors, reloadMarker,
    pairLoadingAllowed, pairsLoadingStarted, pairsLoadingFinished, pairsLoadingFinishedWithError, diffLoadingFinished, resetDiffsLoadingState, reload };
}
