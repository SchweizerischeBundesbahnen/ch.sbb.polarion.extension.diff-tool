const DIFF_TOOL_NEW_DOCUMENT_VALUE = "-1";

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
    const selectedProject = document.getElementById("project-selector").value;
    this.actionInProgress({inProgress: true, message: "Loading spaces"});
    const spaceSelector = document.getElementById("space-selector");
    spaceSelector.innerHTML = "<option disabled selected value> --- select space --- </option>"; // Clear previously loaded content

    document.getElementById("document-selector").innerHTML = ""; // Clear previously loaded content
    document.getElementById("revision-selector").innerHTML = ""; // Clear previously loaded content
    document.getElementById("compare-documents").disabled = true; // Disable "Compare" button

    if (selectedProject) {
      this.callAsync({
        url: `/polarion/diff-tool/rest/internal/projects/${selectedProject}/spaces`
      }).then((response) => {
        this.actionInProgress({inProgress: false});
        for (let {id, name} of response) {
          const option = document.createElement('option');
          option.value = id;
          option.text = name;
          spaceSelector.appendChild(option);
        }
      }).catch(() => {
        this.actionInProgress({inProgress: false});
        this.showAlert({alertType: "error", message: "Error occurred loading spaces"});
      });
    }
  },

  spaceChanged: function () {
    const selectedProject = document.getElementById("project-selector").value;
    const selectedSpace = document.getElementById("space-selector").value;

    this.actionInProgress({inProgress: true, message: "Loading documents"});
    const documentSelector = document.getElementById("document-selector");
    documentSelector.innerHTML = `<option disabled selected value> --- select document --- </option><option value='${DIFF_TOOL_NEW_DOCUMENT_VALUE}'>&lt;&lt; new document &gt;&gt;</option>`;

    document.getElementById("revision-selector").innerHTML = ""; // Clear previously loaded content
    document.getElementById("compare-documents").disabled = true; // Disable "Compare" button

    if (selectedSpace) {
      this.callAsync({
        url: `/polarion/diff-tool/rest/internal/projects/${selectedProject}/spaces/${selectedSpace}/documents`
      }).then((response) => {
        this.actionInProgress({inProgress: false});
        for (let {id, title} of response) {
          const option = document.createElement('option');
          option.value = id;
          option.text = title;
          documentSelector.appendChild(option);
        }
      }).catch(() => {
        this.actionInProgress({inProgress: false});
        this.showAlert({alertType: "error", message: "Error occurred loading documents"});
      });
    }
  },

  documentChanged: function () {
    const sameDoc = this.compareSameDocument();
    const selectedProject = sameDoc ? this.sourceProjectId : document.getElementById("project-selector").value;
    const selectedSpace = sameDoc ? this.sourceSpace : document.getElementById("space-selector").value;
    const selectedDocument = sameDoc ? this.sourceDocument : document.getElementById("document-selector").value;

    if (selectedDocument === DIFF_TOOL_NEW_DOCUMENT_VALUE) {
      this.newDocumentSelected(true);
    } else {
      this.newDocumentSelected(false);
      this.loadRevisions(selectedProject, selectedSpace, selectedDocument)
    }
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
      this.actionInProgress({inProgress: true, message: "Loading revisions"});
      this.callAsync({
        url: `/polarion/diff-tool/rest/internal/projects/${selectedProject}/spaces/${selectedSpace}/documents/${selectedDocument}/revisions`
      }).then((response) => {
        this.actionInProgress({inProgress: false});
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
        this.actionInProgress({inProgress: false});
        this.showAlert({alertType: "error", message: "Error occurred loading revisions"});
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

  newDocumentSelected: function (selected) {
    const selectedProject = this.compareSameDocument() ? this.sourceProjectId : document.getElementById("project-selector").value;

    if (selected) {
      document.querySelectorAll(".comparison.form-wrapper .hide-when-new-document").forEach(element => element.classList.add("hide"));
      document.querySelectorAll(".comparison.form-wrapper .hide-when-comparing").forEach(element => element.classList.remove("hide"));
      if (this.settingsProjectId !== selectedProject) {
        this.reloadSettings(selectedProject);
      }
    } else {
      document.querySelectorAll(".comparison.form-wrapper .hide-when-new-document").forEach(element => element.classList.remove("hide"));
      document.querySelectorAll(".comparison.form-wrapper .hide-when-comparing").forEach(element => element.classList.add("hide"));
      if (this.settingsProjectId !== this.sourceProjectId) {
        this.reloadSettings(this.sourceProjectId);
      }
    }
  },

  reloadSettings: function (projectId) {
    this.actionInProgress({inProgress: true});
    this.callAsync({
      url: `/polarion/diff-tool/rest/internal/settings/diff/names?scope=project/${projectId}/`
    }).then((response) => {
      this.actionInProgress({inProgress: false});
      const configSelector = document.getElementById("config-selector");
      configSelector.innerHTML = ""; // Clear previously loaded content
      for (let {name} of response) {
        const option = document.createElement('option');
        option.value = name;
        option.text = name;
        configSelector.appendChild(option);
      }
      this.settingsProjectId = projectId;
    }).catch(() => {
      this.actionInProgress({inProgress: false});
      this.showAlert({alertType: "error", message: `Error occurred loading project [${projectId}] diff configuration`});
    });

  },

  createNewDocument: function () {
    const selectedProject = document.getElementById("project-selector").value;
    const selectedSpace = document.getElementById("space-selector").value;
    const linkRole = document.getElementById("link-role-selector").value;
    const config = document.getElementById("config-selector").value;

    const requestBody = {
      targetDocumentIdentifier: {
        projectId: selectedProject,
        spaceId: selectedSpace,
        name: this.sourceDocument,
      },
      targetDocumentTitle: this.sourceDocumentTitle,
      linkRoleId: linkRole,
      configName: config
    }
    const revisionUrlPart = this.sourceRevision ? `?revision=${this.sourceRevision}` : '';

    this.actionInProgress({inProgress: true, message: "Creating a document"});
    this.callAsync({
      method: "POST",
      url: `/polarion/diff-tool/rest/internal/projects/${this.sourceProjectId}/spaces/${this.sourceSpace}/documents/${this.sourceDocument}/duplicate${revisionUrlPart}`,
      body: JSON.stringify(requestBody)
    }).then((response) => {
      this.actionInProgress({inProgress: false});

      const alert = document.getElementById(`creation-success`);
      const basePath = `//${window.location.host}${window.location.pathname}`;
      const anchor = alert.querySelector("a");
      anchor.innerText = `${basePath}#/project/${response.projectId}/wiki/${response.spaceId}/${response.name}`;
      anchor.href = `${basePath}#/project/${encodeURIComponent(response.projectId)}/wiki/${encodeURIComponent(response.spaceId)}/${encodeURIComponent(response.name)}`;
      anchor.target = "_blank";
      alert.style.display = "block";
    }).catch((error) => {
      this.actionInProgress({inProgress: false});
      this.showAlert({alertType: "error", message: error?.response?.message ? error.response.message : "Error creating document"});
    });
  },

  showDiffResult: function () {
    const sameDoc = this.compareSameDocument();
    const targetProjectId = sameDoc ? this.sourceProjectId : document.getElementById("project-selector").value;
    const targetSpace = sameDoc ? this.sourceSpace : document.getElementById("space-selector").value;
    const targetDocument = sameDoc ? this.sourceDocument : document.getElementById("document-selector").value;
    const targetRevision = document.getElementById(this.manualRevision() ? "select-revision-manual-input" : "revision-selector").value;
    const linkRole = document.getElementById("link-role-selector").value;
    const config = document.getElementById("config-selector").value;

    let path = `/polarion/diff-tool-app/ui/app/index.html`
        + `?sourceProjectId=${this.sourceProjectId}&sourceSpaceId=${this.sourceSpace}&sourceDocument=${this.sourceDocument}`
        + `&targetProjectId=${targetProjectId}&targetSpaceId=${targetSpace}&targetDocument=${targetDocument}`
        + `&linkRole=${linkRole}&config=${config}`;
    if (this.sourceRevision) {
      path += `&sourceRevision=${this.sourceRevision}`;
    }
    if (targetRevision) {
      path += `&targetRevision=${targetRevision}`;
    }

    if (document.getElementById("use-work-items-filter").checked && localStorage) {
      const filterHash = this.generateUuid();
      localStorage.setItem(filterHash + "_filter", document.getElementById("work-items-filter-input").value);
      localStorage.setItem(filterHash + "_type", document.getElementById("include-work-items").checked ? "include" : "exclude");
      path += `&filter=${filterHash}`;
    }

    window.open(path, '_blank');
  },

  generateUuid : function() {
    return (
        String('xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx')
    ).replace(/[xy]/g, (character) => {
      const random = (Math.random() * 16) | 0;
      const value = character === "x" ? random : (random & 0x3) | 0x8;

      return value.toString(16);
    });
  },

  callAsync: function ({method = "GET", url, body = null}) {
    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest();
      xhr.open(method, url, true);
      xhr.setRequestHeader('Content-Type', 'application/json')
      xhr.responseType = "json";

      xhr.send(body);

      xhr.onreadystatechange = function () {
        if (xhr.readyState === 4) {
          if (xhr.status === 200 || xhr.status === 204) {
            resolve(xhr.response);
          } else {
            reject(xhr)
          }
        }
      };
      xhr.onerror = function (error) {
        reject(error);
      };
    });
  },

  actionInProgress: function ({inProgress, message}) {
    if (inProgress) {
      this.hideAlerts();
    }
    document.querySelectorAll(".comparison.form-wrapper .action-button").forEach(button => {
      button.disabled = inProgress;
    });
    document.getElementById("in-progress-message").innerHTML = message;
    if (inProgress) {
      document.querySelector(".comparison.form-wrapper .in-progress-overlay").classList.add("show");
    } else {
      document.querySelector(".comparison.form-wrapper .in-progress-overlay").classList.remove("show");
    }
  },

  showAlert: function ({alertType, message}) {
    const alert = document.querySelector(`.comparison.form-wrapper .alert.alert-${alertType}`);
    if (alert) {
      alert.innerHTML = message;
      alert.style.display = "block";
    }
  },

  hideAlerts: function () {
    document.querySelectorAll(".comparison.form-wrapper .alert").forEach(alert => {
      alert.style.display = "none";
    });
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
}
