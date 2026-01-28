export default class GenericMixin {

  loadSpaces(diff, postActionCallback) {
    const selectedProject = document.getElementById(`${this.prefix(diff)}-project-selector`).value;
    const spaceSelector = document.getElementById(`${this.prefix(diff)}-space-selector`);
    spaceSelector.innerHTML = "";
    if (!selectedProject) {
      if (postActionCallback) {
        postActionCallback();
      }
      return;
    }
    this.actionInProgress({inProgress: true, message: "Loading spaces", diff: diff});
    this.callAsync({
      url: `/polarion/diff-tool/rest/internal/projects/${selectedProject}/spaces`
    }).then((response) => {
      this.actionInProgress({inProgress: false, diff: diff});
      spaceSelector.innerHTML = "";
      for (const {id, name} of response) {
        const option = document.createElement('option');
        option.value = id;
        option.text = name;
        spaceSelector.appendChild(option);
      }
    }).then(() => {
      if (postActionCallback) {
        postActionCallback();
      }
    }).catch(() => {
      this.actionInProgress({inProgress: false, diff: diff});
      this.showAlert({alertType: "error", message: "Error occurred loading spaces", diff: diff});
    });
  }

  reloadSettings(projectId, diff) {
    this.actionInProgress({inProgress: true, diff: diff});
    this.callAsync({
      url: `/polarion/diff-tool/rest/internal/settings/diff/names?scope=project/${projectId}/`
    }).then((response) => {
      this.actionInProgress({inProgress: false, diff: diff});
      const configSelector = document.getElementById(`${this.prefix(diff)}-config-selector`);
      configSelector.innerHTML = "";
      for (const {name} of response) {
        const option = document.createElement('option');
        option.value = name;
        option.text = name;
        configSelector.appendChild(option);
      }
    }).catch(() => {
      this.actionInProgress({inProgress: false, diff: diff});
      this.showAlert({alertType: "error", message: `Error occurred loading project [${projectId}] diff configuration`, diff: diff});
    });
  }

  callAsync({method = "GET", url, body = null}) {
    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest();
      xhr.open(method, url, true);
      xhr.setRequestHeader('Content-Type', 'application/json');
      xhr.responseType = "json";

      xhr.send(body);

      xhr.onreadystatechange = function () {
        if (xhr.readyState === 4) {
          if (xhr.status === 200 || xhr.status === 204) {
            resolve(xhr.response);
          } else {
            reject(xhr);
          }
        }
      };
      xhr.onerror = function (error) {
        reject(error);
      };
    });
  }

  actionInProgress({inProgress, message, diff}) {
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
  }

  showAlert({alertType, message, diff}) {
    const alert = document.querySelector(`.${this.prefix(diff)}.form-wrapper .alert.alert-${alertType}`);
    if (alert) {
      alert.innerText = message;
      alert.style.display = "block";
    }
  }

  hideAlerts(diff) {
    document.querySelectorAll(`.${this.prefix(diff)}.form-wrapper .alert`).forEach(alert => {
      alert.style.display = "none";
    });
  }

  prefix(diff) {
    return diff ? "comparison" : "copy";
  }

  resetParentsOverflowHidden(element) {
    if (element) {
      const parentCell = element.closest("td");
      if (parentCell) {
        for (const child of parentCell.children) {
          if (child.tagName === "DIV" && child.style.overflow === "hidden") {
            child.style.overflow = "visible";
            break;
          }
        }
      }
    }
  }
}
