import {useContext, useEffect, useRef, useState} from "react";
import FieldsDiff from "@/components/diff/FieldsDiff";
import AppContext from "@/components/AppContext";

export default function DiffContent({workItemsPair, pairSelected, pairSelectionTrigger, pairSelectedCallback, diffs, expanded}) {
  const context = useContext(AppContext);
  const ref = useRef(null);
  const [height, setHeight] = useState(0);

  useEffect(() => {
    const timer = setTimeout(() => {
      if (ref.current) setHeight(ref.current.height);
    }, 1000);
    return () => clearTimeout(timer);
  }, [ref, diffs]);

  useEffect(() => {
    if (ref.current) setHeight(ref.current.height);
  }, [context.state.showOutlineNumbersDiff]);

  return <div style={{
    transition: ".6s",
    height: expanded ? height : 0
  }} className={`content row g-0`} ref={ref} data-testid="diff-content">

    {diffs.map((diff, index) => (
        <FieldsDiff key={index} workItemsPair={workItemsPair} pairSelected={pairSelected}
                    pairSelectionTrigger={pairSelectionTrigger} pairSelectedCallback={pairSelectedCallback}
                    fieldId={diff.id} fieldName={diff.name} oldValue={diff.oldValue} newValue={diff.newValue} issues={diff.issues} />
    ))}

  </div>;
}
