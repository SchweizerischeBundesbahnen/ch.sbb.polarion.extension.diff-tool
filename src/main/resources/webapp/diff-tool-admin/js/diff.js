SbbCommon.init({
  extension: 'diff-tool',
  setting: 'diff',
  scope: SbbCommon.getValueById('scope')
});
Configurations.init({
  setConfigurationContentCallback: setDiffFields
});

const Fields = {
  fields: [],
  availableFields: document.getElementById("available-fields"),
  selectedFields: document.getElementById("selected-fields"),
  addButton: document.getElementById("add-button"),
  removeButton: document.getElementById("remove-button"),
  hyperlinkSettingsContainer: document.getElementById("hyperlink-settings-container"),
  hyperlinkRolesSearchInput: document.getElementById("search-hyperlink-roles-input"),

  init: function () {
    document.getElementById("fields-load-error").style.display = "none";

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

    return new Promise((resolve, reject) => {
      SbbCommon.callAsync({
        method: 'GET',
        url: `/polarion/${SbbCommon.extension}/rest/internal/projects/${SbbCommon.getValueById('project-id')}/workitem-fields`,
        contentType: 'application/json',
        onOk: (responseText) => {
          this.fields = JSON.parse(responseText);
          resolve();
        },
        onError: () => {
          document.getElementById("fields-load-error").style.display = "block";
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
  }
}

const Statuses = {
  statusesToIgnore: document.getElementById("statuses-to-ignore"),

  load: function () {
    document.getElementById("statuses-load-error").style.display = "none";

    return new Promise((resolve, reject) => {
      SbbCommon.callAsync({
        method: 'GET',
        url: `/polarion/${SbbCommon.extension}/rest/internal/projects/${SbbCommon.getValueById('project-id')}/workitem-statuses`,
        contentType: 'application/json',
        onOk: (responseText) => {
          for (let status of JSON.parse(responseText)) {
            const opt = document.createElement('option');
            opt.value = status.id;
            opt.innerHTML = status.name;
            this.statusesToIgnore.appendChild(opt);
          }
          resolve();
        },
        onError: () => {
          document.getElementById("statuses-load-error").style.display = "block";
          reject();
        }
      });
    });
  },

}

const HyperlinkRoles = {
  roles: document.getElementById("hyperlink-roles"),

  load: function () {
    document.getElementById("hyperlink-roles-load-error").style.display = "none";

    return new Promise((resolve, reject) => {
      SbbCommon.callAsync({
        method: 'GET',
        url: `/polarion/${SbbCommon.extension}/rest/internal/projects/${SbbCommon.getValueById('project-id')}/hyperlink-roles`,
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
          document.getElementById("hyperlink-roles-load-error").style.display = "block";
          reject();
        }
      });
    });
  },

}

function saveDiffFields() {
  SbbCommon.hideActionAlerts();

  SbbCommon.callAsync({
    method: 'PUT',
    url: `/polarion/${SbbCommon.extension}/rest/internal/settings/${SbbCommon.setting}/names/${Configurations.getSelectedConfiguration()}/content?scope=${SbbCommon.scope}`,
    contentType: 'application/json',
    body: JSON.stringify({
      'diffFields': Array.from(Fields.selectedFields.options).map(option => {
        return {key: option.value, wiTypeId: "wiTypeId" in option.dataset ? option.dataset.wiTypeId : undefined}
      }),
      'statusesToIgnore': Array.from(Statuses.statusesToIgnore.selectedOptions).map(option => option.value),
      'hyperlinkRoles': Array.from(HyperlinkRoles.roles.selectedOptions).map(option => option.value)
    }),
    onOk: () => {
      SbbCommon.showSaveSuccessAlert();
      SbbCommon.setNewerVersionNotificationVisible(false);
      Configurations.loadConfigurationNames();
    },
    onError: () => SbbCommon.showSaveErrorAlert()
  });
}

function revertToDefault() {
  if (confirm("Are you sure you want to return the default value?")) {
    loadDefaultContent()
        .then((responseText) => {
          setDiffFields(responseText);
          SbbCommon.showRevertedToDefaultAlert();
        })
  }
}

function setDiffFields(text) {
  const diffFieldsModel = JSON.parse(text);
  Fields.resetState(diffFieldsModel.diffFields);
  Array.from(Statuses.statusesToIgnore.options).forEach(option => option.selected = diffFieldsModel.statusesToIgnore.includes(option.value));
  Array.from(HyperlinkRoles.roles.options).forEach(option => option.selected = diffFieldsModel.hyperlinkRoles.includes(option.value));
  if (diffFieldsModel.bundleTimestamp !== SbbCommon.getValueById('bundle-timestamp')) {
    loadDefaultContent()
        .then((responseText) => {
          const defaultDiffFieldsModel = JSON.parse(responseText);
          SbbCommon.setNewerVersionNotificationVisible(diffFieldsModel.diffFields && defaultDiffFieldsModel.diffFields
              && (diffFieldsModel.diffFields.length !== defaultDiffFieldsModel.diffFields.length || diffFieldsModel.diffFields !== defaultDiffFieldsModel.diffFields));
        })
  }
}

function loadDefaultContent() {
  return new Promise((resolve, reject) => {
    SbbCommon.setLoadingErrorNotificationVisible(false);
    SbbCommon.hideActionAlerts();

    SbbCommon.callAsync({
      method: 'GET',
      url: `/polarion/${SbbCommon.extension}/rest/internal/settings/${SbbCommon.setting}/default-content`,
      contentType: 'application/json',
      onOk: (responseText) => resolve(responseText),
      onError: () => {
        SbbCommon.setLoadingErrorNotificationVisible(true);
        reject();
      }
    });
  });
}

Promise.all([
  Fields.init(),
  Statuses.load(),
  HyperlinkRoles.load()
]).then(() => {
  Configurations.loadConfigurationNames();
});
