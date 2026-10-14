import {useContext} from "react";
import AppContext from "@/components/AppContext";
import {isMovedPlaceholder, LEFT, RIGHT, UnifiedWorkItemHeader, WorkItemHeader} from "@/components/WorkItemHeader";
import * as DiffViewModes from "@/DiffViewModes";

/**
 * The header row of a work item pair, shared by the documents, work items and content anchors diffs.
 * Side by side, the status button sits between the two sides. Unified, both sides share one row and the
 * status button sits next to the merge ticker. The placeholder of a moved pair has neither, only the
 * empty ticker column: its other position carries them, along with the diff.
 */
export default function PairHeader({leftWorkItem, rightWorkItem, asHeaderInDocument, ticker, statusButton, style, testId}) {
  const context = useContext(AppContext);

  if (context.state.diffViewMode === DiffViewModes.UNIFIED) {
    const placeholder = isMovedPlaceholder(rightWorkItem);
    return (
        <div style={style} className="header row g-0 unified" data-testid={testId}>
          {placeholder ? <span className="merge-ticker" aria-hidden="true"/> : ticker}
          {!placeholder && statusButton}
          <UnifiedWorkItemHeader leftWorkItem={leftWorkItem} rightWorkItem={rightWorkItem} asHeaderInDocument={asHeaderInDocument} />
        </div>
    );
  }

  return (
      <div style={style} className="header row g-0" data-testid={testId}>
        {ticker}
        <WorkItemHeader workItem={leftWorkItem} asHeaderInDocument={asHeaderInDocument}
                        movedOutlineNumber={rightWorkItem?.movedOutlineNumber}
                        moveDirection={rightWorkItem?.moveDirection} side={LEFT} />
        {statusButton}
        <WorkItemHeader workItem={rightWorkItem} asHeaderInDocument={asHeaderInDocument}
                        movedOutlineNumber={leftWorkItem?.movedOutlineNumber}
                        moveDirection={leftWorkItem?.moveDirection} side={RIGHT} />
      </div>
  );
}
