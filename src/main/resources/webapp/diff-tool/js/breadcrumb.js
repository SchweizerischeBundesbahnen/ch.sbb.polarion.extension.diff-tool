/**
 * Shows the current Diff Tool topic in the Polarion app-header breadcrumb via the shared generic
 * BreadcrumbBridge (injected into the shell window).
 *
 * Configuration comes from data-* attributes on this script's own tag, so every nav-topic page can
 * load the same file:
 *
 *   <script type="text/javascript" src="../js/breadcrumb.js"
 *           data-marker="diff-tool" data-title="Multiple Work Items"
 *           data-parent="Diff Tool" data-icon="/polarion/ria/images/..."></script>
 *
 * Must stay a classic, non-deferred script in <head>: it relies on document.currentScript to read
 * its own attributes, and it re-runs on every topic page load so sub-topics re-label. It stays out
 * of the Administration area on its own.
 */
(function () {
    var script = document.currentScript;
    if (!script) {
        return;
    }
    try {
        var shell = window.top;
        var cfg = {
            marker: script.getAttribute('data-marker'),
            title: script.getAttribute('data-title')
        };
        var parent = script.getAttribute('data-parent');
        if (parent) {
            cfg.parent = parent;
        }
        var icon = script.getAttribute('data-icon');
        if (icon) {
            cfg.icon = icon;
        }

        if (shell.SbbBreadcrumbBridge) {
            shell.SbbBreadcrumbBridge.install(cfg);
            return;
        }
        var doc = shell.document;
        if (!doc || !doc.head) {
            return;
        }
        var old = doc.getElementById('sbb-breadcrumb-bridge-loader');
        if (old) {
            old.parentNode.removeChild(old);
        }
        var s = doc.createElement('script');
        s.id = 'sbb-breadcrumb-bridge-loader';
        s.type = 'text/javascript';
        s.src = window.location.pathname.replace(/\/pages\/[^/]*$/, '/ui/generic/js/modules/') + 'BreadcrumbBridge.js';
        s.setAttribute('data-marker', cfg.marker);
        s.setAttribute('data-title', cfg.title);
        if (cfg.parent) {
            s.setAttribute('data-parent', cfg.parent);
        }
        if (cfg.icon) {
            s.setAttribute('data-icon', cfg.icon);
        }
        doc.head.appendChild(s);
    } catch (e) { /* no accessible shell window */ }
})();
