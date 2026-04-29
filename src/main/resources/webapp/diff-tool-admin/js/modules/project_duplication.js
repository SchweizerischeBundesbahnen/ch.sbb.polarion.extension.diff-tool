const REST_BASE = '/polarion/diff-tool/rest/internal';
const POLL_ACTIVE_MS = 3000;
const POLL_IDLE_MS = 30000;

const sourceSelect = document.getElementById('source-project');
const targetInput = document.getElementById('target-project-id');
const locationInput = document.getElementById('location');
const prefixInput = document.getElementById('tracker-prefix');
const startButton = document.getElementById('start-duplication');
const statusBox = document.getElementById('status-message');
const jobsTbody = document.getElementById('jobs-tbody');
const refreshNote = document.getElementById('refresh-note');

const expandedJobIds = new Set();
let pollTimer = null;
let lastJobs = [];
let inFlight = false;

function showStatus(message, kind) {
  statusBox.textContent = message;
  statusBox.classList.remove('error', 'success');
  if (kind) statusBox.classList.add(kind);
  statusBox.style.display = 'block';
}

function clearStatus() {
  statusBox.style.display = 'none';
  statusBox.textContent = '';
  statusBox.classList.remove('error', 'success');
}

async function loadProjects() {
  try {
    const response = await fetch(`${REST_BASE}/projects`, { headers: { 'Accept': 'application/json' } });
    if (!response.ok) throw new Error(`Failed to load projects (HTTP ${response.status})`);
    const projects = await response.json();
    sourceSelect.innerHTML = '';
    const placeholder = document.createElement('option');
    placeholder.value = '';
    placeholder.textContent = '-- Select source project --';
    sourceSelect.appendChild(placeholder);
    projects.forEach((project) => {
      const option = document.createElement('option');
      option.value = project.id;
      option.textContent = project.name ? `${project.name} (${project.id})` : project.id;
      sourceSelect.appendChild(option);
    });
  } catch (err) {
    sourceSelect.innerHTML = '<option value="">(failed to load)</option>';
    showStatus(err.message, 'error');
  }
}

function readForm() {
  return {
    sourceProjectId: sourceSelect.value.trim(),
    targetProjectId: targetInput.value.trim(),
    location: locationInput.value.trim(),
    trackerPrefix: prefixInput.value.trim()
  };
}

function validate(payload) {
  const missing = Object.entries(payload).filter(([, v]) => !v).map(([k]) => k);
  return missing.length === 0 ? null : `Please fill in: ${missing.join(', ')}`;
}

