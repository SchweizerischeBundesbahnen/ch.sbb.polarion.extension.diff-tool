<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">

<%! String bundleTimestamp = ch.sbb.polarion.extension.generic.util.VersionUtils.getVersion().getBundleBuildTimestampDigitsOnly(); %>

<head>
    <title>Diff Configurations</title>
    <link rel="stylesheet" href="../ui/generic/css/common.css?bundle=<%= bundleTimestamp %>">
    <link rel="stylesheet" href="../ui/generic/css/custom-select.css?bundle=<%= bundleTimestamp %>">
    <link rel="stylesheet" href="../ui/generic/css/configurations.css?bundle=<%= bundleTimestamp %>">
    <script type="module" src="../js/modules/diff.js?bundle=<%= bundleTimestamp %>"></script>
    <style type="text/css">
        html {
            height: 100%;
        }
        body {
            height: 100%;
            padding-left: 10px;
            padding-right: 10px;
            margin: 0;
            display: flex;
            flex-direction: column;
        }
        .standard-admin-page {
            flex: 1;
            display: flex;
            flex-direction: column;
        }
        .diff-fields-error {
            color: red;
        }
        .flex-container {
            display: flex;
            column-gap: 20px;
            flex-direction: row;
        }
        .column {
            display: flex;
            flex-direction: column;
            row-gap: 5px;
        }
        .select-column {
            width: 440px;
            min-height: 200px;
        }
        .select-column select {
            height: 100%;
        }
        .buttons-column {
            width: 100px;
            justify-content: center;
        }
        .toolbar-button {
            text-align: center;
        }
        input[type="checkbox"] {
            width: auto;
            vertical-align: middle;
        }
        .checkbox.input-group label {
            width: auto;
        }
    </style>
</head>

<body>
<div class="standard-admin-page">
    <h1>Diff Configurations</h1>

    <jsp:include page='/common/jsp/notifications.jsp' />

    <jsp:include page='/common/jsp/configurations.jsp' />

    <div class="content-area" style="border-top: 1px solid #ccc; margin-top: 20px; padding-top: 15px;">
        <div id="fields-load-error" class="diff-fields-error" style="display: none; margin-bottom: 15px">
            There was an error loading list of available fields.
        </div>
        <div id="statuses-load-error" class="diff-fields-error" style="display: none; margin-bottom: 15px">
            There was an error loading list of available statuses.
        </div>
        <div id="hyperlink-roles-load-error" class="diff-fields-error" style="display: none; margin-bottom: 15px">
            There was an error loading list of available hyperlink roles.
        </div>
        <div id="linked-workitem-roles-load-error" class="diff-fields-error" style="display: none; margin-bottom: 15px">
            There was an error loading list of available roles of linked WorkItems.
        </div>

        <div class="flex-container">
            <div class="column select-column">
                <label for="available-fields">Available fields:</label>
                <select id="available-fields" multiple size="22">
                </select>
            </div>
            <div class="column buttons-column">
                <button id="add-button" class="toolbar-button" disabled>Add &gt;</button>
                <button id="remove-button" class="toolbar-button" disabled>&lt; Remove</button>
            </div>
            <div class="column select-column">
                <label for="selected-fields">Fields selected for diff:</label>
                <select id="selected-fields" multiple size="22">
                </select>
            </div>
        </div>

        <div class="flex-container" style="margin-top: 20px; padding-top: 16px; border-top: 1px solid #cccccc">
            <div class="column select-column">
                <label for="statuses-to-ignore">Statuses of WorkItems in a source document to ignore when diffing:</label>
                <select id="statuses-to-ignore" multiple size="10" style="height: 230px">
                </select>
            </div>

            <div class="column buttons-column">
                <%-- fake component. used just to emulate same gap betweeen areas in the top block--%>
            </div>

            <div id="hyperlink-settings-container" class="column select-column">
                <label for="hyperlink-roles">Hyperlink roles to diff and merge</label>
                <input type="text" id="search-hyperlink-roles-input" placeholder="Filter items">
                <select id="hyperlink-roles" multiple size="10" style="height: 200px">
                </select>
            </div>

            <div id="linked-workitem-settings-container" class="column select-column">
                <label for="hyperlink-roles">Roles of linked WorkItems to diff and merge</label>
                <input type="text" id="search-linked-workitem-roles-input" placeholder="Filter items">
                <select id="linked-workitem-roles" multiple size="10" style="height: 200px">
                </select>
            </div>
        </div>
    </div>

    <input id="scope" type="hidden" value="<%= request.getParameter("scope")%>"/>
    <input id="project-id" type="hidden" value="<%= ch.sbb.polarion.extension.generic.util.ScopeUtils.getProjectFromScope(request.getParameter("scope")) %>"/>
    <input id="bundle-timestamp" type="hidden" value="<%= ch.sbb.polarion.extension.generic.util.VersionUtils.getVersion().getBundleBuildTimestamp() %>"/>
</div>

<jsp:include page='/common/jsp/buttons.jsp'/>

</body>
</html>
