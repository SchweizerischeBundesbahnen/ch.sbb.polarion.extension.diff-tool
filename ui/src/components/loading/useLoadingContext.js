import {useContext, useEffect, useState} from "react";
import AppContext from "@/components/AppContext";
import useRemote from "@/services/useRemote";

const PAUSED = 0;
const READY = 1;
const FINISHED = 2;

export default function useLoadingContext() {
  const remote = useRemote();
  const appContext = useContext(AppContext);

  const [chunkSize, setChunkSize] = useState(2);
  const [pairsLoading, setPairsLoading] = useState(true); // We consider initial state as pairs loading even prior sending factual request, otherwise view will be undefined
  const [pairsLoadingError, setPairsLoadingError] = useState(null);
  const [pairs, setPairs] = useState([]);
  const [pairsCount, setPairsCount] = useState(0);
  const [loadingRegistry, setLoadingRegistry] = useState(new Map());
  const [loadedDiffs, setLoadedDiffs] = useState(0);
  const [diffsLoadingProgress, setDiffsLoadingProgress] = useState(0);
  const [diffsLoadingErrors, setDiffsLoadingErrors] = useState(false);

  const [fieldsDiffLoading, setFieldsDiffLoading] = useState(true);
  const [fieldsDiffLoadingError, setFieldsDiffLoadingError] = useState(null);

  const [reloadMarker, setReloadMarker] = useState(0);

  useEffect(() => {
    remote
        .sendRequest({
          method: "GET",
          url: `/communication/settings`
        })
        .then(response => response.json())
        .then(json => {
          if (json.chunkSize) {
            setChunkSize(json.chunkSize);
          } else {
            console.warn("chunkSize not found in response");
          }
        })
        .catch(error => {
          console.error("Failed to fetch settings:", error);
        });
  }, []);

  useEffect(() => {
    if (pairs.length > 0) {
      pairsLoadingFinished(pairs);
    }
  }, [chunkSize, pairs]);

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
    appContext.state.setDiffsExist(false);
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
        break;
      }
    }
  };

  const fieldsDiffLoadingStarted = () => {
    setFieldsDiffLoading(true);
    setDiffsLoadingProgress(0);
    appContext.state.setDiffsExist(false);
    appContext.state.setDataLoaded(false);
  };

  const fieldsDiffLoadingFinished = () => {
    setFieldsDiffLoading(false);
    setDiffsLoadingProgress(100);
    appContext.state.setDataLoaded(true);
  };

  const fieldsDiffLoadingFinishedWithError = (error) => {
    setFieldsDiffLoading(false);
    setFieldsDiffLoadingError(error);
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
  };

  return {
    pairsLoading,
    pairsLoadingError,
    pairs,
    pairsCount,
    loadedDiffs,
    diffsLoadingProgress,
    diffsLoadingErrors,
    reloadMarker,
    pairLoadingAllowed,
    pairsLoadingStarted,
    pairsLoadingFinished,
    pairsLoadingFinishedWithError,
    diffLoadingFinished,
    resetDiffsLoadingState,
    reload,

    fieldsDiffLoading,
    fieldsDiffLoadingError,
    fieldsDiffLoadingStarted,
    fieldsDiffLoadingFinished,
    fieldsDiffLoadingFinishedWithError,

  };
}
