export default function Modal({title, cancelButtonTitle, actionButtonTitle, actionButtonHandler, onClose, visible, setVisible, className, children}) {

  const closeModal = () => {
    setVisible(false);
    onClose ? onClose() : null;
  }

  return (
      <div className={`modal fade ${className}`} tabIndex="-1" style={{
        display: visible ? 'flex' : 'none',
        opacity: visible ? 1 : 0,
        backgroundColor: 'rgba(255,255,255,0.7)',
        alignItems: 'center'
      }} onClick={(event) => event.target === event.currentTarget && closeModal()}>
        <div className="modal-dialog modal-dialog-scrollable">
          <div className="modal-content">
            <div className="modal-header">
              <h5 className="modal-title">{title}</h5>
              <button type="button" className="btn-close" aria-label="Close" onClick={closeModal}></button>
            </div>
            <div className="modal-body">
              {children}
            </div>
            <div className="modal-footer">
              <button type="button" className="btn btn-sm btn-light" onClick={closeModal}>{cancelButtonTitle}</button>
              {actionButtonTitle && actionButtonHandler
                  && <button type="button" className="btn btn-sm btn-secondary" onClick={() => {closeModal(); actionButtonHandler();}}>{actionButtonTitle}</button>
              }
            </div>
          </div>
        </div>
      </div>
  );
}
