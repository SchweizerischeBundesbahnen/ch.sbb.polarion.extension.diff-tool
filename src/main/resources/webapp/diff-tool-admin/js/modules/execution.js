import ExtensionContext from '../../ui/generic/js/modules/ExtensionContext.js';

const ctx = new ExtensionContext({
  extension: 'diff-tool',
  setting: 'executionQueue',
  scopeFieldId: 'scope'
});

ctx.onClick(
    'save-toolbar-button', saveSettings,
    'cancel-toolbar-button', ctx.cancelEdit,
    'default-toolbar-button', revertToDefault,
    'revisions-toolbar-button', ctx.toggleRevisions,
    // 'reset-cpu-chart', () => {ctx.cpu_chart.resetZoom()},
    // 'clear-cpu-chart', clearChart
);

const colors = [
  "#3366CC", "#DC3912", "#FF9900", "#109618",
  "#990099", "#3B3EAC", "#0099C6", "#DD4477",
  "#66AA00", "#B82E2E", "#316395", "#994499",
  "#22AA99", "#AAAA11", "#6633CC", "#F67300",
  "#8B0707", "#329262", "#5574A6", "#651067",
];

function parseAndSetSettings(jsonResponse) {
  const settings = JSON.parse(jsonResponse);

  ctx.currentSettings = settings;

  if (settings.bundleTimestamp !== ctx.getValueById('bundle-timestamp')) {
    ctx.setNewerVersionNotificationVisible(true);
  }

  const usedWorkers = new Set();
  usedWorkers.add("CPU_LOAD");
  for (const key in settings.workers) {

    ctx.getElementById('feature-' + key).innerText = featuresLocalization[key];

    if (settings.workers.hasOwnProperty(key)) {
      const currentWorker = settings.workers[key].toString();
      usedWorkers.add(currentWorker);
      ctx.getElementById('current-worker-' + key).innerText = currentWorker;
      let selector = ctx.getElementById('new-worker-' + key);
      selector.value = '';
      let options = selector.querySelectorAll("option");
      for (const item of options) {
        item.style.display = item.value === currentWorker ? 'none' : 'block';
      }
    }
  }

  for (const key in settings.threads) {
    if (settings.threads.hasOwnProperty(key)) {
      ctx.getElementById('current-threads-' + key).innerText = settings.threads[key];
      ctx.setValue('new-threads-' + key, '');
    }
  }

  if (!ctx.charts) {
    ctx.charts = {};
    ctx.fromFilter = {};
  }
  const allWorkers = ["1", "2", "3", "4", "5", "6", "7", "8", "CPU_LOAD"];
  ctx.usedWorkers = allWorkers.filter(w => usedWorkers.has(w));

  for (const worker of allWorkers) {
    let show = ctx.usedWorkers.includes(worker);
    ctx.displayIf("chart-container-" + worker, show);
    if (show && !ctx.charts[worker]) {
      ctx.getElementById('select-interval-' + worker).onchange = buildIntervalChangedHandler(worker);
      ctx.charts[worker] = initChart(worker);
      ctx.fromFilter[String(worker)] = '';
    }
  }

  if (!ctx.refreshStarted) {
    refreshData();
    setInterval(function () {
      refreshData();
    }, 3000);
    ctx.refreshStarted = true;
  }
}

function buildIntervalChangedHandler(worker) {
  return function () {
    console.log(`Interval changed for worker ${worker} to ${this.value} seconds`)
    //TODO
  }
}

function refreshData() {
  ctx.callAsync({
    method: 'POST',
    url: `/polarion/${ctx.extension}/rest/internal/queueStatistics`,
    body: JSON.stringify({
      'from': ctx.fromFilter
    }),
    contentType: 'application/json',
    onOk: (responseText) => {
      parseAndSetData(responseText);
    },
    onError: () => ctx.setLoadingErrorNotificationVisible(true)
  });
}

