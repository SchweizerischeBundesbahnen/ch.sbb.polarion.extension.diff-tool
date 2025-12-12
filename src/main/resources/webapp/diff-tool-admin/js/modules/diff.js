import ExtensionContext from '../../ui/generic/js/modules/ExtensionContext.js';
import ConfigurationsPane from '../../ui/generic/js/modules/ConfigurationsPane.js';

const ctx = new ExtensionContext({
  extension: 'diff-tool',
  setting: 'diff',
  scopeFieldId: 'scope'
});

const conf = new ConfigurationsPane({
  ctx: ctx,
  setConfigurationContentCallback: setConfiguration,
});

ctx.onClick(
    'save-toolbar-button', saveDiffFields,
    'cancel-toolbar-button', ctx.cancelEdit,
    'default-toolbar-button', revertToDefault,
    'revisions-toolbar-button', ctx.toggleRevisions,
);

const Fields = {
  fields: [],
  availableFields: ctx.getElementById("available-fields"),
  selectedFields: ctx.getElementById("selected-fields"),
  addButton: ctx.getElementById("add-button"),
  removeButton: ctx.getElementById("remove-button"),
  hyperlinkSettingsContainer: ctx.getElementById("hyperlink-settings-container"),
  hyperlinkRolesSearchInput: ctx.getElementById("search-hyperlink-roles-input"),
  linkedWorkitemSettingsContainer: ctx.getElementById("linked-workitem-settings-container"),
  linkedWorkitemRolesSearchInput: ctx.getElementById("search-linked-workitem-roles-input"),

  init: function () {
    ctx.getElementById("fields-load-error").style.display = "none";

    this.availableFields.addEventListener("change", (event) => this.addButton.disabled = event.target.selectedIndex === -1);
    this.selectedFields.addEventListener("change", (event) => this.removeButton.disabled = event.target.selectedIndex === -1);
    this.addButton.addEventListener("click", () => this.addFieldClicked());
    this.removeButton.addEventListener("click", () => this.removeFieldClicked());

    this.hyperlinkRolesSearchInput.addEventListener('input', function() {
      const searchTerm = this.value.toLowerCase().trim();
      const options = HyperlinkRoles.roles.options;

      // Store currently selected values
      const selectedValues = Array.from(HyperlinkRoles.roles.selectedOptions)
          .map(option => option.value);

      const searchParts = searchTerm.split(/\s+/).filter(part => part.length > 0);

      // Filter options
      for (let i = 0; i < options.length; i++) {
        const option = options[i];
        const isMatch = searchParts.length === 0 ||
            searchParts.every(part =>
                option.text.toLowerCase().includes(part)
            );

        // Toggle visibility based on search match
        option.hidden = !isMatch;
      }

      // Re-select previously selected options
      selectedValues.forEach(value => {
        const option = HyperlinkRoles.roles.querySelector(`option[value="${value}"]`);
        if (option) {
          option.selected = true;
        }
      });
    });

    this.linkedWorkitemRolesSearchInput.addEventListener('input', function() {
      const searchTerm = this.value.toLowerCase().trim();
      const options = LinkedWorkItemRoles.roles.options;

      // Store currently selected values
      const selectedValues = Array.from(LinkedWorkItemRoles.roles.selectedOptions)
          .map(option => option.value);

      const searchParts = searchTerm.split(/\s+/).filter(part => part.length > 0);

      // Filter options
      for (let i = 0; i < options.length; i++) {
        const option = options[i];
        const isMatch = searchParts.length === 0 ||
            searchParts.every(part =>
                option.text.toLowerCase().includes(part)
            );

        // Toggle visibility based on search match
        option.hidden = !isMatch;
      }

      // Re-select previously selected options
      selectedValues.forEach(value => {
        const option = LinkedWorkItemRoles.roles.querySelector(`option[value="${value}"]`);
        if (option) {
          option.selected = true;
        }
      });
    });

    return new Promise((resolve, reject) => {
      ctx.callAsync({
        method: 'GET',
        url: `/polarion/${ctx.extension}/rest/internal/projects/${ctx.getValueById('project-id')}/workitem-fields`,
        contentType: 'application/json',
        onOk: (responseText) => {
          this.fields = JSON.parse(responseText);
          resolve();
        },
        onError: () => {
          ctx.getElementById("fields-load-error").style.display = "block";
          reject();
        }
      });
    });
  },

  resetState: function (selectedFields) {
    this.availableFields.innerHTML = "";
    this.selectedFields.innerHTML = "";
    this.addButton.disabled = true;
    this.removeButton.disabled = true;

    for (let field of this.fields) {
      this.addOption(this.availableFields, field.key, field.wiTypeId);
    }
    for (const field of selectedFields) {
      this.removeOption(this.availableFields, field.key, field.wiTypeId);
    }
    for (const field of selectedFields) {
      this.addOption(this.selectedFields, field.key, field.wiTypeId);
    }
    this.checkHyperlinkSettingsVisibility();
    this.checkLinkedWorkitemSettingsVisibility();
  },

  addFieldClicked: function () {
    this.moveSelectedFields(this.availableFields, this.selectedFields);
  },

  removeFieldClicked: function () {
    this.moveSelectedFields(this.selectedFields, this.availableFields);
  },

  moveSelectedFields: function (fromSelect, toSelect) {
    const selectedFields = Array.from(fromSelect.selectedOptions).map(option => {
      return {key: option.value, wiTypeId: "wiTypeId" in option.dataset ? option.dataset.wiTypeId : undefined}
    });
    for (const field of selectedFields) {
      this.removeOption(fromSelect, field.key, field.wiTypeId);
    }

    const newFields = [...selectedFields, ...Array.from(toSelect.options).map(option => {
      return {key: option.value, wiTypeId: "wiTypeId" in option.dataset ? option.dataset.wiTypeId : undefined}
    })];

    const newFieldsSorted = this.fields
        .filter(f => this.includesField(newFields, f))
        .sort((a, b) => a.name.localeCompare(b.name));

    // Reconstruct target dropdown to display new options in sorted order
    toSelect.innerHTML = "";
    for (const field of newFieldsSorted) {
      this.addOption(toSelect, field.key, field.wiTypeId);
    }
    this.checkHyperlinkSettingsVisibility();
  },

  addOption: function (select, fieldKey, wiTypeId) {
    const field = this.fields.find(f => f.key === fieldKey && this.sameWorkItemTypeIds(f.wiTypeId, wiTypeId));
    if (field) {
      const opt = document.createElement('option');
      opt.value = field.key;
      if (wiTypeId) {
        opt.dataset.wiTypeId = wiTypeId
      }
      opt.innerHTML = `${field.name} [${field.key}${field.wiTypeId ? " - " + field.wiTypeName : ""}]`;
      select.appendChild(opt);
    }
  },

  removeOption: function (select, fieldKey, wiTypeId) {
    for (let i=0; i < select.length; i++) {
      const option = select.options[i];
      if (option.value === fieldKey && this.sameWorkItemTypeIds(option.dataset.wiTypeId, wiTypeId)) {
        select.remove(i);
      }
    }
  },

  includesField: function (fieldsArray, field) {
    return fieldsArray.find(f => f.key === field.key && this.sameWorkItemTypeIds(f.wiTypeId, field.wiTypeId));
  },

  sameWorkItemTypeIds: function (wiTypeId1, wiTypeId2) {
    return wiTypeId1 === wiTypeId2 || (!wiTypeId1 && !wiTypeId2);
  },

  checkHyperlinkSettingsVisibility: function () {
    this.hyperlinkSettingsContainer.style.display = Array.from(Fields.selectedFields.options).some(option => option.value === 'hyperlinks') ? "flex" : "none";
  },

  checkLinkedWorkitemSettingsVisibility: function () {
    this.linkedWorkitemSettingsContainer.style.display = Array.from(Fields.selectedFields.options).some(option => option.value === 'linkedWorkItems') ? "flex" : "none";
  }
}

