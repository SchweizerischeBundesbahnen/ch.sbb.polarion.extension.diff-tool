import { BreadcrumbInjector } from '@grigoriev/react-sbb-polarion';
import { DIFF_TOOL, findTopic } from './topics';

/**
 * Topic router for the Diff Tool navigation pages: one bundle, one topics.html, the page chosen by
 * `?topic=<id>` (see topics.tsx and the getPageUrl() of the navigation nodes in
 * ch.sbb.polarion.extension.diff_tool.navigation). An unknown or missing topic falls back to the root topic,
 * so a stale bookmark still lands somewhere usable.
 *
 * `.diff-topics` is deliberately not `.app`: react-sbb-polarion claims that class for the admin shell, the
 * same reason the diff/merge viewer uses `.diff-app`. `sbb-ui` sits here as well as on <body> (topics.html),
 * exactly as the viewer's `.diff-app.sbb-ui` shell does: the RSP controls resolve their --sbb-* tokens from
 * that class, so keeping it on the shell is what lets the page render fully in a test that mounts the
 * component on its own.
 */
export default function TopicsApp() {
  const topic = findTopic(new URLSearchParams(window.location.search).get('topic')) ?? findTopic(DIFF_TOOL);
  if (!topic) {
    return null;
  }
  const Page = topic.component;

  return (
    <div className="diff-topics sbb-ui">
      {/* Polarion shows a generic "home" in the app header for an extension's navigation topic; this puts
          the topic's own name there. The bridge takes a single label, so a sub-topic carries its parent in
          the title - the legacy breadcrumb.js passed the two separately. */}
      <BreadcrumbInjector
        marker="diff-tool"
        title={topic.parent ? `${topic.parent} / ${topic.title}` : topic.title}
        icon={topic.icon}
      />
      <Page />
    </div>
  );
}
