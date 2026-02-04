import ExtensionContext from "/polarion/diff-tool/ui/generic/js/modules/ExtensionContext.js";
import SearchableDropdown from "/polarion/diff-tool/ui/generic/js/modules/SearchableDropdown.js";
import GenericMixin from "./GenericMixin.js";

export default class CopyTool extends GenericMixin {

  constructor(sourceProjectId, sourceSpace, sourceDocument, sourceDocumentTitle, sourceRevision) {
    super();

    this.ctx = new ExtensionContext({});

    this.sourceProjectId = sourceProjectId;
    this.settingsProjectId = sourceProjectId;
    this.sourceSpace = sourceSpace;
    this.sourceDocument = sourceDocument;
    this.sourceDocumentTitle = sourceDocumentTitle;
    this.sourceRevision = sourceRevision;

    this.ctx.onClick('create-document', () => this.createNewDocument());

    this.resetParentsOverflowHidden(this.ctx.getElementById('copy-project-selector'));

    // First generate dropdown with data, remove selection and only then assign event listener
    this.projectDropdown = new SearchableDropdown({
      element: '#copy-project-selector',
      placeholder: 'Select Project...'
    });
    this.ctx.onChange('copy-project-selector', () => this.projectChanged());
    this.projectDropdown.restoreSelection();

    // First generate dropdown with data, remove selection and only then assign event listener
    this.spaceDropdown = new SearchableDropdown({
      element: '#copy-space-selector',
      placeholder: 'Select Space...'
    });
    this.ctx.onChange('copy-space-selector', () => this.spaceChanged());

    this.linkRoleDropdown = new SearchableDropdown({
      element: '#copy-link-role-selector',
      placeholder: 'Select Link Role...'
    });
    this.linkRoleDropdown.restoreSelection();

    this.configDropdown = new SearchableDropdown({
      element: '#copy-config-selector',
      placeholder: 'Select Configuration...'
    });
    this.configDropdown.restoreSelection();

    this.handleRefsDropdown = new SearchableDropdown({
      element: '#handle-refs-selector',
      placeholder: 'Select Behaviour...',
      searchable: false
    });
    this.handleRefsDropdown.restoreSelection();

  }

  projectChanged() {
    this.ctx.disableIf("create-document", true);
    this.loadSpaces(false, () => {
      this.spaceDropdown.refresh();
      this.reloadSettings(this.ctx.getValue("copy-project-selector"), false)
    });
  }

  spaceChanged() {
    const selectedSpace = this.ctx.getValue("copy-space-selector");
    this.ctx.disableIf("create-document", !selectedSpace);
  }

  createNewDocument() {
    const selectedProject = this.ctx.getValue("copy-project-selector");
    const selectedSpace = this.ctx.getValue("copy-space-selector");
    const linkRole = this.ctx.getValue("copy-link-role-selector");
    const config = this.ctx.getValue("copy-config-selector");
    const handleRefs = this.ctx.getValue("handle-refs-selector");

    const requestBody = {
      targetDocumentIdentifier: {
        projectId: selectedProject,
        spaceId: selectedSpace,
        name: this.sourceDocument,
      },
      targetDocumentTitle: this.sourceDocumentTitle,
      linkRoleId: linkRole,
      configName: config,
      handleReferences: handleRefs,
    };
    const revisionUrlPart = this.sourceRevision ? `?revision=${this.sourceRevision}` : '';

    this.actionInProgress({inProgress: true, message: "Creating a document", diff: false});
    this.callAsync({
      method: "POST",
      url: `/polarion/diff-tool/rest/internal/projects/${this.sourceProjectId}/spaces/${this.sourceSpace}/documents/${this.sourceDocument}/duplicate${revisionUrlPart}`,
      body: JSON.stringify(requestBody)
    }).then((response) => {
      this.actionInProgress({inProgress: false, diff: false});

      const alert = this.ctx.getElementById(`creation-success`);
      const basePath = `//${window.location.host}${window.location.pathname}`;
      const anchor = alert.querySelector("a");

      const showSpace = response.spaceId && "_default" !== response.spaceId;
      const spaceInnerText = showSpace ? `${response.spaceId}/` : "";
      const spacePathPart = showSpace ? `${encodeURIComponent(response.spaceId)}/` : "";

      anchor.innerText = `${basePath}#/project/${response.projectId}/${spaceInnerText}${response.name}`;
      anchor.href = `${basePath}#/project/${encodeURIComponent(response.projectId)}/wiki/${spacePathPart}${encodeURIComponent(response.name)}`;
      anchor.target = "_blank";
      alert.style.display = "block";
    }).catch((error) => {
      this.actionInProgress({inProgress: false, diff: false});
      this.showAlert({alertType: "error", message: error?.response?.message ? error.response.message : "Error creating document", diff: false});
    });
  }
}
