import { COMPARE_COLLECTIONS, COMPARE_WORK_ITEMS } from './topics';

/**
 * The Diff Tool root navigation topic: two links into its sub-topics.
 *
 * React port of `webapp/diff-tool/pages/diff-tool.jsp`. The links still work by appending the node id to the
 * top frame's URL and reloading it: that URL is Polarion's own topic path, and appending to it is what makes
 * the portal select the sub-topic in the navigation tree.
 */
/** The topic path of a sub-topic: Polarion's own topic URL with the node id appended. */
export function subTopicHref(currentHref: string, nodeId: string): string {
  return `${currentHref}/${nodeId}`;
}

export default function DiffToolHomePage() {
  const openSubTopic = (nodeId: string) => {
    const shell = window.top;
    if (shell) {
      shell.location.assign(subTopicHref(shell.location.href, nodeId));
    }
  };

  return (
    <div className="header">
      <h3>Diff Tool</h3>
      <p>Please, select below what you wish to compare:</p>
      <ul>
        <li>
          <button type="button" className="link-button" onClick={() => openSubTopic(COMPARE_WORK_ITEMS)}>
            Compare multiple Work Items
          </button>
        </li>
        <li>
          <button type="button" className="link-button" onClick={() => openSubTopic(COMPARE_COLLECTIONS)}>
            Compare Collections
          </button>
        </li>
      </ul>
    </div>
  );
}
