import { BreadcrumbInjector } from '@grigoriev/react-sbb-polarion';
import { DIFF_TOOL, findTopic } from './topics';

/**
 * Topic router for the Diff Tool navigation pages: one bundle, one topics.html, the page chosen by
 * `?topic=<id>` (see topics.tsx and the getPageUrl() of the navigation nodes in
 * ch.sbb.polarion.extension.diff_tool.navigation). An unknown or missing topic falls back to the root topic,
 * so a stale bookmark still lands somewhere usable.
 *
 * Three classes on the shell, all load-bearing:
 * - `diff-topics` scopes topics.css. Deliberately not `.app`: react-sbb-polarion claims that for the admin
 *   shell, the same reason the diff/merge viewer uses `.diff-app`.
 * - `sbb-ui` defines the `--sbb-*` design tokens. It sits here as well as on <body> (topics.html), like the
 *   viewer's `.diff-app.sbb-ui`, so the page also renders fully in a test that mounts this component alone.
 * - `form-wrapper` is what scopes RSP's control styling for plain markup - inputs, checkboxes and radios.
 *   The legacy JSPs used the same class on their page shell, which is why their table checkboxes and query
 *   inputs looked like Polarion's own.
 */
export default function TopicsApp() {
  const topic = findTopic(new URLSearchParams(window.location.search).get('topic')) ?? findTopic(DIFF_TOOL);
  if (!topic) {
    return null;
  }
  const Page = topic.component;

  return (
    <div className="diff-topics sbb-ui form-wrapper">
      {/* Polarion shows a generic "home" in the app header for an extension's navigation topic; this puts
          the topic's own name there, with the parent topic before it. */}
      <BreadcrumbInjector marker="diff-tool" title={topic.title} parent={topic.parent} icon={topic.icon} />
      <Page />
    </div>
  );
}
