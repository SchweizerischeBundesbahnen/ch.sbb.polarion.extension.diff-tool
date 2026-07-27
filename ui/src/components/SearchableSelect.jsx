import {useEffect, useRef} from "react";

// The shared generic combobox factory (createSearchableSelect) is a vanilla-JS module served at
// runtime from the embedded generic.app (not an npm dependency). Its URL is derived from the app's
// own location so there is no hardcoded /<ext>-app/ segment; @vite-ignore keeps the bundler from
// resolving it at build time.
const GENERIC_MODULES = "/ui/generic/js/modules/";

// Drop-in replacement for a native <select> that renders as the shared Polarion SearchableDropdown,
// so diff-tool's combos look exactly like every other extension's. The underlying <select> stays
// React-controlled (value/onChange); SearchableDropdown mirrors the selection back onto it and
// dispatches `change`, so onChange keeps firing. If the module can't be loaded (e.g. `vite dev`
// outside Polarion, where the path has no /ui/ segment to rewrite) it silently falls back to the
// plain native <select>.
export default function SearchableSelect({id, className, value, onChange, disabled, children, searchable = true}) {
  const selectRef = useRef(null);
  const sdRef = useRef(null);

  useEffect(() => {
    let cancelled = false;
    const element = selectRef.current;
    const base = window.location.pathname.replace(/\/ui\/.*$/, GENERIC_MODULES);
    import(/* webpackIgnore: true */ /* @vite-ignore */ base + "searchableSelect.js")
      .then((module) => {
        if (cancelled || !element) return;
        sdRef.current = module.createSearchableSelect(element, {searchable});
      })
      .catch(() => { /* keep the native <select> */ });
    return () => {
      cancelled = true;
      if (sdRef.current) {
        sdRef.current.destroy();
        sdRef.current = null;
      }
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Keep the dropdown trigger in sync when the value is driven from React state.
  useEffect(() => {
    if (sdRef.current && typeof sdRef.current.selectValue === "function") {
      sdRef.current.selectValue(value);
    }
  }, [value]);

  return (
    <select id={id} className={className} ref={selectRef} value={value} onChange={onChange} disabled={disabled}>
      {children}
    </select>
  );
}
