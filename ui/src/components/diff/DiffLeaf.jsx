import {useEffect, useRef} from "react";

const DIFF_SIDES = {
  LEFT: "LEFT",
  RIGHT: "RIGHT",
  UNIFIED: "UNIFIED" // Both sides in one leaf: the unified diff view
}

function DiffLeaf({htmlDiff, diffSide}) {
  const ref = useRef(null);

  useEffect(() => {
    if (htmlDiff) {
      ref.current.innerHTML = htmlDiff;
    }
  }, [htmlDiff]);

  return (
      <div className={`diff-leaf ${diffSide.toLowerCase()}`} ref={ref}>

        {/* will be filled in useEffect hook */}

      </div>
  );
}

export {DIFF_SIDES, DiffLeaf};
