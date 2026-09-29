import {useEffect, useId, useRef} from "react";

const FOCUSABLE = 'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

export default function Modal({title, cancelButtonTitle, actionButtonTitle, actionButtonHandler, onClose, visible, setVisible, className, children, testId}) {

  const closeButtonRef = useRef(null);
  const dialogRef = useRef(null);
  const titleId = useId();

  // Escape is handled by the modal's own onKeyDown, which the button that opened it, outside the modal, never reaches.
  // The focus goes back to that button on close: the hidden close button would drop it to <body>.
  useEffect(() => {
    if (!visible) {
      return undefined;
    }
    const opener = document.activeElement;
    closeButtonRef.current?.focus();
    return () => opener?.focus?.();
  }, [visible]);

  const closeModal = () => {
    setVisible(false);
    onClose ? onClose() : null;
  }

  // aria-modal tells assistive technology the page behind is unavailable, so Tab must not reach it either.
  const keepFocusInside = (event) => {
    const focusable = [...dialogRef.current.querySelectorAll(FOCUSABLE)].filter((element) => element.getClientRects().length > 0);
    if (focusable.length === 0) {
      return;
    }
    const first = focusable[0];
    const last = focusable[focusable.length - 1];
    const inside = dialogRef.current.contains(document.activeElement);
    if (event.shiftKey && (document.activeElement === first || !inside)) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && (document.activeElement === last || !inside)) {
      event.preventDefault();
      first.focus();
    }
  };

  const onKeyDown = (event) => {
    if (event.key === 'Escape' && !event.defaultPrevented) {
      closeModal();
    } else if (event.key === 'Tab') {
      keepFocusInside(event);
    }
  };

  return (
      <div className={`modal fade ${className}`} data-testid={testId} tabIndex="-1" role="presentation" style={{
        display: visible ? 'flex' : 'none',
        opacity: visible ? 1 : 0,
        backgroundColor: 'rgba(255,255,255,0.7)',
        alignItems: 'center'
      }} onClick={(event) => event.target === event.currentTarget && closeModal()}
         onKeyDown={onKeyDown}>
        <div className="modal-dialog modal-dialog-scrollable" ref={dialogRef} role="dialog" aria-modal="true" aria-labelledby={titleId}>
          <div className="modal-content">
            <div className="modal-header">
              <h5 className="modal-title" id={titleId}>{title}</h5>
              <button type="button" className="btn-close" aria-label="Close" ref={closeButtonRef} onClick={closeModal}></button>
            </div>
            <div className="modal-body">
              {children}
            </div>
            <div className="modal-footer">
              <button type="button" className="btn btn-sm btn-light" data-testid={`${testId}-cancel-button`} onClick={closeModal}>{cancelButtonTitle}</button>
              {actionButtonTitle && actionButtonHandler
                  && <button type="button" className="btn btn-sm btn-secondary"
                             data-testid={`${testId}-action-button`} onClick={() => {closeModal(); actionButtonHandler();}}>
                    {actionButtonTitle}
                  </button>
              }
            </div>
          </div>
        </div>
      </div>
  );
}
