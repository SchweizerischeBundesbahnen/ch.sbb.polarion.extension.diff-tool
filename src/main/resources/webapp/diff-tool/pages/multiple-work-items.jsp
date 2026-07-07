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
    <script type="text/javascript">
        // Breadcrumb: "Diff Tool › Multiple Work Items" via the shared generic BreadcrumbBridge.
        (function () {
            try {
                var shell = window.top;
                var cfg = { marker: 'diff-tool', title: 'Multiple Work Items', parent: 'Diff Tool', icon: '/polarion/ria/images/topicIconsSmall/workItems.svg' };
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
<body onload="document.getElementById('source-query-input').addEventListener('keydown', (event) => {
    if (event.code === 'Enter') { // Apply query by pressing Enter key being in input field
        const newValue = document.getElementById('source-query-input').value;
        top.location.href = DiffToolWidgetUtils.replaceUrlParam(top.location.href, 'sourceQuery', encodeURIComponent(newValue));
        top.location.reload()
    }
});">
<div class="polarion-rpe-content">
    <div class="polarion-rp-column">
        <div class="polarion-rp-column-container">
            <div class="polarion-rp-widget-part">
                <div class="polarion-rp-widget-content">
                    <div class="polarion-DiffTool form-wrapper">
                        <div class="header">
                            <h3>Compare work items</h3>
                        </div>
                        <jsp:include page="work-items-diff-widget.jsp"/>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
<script type="module">
    // Upgrade the widget's native <select>s to the shared Polarion-styled dropdown, matching the
    // admin pages. Preserve each select's rendered width so the trigger keeps the same size.
    import SearchableDropdown from '../ui/generic/js/modules/SearchableDropdown.js';
    const upgradeSelects = () => {
        document.querySelectorAll('.polarion-DiffTool select').forEach((sel) => {
            if (sel._searchableDropdown) return;
            const w = sel.offsetWidth;
            if (w) sel.style.width = w + 'px';
            new SearchableDropdown({ element: sel });
        });
    };
    if (document.readyState === 'loading') {
        window.addEventListener('DOMContentLoaded', upgradeSelects);
    } else {
        upgradeSelects();
    }
</script>
</body>
</html>
