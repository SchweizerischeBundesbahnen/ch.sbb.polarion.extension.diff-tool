import {useContext, useEffect, useState} from "react";
import AppContext from "@/components/AppContext";
import DiffViewer from "@/components/diff/DiffViewer";
import { usePopperTooltip } from 'react-popper-tooltip';
import 'react-popper-tooltip/dist/styles.css';

export default function FieldsDiff({fieldId, fieldName, oldValue, newValue, issues, fieldsDiff}) {
  const context = useContext(AppContext);

  const [display, setDisplay] = useState(true);

  const {
    getArrowProps,
    getTooltipProps,
    setTooltipRef,
    setTriggerRef,
    visible,
  } = usePopperTooltip();

  useEffect(() => {
    setDisplay(fieldId !== "outlineNumber" || context.state.showOutlineNumbersDiff);
  }, [fieldId, context.state.showOutlineNumbersDiff]);

  return (
      <div className={`container-fluid g-0 ${fieldsDiff ? "fields-diff-wrapper" : "diff-wrapper"} ${display ? "d-block" : "d-none"}`}>
        <div className={`diff-header ${issues && issues.length > 0 ? "diff-issues" : ""}`} ref={setTriggerRef}>
          {fieldName}
        </div>
        {visible && issues && issues.length > 0 &&
            <div ref={setTooltipRef} {...getTooltipProps({ className: 'tooltip-container diff-issues' })}>
              <div {...getArrowProps({ className: 'tooltip-arrow diff-issues' })} />
              <ul>
                {issues.map((issue, index) => (
                    <li key={index}>
                      {issue}
                    </li>
                ))}
              </ul>
            </div>
        }
        <DiffViewer oldValue={oldValue} newValue={newValue}/>
      </div>
  );
}
