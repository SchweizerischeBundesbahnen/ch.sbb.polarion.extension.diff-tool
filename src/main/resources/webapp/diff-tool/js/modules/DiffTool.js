import ExtensionContext from "/polarion/diff-tool/ui/generic/js/modules/ExtensionContext.js";
import SearchableDropdown from "/polarion/diff-tool/ui/generic/js/modules/SearchableDropdown.js";
import GenericMixin from "./GenericMixin.js";

export default class DiffTool extends GenericMixin {

  constructor(sourceProjectId, sourceSpace, sourceDocument, sourceDocumentTitle, sourceRevision) {
    super();

    this.ctx = new ExtensionContext({});

    const diffToolPane = this.ctx.querySelector(".comparison.form-wrapper");
    if (!diffToolPane || diffToolPane.classList.contains("initialized")) {
      return; // Skip if diff-tool pane wasn't found or was already initialized
    }
    diffToolPane.classList.add("initialized");

    this.sourceProjectId = sourceProjectId;
    this.settingsProjectId = sourceProjectId;
    this.sourceSpace = sourceSpace;
    this.sourceDocument = sourceDocument;
    this.sourceDocumentTitle = sourceDocumentTitle;
    this.sourceRevision = sourceRevision;

    this.ctx.onClick('compare-with-same-checkbox', () => this.toggleCompareSameDocument());
    this.ctx.onClick('revision-enter-manually', () => this.toggleManualRevision());
    this.ctx.onClick('revision-select-from-list', () => this.toggleListRevision());
    this.ctx.onClick('baseline-checkbox', () => this.baselineSelected());
    this.ctx.onClick('use-work-items-filter', () => this.toggleWorkItemsFilter());
    this.ctx.onClick('compare-documents', () => this.showDiffResult());

    // This is to remove "overflow: hidden" from parent div (Polarion internal code) for custom dropdown popups not to be cut off by parent div box border
    this.resetParentsOverflowHidden(this.ctx.getElementById('comparison-project-selector'));

    // First generate dropdown with data, remove selection and only then assign event listener
    this.projectDropdown = new SearchableDropdown({
      element: '#comparison-project-selector',
      placeholder: 'Select Project...'
    });
    this.ctx.onChange('comparison-project-selector', () => this.projectChanged());
    this.projectDropdown.restoreSelection();

    // First generate dropdown with data, remove selection and only then assign event listener
    this.spaceDropdown = new SearchableDropdown({
      element: '#comparison-space-selector',
      placeholder: 'Select Space...'
    });
    this.ctx.onChange('comparison-space-selector', () => this.spaceChanged());

    // First generate dropdown with data, remove selection and only then assign event listener
    this.documentDropdown = new SearchableDropdown({
      element: '#document-selector',
      placeholder: 'Select Document...'
    });
    this.ctx.onChange('document-selector', () => this.documentChanged());

    this.revisionDropdown = new SearchableDropdown({
      element: '#revision-selector',
      placeholder: 'Select Revision...'
    });

    this.linkRoleDropdown = new SearchableDropdown({
      element: '#comparison-link-role-selector',
      placeholder: 'Select Link Role...'
    });
    this.linkRoleDropdown.restoreSelection();

    this.configDropdown = new SearchableDropdown({
      element: '#comparison-config-selector',
      placeholder: 'Select Configuration...'
    });
    this.configDropdown.restoreSelection();
  }

  projectChanged() {
    document.getElementById("document-selector").innerHTML = "";
    document.getElementById("revision-selector").innerHTML = "";
    this.ctx.disableIf("compare-documents", true);
    this.loadSpaces(true, () => this.spaceDropdown.refresh());
  }

  spaceChanged() {
    const selectedProject = this.ctx.getValue("comparison-project-selector");
    const selectedSpace = this.ctx.getValue("comparison-space-selector");

    const documentSelector = this.ctx.getElementById("document-selector");
    documentSelector.innerHTML = "";

    document.getElementById("revision-selector").innerHTML = "";
    this.ctx.disableIf("compare-documents", true);

    if (selectedSpace) {
      this.actionInProgress({inProgress: true, message: "Loading documents", diff: true});
      this.callAsync({
        url: `/polarion/diff-tool/rest/internal/projects/${selectedProject}/spaces/${selectedSpace}/documents`
      }).then((response) => {
        this.actionInProgress({inProgress: false, diff: true});
        documentSelector.innerHTML = "";
        for (const {id, title} of response) {
          const option = document.createElement('option');
          option.value = id;
          option.text = title;
          documentSelector.appendChild(option);
        }
      }).then(() => {
        this.documentDropdown.refresh();
      }).catch(() => {
        this.actionInProgress({inProgress: false, diff: true});
        this.showAlert({alertType: "error", message: "Error occurred loading documents", diff: true});
      });
    } else {
      this.documentDropdown.refresh();
    }
  }

  documentChanged() {
    const sameDoc = this.compareSameDocument();
    const selectedProject = sameDoc ? this.sourceProjectId : this.ctx.getValue("comparison-project-selector");
    const selectedSpace = sameDoc ? this.sourceSpace : this.ctx.getValue("comparison-space-selector");
    const selectedDocument = sameDoc ? this.sourceDocument : this.ctx.getValue("document-selector");

    this.newDocumentSelected();
    this.loadRevisions(selectedProject, selectedSpace, selectedDocument);
  }

