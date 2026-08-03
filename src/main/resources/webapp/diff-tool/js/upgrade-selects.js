// Upgrades the widget's native <select>s to the shared Polarion-styled dropdown, matching the admin
// pages. Preserves each select's rendered width so the trigger keeps the same size.
//
// Loaded as <script type="module" src="../js/upgrade-selects.js"> from the nav-topic pages. The
// relative import below resolves the same from js/ as it did from pages/ - both are one level under
// the diff-tool webapp root.
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
