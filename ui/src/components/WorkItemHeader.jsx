import {useState} from "react";
import {faArrowRightLong} from "@fortawesome/free-solid-svg-icons";
import {FontAwesomeIcon} from "@fortawesome/react-fontawesome";
import MovedItem from "@/components/documents/workitems/MovedItem";

export const LEFT = "left";
export const RIGHT = "right";

/** The target of the "moved to" links: one anchor per side and outline number. */
const anchorId = (workItem, side) => workItem && workItem.outlineNumber ? `${side}${workItem.outlineNumber}` : null;

// workItem.outlineNumber may become empty on header deletion
const headerLevelOf = (workItem, asHeaderInDocument) => workItem && workItem.outlineNumber && asHeaderInDocument ? workItem.outlineNumber.split(".").length : 0;

const fontSizeOf = (headerLevel, asHeaderInDocument) => asHeaderInDocument
    ? (headerLevel === 1 ? "1.8em"
        : (headerLevel === 2
            ? "1.6em"
            : (headerLevel === 3 ? "1.4em" : "1.2em")
        )
    ) : "1em";

const badgeFontSizeOf = (headerLevel, asHeaderInDocument) => asHeaderInDocument
    ? (headerLevel === 1
            ? ".45em"
            : (headerLevel === 2
                ? ".5em"
                : ".55em"
            )
    ) : ".75em";

// Capped at 6 like <h1>-<h6>: screen readers do not agree on a level beyond that.
const headingPropsOf = (headerLevel) => headerLevel > 0 ? {role: "heading", "aria-level": Math.min(headerLevel, 6)} : {};

const workItemUrl = (workItem) => `/polarion/#/project/${workItem.projectId}/workitem?id=${workItem.id}${workItem.revision ? "&revision=" + workItem.revision : ""}`;

export function WorkItemHeader({workItem, asHeaderInDocument, movedOutlineNumber, moveDirection, side}) {
  const [id] = useState(movedOutlineNumber ? null : anchorId(workItem, side));

  const headerLevel = headerLevelOf(workItem, asHeaderInDocument);
  const fontSize = fontSizeOf(headerLevel, asHeaderInDocument);
  const badgeFontSize = badgeFontSizeOf(headerLevel, asHeaderInDocument);
  const headingProps = movedOutlineNumber ? {} : headingPropsOf(headerLevel);

  const badgeContent = () => {
    return (
        <a href={workItemUrl(workItem)}
           target="_blank"
           rel="noopener noreferrer"
           className="badge wi-badge"
           style={{
              fontSize: badgeFontSize,
           }}>
          {workItem.id}{asHeaderInDocument ? "" : " - " + workItem.title}
        </a>
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

  return (
      <div style={{
        paddingLeft: asHeaderInDocument ? "2rem" : "5rem",
        fontWeight: asHeaderInDocument ? "600" : "300",
        fontSize: fontSize
      }} id={id} {...headingProps} className={`wi-header col collapsed-border ${workItem && movedOutlineNumber ? "moved" : ""} ${side}`} data-testid={`${side}-wi`}>
        {workItem && !movedOutlineNumber && asHeaderInDocument && (workItem.outlineNumber + " " + workItem.title)} {workItem && !movedOutlineNumber && badgeContent()}
        {workItem && !movedOutlineNumber && workItem.referenced && referencedMarker(workItem.externalProjectWorkItem)}
        {workItem && !movedOutlineNumber && workItem.referenced && workItem.revision && !asHeaderInDocument && revisionLabel(workItem.revision)}
        {workItem && movedOutlineNumber && <MovedItem side={side} movedOutlineNumber={movedOutlineNumber} moveDirection={moveDirection} /> }
        {!workItem && <span style={{fontSize: ".75em"}}>&ndash;</span>}
      </div>
  );
}

/**
 * The one-row header of a pair in the unified view. It names the work item once, by its source title
 * (or the target one, if the source is missing), and links both work items: source, then target.
 * A row which only marks where a moved work item was shows the work item of its other side and the link
 * to that position.
 */
export function UnifiedWorkItemHeader({leftWorkItem, rightWorkItem, asHeaderInDocument}) {
  // A side is a placeholder when the work item of the other side moved away from this position.
  const leftShown = !!leftWorkItem && !rightWorkItem?.movedOutlineNumber;
  const rightShown = !!rightWorkItem && !leftWorkItem?.movedOutlineNumber;
  const [leftId] = useState(leftShown ? anchorId(leftWorkItem, LEFT) : null);
  const [rightId] = useState(rightShown ? anchorId(rightWorkItem, RIGHT) : null);

  const workItem = leftShown ? leftWorkItem : (rightShown ? rightWorkItem : null);
  const headerLevel = headerLevelOf(workItem, asHeaderInDocument);
  const badgeFontSize = badgeFontSizeOf(headerLevel, asHeaderInDocument);

  const sideBadge = (sideWorkItem, sideName) => (
      <span className="wi-side">
        <a href={workItemUrl(sideWorkItem)} target="_blank" rel="noopener noreferrer" className="badge wi-badge"
           style={{fontSize: badgeFontSize}} aria-label={`${sideName} WorkItem ${sideWorkItem.id}`}
           title={`${sideName} WorkItem`}>
          {sideWorkItem.id}
        </a>
        {sideWorkItem.referenced && <span className="wi-tag" title="Referenced WorkItem">
          Referenced{sideWorkItem.revision && !asHeaderInDocument ? `, rev.\u00a0${sideWorkItem.revision}` : ""}
        </span>}
        {sideWorkItem.externalProjectWorkItem && <span className="wi-tag external-project">references different project</span>}
      </span>
  );

  const movedLink = (movedOutlineNumber, side) => (
      <a className="wi-tag moved" href={`#${side}${movedOutlineNumber}`}>moved to {movedOutlineNumber}</a>
  );

  return (
      <div style={{
        paddingLeft: asHeaderInDocument ? "4rem" : "7rem",
        fontWeight: asHeaderInDocument ? "600" : "300",
        fontSize: fontSizeOf(headerLevel, asHeaderInDocument)
      }} {...headingPropsOf(headerLevel)} className="wi-header unified col collapsed-border" data-testid="unified-wi">
        {leftId && <span id={leftId} className="wi-anchor"/>}
        {rightId && <span id={rightId} className="wi-anchor"/>}
        {workItem && <span className="wi-title">{asHeaderInDocument ? `${workItem.outlineNumber} ${workItem.title}` : workItem.title}</span>}
        {leftShown && sideBadge(leftWorkItem, "Source")}
        {leftShown && rightShown && <FontAwesomeIcon icon={faArrowRightLong} className="wi-arrow"/>}
        {rightShown && sideBadge(rightWorkItem, "Target")}
        {leftWorkItem && !rightWorkItem && <span className="wi-tag only-in-source">Only in source</span>}
        {!leftWorkItem && rightWorkItem && <span className="wi-tag only-in-target">Only in target</span>}
        {rightWorkItem?.movedOutlineNumber && movedLink(rightWorkItem.movedOutlineNumber, LEFT)}
        {leftWorkItem?.movedOutlineNumber && movedLink(leftWorkItem.movedOutlineNumber, RIGHT)}
        {!workItem && <span style={{fontSize: ".75em"}}>&ndash;</span>}
      </div>
  );
}
