import ExtensionContext from '../../ui/generic/js/modules/ExtensionContext.js';
import ConfigurationsPane from '../../ui/generic/js/modules/ConfigurationsPane.js';
import SearchableDropdown from '../../ui/generic/js/modules/SearchableDropdown.js';

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
  availableFieldsFilter: ctx.getElementById("available-fields-filter"),
  selectedFields: ctx.getElementById("selected-fields"),
  addButton: ctx.getElementById("add-button"),
  removeButton: ctx.getElementById("remove-button"),
  hyperlinkSettingsContainer: ctx.getElementById("hyperlink-settings-container"),
  linkedWorkitemSettingsContainer: ctx.getElementById("linked-workitem-settings-container"),

  init: function () {
    ctx.getElementById("fields-load-error").style.display = "none";

    this.availableFields.addEventListener("change", (event) => this.addButton.disabled = event.target.selectedIndex === -1);
    this.selectedFields.addEventListener("change", (event) => this.removeButton.disabled = event.target.selectedIndex === -1);
    this.addButton.addEventListener("click", () => this.addFieldClicked());
    this.removeButton.addEventListener("click", () => this.removeFieldClicked());
    this.availableFieldsFilter.addEventListener("input", () => this.applyAvailableFieldsFilter());

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
    this.applyAvailableFieldsFilter();
    this.checkHyperlinkSettingsVisibility();
    this.checkLinkedWorkitemSettingsVisibility();
  },

  // Hide options in the "Available fields" list whose label doesn't match every whitespace-separated
  // part of the filter term. Re-applied whenever the list is rebuilt so the filter survives moves.
  applyAvailableFieldsFilter: function () {
    const searchParts = this.availableFieldsFilter.value.toLowerCase().trim().split(/\s+/).filter(part => part.length > 0);
    for (const option of this.availableFields.options) {
      const hidden = searchParts.length > 0 && !searchParts.every(part => option.text.toLowerCase().includes(part));
      option.hidden = hidden;
      // Deselect options the filter hides so they can't be moved while invisible.
      if (hidden) {
        option.selected = false;
      }
    }
    // Setting option.selected programmatically doesn't fire 'change', so refresh the Add button state.
    this.addButton.disabled = this.availableFields.selectedIndex === -1;
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
    this.applyAvailableFieldsFilter();
    this.checkHyperlinkSettingsVisibility();
    this.checkLinkedWorkitemSettingsVisibility();
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
  dropdown: null,

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
            if (status.iconUrl) {
              opt.setAttribute('data-icon', status.iconUrl);
            }
            opt.innerHTML = status.wiTypeName ? `${status.name} [${status.id} - ${status.wiTypeName}]` : `${status.name} [${status.id}]`;
            this.statusesToIgnore.appendChild(opt);
          }
          // Upgrade to the shared Polarion-styled multiselect (chips + built-in search). Instantiated
          // after the options are populated so it reflects the loaded list.
          this.dropdown = new SearchableDropdown({
            element: this.statusesToIgnore,
            multiselect: true,
            placeholder: 'Select statuses to ignore...'
          });
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
  dropdown: null,

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
          this.dropdown = new SearchableDropdown({
            element: this.roles,
            multiselect: true,
            placeholder: 'Select hyperlink roles...'
          });
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
  dropdown: null,

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
          this.dropdown = new SearchableDropdown({
            element: this.roles,
            multiselect: true,
            placeholder: 'Select linked WorkItem roles...'
          });
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
  // Reflect the loaded selection in the wrapping dropdowns (setting option.selected above does not
  // dispatch a change event, so the chips must be synced explicitly).
  if (Statuses.dropdown) {
    Statuses.dropdown.syncFromElement();
  }
  if (HyperlinkRoles.dropdown) {
    HyperlinkRoles.dropdown.syncFromElement();
  }
  if (LinkedWorkItemRoles.dropdown) {
    LinkedWorkItemRoles.dropdown.syncFromElement();
  }
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