const Statuses = {
  statusesToIgnore: ctx.getElementById("statuses-to-ignore"),

  load: function () {
    ctx.getElementById("statuses-load-error").style.display = "none";

    return new Promise((resolve, reject) => {
      ctx.callAsync({
        method: 'GET',
        url: `/polarion/${ctx.extension}/rest/internal/projects/${ctx.getValueById('project-id')}/workitem-statuses`,
        contentType: 'application/json',
        onOk: (responseText) => {
          for (let status of JSON.parse(responseText)) {
            const opt = document.createElement('option');
            opt.value = status.id;
            opt.innerHTML = status.wiTypeName ? `${status.name} [${status.id} - ${status.wiTypeName}]` : `${status.name} [${status.id}]`;
            this.statusesToIgnore.appendChild(opt);
          }
          resolve();
        },
        onError: () => {
          ctx.getElementById("statuses-load-error").style.display = "block";
          reject();
        }
      });
    });
  },

}

const HyperlinkRoles = {
  roles: ctx.getElementById("hyperlink-roles"),

  load: function () {
    ctx.getElementById("hyperlink-roles-load-error").style.display = "none";

    return new Promise((resolve, reject) => {
      ctx.callAsync({
        method: 'GET',
        url: `/polarion/${ctx.extension}/rest/internal/projects/${ctx.getValueById('project-id')}/hyperlink-roles`,
        contentType: 'application/json',
        onOk: (responseText) => {
          for (let role of JSON.parse(responseText)) {
            const opt = document.createElement('option');
            opt.value = `${role.combinedId}`;
            opt.innerHTML = `[${role.workItemTypeName}] ${role.name}`;
            this.roles.appendChild(opt);
          }
          resolve();
        },
        onError: () => {
          ctx.getElementById("hyperlink-roles-load-error").style.display = "block";
          reject();
        }
      });
    });
  }
}

