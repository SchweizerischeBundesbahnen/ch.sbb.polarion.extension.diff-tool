import {useContext, useEffect, useState} from "react";
import {faAnglesLeft, faAnglesRight} from "@fortawesome/free-solid-svg-icons";
import AppContext from "@/components/AppContext";
import {FontAwesomeIcon} from "@fortawesome/react-fontawesome";
import {usePathname, useRouter, useSearchParams} from "next/navigation";
import useRemote from "@/services/useRemote";
import usePdf from "@/services/usePdf";

export default function ControlPane() {
  const context = useContext(AppContext);
  const searchParams = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();
  const remote = useRemote();
  const pdf = usePdf();
  const [projectId] = useState(searchParams.get('sourceProjectId'));
  const [configurations, setConfigurations] = useState([]);
  const [selectedConfiguration, setSelectedConfiguration] = useState(searchParams.get("config") || 'Default');
  const [paperSize, setPaperSize] = useState("A4");
  const [orientation, setOrientation] = useState("landscape");
  const [exportInProgress, setExportInProgress] = useState(false);

  useEffect(() => {
    if (remote && projectId) {
      remote.sendRequest({
        method: "GET",
        url: `/settings/diff/names?scope=project/${projectId}/`,
        contentType: "text/html"
      }).then(response => {
        if (response.ok) {
          return response.text();
        } else {
          throw response.json();
        }
      }).then(data => {
        setConfigurations(JSON.parse(data).map(setting => setting.name));
      }).catch(errorResponse => {
        Promise.resolve(errorResponse).then((error) => alert("Error occurred loading setting names" + (error && error.message ? ": " + error.message : "")));
      });
    }
  }, [projectId]);

  useEffect(() => {
    if (searchParams.get('config') !== selectedConfiguration) {
      const params = [];
      for (const [key, value] of searchParams.entries()) {
        if (key === 'config') {
          params.push(`${key}=${selectedConfiguration}`); // update 'config' parameter
        } else {
          params.push(`${key}=${value}`); // ...and leave other as they are
        }
      }

      router.push(pathname + '?' + params.join('&'));
    }
  }, [selectedConfiguration]);

  const exportToPDF = () => {
    let body = pdf.preProcess(document.documentElement.innerHTML, paperSize, orientation);
    setExportInProgress(true);
    remote.sendRequest({
      method: "POST",
      url: `/conversion/html-to-pdf?orientation=${orientation}&paperSize=${paperSize}`,
      body: `<html lang='en'>${body}</html>`,
      contentType: "text/html"
    }).then(response => {
      if (response.ok) {
        return response.blob();
      } else {
        throw response.json();
      }
    }).then(data => {
      const objectURL = (window.URL ? window.URL : window.webkitURL).createObjectURL(data);
      const anchorElement = document.createElement("a");
      anchorElement.href = objectURL;
      anchorElement.download = "DiffResult.pdf";
      anchorElement.target = "_blank";
      anchorElement.click();
      anchorElement.remove();
      setTimeout(() => URL.revokeObjectURL(objectURL), 100);
    })
    .catch(errorResponse => {
      Promise.resolve(errorResponse).then((error) => alert("Error occurred converting diff data to PDF" + (error && error.message ? ": " + error.message : "")));
    }).finally(() => {
      setExportInProgress(false);
    });
  };

  return (
      <div className={`control-pane ${context.state.headerPinned ? "no-app-header" : ""} ${context.state.controlPaneExpanded ? "expanded" : ""}`}>
        <div style={{
          position: "absolute",
          right: "5px"
        }}>
          {context.state.controlPaneAccessible
              && <FontAwesomeIcon icon={context.state.controlPaneExpanded ? faAnglesLeft : faAnglesRight} className="expand-button"
                           onClick={() => context.state.setControlPaneExpanded(!context.state.controlPaneExpanded)}/>
          }
        </div>
        <div className="controls">
          <div style={{
            marginBottom: "6px"
          }} className="select-set">
            <label htmlFor="configuration">
              Configuration:
            </label>
            <select id="configuration" className="form-select" value={selectedConfiguration} onChange={(event) => setSelectedConfiguration(event.target.value)}>
              {configurations.map((configuration, index) => {
                return <option key={index} value={configuration}>{configuration}</option>
              })}
            </select>
          </div>
          <div className="form-check">
            <input className="form-check-input" type="checkbox" value="" id="outline-numbers"
                   onChange={() => context.state.setShowOutlineNumbersDiff(!context.state.showOutlineNumbersDiff)}/>
            <label className="form-check-label" htmlFor="outline-numbers">
              Show difference in outline numbers
            </label>
          </div>
          <div className="form-check">
            <input className="form-check-input" type="checkbox" value="" id="counterparts-differ"
                   onChange={() => context.state.setCounterpartWorkItemsDiffer(!context.state.counterpartWorkItemsDiffer)}/>
            <label className="form-check-label" htmlFor="counterparts-differ">
              Counterpart WorkItems differ
            </label>
          </div>
          <div className="form-check">
            <input className="form-check-input" type="checkbox" value="" checked={context.state.hideChaptersIfNoDifference} id="hide-chapters"
                   onChange={() => context.state.setHideChaptersIfNoDifference(!context.state.hideChaptersIfNoDifference)}/>
            <label className="form-check-label" htmlFor="hide-chapters">
              Hide chapters if no difference
            </label>
          </div>
          <div className="export-controls">
            <h1>Export to PDF</h1>
            <div className="select-set">
              <label htmlFor="paper-size">
                Paper size:
              </label>
              <select id="paper-size" className="form-select" value={paperSize} onChange={(event) => setPaperSize(event.target.value)}>
                <option value="A4">A4</option>
                <option value="A3">A3</option>
              </select>
            </div>
            <div className="select-set">
              <label htmlFor="oreientation">
                Orientation:
              </label>
              <select id="oreientation" className="form-select" value={orientation} onChange={(event) => setOrientation(event.target.value)}>
                <option value="landscape">Landscape</option>
                <option value="portrait">Portrait</option>
              </select>
            </div>
            <div className="text-end">
              <button className="btn btn-secondary btn-sm form-button" onClick={exportToPDF} disabled={exportInProgress}>
                Export {exportInProgress && <span className="loader" style={{display: "inline-block", width: "16px", verticalAlign: "text-top", marginLeft: "5px"}}></span>}
              </button>
            </div>
          </div>
        </div>
      </div>
  );
}