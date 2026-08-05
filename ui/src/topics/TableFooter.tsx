import { useState } from 'react';
import infoIcon from './info.svg';
import openInTableIcon from './open-in-table.svg';

interface TableFooterProps {
  totalCount: number;
  shownCount: number;
  /** The query as the backend executed it, project restriction included. */
  query: string;
  /** Polarion's own table view of the same query. Absent for collections, which have no such view. */
  openInTableUrl?: string | null;
}

/**
 * The counts, "open in table" link and query popup under a picker table.
 *
 * Both texts are the ones Polarion's rich page table footer used (`form.modules.label.showMultiOf.item` and
 * `form.modules.label.showMulti.item`), and the counts link to the table view where there is one - as
 * BottomQueryLinksBuilder had it. The query is hidden until the info icon is clicked.
 */
export default function TableFooter({ totalCount, shownCount, query, openInTableUrl }: Readonly<TableFooterProps>) {
  const [queryShown, setQueryShown] = useState(false);
  const counts =
    shownCount < totalCount ? `Showing ${shownCount} items of ${totalCount} found` : `${totalCount} items found`;

  return (
    <div className="table-footer">
      <div className="table-footer-line">
        <span className="table-counts">
          {openInTableUrl ? (
            <a className="sbb-btn--link" href={openInTableUrl} target="_top">
              {counts}
            </a>
          ) : (
            counts
          )}
        </span>

        {openInTableUrl ? (
          <a className="footer-icon" href={openInTableUrl} target="_blank" rel="noreferrer" title="Open in table">
            <img src={openInTableIcon} alt="Open in table" />
          </a>
        ) : null}

        {query ? (
          <button
            type="button"
            className="footer-icon"
            title="Show query"
            aria-expanded={queryShown}
            onClick={() => setQueryShown(!queryShown)}
          >
            <img src={infoIcon} alt="Show query" />
          </button>
        ) : null}
      </div>

      {queryShown && query ? <div className="query-text">{query}</div> : null}
    </div>
  );
}
