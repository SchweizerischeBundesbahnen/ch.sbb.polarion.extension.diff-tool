import {useState} from "react";
import {faArrowDownLong, faArrowRightLong, faArrowUpLong} from "@fortawesome/free-solid-svg-icons";
import {FontAwesomeIcon} from "@fortawesome/react-fontawesome";
import MovedItem, {MOVED_UP} from "@/components/documents/workitems/MovedItem";

export const LEFT = "left";
export const RIGHT = "right";

/** The target of the "moved to" and "moved from" links: one anchor per side and outline number. */
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

/**
 * Size attributes of an icon, in pixels at its usual font size. A browser sizes the icon by CSS and ignores
 * them, but the PDF export renders with WeasyPrint, which sizes an inline SVG only by them, or else by its
 * view box: hundreds of pixels for a Font Awesome icon.
 */
const iconSize = (icon, height) => ({height: height, width: Math.round(height * icon.icon[0] / icon.icon[1])});

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
 * A moved pair of the unified view is shown with its diff at its target position, since the unified diff
 * shows the target as the new state. Its source position is a placeholder.
 */
export const isMovedPlaceholder = (leftWorkItem) => !!leftWorkItem?.movedOutlineNumber;

/**
 * The one-row header of a pair in the unified view. It names the work item once, by its target title
 * (or the source one, if the target is missing), and links both work items: source, then target.
 * A moved pair links its two positions to each other. The placeholder at the source position is one
 * slim line, no heading, so the document outline reads as the target.
 */
export function UnifiedWorkItemHeader({leftWorkItem, rightWorkItem, asHeaderInDocument}) {
  const placeholder = isMovedPlaceholder(leftWorkItem);
  const movedHere = !!rightWorkItem?.movedOutlineNumber;
  // One anchor per position, as the "moved to" and "moved from" links of both views point to them.
  const [leftId] = useState(movedHere ? null : anchorId(leftWorkItem, LEFT));
  const [rightId] = useState(placeholder ? null : anchorId(rightWorkItem, RIGHT));

  const workItem = placeholder ? leftWorkItem : (rightWorkItem || leftWorkItem);
  const asHeading = asHeaderInDocument && !placeholder;
  const headerLevel = headerLevelOf(workItem, asHeading);
  const badgeFontSize = badgeFontSizeOf(headerLevel, asHeading);

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

  const movedIcon = (movedWorkItem) => movedWorkItem.moveDirection === MOVED_UP ? faArrowUpLong : faArrowDownLong;

  const movedTag = (movedWorkItem, side, label) => (
      <a className="wi-tag moved" href={`#${side}${movedWorkItem.movedOutlineNumber}`} title="Go to the other position of this WorkItem">
        <FontAwesomeIcon icon={movedIcon(movedWorkItem)} {...iconSize(movedIcon(movedWorkItem), 11)}/> {label} {movedWorkItem.movedOutlineNumber}
      </a>
  );

  const title = workItem && asHeaderInDocument ? `${workItem.outlineNumber} ${workItem.title}` : workItem?.title;

  return (
      <div style={{
        paddingLeft: asHeaderInDocument ? "4rem" : "7rem",
        fontWeight: asHeading ? "600" : "300",
        fontSize: placeholder ? "0.85em" : fontSizeOf(headerLevel, asHeading)
      }} {...headingPropsOf(headerLevel)} className={`wi-header unified col collapsed-border ${placeholder ? "moved-placeholder" : ""}`}
           data-testid="unified-wi">
        {leftId && <span id={leftId} className="wi-anchor"/>}
        {rightId && <span id={rightId} className="wi-anchor"/>}
        {workItem && <span className="wi-title">{title}</span>}
        {leftWorkItem && sideBadge(leftWorkItem, "Source")}
        {leftWorkItem && rightWorkItem && <FontAwesomeIcon icon={faArrowRightLong} className="wi-arrow" {...iconSize(faArrowRightLong, 12)}/>}
        {rightWorkItem && sideBadge(rightWorkItem, "Target")}
        {leftWorkItem && !rightWorkItem && <span className="wi-tag only-in-source">Only in source</span>}
        {!leftWorkItem && rightWorkItem && <span className="wi-tag only-in-target">Only in target</span>}
        {placeholder && movedTag(leftWorkItem, RIGHT, "Moved to")}
        {movedHere && movedTag(rightWorkItem, LEFT, "Moved from")}
        {!workItem && <span style={{fontSize: ".75em"}}>&ndash;</span>}
      </div>
  );
}