function initChart(worker) {
  const dataSet = {datasets: []};
  let colorCounter = 0;
  if (worker === 'CPU_LOAD') {
    dataSet.datasets.push({
      label: featuresLocalization['CPU_LOAD'],
      data: [],
      borderColor: colors[colorCounter++],
      // tension: 0.1
    })
  } else {
    for (const key in ctx.currentSettings.workers) {
      if (ctx.currentSettings.workers[key]?.toString() === worker) {
        dataSet.datasets.push({
          label: featuresLocalization[key] + ' (queue)',
          data: [],
          borderColor: colors[colorCounter++],
        });
        dataSet.datasets.push({
          label: featuresLocalization[key] + ' (running)',
          data: [],
          borderColor: colors[colorCounter++],
        })
      }
    }
  }

  return new Chart(document.getElementById('chart-' + worker).getContext('2d'), {
    type: 'line',
    data: dataSet,
    options: {
      responsive: true,
      maintainAspectRatio: false,
      scales: {
        x: {
          type: 'time',
          time: {
            displayFormats: {
              hour: 'HH:mm',
              minute: 'HH:mm',
              second: 'HH:mm:ss'
            },
            tooltipFormat: 'HH:mm:ss'
          },
          ticks: {
            callback: function (value, index, ticks) {
              const date = new Date(value);
              return date.getHours().toString().padStart(2, '0') + ':' +
                  date.getMinutes().toString().padStart(2, '0') + ':' +
                  date.getSeconds().toString().padStart(2, '0');
            }
          }
        },
        y: {
          beginAtZero: true,
          title: {
            display: true,
            text: worker === 'CPU_LOAD' ? 'Load (%)' : 'Requests'
          }
        }
      },
      plugins: {
        tooltip: {
          callbacks: {
            title: function (tooltipItems) {
              // Format the tooltip title (timestamp) as HH:mm:ss
              const date = new Date(tooltipItems[0].parsed.x);
              return date.getHours().toString().padStart(2, '0') + ':' +
                  date.getMinutes().toString().padStart(2, '0') + ':' +
                  date.getSeconds().toString().padStart(2, '0');
            }
          }
        },
        zoom: {
          // limits: {
          //   x: {min: 'original', max: 'original'},
          //   y: {min: 'original', max: 'original'}
          // },
          pan: {
            enabled: true,
            mode: 'xy',
          },
          zoom: {
            wheel: {
              enabled: true,
              modifierKey: 'ctrl'
            },
            pinch: {
              enabled: true  // Enable pinch zooming for touch devices
            },
            mode: 'xy',      // Allow zooming on both axes
          }
        }
      },
      elements: {
        point: {
          radius: 0
        }
      }
    }
  });
}

function parseAndSetData(jsonResponse) {
  if (!ctx.charts) {
    console.log('Warning: no charts to insert data into.')
  }

  const data = JSON.parse(jsonResponse);

  for (const worker of ctx.usedWorkers) {
    let chart = ctx.charts[worker];
    let datasetCounter = 0;
    for (const key in ctx.currentSettings.workers) {
      if(ctx.currentSettings.workers[key]?.toString() === worker) {

        ctx.fromFilter[String(worker)] = getLastArrayEntry(data[worker][key], {})['timestamp'];

        chart.data.datasets[datasetCounter++].data.push(...data[worker][key].map(item => ({
          x: new Date(item.timestamp),
          y: item.queued
        })));

        chart.data.datasets[datasetCounter++].data.push(...data[worker][key].map(item => ({
          x: new Date(item.timestamp),
          y: item.executing
        })));
        chart.update();
      }
    }
    if (worker === 'CPU_LOAD') {
      ctx.fromFilter[String(worker)] = getLastArrayEntry(data['COMMON']['CPU_LOAD'], {})['timestamp'];
      chart.data.datasets[0].data.push(...data['COMMON']['CPU_LOAD'].map(item => ({
        x: new Date(item.timestamp),
        y: parseFloat((item.value * 100).toFixed(2))
      })));
    }
    chart.update();
  }
}

