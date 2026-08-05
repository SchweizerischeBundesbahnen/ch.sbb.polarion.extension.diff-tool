import { useState } from 'react';
import infoIcon from './info.svg';
import openInTableIcon from './open-in-table.svg';

interface TableFooterProps {
  totalCount: number;
  shownCount: number;
  query: string;
  /** Polarion's own table view of the same query. Absent for collections, which have no such view. */
  openInTableUrl?: string | null;
}

/**
 * The counts, "open in table" link and query popup under a picker table, as the Java
 * BottomQueryLinksBuilder rendered them. The counts link to Polarion's table view where there is one.
 */
export default function TableFooter({ totalCount, shownCount, query, openInTableUrl }: Readonly<TableFooterProps>) {
  const [queryShown, setQueryShown] = useState(false);
  const counts = shownCount < totalCount ? `${shownCount} of ${totalCount} items` : `${totalCount} items`;

  return (
    <div className="table-footer">
      <div className="table-counts">
        {openInTableUrl ? (
          <a href={openInTableUrl} target="_top">
            {counts}
          </a>
        ) : (
          counts
        )}
      </div>

      {openInTableUrl ? (
        <div className="open-in-table">
          <a href={openInTableUrl} target="_blank" rel="noreferrer">
            <img src={openInTableIcon} title="Open in table" alt="Open in table" />
          </a>
        </div>
      ) : null}

      {query ? (
        <div className="show-query">
          <button type="button" className="icon-button" title="Show query" onClick={() => setQueryShown(!queryShown)}>
            <img src={infoIcon} alt="Show query" />
          </button>
          {queryShown ? <div className="query-text">{query}</div> : null}
        </div>
      ) : null}
    </div>
  );
}
