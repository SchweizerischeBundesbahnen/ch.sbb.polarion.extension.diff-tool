import {useContext} from "react";
import AppContext from "@/components/AppContext";
import useSwapColors from "@/utils/useSwapColors";

/**
 * The class list of `#diff-body`, the same for every viewer. The view mode class only changes the markup's
 * look, so switching the view keeps the loaded diffs and the merge selection.
 */
export default function useDiffBodyClassName() {
  const context = useContext(AppContext);
  const [swapColors] = useSwapColors();

  return [
    "doc-diff",
    `diff-view-${context.state.diffViewMode}`,
    swapColors ? "swap-colors" : "",
    context.state.controlPaneExpanded ? "control-pane-expanded" : ""
  ].filter(Boolean).join(" ");
}