const LinkedWorkItemRoles = {
  roles: ctx.getElementById("linked-workitem-roles"),

  load: function () {
    ctx.getElementById("linked-workitem-roles-load-error").style.display = "none";

    return new Promise((resolve, reject) => {
      ctx.callAsync({
        method: 'GET',
        url: `/polarion/${ctx.extension}/rest/internal/projects/${ctx.getValueById('project-id')}/linked-workitem-roles`,
        contentType: 'application/json',
        onOk: (responseText) => {
          for (let role of JSON.parse(responseText)) {
            const opt = document.createElement('option');
            opt.value = `${role.id}`;
            opt.innerHTML = `${role.name}`;
            this.roles.appendChild(opt);
          }
          resolve();
        },
        onError: () => {
          ctx.getElementById("linked-workitem-roles-load-error").style.display = "block";
          reject();
        }
      });
    });
  }
}

function saveDiffFields() {
  ctx.hideActionAlerts();

  ctx.callAsync({
    method: 'PUT',
    url: `/polarion/${ctx.extension}/rest/internal/settings/${ctx.setting}/names/${conf.getSelectedConfiguration()}/content?scope=${ctx.scope}`,
    contentType: 'application/json',
    body: JSON.stringify({
      'diffFields': Array.from(Fields.selectedFields.options).map(option => {
        return {key: option.value, wiTypeId: "wiTypeId" in option.dataset ? option.dataset.wiTypeId : undefined}
      }),
      'statusesToIgnore': Array.from(Statuses.statusesToIgnore.selectedOptions).map(option => option.value),
      'hyperlinkRoles': Array.from(HyperlinkRoles.roles.selectedOptions).map(option => option.value),
      'linkedWorkItemRoles': Array.from(LinkedWorkItemRoles.roles.selectedOptions).map(option => option.value)
    }),
    onOk: () => {
      ctx.showSaveSuccessAlert();
      ctx.setNewerVersionNotificationVisible(false);
      conf.loadConfigurationNames();
    },
    onError: () => ctx.showSaveErrorAlert()
  });
}

function revertToDefault() {
  if (confirm("Are you sure you want to return the default value?")) {
    loadDefaultContent()
        .then((responseText) => {
          setConfiguration(responseText);
          ctx.showRevertedToDefaultAlert();
        })
  }
}

function setConfiguration(text) {
  const diffModel = JSON.parse(text);
  Fields.resetState(diffModel.diffFields);
  Array.from(Statuses.statusesToIgnore.options).forEach(option => option.selected = diffModel.statusesToIgnore.includes(option.value));
  Array.from(HyperlinkRoles.roles.options).forEach(option => option.selected = diffModel.hyperlinkRoles.includes(option.value));
  Array.from(LinkedWorkItemRoles.roles.options).forEach(option => option.selected = diffModel.linkedWorkItemRoles.includes(option.value));
  if (diffModel.bundleTimestamp !== ctx.getValueById('bundle-timestamp')) {
    loadDefaultContent()
        .then((responseText) => {
          const defaultDiffFieldsModel = JSON.parse(responseText);
          ctx.setNewerVersionNotificationVisible(diffModel.diffFields && defaultDiffFieldsModel.diffFields
              && (diffModel.diffFields.length !== defaultDiffFieldsModel.diffFields.length || diffModel.diffFields !== defaultDiffFieldsModel.diffFields));
        })
  }
}

function loadDefaultContent() {
  return new Promise((resolve, reject) => {
    ctx.setLoadingErrorNotificationVisible(false);
    ctx.hideActionAlerts();

    ctx.callAsync({
      method: 'GET',
      url: `/polarion/${ctx.extension}/rest/internal/settings/${ctx.setting}/default-content`,
      contentType: 'application/json',
      onOk: (responseText) => resolve(responseText),
      onError: () => {
        ctx.setLoadingErrorNotificationVisible(true);
        reject();
      }
    });
  });
}

Promise.all([
  Fields.init(),
  Statuses.load(),
  HyperlinkRoles.load(),
  LinkedWorkItemRoles.load(),
]).then(() => {
  conf.loadConfigurationNames();
});
