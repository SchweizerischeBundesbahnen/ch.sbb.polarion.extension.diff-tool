import {useContext, useEffect, useRef, useState} from "react";
import FieldsDiff from "@/components/documents/workitems/fields/FieldsDiff";
import AppContext from "@/components/AppContext";

export default function DiffContent({diffs, expanded}) {
  const context = useContext(AppContext);
  const ref = useRef(null);
  const [height, setHeight] = useState(0);

  useEffect(() => {
    setTimeout(() => setHeight(ref.current.height), 1000);
  }, [ref, diffs]);

  useEffect(() => {
    setHeight(ref.current.height);
  }, [context.state.showOutlineNumbersDiff]);

  return <div style={{
    transition: ".6s",
    height: expanded ? height : 0
  }} className={`content row g-0`} ref={ref}>

    {diffs.map((diff, index) => (
        <FieldsDiff key={index} fieldId={diff.id} fieldName={diff.name} oldValue={diff.oldValue} newValue={diff.newValue} issues={diff.issues} />
    ))}

  </div>;
}