function saveSettings() {
  ctx.hideActionAlerts();

  for (const key in ctx.currentSettings.workers) {
    if (ctx.currentSettings.workers.hasOwnProperty(key)) {
      const value = ctx.getValue('new-worker-' + key);
      if (value) {
        ctx.currentSettings.workers[key] = value;
      }
    }
  }

  for (const key in ctx.currentSettings.threads) {
    if (ctx.currentSettings.threads.hasOwnProperty(key)) {
      const value = ctx.getValue('new-threads-' + key);
      if (value) {
        ctx.currentSettings.threads[key] = value;
      }
    }
  }

  ctx.callAsync({
    method: 'PUT',
    url: `/polarion/${ctx.extension}/rest/internal/settings/${ctx.setting}/names/${ExtensionContext.DEFAULT}/content?scope=${ctx.scope}`,
    contentType: 'application/json',
    body: JSON.stringify(ctx.currentSettings),
    onOk: () => {
      for (const chartKey in ctx.charts) {
        if (ctx.charts.hasOwnProperty(chartKey)) {
          ctx.charts[chartKey].destroy();
        }
      }
      ctx.charts = undefined;
      ctx.showSaveSuccessAlert();
      ctx.setNewerVersionNotificationVisible(false);
      // readAndFillRevisions();
      // readSettings();
      ctx.callAsync({
        method: 'POST',
        url: `/polarion/${ctx.extension}/rest/internal/refreshQueueConfiguration`,
        contentType: 'application/json',
        onOk: () => {
          readSettings();
        },
        onError: () => ctx.setLoadingErrorNotificationVisible(true)
      });
    },
    onError: () => ctx.showSaveErrorAlert()
  });
}

function cancelEdit() {
  if (confirm("Are you sure you want to cancel editing and revert all changes made?")) {
    readSettings();
  }
}

function readSettings() {
  ctx.setLoadingErrorNotificationVisible(false);

  ctx.callAsync({
    method: 'GET',
    url: `/polarion/${ctx.extension}/rest/internal/settings/${ctx.setting}/names/${ExtensionContext.DEFAULT}/content?scope=${ctx.scope}`,
    contentType: 'application/json',
    onOk: (responseText) => {
      parseAndSetSettings(responseText, true);
      readAndFillRevisions();
    },
    onError: () => ctx.setLoadingErrorNotificationVisible(true)
  });
}

function readAndFillRevisions() {
  ctx.readAndFillRevisions({
    revertToRevisionCallback: (responseText) => parseAndSetSettings(responseText)
  });
}

function revertToDefault() {
  if (confirm("Are you sure you want to return the default values?")) {
    ctx.setLoadingErrorNotificationVisible(false);
    ctx.hideActionAlerts();

    ctx.callAsync({
      method: 'GET',
      url: `/polarion/${ctx.extension}/rest/internal/settings/${ctx.setting}/default-content`,
      contentType: 'application/json',
      onOk: (responseText) => {
        parseAndSetSettings(responseText);
        ctx.showRevertedToDefaultAlert();
      },
      onError: () => ctx.setLoadingErrorNotificationVisible(true)
    });
  }
}

function clearChart() {
  ctx.callAsync({
    method: 'DELETE',
    url: `/polarion/${ctx.extension}/rest/internal/queueStatistics`,
    contentType: 'application/json',
    onOk: () => {
    },
    onError: () => {
    }
  });
}

const featuresLocalization = {
  'CPU_LOAD': 'CPU Load',
  'DIFF_DOCUMENTS': 'Diff docs',
  'DIFF_WORKITEMS_PAIRS': 'Diff WI pairs',
  'DIFF_COLLECTIONS': 'Diff collections',
  'DIFF_DOCUMENT_WORKITEMS': 'Diff WI',
  'DIFF_DETACHED_WORKITEMS': 'Diff detached WI',
  'DIFF_DOCUMENTS_FIELDS': 'Diff docs fields',
  'DIFF_DOCUMENTS_CONTENT': 'Diff docs content',
  'DIFF_HTML': 'Diff HTML',
  'DIFF_TEXT': 'DIff text',
}

function getLastArrayEntry(arr, defaultValue) {
  if (!arr || arr.length === 0) {
    return defaultValue; // Or throw an error, depending on desired behavior for empty arrays
  }
  return arr[arr.length - 1];
}

readSettings();
