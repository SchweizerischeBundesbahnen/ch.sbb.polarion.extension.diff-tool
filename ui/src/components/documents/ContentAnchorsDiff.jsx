import PairHeader from "@/components/PairHeader";
import {useEffect, useState} from "react";
import ContentBlocksDiff from "@/components/documents/ContentBlocksDiff";

export const CONTENT_ABOVE = "ABOVE";
export const CONTENT_BELOW = "BELOW";

export default function ContentAnchorsDiff({ anchorsPair, currentIndex, mergingContext }) {
  const [asHeaderInDocument] = useState(anchorsPair.leftAnchor ?
      anchorsPair.leftAnchor.outlineNumber && !anchorsPair.leftAnchor.outlineNumber.includes('-') :
      anchorsPair.rightAnchor.outlineNumber && !anchorsPair.rightAnchor.outlineNumber.includes('-'));

  const [leftDiffAbove] = useState(anchorsPair.leftAnchor && anchorsPair.leftAnchor.diffAbove);
  const [rightDiffAbove] = useState(anchorsPair.rightAnchor && anchorsPair.rightAnchor.diffAbove);
  const [leftDiffBelow] = useState(anchorsPair.leftAnchor && anchorsPair.leftAnchor.diffBelow);
  const [rightDiffBelow] = useState(anchorsPair.rightAnchor && anchorsPair.rightAnchor.diffBelow);

  const [aboveDiffIndex] = useState(currentIndex + CONTENT_ABOVE);
  const [aboveDiffSelected, setAboveDiffSelected] = useState(mergingContext.isIndexSelected(aboveDiffIndex));
  const [belowDiffIndex] = useState(currentIndex + CONTENT_BELOW);
  const [belowDiffSelected, setBelowDiffSelected] = useState(mergingContext.isIndexSelected(belowDiffIndex));

  useEffect(() => {
    if (leftDiffAbove || rightDiffAbove) {
      mergingContext.setPairSelected(aboveDiffIndex, anchorsPair, false);
    }
    if (leftDiffBelow || rightDiffBelow) {
      mergingContext.setPairSelected(belowDiffIndex, anchorsPair, false);
    }
  }, [leftDiffAbove, rightDiffAbove, leftDiffBelow, rightDiffBelow]);

  useEffect(() => {
    setAboveDiffSelected(mergingContext.isIndexSelected(aboveDiffIndex));
    setBelowDiffSelected(mergingContext.isIndexSelected(belowDiffIndex));
  }, [mergingContext.selectionRegistry]);

  const anchorId = (anchorsPair.leftAnchor || anchorsPair.rightAnchor)?.id;

  const changeDiffSelected = (contentPosition) => {
    const index = contentPosition === CONTENT_ABOVE ? aboveDiffIndex : belowDiffIndex;
    const newValue = !(contentPosition === CONTENT_ABOVE ? aboveDiffSelected : belowDiffSelected);
    mergingContext.setPairSelected(index, anchorsPair, newValue);
  }

  return <div className={`wi-diff container-fluid g-0`} data-testid={`${anchorsPair.leftAnchor ? anchorsPair.leftAnchor.id : "NONE"}_${anchorsPair.rightAnchor ? anchorsPair.rightAnchor.id : "NONE"}`}>
    {(leftDiffAbove || rightDiffAbove)
        && <ContentBlocksDiff label={`Select the document text above ${anchorId} for merge`} oldValue={leftDiffAbove} newValue={rightDiffAbove} selected={aboveDiffSelected} selectedCallback={() => changeDiffSelected(CONTENT_ABOVE)} />
    }
    <PairHeader leftWorkItem={anchorsPair.leftAnchor} rightWorkItem={anchorsPair.rightAnchor} asHeaderInDocument={asHeaderInDocument}
                style={{backgroundColor: "#f6f6f6", display: 'flex'}} />
    {(leftDiffBelow || rightDiffBelow)
        && <ContentBlocksDiff label={`Select the document text below ${anchorId} for merge`} oldValue={leftDiffBelow} newValue={rightDiffBelow} selected={belowDiffSelected} selectedCallback={() => changeDiffSelected(CONTENT_BELOW)}/>
    }
  </div>
}
