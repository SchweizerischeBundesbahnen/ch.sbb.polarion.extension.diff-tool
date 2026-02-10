DiffToolWidgetUtils = {

  updateWorkItemsDiffButton: function(clickedElement, sourceProjectId) {
    const DISABLED_BUTTON_CLASS = "polarion-TestsExecutionButton-buttons-defaultCursor";

    const diffWidget = clickedElement?.closest("div.polarion-DiffTool");
    const button = diffWidget?.querySelector(".polarion-TestsExecutionButton-buttons");
    if (button) {
      if (button.classList.contains(DISABLED_BUTTON_CLASS) && diffWidget.querySelectorAll('input[type="checkbox"]:checked').length > 0) {
        button.classList.remove(DISABLED_BUTTON_CLASS);
        this.registeredButtonClickListener = () => this.openWorkItemsDiffApplication(diffWidget, sourceProjectId);
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

  updateCollectionsDiffButton: function(clickedElement, sourceProjectId) {
    const DISABLED_BUTTON_CLASS = "polarion-TestsExecutionButton-buttons-defaultCursor";

    const diffWidget = clickedElement?.closest("div.polarion-DiffTool");
    const button = diffWidget?.querySelector(".polarion-TestsExecutionButton-buttons");
    if (button) {
      const leftCollectionSelected = diffWidget.querySelector('input[type="radio"][name="source-collection"]:checked');
      const rightCollectionSelected = diffWidget.querySelector('input[type="radio"][name="target-collection"]:checked');

      if (button.classList.contains(DISABLED_BUTTON_CLASS) && leftCollectionSelected && rightCollectionSelected) {
        button.classList.remove(DISABLED_BUTTON_CLASS);
        this.registeredButtonClickListener = () => this.openCollectionsDiffApplication(diffWidget, sourceProjectId);
        button.addEventListener("click", this.registeredButtonClickListener);
      }
      if (!button.classList.contains(DISABLED_BUTTON_CLASS) && !(leftCollectionSelected || rightCollectionSelected)) {
        button.classList.add(DISABLED_BUTTON_CLASS);
        button.removeEventListener("click", this.registeredButtonClickListener);
      }
    }
  },

  selectAllItems: function(clickedElement) {
    const diffWidget = clickedElement?.closest("div.polarion-DiffTool");
    if (diffWidget) {
      diffWidget.querySelectorAll('input[type="checkbox"]:not(.export-all)').forEach(checkbox => {
        checkbox.checked = clickedElement.checked;
      });
    }
  },

  openWorkItemsDiffApplication: function(diffWidget, sourceProjectId) {
    const targetProjectId = document.getElementById("target-project-selector").value;
    const linkRole = document.getElementById("link-role-selector").value;
    const config = document.getElementById("config-selector").value;

    let path = `/polarion/diff-tool-app/ui/app/workitems.html`
        + `?sourceProjectId=${sourceProjectId}&targetProjectId=${targetProjectId}&linkRole=${linkRole}&config=${config}`;

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

  openCollectionsDiffApplication: function(diffWidget, sourceProjectId) {
    const linkRole = document.getElementById("link-role-selector").value;
    const config = document.getElementById("config-selector").value;
    const targetProjectId = document.getElementById("target-project-selector").value;
    const sourceCollectionSelected = diffWidget.querySelector('input[type="radio"][name="source-collection"]:checked');
    const targetCollectionSelected = diffWidget.querySelector('input[type="radio"][name="target-collection"]:checked');

    const path = `/polarion/diff-tool-app/ui/app/collections.html`
        + `?sourceProjectId=${sourceProjectId}&sourceCollectionId=${sourceCollectionSelected.dataset.id}`
        + `&targetProjectId=${targetProjectId}&targetCollectionId=${targetCollectionSelected.dataset.id}`
        + `&linkRole=${linkRole}&config=${config}&compareAs=Workitems`;

    window.open(path, '_blank');
  },

  replaceUrlParam: function(url, paramName, paramValue) {
    if (paramValue == null) {
      paramValue = '';
    }
    const pattern = new RegExp('\\b(' + paramName + '=).*?(&|#|$)');
    if (url.search(pattern) >= 0) {
      return url.replace(pattern, '$1' + paramValue + '$2');
    }
    url = url.replace(/[?#]$/, '');
    return url + (url.indexOf('?') > 0 ? '&' : '?') + paramName + '=' + paramValue;
  },

  digestMessage: async function(message) {
    const msgUint8 = new TextEncoder().encode(message);
    const hashBuffer = await window.crypto.subtle.digest("SHA-1", msgUint8);
    const hashArray = Array.from(new Uint8Array(hashBuffer));
    return hashArray
        .map((b) => b.toString(16).padStart(2, "0"))
        .join("");
  }
}
