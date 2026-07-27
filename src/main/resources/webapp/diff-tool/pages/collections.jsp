<%@ page import="ch.sbb.polarion.extension.generic.util.ExtensionInfo" %>
<%@ page import="ch.sbb.polarion.extension.generic.rest.model.Version" %>
<%@ page import="com.polarion.core.config.Configuration" %>
<%@ page import="com.polarion.core.config.IProduct" %>
<%! IProduct product = Configuration.getInstance().getProduct(); %>
<%! Version extensionVersion = ExtensionInfo.getInstance().getVersion(); %>
<!DOCTYPE html>
<html>
<head>
    <link rel="stylesheet" type="text/css" href="/polarion/gwt/gwt/polarion/polarion.css?buildId=<%= product.buildNumber() %>">
    <link rel="stylesheet" type="text/css" href="../css/common.css?bundle=<%= extensionVersion.getBundleBuildTimestampDigitsOnly() %>">
    <link rel="stylesheet" type="text/css" href="../ui/generic/css/checkboxes.css?bundle=<%= extensionVersion.getBundleBuildTimestampDigitsOnly() %>">
    <link rel="stylesheet" type="text/css" href="../ui/generic/css/radios.css?bundle=<%= extensionVersion.getBundleBuildTimestampDigitsOnly() %>">
    <link rel="stylesheet" type="text/css" href="../ui/generic/css/inputs.css?bundle=<%= extensionVersion.getBundleBuildTimestampDigitsOnly() %>">
    <link rel="stylesheet" type="text/css" href="../ui/generic/css/searchable-dropdown.css?bundle=<%= extensionVersion.getBundleBuildTimestampDigitsOnly() %>">
    <script type="text/javascript" src="../js/diff-tool-widget-utils.js?bundle=<%= extensionVersion.getBundleBuildTimestampDigitsOnly() %>"></script>
    <script type="text/javascript" src="../js/breadcrumb.js?bundle=<%= extensionVersion.getBundleBuildTimestampDigitsOnly() %>"
            data-marker="diff-tool"
            data-title="Collections"
            data-parent="Diff Tool"
            data-icon="/polarion/ria/images/topicIconsSmall/collectionsTopic.svg"></script>
</head>
<body onload="
document.getElementById('source-query-input').addEventListener('keydown', (event) => {
    if (event.code === 'Enter') { // Apply query by pressing Enter key being in input field
        const newValue = document.getElementById('source-query-input').value;
        top.location.href = DiffToolWidgetUtils.replaceUrlParam(top.location.href, 'sourceQuery', encodeURIComponent(newValue));
        top.location.reload();
    }
});
document.getElementById('target-query-input').addEventListener('keydown', (event) => {
    if (event.code === 'Enter') { // Apply query by pressing Enter key being in input field
        const newValue = document.getElementById('target-query-input').value;
        top.location.href = DiffToolWidgetUtils.replaceUrlParam(top.location.href, 'targetQuery', encodeURIComponent(newValue));
        top.location.reload();
    }
});
">
<div class="polarion-rpe-content">
    <div class="polarion-rp-column">
        <div class="polarion-rp-column-container">
            <div class="polarion-rp-widget-part">
                <div class="polarion-rp-widget-content">
                    <div class="polarion-DiffTool form-wrapper">
                        <div class="header">
                            <h3>Compare Collections</h3>
                            <jsp:include page="collections-diff-widget.jsp"/>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
<script type="module" src="../js/upgrade-selects.js?bundle=<%= extensionVersion.getBundleBuildTimestampDigitsOnly() %>"></script>
</body>
</html>
