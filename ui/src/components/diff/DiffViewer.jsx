import {useContext} from "react";
import {DIFF_SIDES, DiffLeaf} from "@/components/diff/DiffLeaf";
import AppContext from "@/components/AppContext";
import * as DiffViewModes from "@/DiffViewModes";

/**
 * Both diff HTMLs mark the source content as removed and the target content as added. The left one hides
 * the added parts, the right one the removed parts, and the unified view shows one of them with both.
 * The right one is the diff the server computes source to target, so the unified view takes it. Only when
 * the target is missing is it empty, and the left one then holds the whole source content as removed.
 */
export default function DiffViewer({oldValue, newValue, unifiedValue, style, children}) {
  const context = useContext(AppContext);

  return (
      <div className="diff-viewer" style={style}>
        {children}
        {context.state.diffViewMode === DiffViewModes.UNIFIED
            ? <DiffLeaf htmlDiff={unifiedValue ?? (newValue || oldValue)} diffSide={DIFF_SIDES.UNIFIED} />
            : <>
              <DiffLeaf htmlDiff={oldValue} diffSide={DIFF_SIDES.LEFT} />
              <DiffLeaf htmlDiff={newValue} diffSide={DIFF_SIDES.RIGHT} />
            </>}
      </div>
  );
}
