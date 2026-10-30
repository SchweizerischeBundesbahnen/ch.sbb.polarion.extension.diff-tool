import {useContext, useEffect, useMemo, useState} from "react";
import {faAnglesLeft, faAnglesRight} from "@fortawesome/free-solid-svg-icons";
import AppContext from "@/components/AppContext";
import {FontAwesomeIcon} from "@fortawesome/react-fontawesome";
import {usePathname, useRouter, useSearchParams} from "@/router/navigation";
import useRemote from "@/services/useRemote";
import {responseError} from "@/services/responseError";
import usePdf from "@/services/usePdf";
import * as DiffTypes from "@/DiffTypes";
import * as DiffViewModes from "@/DiffViewModes";
import Modal from "@/components/Modal";
import {SearchableSelect} from "@sbb-polarion/react-sbb-polarion";
import useSwapColors from "@/utils/useSwapColors";

export default function ControlPane({diff_type}) {
  const context = useContext(AppContext);
  const searchParams = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();
  const remote = useRemote();
  const pdf = usePdf();
  const [swapColors, setSwapColors] = useSwapColors();
  // Derived from the query string, not frozen in state: navigation re-renders this pane without remounting it,
  // so back/forward and swapping the documents must show up here too.
  const projectId = searchParams.get('sourceProjectId');
  const selectedCompareAs = searchParams.get("compareAs") || 'Workitems';
  const [configurations, setConfigurations] = useState([]);
  const selectedConfiguration = searchParams.get("config") || 'Default';
  const [documentSelectionToBeConfirmed, setDocumentSelectionToBeConfirmed] = useState("");
  // Derived from the query string, not frozen in state, so the selector reflects the document
  // auto-selected below and every later switch.
  const selectedDocumentLocationPath = useMemo(() => searchParams.has("sourceSpaceId") && searchParams.has("sourceDocument")
      ? `${searchParams.get("sourceSpaceId")}/${searchParams.get("sourceDocument")}` : "", [searchParams]);
  const [paperSize, setPaperSize] = useState("A4");
  const [orientation, setOrientation] = useState("landscape");
  const [exportInProgress, setExportInProgress] = useState(false);

  useEffect(() => {
    if (remote && projectId) {
      // A response for a project which is no longer the source one must not overwrite the names of the current one
      let outdated = false;
      remote.sendRequest({
        method: "GET",
        url: `/settings/diff/names?scope=project/${projectId}/`,
        contentType: "text/html"
      }).then(async response => {
        if (!response.ok) {
          throw await responseError(response);
        }
        return response.text();
      }).then(data => {
        if (!outdated) {
          setConfigurations(JSON.parse(data).map(setting => setting.name));
        }
      }).catch(error => {
        console.log("Error occurred loading setting names" + (error && error.message ? ": " + error.message : ""));
      });
      return () => {
        outdated = true;
      };
    }
  }, [projectId]);

  // The diffs read 'compareAs' and 'config' from the query string, so the defaults are written into it.
  // Replaced, not pushed: writing a default is no navigation of its own, and both params go in one write.
  useEffect(() => {
    const params = new URLSearchParams(searchParams);
    if (diff_type !== DiffTypes.WORK_ITEMS_DIFF && !searchParams.get('compareAs')) {
      params.set('compareAs', selectedCompareAs);
    }
    if (!searchParams.get('config')) {
      params.set('config', selectedConfiguration);
    }
    if (params.toString() !== searchParams.toString()) {
      router.replace(pathname + '?' + params.toString());
    }
  }, [searchParams, diff_type, selectedCompareAs, selectedConfiguration, router, pathname]);

  const selectParam = (key, value, currentValue) => {
    if (value !== currentValue) {
      const params = new URLSearchParams(searchParams);
      params.set(key, value);
      router.push(pathname + '?' + params.toString());
    }
  };

  useEffect(() => {
    if (!selectedDocumentLocationPath && context.state.pairedDocuments && context.state.pairedDocuments.length > 0) {
      documentSelected(context.state.pairedDocuments[0].leftDocument.locationPath);
    }
  }, [selectedDocumentLocationPath, context.state.pairedDocuments]);

  const changeSelectedDocument = (locationPath) => {
    if (context.state.selectedItemsCount > 0) {
      setDocumentSelectionToBeConfirmed(locationPath);
    } else {
      documentSelected(locationPath);
    }
  };

  const documentSelected = (locationPath) => {
    // URLSearchParams encodes the values: a document or space ID may contain '&', '#' or '='
    const params = new URLSearchParams(searchParams);
    ["sourceSpaceId", "sourceDocument", "targetSpaceId", "targetDocument"].forEach(key => params.delete(key));

    const selectedDocumentsPair = locationPath && context.state.pairedDocuments.find(documentsPair => documentsPair.leftDocument.locationPath === locationPath);
    if (selectedDocumentsPair) {
      params.set("sourceSpaceId", selectedDocumentsPair.leftDocument.spaceId);
      params.set("sourceDocument", selectedDocumentsPair.leftDocument.id);
      if (selectedDocumentsPair.rightDocument) {
        params.set("targetSpaceId", selectedDocumentsPair.rightDocument.spaceId);
        params.set("targetDocument", selectedDocumentsPair.rightDocument.id);
      }
    }

    router.push(pathname + '?' + params.toString());
  };

  const exportToPDF = () => {
    let body = pdf.preProcess(document.documentElement.innerHTML, paperSize, orientation);
    setExportInProgress(true);
    remote.sendRequest({
      method: "POST",
      url: `/conversion/html-to-pdf?orientation=${orientation}&paperSize=${paperSize}`,
      body: `<html lang='en'>${body}</html>`,
      contentType: "text/html"
    }).then(async response => {
      if (response.headers?.get("x-com-ibm-team-repository-web-auth-msg") === "authrequired") {
        alert("Your session has expired. Please refresh the page to log in. Note that any unsaved changes will be lost.");
        return Promise.resolve();
      }
      if (!response.ok) {
        throw await responseError(response);
      }
      return response.blob();
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
    .catch(error => {
      alert("Error occurred converting diff data to PDF" + (error && error.message ? ": " + error.message : ""));
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
              && <button type="button" className="expand-button"
                         aria-expanded={context.state.controlPaneExpanded}
                         aria-label={context.state.controlPaneExpanded ? "Collapse the configuration pane" : "Expand the configuration pane"}
                         onClick={() => context.state.setControlPaneExpanded(!context.state.controlPaneExpanded)}>
                <FontAwesomeIcon icon={context.state.controlPaneExpanded ? faAnglesLeft : faAnglesRight}/>
              </button>
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
                <SearchableSelect id="source-document" value={selectedDocumentLocationPath} onChange={changeSelectedDocument}
                                  options={context.state.pairedDocuments.map(({leftDocument}) => ({
                                    id: leftDocument.locationPath,
                                    name: `${leftDocument.spaceId === "_default" ? "Default Space" : leftDocument.spaceId} / ${leftDocument.title}`
                                  }))} />
              </div>
          }
          {diff_type !== DiffTypes.WORK_ITEMS_DIFF &&
              <div style={{
                marginBottom: "6px"
              }} className="select-set">
                <label htmlFor="target-type">
                  Compare as:
                </label>
                <SearchableSelect id="target-type" value={selectedCompareAs} onChange={compareAs => selectParam('compareAs', compareAs, selectedCompareAs)}
                                  options={["Workitems", "Fields", "Content"].map((compareAs) => ({id: compareAs, name: compareAs}))} />
              </div>
          }
          {diff_type !== DiffTypes.DOCUMENTS_FIELDS_DIFF && diff_type !== DiffTypes.DOCUMENTS_CONTENT_DIFF &&
              <div style={{
                marginBottom: "6px"
              }} className="select-set">
                <label htmlFor="configuration">
                  Configuration:
                </label>
                <SearchableSelect id="configuration" value={selectedConfiguration} onChange={configuration => selectParam('config', configuration, selectedConfiguration)}
                                  options={configurations.map((configuration) => ({id: configuration, name: configuration}))} />
              </div>
          }
          <div style={{
            marginBottom: "6px"
          }} className="select-set">
            <label htmlFor="diff-view">
              Diff view:
            </label>
            <SearchableSelect id="diff-view" value={context.state.diffViewMode} onChange={context.state.setDiffViewMode} searchable={false}
                              options={[{id: DiffViewModes.SIDE_BY_SIDE, name: "Side by side"}, {id: DiffViewModes.UNIFIED, name: "Unified"}]} />
          </div>
          {(diff_type === DiffTypes.DOCUMENTS_DIFF || diff_type === DiffTypes.COLLECTIONS_DIFF) &&
              <div className="form-check">
                <input className="form-check-input" type="checkbox" value="" id="outline-numbers"
                       onChange={() => context.state.setShowOutlineNumbersDiff(!context.state.showOutlineNumbersDiff)}/>
                <label className="form-check-label" htmlFor="outline-numbers">
                  Show difference in outline numbers
                </label>
              </div>
          }
          {(diff_type === DiffTypes.DOCUMENTS_DIFF || diff_type === DiffTypes.COLLECTIONS_DIFF) &&
              <div className="form-check">
                <input className="form-check-input" type="checkbox" checked={context.state.syncStructure} id="sync-structure"
                       onChange={() => context.state.setSyncStructure(!context.state.syncStructure)}/>
                <label className="form-check-label" htmlFor="sync-structure" title="When unchecked, moved, added and removed items are hidden and only content changes of items present in both documents are compared and merged.">
                  Sync structure
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
          {diff_type !== DiffTypes.DOCUMENTS_FIELDS_DIFF && diff_type !== DiffTypes.DOCUMENTS_CONTENT_DIFF &&
              <div className="form-check">
                <input className="form-check-input" type="checkbox" value="" checked={context.state.updateAttachmentReferences} id="update-attachment-references"
                       onChange={() => context.state.setUpdateAttachmentReferences(!context.state.updateAttachmentReferences)}/>
                <label className="form-check-label" htmlFor="update-attachment-references" title="When attachments are merged, their file names can change. When this option is selected DiffTool will try to update rich text fields to reflect this change, when not selected DiffTool won't modify attachment references in rich text fields.">
                  Update attachment references in rich text fields
                </label>
              </div>
          }
          {diff_type !== DiffTypes.DOCUMENTS_FIELDS_DIFF && diff_type !== DiffTypes.DOCUMENTS_CONTENT_DIFF &&
              <div className="form-check">
                <input className="form-check-input" type="checkbox" value="" checked={context.state.individualFieldsSelection} id="per-field-merge-selection"
                       onChange={() => context.state.setIndividualFieldsSelection(!context.state.individualFieldsSelection)} data-testid="per-field-merge-selection" />
                <label className="form-check-label" htmlFor="per-field-merge-selection" title="Allows you to refine the merge scope to specific fields within each item">
                  Individual fields selection
                </label>
              </div>
          }
          <div className="form-check">
            <input className="form-check-input" type="checkbox" checked={swapColors} id="swap-colors"
                   onChange={() => setSwapColors(!swapColors)}/>
            <label className="form-check-label" htmlFor="swap-colors" title="Show removed content in green and added content in red.">
              Swap diff colors
            </label>
          </div>
          <div className="export-controls">
            <h1>Export to PDF</h1>
            <div className="select-set">
              <label htmlFor="paper-size">
                Paper size:
              </label>
              <SearchableSelect id="paper-size" value={paperSize} onChange={setPaperSize} searchable={false}
                                options={[{id: "A4", name: "A4"}, {id: "A3", name: "A3"}]} />
            </div>
            <div className="select-set">
              <label htmlFor="orientation">
                Orientation:
              </label>
              <SearchableSelect id="orientation" value={orientation} onChange={setOrientation} searchable={false}
                                options={[{id: "landscape", name: "Landscape"}, {id: "portrait", name: "Portrait"}]} />
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
