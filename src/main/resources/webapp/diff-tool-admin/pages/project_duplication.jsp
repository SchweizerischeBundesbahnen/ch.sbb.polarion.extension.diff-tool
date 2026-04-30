<%@ page import="ch.sbb.polarion.extension.generic.rest.model.Version" %>
<%@ page import="ch.sbb.polarion.extension.generic.util.ExtensionInfo" %>
<%@ page import="com.polarion.core.util.EscapeChars" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">

<%! Version version = ExtensionInfo.getInstance().getVersion();%>

<head>
    <title>Project Duplication</title>
    <link rel="stylesheet" href="../ui/generic/css/common.css?bundle=<%= version.getBundleBuildTimestampDigitsOnly() %>">
    <script type="module" src="../js/modules/project_duplication.js?bundle=<%= version.getBundleBuildTimestampDigitsOnly() %>"></script>
    <style type="text/css">
        .duplication-form {
            margin: 20px;
            max-width: 720px;
        }

        .duplication-form .form-row {
            margin-bottom: 12px;
        }

        .duplication-form label {
            display: inline-block;
            width: 200px;
            font-weight: bold;
            vertical-align: top;
        }

        .duplication-form input[type=text],
        .duplication-form select {
            width: 360px;
            box-sizing: border-box;
            padding: 6px 8px;
            font: inherit;
            line-height: 1.4;
            border: 1px solid #aaa;
            border-radius: 2px;
            background-color: #fff;
        }

        .duplication-form .field-hint {
            display: block;
            margin-left: 200px;
            color: #666;
            font-size: 0.9em;
        }

        .duplication-form .actions {
            margin-top: 20px;
            margin-left: 200px;
        }

        .duplication-form .status-message {
            margin-top: 16px;
            margin-left: 200px;
            padding: 8px;
            border: 1px solid transparent;
        }

        .duplication-form .status-message.error {
            background-color: #fdecea;
            border-color: #d57373;
            color: #b71c1c;
        }

        .duplication-form .status-message.success {
            background-color: #e6f4ea;
            border-color: #81c784;
            color: #1b5e20;
        }

        .jobs-section {
            margin: 20px;
        }

        .jobs-table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 10px;
            font-size: 0.95em;
        }

        .jobs-table th, .jobs-table td {
            border: 1px solid #ddd;
            padding: 6px 10px;
            text-align: left;
            vertical-align: top;
        }

        .jobs-table th {
            background-color: #f4f4f4;
            font-weight: bold;
        }

        .jobs-table tr.row-clickable { cursor: pointer; }
        .jobs-table tr.row-clickable:hover td { background-color: #f9f9f9; }

        .jobs-table .state-RUNNING, .jobs-table .state-WAITING, .jobs-table .state-ACTIVATING { color: #0d47a1; font-weight: bold; }
        .jobs-table .state-FINISHED.status-OK { color: #1b5e20; font-weight: bold; }
        .jobs-table .state-FINISHED.status-FAILED { color: #b71c1c; font-weight: bold; }
        .jobs-table .state-FINISHED.status-CANCELLED { color: #888; font-weight: bold; }
        .jobs-table .state-ABORTED { color: #888; font-weight: bold; }

        .job-log-row td {
            background-color: #fafafa;
            padding: 0;
        }

        .job-log-frame {
            width: 100%;
            min-height: 280px;
            border: none;
            background: #ffffff;
        }

        .auto-refresh-note {
            color: #666;
            font-size: 0.85em;
            margin-left: 8px;
        }
    </style>
</head>

<body>

<div class="standard-admin-page">
    <h1>Project Duplication</h1>

    <jsp:include page='/common/jsp/notifications.jsp'/>

    <div class="duplication-form" id="duplication-form">
        <div class="form-row">
            <label for="source-project">Source project:</label>
            <select id="source-project">
                <option value="">Loading...</option>
            </select>
            <span class="field-hint">Existing project to clone.</span>
        </div>
        <div class="form-row">
            <label for="target-project-id">New project ID:</label>
            <input type="text" id="target-project-id" placeholder="e.g. my_new_project"/>
            <span class="field-hint">Must be unique. Allowed: letters, digits, underscore, hyphen.</span>
        </div>
        <div class="form-row">
            <label for="location">Location:</label>
            <input type="text" id="location" placeholder="/MyProjects/my_new_project"/>
            <span class="field-hint">Repository path where the new project will live.</span>
        </div>
        <div class="form-row">
            <label for="tracker-prefix">Tracker prefix:</label>
            <input type="text" id="tracker-prefix" placeholder="MNP"/>
            <span class="field-hint">Prefix for new work item IDs (trailing dash added automatically).</span>
        </div>
        <div class="actions">
            <button type="button" id="start-duplication">Start duplication</button>
        </div>
        <div class="status-message" id="status-message" style="display:none"></div>
    </div>

    <input id="scope" type="hidden" value="<%= EscapeChars.forHTMLAttribute(request.getParameter("scope")) %>"/>
    <input id="bundle-timestamp" type="hidden" value="<%= ch.sbb.polarion.extension.generic.util.VersionUtils.getVersion().getBundleBuildTimestamp() %>"/>
</div>

<div class="standard-admin-page">
    <h2>Duplication Jobs <span class="auto-refresh-note" id="refresh-note"></span></h2>
    <div class="jobs-section">
        <table class="jobs-table" id="jobs-table">
            <thead>
                <tr>
                    <th>Job</th>
                    <th>Started</th>
                    <th>Duration</th>
                    <th>State</th>
                    <th>Status / Message</th>
                    <th>Progress</th>
                </tr>
            </thead>
            <tbody id="jobs-tbody">
                <tr><td colspan="6">Loading jobs…</td></tr>
            </tbody>
        </table>
    </div>
</div>

<div class="standard-admin-page">
    <h2>Quick Help</h2>

    <div class="quick-help-text">
        <h3>How it works</h3>
        <p>The selected source project is exported to a temporary template, and a new project is created from that template at the specified location with the chosen tracker prefix. The temporary template is removed once duplication finishes.</p>
        <p>Duplication runs as an asynchronous Polarion job. The list above shows all duplication jobs (newest first) and refreshes automatically while any job is still running. Click a row to open / close its log.</p>
        <p>This action is restricted to global Polarion administrators.</p>
    </div>
</div>
</body>
</html>
