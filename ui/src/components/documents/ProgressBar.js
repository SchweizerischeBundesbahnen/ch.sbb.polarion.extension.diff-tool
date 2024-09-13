import {useEffect, useState} from "react";

export default function ProgressBar({loadingContext}) {

  const [display, setDisplay] = useState('flex');

  useEffect(() => {
    if (loadingContext.diffsLoadingProgress === 100 && !loadingContext.diffsLoadingErrors) {
      setTimeout(() => setDisplay('none'), 1000);
    } else {
      setDisplay('flex');
    }
  }, [loadingContext.diffsLoadingProgress, loadingContext.diffsLoadingErrors]);

  return (
      <div className="row g-0">
        <div className="progress" role="progressbar" aria-label="Default striped example" aria-valuenow="10" aria-valuemin="0" aria-valuemax="100" style={{
          height: "4em",
          display: display
        }}>
          <div className={`progress-bar progress-bar-striped progress-bar-animated ${loadingContext.diffsLoadingErrors ? 'bg-danger' : ''}`} style={{
            width: `${loadingContext.diffsLoadingProgress}%`,
            flexDirection: "row",
            flexWrap: "wrap",
            alignContent: "center",
            alignItems: "center",
            columnGap: "20px"
          }}>
            <span>
              {!loadingContext.diffsLoadingErrors
                  ? (loadingContext.diffsLoadingProgress < 100 ? loadingContext.diffsLoadingProgress + '%' : 'Data loaded successfully')
                  : 'Failure occurred during diff data load. Process is stopped. Either retry or contact system administrator'}
            </span>
            <button className="btn btn-secondary btn-sm" style={{
              display: loadingContext.diffsLoadingErrors ? "flex" : "none"
            }} onClick={() => location.reload()}>
              Retry
            </button>
          </div>
        </div>
      </div>
  );
}