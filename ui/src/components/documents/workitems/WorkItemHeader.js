import {useEffect, useState} from "react";
import MovedItem from "@/components/documents/workitems/MovedItem";

const TRANSPARENT_BACKGROUND = "rgba(255, 255, 255, 0)";
const SELECTED_BACKGROUND = "rgba(150, 190, 250, 0.2)";

export const LEFT = "left";
export const RIGHT = "right";

export function WorkItemHeader({workItem, asHeaderInDocument, movedOutlineNumber, moveDirection, side, selected}) {
  const [backgroundColor, setBackgroundColor] = useState(TRANSPARENT_BACKGROUND);
  const [id] = useState(workItem && workItem.outlineNumber && !movedOutlineNumber ? `${side}${workItem && workItem.outlineNumber}` : null);

  // workItem.outlineNumber may become empty on header deletion
  const headerLevel = workItem && workItem.outlineNumber && asHeaderInDocument ? workItem.outlineNumber.split(".").length : 0;
  const fontSize = asHeaderInDocument
      ? (headerLevel === 1 ? "1.8em"
          : (headerLevel === 2
              ? "1.6em"
              : (headerLevel === 3 ? "1.4em" : "1.2em")
          )
      ) : "1em";

  const badgeFontSize = asHeaderInDocument
      ? (headerLevel === 1
              ? ".45em"
              : (headerLevel === 2
                  ? ".5em"
                  : ".55em"
              )
      ) : ".75em";

  const badgeContent = () => {
    return (
        <span style={{
          fontSize: badgeFontSize,
        }} className="badge wi-badge" >
          {workItem.id}{asHeaderInDocument ? "" : " - " + workItem.title}
        </span>
    );
  };

  const referencedMarker = (externalProjectWorkItem) => {
    return (
        <span className="referenced-wi" title="Referenced WorkItem">
          R {externalProjectWorkItem ? <span className="external-project-wi">(references different project)</span> : null}
        </span>
    );
  };

  const revisionLabel = (revision) => {
    return (
        <span className="revision">
          rev.&nbsp;{revision}
        </span>
    );
  };

  useEffect(() => {
    setBackgroundColor(selected ? SELECTED_BACKGROUND : TRANSPARENT_BACKGROUND);
  }, [selected]);

  return (
      <div style={{
        paddingLeft: asHeaderInDocument ? "2rem" : "5rem",
        fontWeight: asHeaderInDocument ? "600" : "300",
        fontSize: fontSize,
        backgroundColor: backgroundColor
      }} id={id} className={`wi-header col collapsed-border ${workItem && movedOutlineNumber ? "moved" : ""} ${side}`}>
        {workItem && !movedOutlineNumber && asHeaderInDocument && (workItem.outlineNumber + " " + workItem.title)} {workItem && !movedOutlineNumber && badgeContent()}
        {workItem && !movedOutlineNumber && workItem.referenced && referencedMarker(workItem.externalProjectWorkItem)}
        {workItem && !movedOutlineNumber && workItem.referenced && workItem.revision && !asHeaderInDocument && revisionLabel(workItem.revision)}
        {workItem && movedOutlineNumber && <MovedItem side={side} movedOutlineNumber={movedOutlineNumber} moveDirection={moveDirection} /> }
        {!workItem && <span style={{fontSize: ".75em"}}>&ndash;</span>}
      </div>
  );
}