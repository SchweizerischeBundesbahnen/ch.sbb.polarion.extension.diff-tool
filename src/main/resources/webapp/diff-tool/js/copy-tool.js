CopyTool = {

  init: function (sourceProjectId, sourceSpace, sourceDocument, sourceDocumentTitle, sourceRevision) {
    this.sourceProjectId = sourceProjectId;
    this.settingsProjectId = sourceProjectId;
    this.sourceSpace = sourceSpace;
    this.sourceDocument = sourceDocument;
    this.sourceDocumentTitle = sourceDocumentTitle;
    this.sourceRevision = sourceRevision;
  },

  projectChanged: function () {
    document.getElementById("create-document").disabled = true; // Disable "Create" button
    Common.loadSpaces(false, () =>
        Common.reloadSettings(document.getElementById("copy-project-selector").value, false));
  },

  spaceChanged: function () {
    const selectedSpace = document.getElementById("copy-space-selector").value;
    document.getElementById("create-document").disabled = !selectedSpace;
  },

  createNewDocument: function () {
    const selectedProject = document.getElementById("copy-project-selector").value;
    const selectedSpace = document.getElementById("copy-space-selector").value;
    const linkRole = document.getElementById("copy-link-role-selector").value;
    const config = document.getElementById("copy-config-selector").value;

    const requestBody = {
      targetDocumentIdentifier: {
        projectId: selectedProject,
        spaceId: selectedSpace,
        name: this.sourceDocument,
      },
      targetDocumentTitle: this.sourceDocumentTitle,
      linkRoleId: linkRole,
      configName: config,
      handleReferences: document.getElementById("overwrite-referenced-workitems").checked ? 'ALWAYS_OVERWRITE' : 'DEFAULT',
    }
    const revisionUrlPart = this.sourceRevision ? `?revision=${this.sourceRevision}` : '';

    Common.actionInProgress({inProgress: true, message: "Creating a document", diff: false});
    Common.callAsync({
      method: "POST",
      url: `/polarion/diff-tool/rest/internal/projects/${this.sourceProjectId}/spaces/${this.sourceSpace}/documents/${this.sourceDocument}/duplicate${revisionUrlPart}`,
      body: JSON.stringify(requestBody)
    }).then((response) => {
      Common.actionInProgress({inProgress: false, diff: false});

      const alert = document.getElementById(`creation-success`);
      const basePath = `//${window.location.host}${window.location.pathname}`;
      const anchor = alert.querySelector("a");

      // do not use space if it is "_default"
      const showSpace = response.spaceId && "_default" !== response.spaceId;
      const spaceInnerText = showSpace ? `${response.spaceId}/` : "";
      const spacePathPart = showSpace ? `${encodeURIComponent(response.spaceId)}/` : "";

      anchor.innerText = `${basePath}#/project/${response.projectId}/${spaceInnerText}${response.name}`;
      anchor.href = `${basePath}#/project/${encodeURIComponent(response.projectId)}/wiki/${spacePathPart}${encodeURIComponent(response.name)}`;
      anchor.target = "_blank";
      alert.style.display = "block";
    }).catch((error) => {
      Common.actionInProgress({inProgress: false, diff: false});
      Common.showAlert({alertType: "error", message: error?.response?.message ? error.response.message : "Error creating document", diff: false});
    });
  },
}
