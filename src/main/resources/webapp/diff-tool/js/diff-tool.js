DiffTool = {

  init: function (sourceProjectId, sourceSpace, sourceDocument, sourceDocumentTitle, sourceRevision) {
    this.sourceProjectId = sourceProjectId;
    this.settingsProjectId = sourceProjectId;
    this.sourceSpace = sourceSpace;
    this.sourceDocument = sourceDocument;
    this.sourceDocumentTitle = sourceDocumentTitle;
    this.sourceRevision = sourceRevision;
  },

  projectChanged: function () {
    document.getElementById("document-selector").innerHTML = ""; // Clear previously loaded content
    document.getElementById("revision-selector").innerHTML = ""; // Clear previously loaded content
    document.getElementById("compare-documents").disabled = true; // Disable "Compare" button
    Common.loadSpaces(true);
  },

  spaceChanged: function () {
    const selectedProject = document.getElementById("comparison-project-selector").value;
    const selectedSpace = document.getElementById("comparison-space-selector").value;

    Common.actionInProgress({inProgress: true, message: "Loading documents", diff: true});
    const documentSelector = document.getElementById("document-selector");
    documentSelector.innerHTML = `<option disabled selected value> --- select document --- </option>`;

    document.getElementById("revision-selector").innerHTML = ""; // Clear previously loaded content
    document.getElementById("compare-documents").disabled = true; // Disable "Compare" button

    if (selectedSpace) {
      Common.callAsync({
        url: `/polarion/diff-tool/rest/internal/projects/${selectedProject}/spaces/${selectedSpace}/documents`
      }).then((response) => {
        Common.actionInProgress({inProgress: false, diff: true});
        for (let {id, title} of response) {
          const option = document.createElement('option');
          option.value = id;
          option.text = title;
          documentSelector.appendChild(option);
        }
      }).catch(() => {
        Common.actionInProgress({inProgress: false, diff: true});
        Common.showAlert({alertType: "error", message: "Error occurred loading documents", diff: true});
      });
    }
  },

  documentChanged: function () {
    const sameDoc = this.compareSameDocument();
    const selectedProject = sameDoc ? this.sourceProjectId : document.getElementById("comparison-project-selector").value;
    const selectedSpace = sameDoc ? this.sourceSpace : document.getElementById("comparison-space-selector").value;
    const selectedDocument = sameDoc ? this.sourceDocument : document.getElementById("document-selector").value;

    this.newDocumentSelected();
    this.loadRevisions(selectedProject, selectedSpace, selectedDocument)
  },

  loadRevisions: function (selectedProject, selectedSpace, selectedDocument) {

    if (this.manualRevision()) {
      document.getElementById("compare-documents").disabled = false;
      return;
    }

    const revisionSelector = document.getElementById("revision-selector");
    revisionSelector.innerHTML = ""; // Clear previously loaded content

    document.getElementById("compare-documents").disabled = true; // Disable "Compare" button

    if (selectedDocument) {
      Common.actionInProgress({inProgress: true, message: "Loading revisions", diff: true});
      Common.callAsync({
        url: `/polarion/diff-tool/rest/internal/projects/${selectedProject}/spaces/${selectedSpace}/documents/${selectedDocument}/revisions`
      }).then((response) => {
        Common.actionInProgress({inProgress: false, diff: true});
        const headOption = document.createElement('option');
        headOption.value = "";
        headOption.text = "HEAD";
        revisionSelector.appendChild(headOption);
        for (let {name, baselineName} of response) {
          const option = document.createElement('option');
          option.value = name;
          if (baselineName) {
            option.text = name + ' | ' + baselineName;
            option.setAttribute("baseline-name", baselineName)
          } else {
            option.text = name;
          }
          revisionSelector.appendChild(option);
        }
        document.getElementById("compare-documents").disabled = false; // Enable "Compare" button
        this.baselineSelected();
      }).catch(() => {
        Common.actionInProgress({inProgress: false, diff: true});
        Common.showAlert({alertType: "error", message: "Error occurred loading revisions", diff: true});
      });
    }
  },

  baselineSelected: function () {
    const revisionSelector = document.getElementById("revision-selector");
    const baselineCheckBox = document.getElementById("baseline-checkbox");

    let options = revisionSelector.options;
    let selectionIndex = 0;
    let latestBaselineFound = false;
    for (let i = 0; i < options.length; i++) {
      let option = options[i];
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
    options[selectionIndex].selected = 'selected'; // Select latest baseline option or (when no baselines found) latest revision
  },

  newDocumentSelected: function () {
      if (this.settingsProjectId !== this.sourceProjectId) {
        Common.reloadSettings(this.sourceProjectId, true);
      }
  },

  showDiffResult: function () {
    const sameDoc = this.compareSameDocument();
    const targetProjectId = sameDoc ? this.sourceProjectId : document.getElementById("comparison-project-selector").value;
    const targetSpace = sameDoc ? this.sourceSpace : document.getElementById("comparison-space-selector").value;
    const targetDocument = sameDoc ? this.sourceDocument : document.getElementById("document-selector").value;
    const targetRevision = document.getElementById(this.manualRevision() ? "select-revision-manual-input" : "revision-selector").value;
    const linkRole = document.getElementById("comparison-link-role-selector").value;
    const config = document.getElementById("comparison-config-selector").value;

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
      const filter = document.getElementById("work-items-filter-input").value;
      this.digestMessage(filter).then(digestHex => {
        localStorage.setItem(digestHex + "_filter", filter);
        localStorage.setItem(digestHex + "_type", document.getElementById("include-work-items").checked ? "include" : "exclude");
        path += `&filter=${digestHex}`;
        window.open(path, '_blank');
      });
    } else {
      window.open(path, '_blank');
    }
  },

  compareSameDocument: function () {
    return document.querySelector('#compare-with-same-checkbox').checked;
  },

  toggleCompareSameDocument: function () {
    // hide/show unnecessary fields
    const elements = document.getElementsByClassName('hide-when-comparing-same');
    for (let i = 0; i < elements.length; i++) {
      if (this.compareSameDocument()) {
        elements[i].classList.add("hide");
      } else {
        elements[i].classList.remove("hide");
      }
    }
    this.documentChanged();
  },

  manualRevision: function () {
    return document.getElementById('revision-enter-manually').checked;
  },

  toggleManualRevision: function () {
    document.getElementById("select-revision-list-container").style.display = "none";
    document.getElementById("select-revision-manual-container").style.display = "block";

    document.getElementById("compare-documents").disabled = false;
  },

  toggleListRevision: function () {
    document.getElementById("select-revision-list-container").style.display = "block";
    document.getElementById("select-revision-manual-container").style.display = "none";

    this.documentChanged();
  },

  toggleWorkItemsFilter: function () {
    document.getElementById("work-items-filter-pane").style.display = document.getElementById("use-work-items-filter").checked ? "block" : "none";
  },

  updateWorkItemsDiffButton: function (clickedElement, sourceProjectId) {
    const DISABLED_BUTTON_CLASS= "polarion-TestsExecutionButton-buttons-defaultCursor";

    const diffWidget = clickedElement?.closest("div.polarion-DiffTool");
    const button = diffWidget?.querySelector(".polarion-TestsExecutionButton-buttons");
    if (button) {
      if (button.classList.contains(DISABLED_BUTTON_CLASS) && diffWidget.querySelectorAll('input[type="checkbox"]:checked').length > 0) {
        button.classList.remove(DISABLED_BUTTON_CLASS);
        // We need to remember click listener to remove it from button later
        this.registeredButtonClickListener = () => DiffTool.openWorkItemsDiffApplication(diffWidget, sourceProjectId);
        button.addEventListener("click", this.registeredButtonClickListener);
      }
      if (!button.classList.contains(DISABLED_BUTTON_CLASS) && diffWidget.querySelectorAll('input[type="checkbox"]:checked').length === 0) {
        button.classList.add(DISABLED_BUTTON_CLASS);
        button.removeEventListener("click", this.registeredButtonClickListener);
      }
      if (diffWidget.querySelectorAll('input[type="checkbox"]:not(.export-all):not(:checked)').length > 0) {
        const exportAllCheckbox = diffWidget.querySelector('input[type="checkbox"].export-all');
        if (exportAllCheckbox) {
          exportAllCheckbox.checked = false;
        }
      }
    }
  },

  updateCollectionsDiffButton: function (clickedElement, sourceProjectId) {
    const DISABLED_BUTTON_CLASS= "polarion-TestsExecutionButton-buttons-defaultCursor";

    const diffWidget = clickedElement?.closest("div.polarion-DiffTool");
    const button = diffWidget?.querySelector(".polarion-TestsExecutionButton-buttons");
    if (button) {
      const leftCollectionSelected = diffWidget.querySelector('input[type="radio"][name="source-collection"]:checked');
      const rightCollectionSelected = diffWidget.querySelector('input[type="radio"][name="target-collection"]:checked');

      if (button.classList.contains(DISABLED_BUTTON_CLASS) && leftCollectionSelected && rightCollectionSelected) {
        button.classList.remove(DISABLED_BUTTON_CLASS);
        // We need to remember click listener to remove it from button later
        this.registeredButtonClickListener = () => DiffTool.openCollectionsDiffApplication(diffWidget, sourceProjectId);
        button.addEventListener("click", this.registeredButtonClickListener);
      }
      if (!button.classList.contains(DISABLED_BUTTON_CLASS) && !(leftCollectionSelected || rightCollectionSelected)) {
        button.classList.add(DISABLED_BUTTON_CLASS);
        button.removeEventListener("click", this.registeredButtonClickListener);
      }
    }
  },

  selectAllItems: function (clickedElement) {
    const diffWidget = clickedElement?.closest("div.polarion-DiffTool");
    if (diffWidget) {
      diffWidget.querySelectorAll('input[type="checkbox"]:not(.export-all)').forEach(checkbox => {
        checkbox.checked = clickedElement.checked;
      });
    }
  },

  openWorkItemsDiffApplication: function (diffWidget, sourceProjectId) {
    const targetProjectId = document.getElementById("target-project-selector").value;
    const linkRole = document.getElementById("link-role-selector").value;
    const config = document.getElementById("config-selector").value;

    let path = `/polarion/diff-tool-app/ui/app/workitems.html`
        + `?sourceProjectId=${sourceProjectId}&targetProjectId=${targetProjectId}&linkRole=${linkRole}&config=${config}`;

    // const workItemIdsHash = this.generateUuid();
    const selectedIds = [];
    diffWidget.querySelectorAll('input[type="checkbox"]:checked').forEach((checkbox) => {
      if (checkbox.dataset.id) {
        selectedIds.push(checkbox.dataset.id);
      }
    });
    const selectedIdsString = selectedIds.join(",");
    this.digestMessage(selectedIdsString).then(digestHex => {
      localStorage.setItem(digestHex + "_ids", selectedIdsString);
      path += `&ids=${digestHex}`;
      window.open(path, '_blank');
    });
  },

  openCollectionsDiffApplication: function (diffWidget, sourceProjectId) {
    const linkRole = document.getElementById("link-role-selector").value;
    const config = document.getElementById("config-selector").value;
    const targetProjectId = document.getElementById("target-project-selector").value;
    const sourceCollectionSelected = diffWidget.querySelector('input[type="radio"][name="source-collection"]:checked');
    const targetCollectionSelected = diffWidget.querySelector('input[type="radio"][name="target-collection"]:checked');

    let path = `/polarion/diff-tool-app/ui/app/collections.html`
        + `?sourceProjectId=${sourceProjectId}&sourceCollectionId=${sourceCollectionSelected.dataset.id}`
        + `&targetProjectId=${targetProjectId}&targetCollectionId=${targetCollectionSelected.dataset.id}`
        + `&linkRole=${linkRole}&config=${config}&compareAs=Workitems`;

    window.open(path, '_blank');
  },

  replaceUrlParam: function (url, paramName, paramValue){
    if (paramValue == null) {
      paramValue = '';
    }
    const pattern = new RegExp('\\b('+paramName+'=).*?(&|#|$)');
    if (url.search(pattern) >= 0) {
      return url.replace(pattern,'$1' + paramValue + '$2');
    }
    url = url.replace(/[?#]$/,'');
    return url + (url.indexOf('?')> 0 ? '&' : '?') + paramName + '=' + paramValue;
  },

  digestMessage: async function(message) {
    const msgUint8 = new TextEncoder().encode(message); // encode as (utf-8) Uint8Array
    const hashBuffer = await window.crypto.subtle.digest("SHA-1", msgUint8); // hash the message
    const hashArray = Array.from(new Uint8Array(hashBuffer)); // convert buffer to byte array
     // convert bytes to hex string
    return hashArray
        .map((b) => b.toString(16).padStart(2, "0"))
        .join("");
  }
}
