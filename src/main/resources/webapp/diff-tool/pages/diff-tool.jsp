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
    <script type="text/javascript">
        // Show this topic in the Polarion app-header breadcrumb via the shared generic
        // BreadcrumbBridge (injected into the shell window). Re-runs on every topic page load so
        // sub-topics re-label; it stays out of the Administration area on its own.
        (function () {
            try {
                var shell = window.top;
                var cfg = { marker: 'diff-tool', title: 'Diff Tool', icon: '/polarion/diff-tool-admin/ui/images/menu/30x30/_parent.svg' };
                if (shell.SbbBreadcrumbBridge) { shell.SbbBreadcrumbBridge.install(cfg); return; }
                var doc = shell.document;
                if (!doc || !doc.head) { return; }
                var old = doc.getElementById('sbb-breadcrumb-bridge-loader');
                if (old) { old.parentNode.removeChild(old); }
                var s = doc.createElement('script');
                s.id = 'sbb-breadcrumb-bridge-loader';
                s.type = 'text/javascript';
                s.src = window.location.pathname.replace(/\/pages\/[^/]*$/, '/ui/generic/js/modules/') + 'BreadcrumbBridge.js';
                s.setAttribute('data-marker', cfg.marker);
                s.setAttribute('data-title', cfg.title);
                if (cfg.parent) { s.setAttribute('data-parent', cfg.parent); }
                if (cfg.icon) { s.setAttribute('data-icon', cfg.icon); }
                doc.head.appendChild(s);
            } catch (e) { /* no accessible shell window */ }
        })();
    </script>
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
