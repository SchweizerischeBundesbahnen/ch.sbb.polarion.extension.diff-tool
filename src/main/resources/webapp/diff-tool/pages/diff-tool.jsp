<%@ page import="ch.sbb.polarion.extension.generic.util.ExtensionInfo" %>
<%@ page import="ch.sbb.polarion.extension.generic.rest.model.Version" %>
<%@ page import="com.polarion.core.config.Configuration" %>
<%@ page import="com.polarion.core.config.IProduct" %>
<%@ page import="ch.sbb.polarion.extension.diff_tool.navigation.MultipleWorkItemsNode" %>
<%@ page import="ch.sbb.polarion.extension.diff_tool.navigation.CollectionsNode" %>
<%! IProduct product = Configuration.getInstance().getProduct(); %>
<%! Version extensionVersion = ExtensionInfo.getInstance().getVersion(); %>
<!DOCTYPE html>
<html>
<head>
    <link rel="stylesheet" type="text/css" href="/polarion/gwt/gwt/polarion/polarion.css?buildId=<%= product.buildNumber() %>">
    <link rel="stylesheet" type="text/css" href="../css/common.css?bundle=<%= extensionVersion.getBundleBuildTimestampDigitsOnly() %>">
</head>
<body>
<div class="polarion-rpe-content">
    <div class="polarion-rp-column">
        <div class="polarion-rp-column-container">
            <div class="polarion-rp-widget-part">
                <div class="polarion-rp-widget-content">
                    <div class="polarion-DiffTool">
                        <div class="header">
                            <h3>Diff Tool</h3>
                            <p>Please, select below what you wish to compare:</p>
                            <ul>
                                <li><a href="#" onclick="top.location.href += '/<%= MultipleWorkItemsNode.NODE_ID %>'; top.location.reload();">Compare multiple Work Items</a></li>
                                <li><a href="#" onclick="top.location.href += '/<%= CollectionsNode.NODE_ID %>'; top.location.reload();">Compare Collections</a></li>
                            </ul>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
</body>
</html>
