import {DIFF_SIDES} from "@/components/diff/DiffLeaf";

const SIDE_LABELS = {
  [DIFF_SIDES.LEFT]: "Source",
  [DIFF_SIDES.RIGHT]: "Target"
};

function ComparisonSide({side, lines, linesCount}) {
  return (
      <div className={`comparison-side ${side.toLowerCase()}`} data-testid={`${side}-comparison-side`}>
        <div className="comparison-label"><span className="side-label">{SIDE_LABELS[side]}</span></div>
        {lines.map((line, index) => {
          const lastLine = index === lines.length - 1;
          // Side by side, the last line (the document) takes a row of its own, under the other lines.
          return (
              // A side with fewer lines than the other (a missing counterpart) lets its last line take the rest.
              <div key={index} className={`comparison-line ${lastLine && linesCount > 1 ? "last" : ""}`}
                   style={lastLine ? {"--comparison-line-span": linesCount - index} : undefined}>
                {line}
              </div>
          );
        })}
      </div>
  );
}

/**
 * What is compared with what: the source and the target, each as the same list of lines (the project or the
 * collection, then the document). Side by side, the two sides are columns of three rows: the label, the lines
 * but the last one, and the last one. Unified, the sides are rows and the label and the lines are columns
 * aligned across them.
 */
export default function ComparisonHeader({source, target, swapButton}) {
  const linesCount = Math.max(source.length, target.length);
  return (
      <div className="comparison-header" style={{
        "--comparison-lines": linesCount,
        "--comparison-columns": linesCount + 1,
        "--comparison-rows": linesCount > 1 ? 3 : 2
      }}>
        <ComparisonSide side={DIFF_SIDES.LEFT} lines={source} linesCount={linesCount} />
        {swapButton}
        <ComparisonSide side={DIFF_SIDES.RIGHT} lines={target} linesCount={linesCount} />
      </div>
  );
}
