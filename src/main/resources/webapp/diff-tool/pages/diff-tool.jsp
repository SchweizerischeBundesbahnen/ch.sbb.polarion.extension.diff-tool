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
    <script type="text/javascript" src="../js/diff-tool.js?bundle=<%= extensionVersion.getBundleBuildTimestampDigitsOnly() %>"></script>
</head>
<body onload="document.getElementById('query-input').addEventListener('keydown', (event) => {
    if (event.code === 'Enter') { // Apply query by pressing Enter key being in input field
        const newValue = document.getElementById('query-input').value;
        top.location.href = DiffTool.replaceUrlParam(top.location.href, 'query', newValue);
        top.location.reload()
    }
});">
    <div class="polarion-rpe-content">
        <div class="polarion-rp-column">
            <div class="polarion-rp-column-container">
                <div class="polarion-rp-widget-part">
                    <div class="polarion-rp-widget-content">
                        <div class="polarion-DiffTool">
                            <div class="header">
                                <h3>Compare work items</h3>
                            </div>
                            <jsp:include page="items-table-widget.jsp"/>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</body>
</html>
