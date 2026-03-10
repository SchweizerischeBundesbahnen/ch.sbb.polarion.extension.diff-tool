import {useContext, useEffect, useRef, useState} from "react";
import AppContext from "@/components/AppContext";
import DiffViewer from "@/components/diff/DiffViewer";
import {FloatingArrow, arrow, useFloating, offset, useInteractions, useHover, flip} from '@floating-ui/react';
import FieldMergeTicker from "@/components/merge/FieldMergeTicker";

const ARROW_HEIGHT = 7;
const GAP = 2;

export default function FieldsDiff({workItemsPair, pairSelected, pairSelectedCallback, fieldId, fieldName, oldValue, newValue, issues}) {
  const context = useContext(AppContext);

  const [display, setDisplay] = useState(true);
  const [popupVisible, setPopupVisible] = useState(false);
  const [selected, setSelected] = useState(pairSelected);

  const arrowRef = useRef(null);
  const {refs, floatingStyles, context: floatingContext} = useFloating({
    placement: 'bottom',
    strategy: 'absolute',
    open: popupVisible,
    onOpenChange: setPopupVisible,
    middleware: [
      offset(ARROW_HEIGHT + GAP),
      arrow({
        element: arrowRef,
      }),
      flip()
    ],
  });
  const hover = useHover(floatingContext);
  const {getReferenceProps, getFloatingProps} = useInteractions([
    hover,
  ]);

  useEffect(() => {
    setDisplay(fieldId !== "outlineNumber" || context.state.showOutlineNumbersDiff);
  }, [fieldId, context.state.showOutlineNumbersDiff]);

  useEffect(() => {
    setSelected(pairSelected);
  }, [pairSelected])

  useEffect(() => {
    if (workItemsPair) {
      const fieldToMerge = workItemsPair.fieldsToMerge.find(fieldToMerge => fieldToMerge.id === fieldId);
      if (fieldToMerge) {
        fieldToMerge.selected = selected;
      }

      if (selected) {
        pairSelectedCallback(true, true); // Select pair as soon as any of its field is selected
      } else {
        const anyFieldStillSelected = workItemsPair.fieldsToMerge.find(fieldToMerge => fieldToMerge.selected === true);
        if (!anyFieldStillSelected) {
          pairSelectedCallback(false, true); // Unselect pair if all its field are unselected
        }
      }
    }
  }, [selected])

  const changeSelected = () => {
    setSelected(!selected);
  }

  return (
      <div className={`container-fluid g-0 diff-wrapper ${selected ? "selected" : ""} ${display ? "d-block" : "d-none"}`} data-testid={fieldName}>
        {context.state.individualFieldsSelection && <FieldMergeTicker selected={selected} changeSelectionCallback={changeSelected} />}
        <div className={`diff-header ${issues && issues.length > 0 ? "diff-issues" : ""}`} ref={refs.setReference} {...getReferenceProps()} >
          {fieldName}
        </div>
        {popupVisible && issues && issues.length > 0 &&
            <div ref={refs.setFloating} style={floatingStyles} className={'tooltip-container diff-issues'} {...getFloatingProps()}>
              <FloatingArrow ref={arrowRef} context={floatingContext} fill="#c61010" />
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
