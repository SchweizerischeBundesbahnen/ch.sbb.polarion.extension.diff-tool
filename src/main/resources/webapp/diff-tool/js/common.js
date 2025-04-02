Common = {

  loadSpaces: function (diff) {
    const selectedProject = document.getElementById(`${this.prefix(diff)}-project-selector`).value;
    Common.actionInProgress({inProgress: true, message: "Loading spaces", diff: diff});
    const spaceSelector = document.getElementById(`${this.prefix(diff)}-space-selector`);
    spaceSelector.innerHTML = "<option disabled selected value> --- select space --- </option>"; // Clear previously loaded content
    if(!selectedProject) {
      return;
    }
    this.callAsync({
      url: `/polarion/diff-tool/rest/internal/projects/${selectedProject}/spaces`
    }).then((response) => {
      this.actionInProgress({inProgress: false, diff: diff});
      for (let {id, name} of response) {
        const option = document.createElement('option');
        option.value = id;
        option.text = name;
        spaceSelector.appendChild(option);
      }
    }).catch(() => {
      this.actionInProgress({inProgress: false, diff: diff});
      this.showAlert({alertType: "error", message: "Error occurred loading spaces", diff: diff});
    });
  },

  reloadSettings: function (projectId, diff) {
    this.actionInProgress({inProgress: true, diff: diff});
    this.callAsync({
      url: `/polarion/diff-tool/rest/internal/settings/diff/names?scope=project/${projectId}/`
    }).then((response) => {
      this.actionInProgress({inProgress: false, diff: diff});
      const configSelector = document.getElementById(`${this.prefix(diff)}-config-selector`);
      configSelector.innerHTML = ""; // Clear previously loaded content
      for (let {name} of response) {
        const option = document.createElement('option');
        option.value = name;
        option.text = name;
        configSelector.appendChild(option);
      }
      this.settingsProjectId = projectId;
    }).catch(() => {
      this.actionInProgress({inProgress: false, diff: diff});
      this.showAlert({alertType: "error", message: `Error occurred loading project [${projectId}] diff configuration`, diff: diff});
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

  actionInProgress: function ({inProgress, message, diff}) {
    if (inProgress) {
      this.hideAlerts(diff);
    }
    document.querySelectorAll(`.${this.prefix(diff)}.form-wrapper .action-button`).forEach(button => {
      button.disabled = inProgress;
    });
    document.getElementById(`${this.prefix(diff)}-in-progress-message`).innerHTML = message;
    if (inProgress) {
      document.querySelector(`.${this.prefix(diff)}.form-wrapper .in-progress-overlay`).classList.add("show");
    } else {
      document.querySelector(`.${this.prefix(diff)}.form-wrapper .in-progress-overlay`).classList.remove("show");
    }
  },

  showAlert: function ({alertType, message, diff}) {
    const alert = document.querySelector(`.${this.prefix(diff)}.form-wrapper .alert.alert-${alertType}`);
    if (alert) {
      alert.innerText = message;
      alert.style.display = "block";
    }
  },

  hideAlerts: function (diff) {
    document.querySelectorAll(`.${this.prefix(diff)}.form-wrapper .alert`).forEach(alert => {
      alert.style.display = "none";
    });
  },

  prefix: function (diff) {
    return diff ? "comparison" : "copy";
  }
}
