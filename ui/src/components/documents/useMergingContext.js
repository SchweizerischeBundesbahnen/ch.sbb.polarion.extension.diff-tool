import {useEffect, useState} from "react";

export const LEFT_TO_RIGHT = 0;
export const RIGHT_TO_LEFT = 1;

export function useMergingContext() {
  const [selectionRegistry, setSelectionRegistry] = useState(new Map());
  const [selectionCount, setSelectionCount] = useState(0);
  const [selectAll, setSelectAll] = useState(false);
  const [selectAllTrigger, setSelectAllTrigger] = useState(0);

  useEffect(() => {
    let allSelected = true;
    let count = 0;
    selectionRegistry.forEach((value) => {
      count += (value ? 1 : 0);
      if (!value) {
        allSelected = false;
      }
    });
    setSelectionCount(count);
    if (allSelected !== selectAll) {
      setSelectAll(allSelected);
    }
  }, [selectionRegistry]);

  const resetRegistryEntry = (index) => {
    setSelectionRegistry(r => {
      const registry = new Map(r);
      registry.delete(index);
      return registry;
    });
  };

  const setPairSelected = (index, workItemsPair, selected) => {
    setSelectionRegistry(r => {
      const registry = new Map(r);
      if (selected) {
        registry.set(index, workItemsPair);
      } else {
        registry.set(index, null);
      }
      return registry;
    });
  };

  const isPairSelected = (pairToCheck) => {
    for (let pair of selectionRegistry.values()) {
      if (pair && pairsEqual(pair, pairToCheck)) {
        return true;
      }
    }
    return false;
  };

  const pairsEqual = (pair1, pair2) => {
    return itemsEqual(pair1.leftWorkItem, pair2.leftWorkItem) && itemsEqual(pair1.rightWorkItem, pair2.rightWorkItem);
  };

  const itemsEqual = (item1, item2) => {
    return (!item1 && !item2) || (item1 && item2 && item1.outlineNumber === item2.outlineNumber && item2.movedOutlineNumber === item2.movedOutlineNumber);
  };

  const isIndexSelected = (indexToCheck) => {
    return !!selectionRegistry.get(indexToCheck);
  };

  const getSelectedValues = () => {
    return Array.from(selectionRegistry.values()).filter(value => value);
  };

  const getSelectedIndexes = () => {
    const selectedIndexes = [];
    for (let index of selectionRegistry.keys()) {
      if (selectionRegistry.get(index)) {
        selectedIndexes.push(index);
      }
    }
    return selectedIndexes;
  };

  const resetSelection = () => {
    setSelectionRegistry(r => {
      const registry = new Map(r);
      registry.forEach((value, index) => {
        registry.set(index, null);
      });
      return registry;
    });
  };

  const setAndApplySelectAll = (selectAll) => {
    setSelectAll(selectAll);
    setSelectAllTrigger(selectAllTrigger === Number.MAX_SAFE_INTEGER ? 1 : selectAllTrigger + 1);
  };

  return { selectionRegistry, resetRegistryEntry, selectionCount, setPairSelected, isPairSelected, isIndexSelected, resetSelection,
    getSelectedValues, getSelectedIndexes, selectAll, setSelectAll, selectAllTrigger, setAndApplySelectAll };
}
