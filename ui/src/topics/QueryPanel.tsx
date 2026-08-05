import { type ReactNode, useEffect, useState } from 'react';
import { DEFAULT_RECORDS_PER_PAGE } from './topicParams';

interface QueryPanelProps {
  /** Which table this panel filters; also the id prefix the legacy widget used. */
  side: 'source' | 'target';
  query: string;
  recordsPerPage: number;
  onApply: (query: string, recordsPerPage: number) => void;
  onReset: () => void;
  /** Rendered before the query input, which is where the collections widget put the target project select. */
  children?: ReactNode;
}

/**
 * The Lucene query row of a picker table: query, page size, Apply and Reset.
 *
 * Both inputs are drafts until Apply (or Enter in the query field), as they were when the widget rendered them
 * and Apply pushed the values into the frame URL. Reset restores an empty query and the default page size.
 */
export default function QueryPanel({
  side,
  query,
  recordsPerPage,
  onApply,
  onReset,
  children,
}: Readonly<QueryPanelProps>) {
  const [draftQuery, setDraftQuery] = useState(query);
  const [draftRecordsPerPage, setDraftRecordsPerPage] = useState(String(recordsPerPage));

  // Keeps the drafts in step with the applied state, which changes on Reset and on back/forward.
  useEffect(() => setDraftQuery(query), [query]);
  useEffect(() => setDraftRecordsPerPage(String(recordsPerPage)), [recordsPerPage]);

  const apply = () => {
    const parsed = Number.parseInt(draftRecordsPerPage, 10);
    onApply(draftQuery, Number.isNaN(parsed) || parsed < 1 ? DEFAULT_RECORDS_PER_PAGE : parsed);
  };

  return (
    <div className="query">
      {children}
      <label htmlFor={`${side}-query-input`}>Query (Lucene):</label>
      <input
        id={`${side}-query-input`}
        className="query-input"
        type="text"
        value={draftQuery}
        onChange={(event) => setDraftQuery(event.target.value)}
        onKeyDown={(event) => {
          // Enter applies the query, as the nav-topic JSPs' body onload handler made it do.
          if (event.key === 'Enter') {
            apply();
          }
        }}
      />
      <label htmlFor={`${side}-records-per-page-input`}>Records per page:</label>
      <input
        id={`${side}-records-per-page-input`}
        className="records-per-page"
        type="text"
        value={draftRecordsPerPage}
        onChange={(event) => setDraftRecordsPerPage(event.target.value)}
      />
      <button type="button" onClick={apply}>
        Apply
      </button>
      <button type="button" onClick={onReset}>
        Reset
      </button>
    </div>
  );
}
