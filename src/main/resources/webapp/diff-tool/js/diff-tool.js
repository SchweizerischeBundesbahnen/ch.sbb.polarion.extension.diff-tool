DiffTool = {

  init: function (sourceProjectId, sourceSpace, sourceDocument, sourceRevision) {
    this.sourceProjectId = sourceProjectId;
    this.sourceSpace = sourceSpace;
    this.sourceDocument = sourceDocument;
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
      this.getAsync({
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
    documentSelector.innerHTML = "<option disabled selected value> --- select document --- </option>"; // Clear previously loaded content

    document.getElementById("revision-selector").innerHTML = ""; // Clear previously loaded content
    document.getElementById("compare-documents").disabled = true; // Disable "Compare" button

    if (selectedSpace) {
      this.getAsync({
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
      this.actionInProgress({inProgress: true, message: "Loading revisions"});
      this.getAsync({
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

  showResult: function () {
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
        + `&linkRole=${linkRole}&config=${config}&chunkSize=10`;
    if (this.sourceRevision) {
      path += `&sourceRevision=${this.sourceRevision}`;
    }
    if (targetRevision) {
      path += `&targetRevision=${targetRevision}`;
    }

    window.open(path, '_blank');
  },

  getAsync: function ({url}) {
    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest();
      xhr.open("GET", url, true);
      xhr.setRequestHeader('Content-Type', 'application/json')
      xhr.responseType = "json";

      xhr.send();

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
    let elements = document.getElementsByClassName('hide-when-comparing-same');
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
  }
}