async function startDuplication() {
  clearStatus();
  const payload = readForm();
  const error = validate(payload);
  if (error) { showStatus(error, 'error'); return; }

  startButton.disabled = true;
  try {
    const response = await fetch(`${REST_BASE}/projects/duplicate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
      body: JSON.stringify(payload)
    });
    const text = await response.text();
    if (!response.ok) throw new Error(text || `HTTP ${response.status}`);
    const job = JSON.parse(text);
    showStatus(`Job '${job.jobName}' scheduled (id: ${job.jobId}). Watch progress in the table below.`, 'success');
    expandedJobIds.add(job.jobId);
    refreshJobs();
  } catch (err) {
    showStatus(`Failed to start duplication: ${err.message}`, 'error');
  } finally {
    startButton.disabled = false;
  }
}

function fmtTime(ms) {
  if (!ms) return '—';
  const d = new Date(ms);
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

function fmtDuration(start, end) {
  if (!start) return '—';
  const ms = (end || Date.now()) - start;
  const sec = Math.floor(ms / 1000);
  const m = Math.floor(sec / 60);
  const s = sec % 60;
  return m > 0 ? `${m}m ${s}s` : `${s}s`;
}

function escapeHtml(s) {
  if (s === null || s === undefined) return '';
  return String(s)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

function renderJobs(jobs) {
  if (!jobs || jobs.length === 0) {
    jobsTbody.innerHTML = '<tr><td colspan="6">No duplication jobs yet.</td></tr>';
    return;
  }
  const rows = [];
  for (const job of jobs) {
    const stateClass = `state-${job.state || ''} status-${job.statusType || ''}`;
    const progress = job.completeness != null ? `${Math.round(job.completeness * 100)}%` : '—';
    const stateLabel = job.statusType ? `${job.state} (${job.statusType})` : (job.state || '');
    const message = job.statusMessage || job.currentTaskName || '';
    rows.push(`
      <tr class="row-clickable" data-job-id="${escapeHtml(job.jobId)}">
        <td><div><strong>${escapeHtml(job.jobName)}</strong></div><div style="color:#888;font-size:0.85em">${escapeHtml(job.jobId)}</div></td>
        <td>${escapeHtml(fmtTime(job.startTime || job.creationTime))}</td>
        <td>${escapeHtml(fmtDuration(job.startTime || job.creationTime, job.finishTime))}</td>
        <td class="${stateClass}">${escapeHtml(stateLabel)}</td>
        <td>${escapeHtml(message)}</td>
        <td>${progress}</td>
      </tr>
    `);
    if (expandedJobIds.has(job.jobId)) {
      rows.push(`
        <tr class="job-log-row" data-job-log-for="${escapeHtml(job.jobId)}">
          <td colspan="6">
            <iframe class="job-log-frame" src="${escapeHtml(job.logUrl)}" title="Log for ${escapeHtml(job.jobId)}"></iframe>
          </td>
        </tr>
      `);
    }
  }
  jobsTbody.innerHTML = rows.join('');

  jobsTbody.querySelectorAll('tr.row-clickable').forEach((tr) => {
    tr.addEventListener('click', () => {
      const id = tr.getAttribute('data-job-id');
      if (expandedJobIds.has(id)) expandedJobIds.delete(id);
      else expandedJobIds.add(id);
      const existingLogRow = jobsTbody.querySelector(`tr.job-log-row[data-job-log-for="${CSS.escape(id)}"]`);
      if (existingLogRow) {
        existingLogRow.remove();
      } else {
        const job = lastJobs.find((j) => j.jobId === id);
        if (job) {
          const tmp = document.createElement('tbody');
          tmp.innerHTML = `<tr class="job-log-row" data-job-log-for="${escapeHtml(id)}"><td colspan="6"><iframe class="job-log-frame" src="${escapeHtml(job.logUrl)}" title="Log for ${escapeHtml(id)}"></iframe></td></tr>`;
          tr.after(tmp.firstElementChild);
        }
      }
    });
  });
}

function refreshExpandedLogs() {
  jobsTbody.querySelectorAll('tr.job-log-row').forEach((row) => {
    const id = row.getAttribute('data-job-log-for');
    const job = lastJobs.find((j) => j.jobId === id);
    if (!job) return;
    if (job.state !== 'FINISHED' && job.state !== 'ABORTED') {
      const iframe = row.querySelector('iframe.job-log-frame');
      if (iframe) {
        const url = job.logUrl + (job.logUrl.includes('?') ? '&' : '?') + '_ts=' + Date.now();
        iframe.src = url;
      }
    }
  });
}

async function refreshJobs() {
  if (inFlight) return;
  inFlight = true;
  try {
    const response = await fetch(`${REST_BASE}/projects/duplicate/jobs`, { headers: { 'Accept': 'application/json' } });
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const jobs = await response.json();
    lastJobs = jobs;
    renderJobs(jobs);
    refreshExpandedLogs();
    schedulePolling(jobs);
  } catch (err) {
    jobsTbody.innerHTML = `<tr><td colspan="6">Failed to load jobs: ${escapeHtml(err.message)}</td></tr>`;
    schedulePolling([]);
  } finally {
    inFlight = false;
  }
}

function schedulePolling(jobs) {
  if (pollTimer) { clearTimeout(pollTimer); pollTimer = null; }
  const anyRunning = jobs.some((j) => j.state !== 'FINISHED' && j.state !== 'ABORTED');
  const delay = anyRunning ? POLL_ACTIVE_MS : POLL_IDLE_MS;
  refreshNote.textContent = anyRunning ? `(auto-refreshing every ${delay / 1000}s)` : `(idle; next refresh in ${delay / 1000}s)`;
  pollTimer = setTimeout(refreshJobs, delay);
}

startButton.addEventListener('click', startDuplication);
loadProjects();
refreshJobs();
