import {useEffect, useState} from "react";

export const LEFT_TO_RIGHT = 0;
export const RIGHT_TO_LEFT = 1;

export function useMergingContext() {
  const [selectionRegistry, setSelectionRegistry] = useState(new Map());
  const [selectionCount, setSelectionCount] = useState(0);

  useEffect(() => {
    let count = 0;
    selectionRegistry.forEach((value) => count += (value ? 1 : 0));
    setSelectionCount(count);
  }, [selectionRegistry]);

  const setPairSelected = (index, workItemsPair, selected) => {
    setSelectionRegistry(r => {
      const registry = new Map(r);
      if (selected) {
        registry.set(index, workItemsPair);
      } else {
        registry.delete(index);
      }
      return registry;
    });
  };

  const isPairSelected = (pairToCheck) => {
    for (let pair of selectionRegistry.values()) {
      if (pairsEqual(pair, pairToCheck)) {
        return true;
      }
    }
    return false;
  }

  const pairsEqual = (pair1, pair2) => {
    return itemsEqual(pair1.leftWorkItem, pair2.leftWorkItem) && itemsEqual(pair1.rightWorkItem, pair2.rightWorkItem);
  }

  const itemsEqual = (item1, item2) => {
    return (!item1 && !item2) || (item1 && item2 && item1.outlineNumber === item2.outlineNumber && item2.movedOutlineNumber === item2.movedOutlineNumber);
  }

  const resetSelection = () => {
    setSelectionRegistry(new Map());
  }

  return { selectionRegistry, selectionCount, setPairSelected, isPairSelected, resetSelection };
}