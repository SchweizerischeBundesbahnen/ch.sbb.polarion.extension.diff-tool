import {DIFF_SIDES} from "@/components/diff/DiffLeaf";

const SIDE_LABELS = {
  [DIFF_SIDES.LEFT]: "Source",
  [DIFF_SIDES.RIGHT]: "Target"
};

function ComparisonSide({side, lines, linesCount}) {
  return (
      <div className={`comparison-side ${side.toLowerCase()}`} data-testid={`${side}-comparison-side`}>
        {lines.map((line, index) => (
            // A side with fewer lines than the other (a missing counterpart) lets its last line take the rest.
            <div key={index} className="comparison-line"
                 style={index === lines.length - 1 ? {"--comparison-line-span": linesCount - index} : undefined}>
              {index === 0 && <span className="side-label">{SIDE_LABELS[side]}</span>}
              {line}
            </div>
        ))}
      </div>
  );
}

/**
 * What is compared with what: the source and the target, each as the same list of lines (the project or the
 * collection, then the document). Side by side, the two sides are columns and their lines are rows aligned
 * across them. Unified, the sides are rows and their lines are columns aligned across them.
 */
export default function ComparisonHeader({source, target, swapButton}) {
  const linesCount = Math.max(source.length, target.length);
  return (
      <div className="comparison-header" style={{"--comparison-lines": linesCount}}>
        <ComparisonSide side={DIFF_SIDES.LEFT} lines={source} linesCount={linesCount} />
        {swapButton}
        <ComparisonSide side={DIFF_SIDES.RIGHT} lines={target} linesCount={linesCount} />
      </div>
  );
}
