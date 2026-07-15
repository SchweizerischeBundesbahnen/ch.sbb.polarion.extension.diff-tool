<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">

<%! String bundleTimestamp = ch.sbb.polarion.extension.generic.util.VersionUtils.getVersion().getBundleBuildTimestampDigitsOnly(); %>

<head>
    <title>Diff Configurations</title>
    <link rel="stylesheet" href="../ui/generic/css/common.css?bundle=<%= bundleTimestamp %>">
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
            --select-column-width: 440px;
            --buttons-column-width: 100px;
            --flex-gap: 20px;
            /* Full width of the top two-pane component: both select columns + the button column between
               them + the two gaps. The bottom dropdowns are sized to match this. */
            --top-component-width: calc(2 * var(--select-column-width) + var(--buttons-column-width) + 2 * var(--flex-gap));
        }
        .diff-fields-error {
            color: red;
        }
        .flex-container {
            display: flex;
            column-gap: var(--flex-gap);
            flex-direction: row;
        }
        .column {
            display: flex;
            flex-direction: column;
            row-gap: 5px;
        }
        .select-column {
            width: var(--select-column-width);
            min-height: 200px;
        }
        .select-column select {
            height: 100%;
        }
        .buttons-column {
            width: var(--buttons-column-width);
            justify-content: center;
        }
        /* The three bottom dropdowns are stacked vertically, each spanning the full width of the top
           two-pane component. */
        .bottom-settings {
            flex-direction: column;
            row-gap: var(--flex-gap);
        }
        .bottom-select-column {
            width: var(--top-component-width);
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
        /* The bottom dropdowns are upgraded to the shared SearchableDropdown; let each wrapper fill
           its column (the component only copies an explicit width from the original element). */
        .bottom-select-column .searchable-dropdown {
            width: 100%;
        }
        /* "Available fields:" label and its filter input share one row above the list. */
        .available-fields-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 10px;
        }
        .filter-input {
            flex: 1;
            min-width: 0;
            box-sizing: border-box;
            /* Filter icon pinned to the left, sized so it can't crowd the text. */
            background: var(--sbb-control-bg) url("/polarion/ria/images/filter.png") no-repeat 6px center;
            background-size: 14px 14px;
            padding-left: 26px !important;
            border: 1px solid var(--sbb-control-border);
            border-radius: var(--sbb-control-radius);
            color: var(--sbb-control-text);
            font-family: var(--sbb-control-font-family);
            font-size: var(--sbb-control-font-size);
            font-weight: var(--sbb-control-font-weight);
            outline: none;
        }
        .filter-input:focus {
            border-color: var(--sbb-control-border-focus);
        }
        /* The top "transfer list" keeps its two-pane layout and fixed height, but is restyled to read
           as part of the same design system as the shared controls, reusing the --sbb-* tokens
           (scoped via .standard-admin-page). */
        #available-fields,
        #selected-fields {
            border: 1px solid var(--sbb-control-border);
            border-radius: var(--sbb-control-radius);
            background-color: var(--sbb-control-bg);
            color: var(--sbb-control-text);
            font-family: var(--sbb-control-font-family);
            font-size: var(--sbb-control-font-size);
            font-weight: var(--sbb-control-font-weight);
            padding: 2px;
            outline: none;
            /* Match the soft elevation the generic controls animate on hover/focus. */
            transition: box-shadow 0.15s ease, border-color 0.15s ease;
        }
        #available-fields:hover,
        #selected-fields:hover {
            box-shadow: var(--sbb-control-shadow-hover);
        }
        #available-fields:focus,
        #selected-fields:focus {
            border-color: var(--sbb-control-border-focus);
            box-shadow: var(--sbb-control-shadow-hover);
        }
        #available-fields option,
        #selected-fields option {
            padding: 4px 8px;
        }
        #available-fields option:hover,
        #selected-fields option:hover,
        #available-fields option:checked,
        #selected-fields option:checked {
            background-color: var(--sbb-option-hover);
            color: var(--sbb-control-text);
        }
        #available-fields:focus option:checked,
        #available-fields:focus option:checked:hover,
        #selected-fields:focus option:checked,
        #selected-fields:focus option:checked:hover {
            background: #D1FFF2 linear-gradient(#D1FFF2, #D1FFF2) !important;
            color: var(--sbb-control-text) !important;
            -webkit-text-fill-color: var(--sbb-control-text);
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
                <div class="available-fields-header">
                    <label for="available-fields">Available fields:</label>
                    <input type="text" id="available-fields-filter" class="filter-input" aria-label="Filter available fields">
                </div>
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

        <div class="flex-container bottom-settings" style="margin-top: 20px; padding-top: 16px; border-top: 1px solid #cccccc">
            <div class="column bottom-select-column">
                <label for="statuses-to-ignore">Statuses of WorkItems in a source document to ignore when diffing:</label>
                <select id="statuses-to-ignore" multiple>
                </select>
            </div>

            <div id="hyperlink-settings-container" class="column bottom-select-column">
                <label for="hyperlink-roles">Hyperlink roles to diff and merge</label>
                <select id="hyperlink-roles" multiple>
                </select>
            </div>

            <div id="linked-workitem-settings-container" class="column bottom-select-column">
                <label for="linked-workitem-roles">Roles of linked WorkItems to diff and merge</label>
                <select id="linked-workitem-roles" multiple>
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
