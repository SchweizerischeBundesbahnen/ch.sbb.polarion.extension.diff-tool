interface PaginatorProps {
  page: number;
  lastPage: number;
  onPage: (page: number) => void;
}

/**
 * The page links of a picker table, in the same shape the widget renderer produced: `<<`, `<`, up to two pages
 * on each side of the current one, `>`, `>>`, with an ellipsis where pages are skipped. The current page is
 * plain text, not a link.
 */
export default function Paginator({ page, lastPage, onPage }: Readonly<PaginatorProps>) {
  if (lastPage <= 1) {
    return null;
  }

  const link = (text: string, target: number) => (
    <button type="button" className="paginator-link" key={`${text}-${target}`} onClick={() => onPage(target)}>
      {text}
    </button>
  );

  return (
    <div className="paginator">
      {page > 1 ? (
        <>
          {link('<<', 1)}
          {link('<', page - 1)}
          {page - 3 > 0 ? <span>...</span> : null}
          {page - 2 > 0 ? link(String(page - 2), page - 2) : null}
          {link(String(page - 1), page - 1)}
        </>
      ) : null}

      <span className="paginator-current">{page}</span>

      {page < lastPage ? (
        <>
          {link(String(page + 1), page + 1)}
          {page + 2 <= lastPage ? link(String(page + 2), page + 2) : null}
          {page + 3 <= lastPage ? <span>...</span> : null}
          {link('>', page + 1)}
          {link('>>', lastPage)}
        </>
      ) : null}
    </div>
  );
}
