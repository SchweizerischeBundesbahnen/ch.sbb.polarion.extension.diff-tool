import {useContext, useEffect, useState} from "react";
import {faAnglesLeft, faAnglesRight} from "@fortawesome/free-solid-svg-icons";
import AppContext from "@/components/AppContext";
import {FontAwesomeIcon} from "@fortawesome/react-fontawesome";
import {usePathname, useRouter, useSearchParams} from "next/navigation";
import useRemote from "@/services/useRemote";
import usePdf from "@/services/usePdf";
import * as DiffTypes from "@/DiffTypes";
import Modal from "@/components/Modal";

export default function ControlPane({diff_type}) {
  const context = useContext(AppContext);
  const searchParams = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();
  const remote = useRemote();
  const pdf = usePdf();
  const [projectId] = useState(searchParams.get('sourceProjectId'));
  const [selectedCompareAs, setSelectedCompareAs] = useState(searchParams.get("compareAs") || 'Workitems');
  const [configurations, setConfigurations] = useState([]);
  const [selectedConfiguration, setSelectedConfiguration] = useState(searchParams.get("config") || 'Default');
  const [documentSelectionToBeConfirmed, setDocumentSelectionToBeConfirmed] = useState("");
  const [selectedDocumentLocationPath] = useState(searchParams.has("sourceSpaceId") && searchParams.has("sourceDocument")
      ? `${searchParams.get("sourceSpaceId")}/${searchParams.get("sourceDocument")}` : "");
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
        Promise.resolve(errorResponse).then((error) => console.log("Error occurred loading setting names" + (error && error.message ? ": " + error.message : "")));
      });
    }
  }, [projectId]);

  useEffect(() => {
    if (diff_type !== DiffTypes.WORK_ITEMS_DIFF && searchParams.get('compareAs') !== selectedCompareAs) {
      const params = [];
      for (const [key, value] of searchParams.entries()) {
        if (key === 'compareAs') {
          params.push(`${key}=${selectedCompareAs}`); // update 'compareAs' parameter
        } else {
          params.push(`${key}=${value}`); // ...and leave other as they are
        }
      }
      if (!searchParams.get('compareAs')) {
        params.push(`compareAs=${selectedCompareAs}`);
      }

      router.push(pathname + '?' + params.join('&'));
    }
  }, [selectedCompareAs]);

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
      if (!searchParams.get('config')) {
        params.push(`config=${selectedConfiguration}`);
      }

      router.push(pathname + '?' + params.join('&'));
    }
  }, [selectedConfiguration]);

  useEffect(() => {
    if (!selectedDocumentLocationPath && context.state.pairedDocuments && context.state.pairedDocuments.length > 0) {
      documentSelected(context.state.pairedDocuments[0].leftDocument.locationPath);
    }
  }, [selectedDocumentLocationPath, context.state.pairedDocuments]);

  const changeSelectedDocument = (event) => {
    if (context.state.selectedItemsCount > 0) {
      setDocumentSelectionToBeConfirmed(event.target.value);
    } else {
      documentSelected(event.target.value);
    }
  };

  const documentSelected = (locationPath) => {
    const params = [];
    for (const [key, value] of searchParams.entries()) {
      if (key !== "sourceSpaceId" && key !== "sourceDocument" && key !== "targetSpaceId" && key !== "targetDocument") {
        params.push(`${key}=${value}`);
      }
    }

    const selectedDocumentsPair = locationPath && context.state.pairedDocuments.find(documentsPair => documentsPair.leftDocument.locationPath === locationPath);
    if (selectedDocumentsPair) {
      params.push(`sourceSpaceId=${selectedDocumentsPair.leftDocument.spaceId}`);
      params.push(`sourceDocument=${selectedDocumentsPair.leftDocument.id}`);
      if (selectedDocumentsPair.rightDocument) {
        params.push(`targetSpaceId=${selectedDocumentsPair.rightDocument.spaceId}`);
        params.push(`targetDocument=${selectedDocumentsPair.rightDocument.id}`);
      }
    }

    router.push(pathname + '?' + params.join('&'));
  };

  const exportToPDF = () => {
    let body = pdf.preProcess(document.documentElement.innerHTML, paperSize, orientation);
    setExportInProgress(true);
    remote.sendRequest({
      method: "POST",
      url: `/conversion/html-to-pdf?orientation=${orientation}&paperSize=${paperSize}`,
      body: `<html lang='en'>${body}</html>`,
      contentType: "text/html"
    }).then(response => {
      if (response.headers?.get("x-com-ibm-team-repository-web-auth-msg") === "authrequired") {
        alert("Your session has expired. Please refresh the page to log in. Note that any unsaved changes will be lost.");
        return Promise.resolve();
      }
      if (response.ok) {
        return response.blob();
      } else {
        throw response.json();
      }
    }).then(data => {
      if (!data) return;
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
      <div className={`control-pane ${context.state.controlPaneExpanded ? "expanded" : ""}`}>
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
          {diff_type === DiffTypes.COLLECTIONS_DIFF &&
              <div style={{
                marginBottom: "6px"
              }} className="select-set">
                <label htmlFor="source-document">
                  Source documents:
                </label>
                <select id="source-document" className="form-select" value={selectedDocumentLocationPath} onChange={changeSelectedDocument}>
                  {context.state.pairedDocuments.map((documentsPair, index) => {
                    return <option key={index} value={documentsPair.leftDocument.locationPath}>{documentsPair.leftDocument.spaceId === "_default" ? "Default Space" : documentsPair.leftDocument.spaceId} / {documentsPair.leftDocument.title}</option>
                  })}
                </select>
              </div>
          }
          {diff_type !== DiffTypes.WORK_ITEMS_DIFF &&
              <div style={{
                marginBottom: "6px"
              }} className="select-set">
                <label htmlFor="target-type">
                  Compare as:
                </label>
                <select id="target-type" className="form-select" value={selectedCompareAs} onChange={(event) => setSelectedCompareAs(event.target.value)}>
                  {["Workitems", "Fields", "Content"].map((compareAs, index) => {
                    return <option key={index} value={compareAs}>{compareAs}</option>
                  })}
                </select>
              </div>
          }
          {diff_type !== DiffTypes.DOCUMENTS_FIELDS_DIFF && diff_type !== DiffTypes.DOCUMENTS_CONTENT_DIFF &&
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
          }
          {(diff_type === DiffTypes.DOCUMENTS_DIFF || diff_type === DiffTypes.COLLECTIONS_DIFF) &&
              <div className="form-check">
                <input className="form-check-input" type="checkbox" value="" id="outline-numbers"
                       onChange={() => context.state.setShowOutlineNumbersDiff(!context.state.showOutlineNumbersDiff)}/>
                <label className="form-check-label" htmlFor="outline-numbers">
                  Show difference in outline numbers
                </label>
              </div>
          }
          {diff_type !== DiffTypes.DOCUMENTS_FIELDS_DIFF && diff_type !== DiffTypes.DOCUMENTS_CONTENT_DIFF &&
              <div className="form-check">
                <input className="form-check-input" type="checkbox" value="" id="counterparts-differ"
                       onChange={() => context.state.setCounterpartWorkItemsDiffer(!context.state.counterpartWorkItemsDiffer)}/>
                <label className="form-check-label" htmlFor="counterparts-differ">
                  Counterpart WorkItems differ
                </label>
              </div>
          }
          {diff_type === DiffTypes.DOCUMENTS_FIELDS_DIFF &&
              <div className="form-check">
                <input className="form-check-input" type="checkbox" checked={context.state.compareOnlyMutualFields} id="compare-only-mutual-fields"
                       onChange={() => context.state.setCompareOnlyMutualFields(!context.state.compareOnlyMutualFields)}/>
                <label className="form-check-label" htmlFor="compare-only-mutual-fields">
                  Compare only mutual fields
                </label>
              </div>
          }
          {diff_type !== DiffTypes.DOCUMENTS_CONTENT_DIFF &&
            <div className="form-check">
              <input className="form-check-input" type="checkbox" value="" id="compare-enums-by-id"
                     onChange={() => context.state.setCompareEnumsById(!context.state.compareEnumsById)}/>
              <label className="form-check-label" htmlFor="compare-enums-by-id">
                Compare enums by ID
              </label>
            </div>
          }
          {(diff_type === DiffTypes.DOCUMENTS_DIFF || diff_type === DiffTypes.COLLECTIONS_DIFF) &&
              <div className="form-check">
                <input className="form-check-input" type="checkbox" value="" id="allow-reference-wi-merge"
                       onChange={() => context.state.setAllowReferencedWorkItemMerge(!context.state.allowReferencedWorkItemMerge)}/>
                <label className="form-check-label" htmlFor="allow-reference-wi-merge">
                  Allow reference work item merge
                </label>
              </div>
          }
          {(diff_type === DiffTypes.DOCUMENTS_DIFF || diff_type === DiffTypes.COLLECTIONS_DIFF || diff_type === DiffTypes.DOCUMENTS_CONTENT_DIFF) &&
              <div className="form-check">
                <input className="form-check-input" type="checkbox" value="" checked={context.state.hideChaptersIfNoDifference} id="hide-chapters"
                       onChange={() => context.state.setHideChaptersIfNoDifference(!context.state.hideChaptersIfNoDifference)}/>
                <label className="form-check-label" htmlFor="hide-chapters">
                  Hide chapters if no difference
                </label>
              </div>
          }
          {(diff_type === DiffTypes.DOCUMENTS_DIFF || diff_type === DiffTypes.DOCUMENTS_CONTENT_DIFF) &&
              <div className="form-check">
                <input className="form-check-input" type="checkbox" value="" checked={context.state.preserveComments} id="preserve-comments"
                       onChange={() => context.state.setPreserveComments(!context.state.preserveComments)}/>
                <label className="form-check-label" htmlFor="preserve-comments">
                  Preserve comments
                </label>
              </div>
          }
          {diff_type === DiffTypes.DOCUMENTS_DIFF &&
              <div className="form-check">
                <input className="form-check-input" type="checkbox" value="" checked={context.state.copyMissingDocumentAttachments} id="copy-missing-document-attachments"
                       onChange={() => context.state.setCopyMissingDocumentAttachments(!context.state.copyMissingDocumentAttachments)}/>
                <label className="form-check-label" htmlFor="copy-missing-document-attachments" title="Copy document attachments which are referenced in rich text fields but missing in the target document.">
                  Copy missing document attachments
                </label>
              </div>
          }
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
              <label htmlFor="orientation">
                Orientation:
              </label>
              <select id="orientation" className="form-select" value={orientation} onChange={(event) => setOrientation(event.target.value)}>
                <option value="landscape">Landscape</option>
                <option value="portrait">Portrait</option>
              </select>
            </div>
            <div className="text-end">
              <button className="btn btn-secondary btn-sm form-button" onClick={exportToPDF} disabled={exportInProgress} data-testid="export-button">
                Export {exportInProgress && <span className="loader" style={{display: "inline-block", width: "16px", verticalAlign: "text-top", marginLeft: "5px"}}></span>}
              </button>
            </div>
          </div>
        </div>

        <Modal title="Confirm change of documents pair" visible={!!documentSelectionToBeConfirmed} setVisible={() => setDocumentSelectionToBeConfirmed("")} className="modal-md"
               cancelButtonTitle="Cancel" actionButtonTitle="Confirm" actionButtonHandler={() => documentSelected(documentSelectionToBeConfirmed) }>
          <p>You have selected some Workitems to be merged but didn&apos;t merge them, changing documents to be diffed/merged you will lose this selection information. Do you wish to continue?</p>
        </Modal>

      </div>
  );
}