  loadRevisions(selectedProject, selectedSpace, selectedDocument) {
    if (this.manualRevision()) {
      this.ctx.disableIf("compare-documents", false);
      return;
    }

    const revisionSelector = this.ctx.getElementById("revision-selector");
    revisionSelector.innerHTML = "";

    this.ctx.disableIf("compare-documents", true);

    if (selectedDocument) {
      this.actionInProgress({inProgress: true, message: "Loading revisions", diff: true});
      this.callAsync({
        url: `/polarion/diff-tool/rest/internal/projects/${selectedProject}/spaces/${selectedSpace}/documents/${selectedDocument}/revisions`
      }).then((response) => {
        revisionSelector.innerHTML = "";
        this.actionInProgress({inProgress: false, diff: true});
        const headOption = document.createElement('option');
        headOption.value = "";
        headOption.text = "HEAD";
        revisionSelector.appendChild(headOption);
        for (const {name, baselineName} of response) {
          const option = document.createElement('option');
          option.value = name;
          if (baselineName) {
            option.text = name + ' | ' + baselineName;
            option.setAttribute("baseline-name", baselineName);
          } else {
            option.text = name;
          }
          revisionSelector.appendChild(option);
        }
        this.ctx.disableIf("compare-documents", false);
        this.baselineSelected();
        this.revisionDropdown.refresh();
      }).catch(() => {
        this.actionInProgress({inProgress: false, diff: true});
        this.showAlert({alertType: "error", message: "Error occurred loading revisions", diff: true});
      });
    } else {
      this.revisionDropdown.refresh();
    }
  }

  baselineSelected() {
    const revisionSelector = this.ctx.getElementById("revision-selector");
    const baselineCheckBox = this.ctx.getElementById("baseline-checkbox");

    const options = revisionSelector.options;
    let selectionIndex = 0;
    let latestBaselineFound = false;
    for (let i = 0; i < options.length; i++) {
      const option = options[i];
      if (baselineCheckBox.checked && !option.getAttribute('baseline-name')) {
        option.style.display = 'none';
      } else {
        option.style.display = 'initial';
        if (!latestBaselineFound) {
          latestBaselineFound = true;
          selectionIndex = i;
        }
      }
    }
    options[selectionIndex].selected = 'selected';
    this.revisionDropdown.refresh();
  }

  newDocumentSelected() {
    if (this.settingsProjectId !== this.sourceProjectId) {
      this.reloadSettings(this.sourceProjectId, true);
    }
  }

  showDiffResult() {
    const sameDoc = this.compareSameDocument();
    const targetProjectId = sameDoc ? this.sourceProjectId : this.ctx.getValue("comparison-project-selector");
    const targetSpace = sameDoc ? this.sourceSpace : this.ctx.getValue("comparison-space-selector");
    const targetDocument = sameDoc ? this.sourceDocument : this.ctx.getValue("document-selector");
    const targetRevision = this.ctx.getValue(this.manualRevision() ? "select-revision-manual-input" : "revision-selector");
    const linkRole = this.ctx.getValue("comparison-link-role-selector");
    const config = this.ctx.getValue("comparison-config-selector");

    let path = `/polarion/diff-tool-app/ui/app/documents.html`
        + `?sourceProjectId=${this.sourceProjectId}&sourceSpaceId=${this.sourceSpace}&sourceDocument=${this.sourceDocument}`
        + `&targetProjectId=${targetProjectId}&targetSpaceId=${targetSpace}&targetDocument=${targetDocument}`
        + `&linkRole=${linkRole}&config=${config}&compareAs=Workitems`;
    if (this.sourceRevision) {
      path += `&sourceRevision=${this.sourceRevision}`;
    }
    if (targetRevision) {
      path += `&targetRevision=${targetRevision}`;
    }

    if (document.getElementById("use-work-items-filter").checked) {
      const filter = this.ctx.getValue("work-items-filter-input");
      this.digestMessage(filter).then(digestHex => {
        localStorage.setItem(digestHex + "_filter", filter);
        localStorage.setItem(digestHex + "_type", document.getElementById("include-work-items").checked ? "include" : "exclude");
        path += `&filter=${digestHex}`;
        window.open(path, '_blank');
      });
    } else {
      window.open(path, '_blank');
    }
  }

  compareSameDocument() {
    return document.querySelector('#compare-with-same-checkbox').checked;
  }

  toggleCompareSameDocument() {
    const elements = document.getElementsByClassName('hide-when-comparing-same');
    for (let i = 0; i < elements.length; i++) {
      if (this.compareSameDocument()) {
        elements[i].classList.add("hide");
      } else {
        elements[i].classList.remove("hide");
      }
    }
    this.documentChanged();
  }

  manualRevision() {
    return this.ctx.getElementById('revision-enter-manually').checked;
  }

  toggleManualRevision() {
    this.ctx.getElementById("select-revision-list-container").style.display = "none";
    this.ctx.getElementById("select-revision-manual-container").style.display = "block";
    this.ctx.getElementById("compare-documents").disabled = false;
  }

  toggleListRevision() {
    this.ctx.getElementById("select-revision-list-container").style.display = "block";
    this.ctx.getElementById("select-revision-manual-container").style.display = "none";
    this.documentChanged();
  }

  toggleWorkItemsFilter() {
    this.ctx.getElementById("work-items-filter-pane").style.display = this.ctx.getElementById("use-work-items-filter").checked ? "block" : "none";
  }

  async digestMessage(message) {
    const msgUint8 = new TextEncoder().encode(message);
    const hashBuffer = await window.crypto.subtle.digest("SHA-1", msgUint8);
    const hashArray = Array.from(new Uint8Array(hashBuffer));
    return hashArray
        .map((b) => b.toString(16).padStart(2, "0"))
        .join("");
  }
}
