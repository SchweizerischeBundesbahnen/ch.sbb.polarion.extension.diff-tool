import type { ReactNode } from 'react';

export interface Column<T> {
  key: string;
  label: string;
  render: (item: T) => ReactNode;
}

interface ItemsTableProps<T> {
  columns: Column<T>[];
  items: T[];
  /** The index is the fallback for a row the backend could not even name (an unresolvable item). */
  rowKey: (item: T, index: number) => string;
  /** The checkbox or radio of a row; the widget put it in a leading column of its own. */
  renderSelection: (item: T) => ReactNode;
  /** Reason a row cannot be shown (unresolvable, or not readable by this user), or null for a normal row. */
  unavailableMessage: (item: T) => string | null | undefined;
  /** The header's select-all checkbox. Only the WorkItems table has one. */
  selectAll?: { checked: boolean; onChange: (checked: boolean) => void } | null;
  loading: boolean;
  /** Shown in place of rows when the search returned nothing, or was not run at all. */
  emptyMessage: string;
  footer?: ReactNode;
}

/**
 * The selection table of a picker page: a leading column of checkboxes or radios, then one column per field.
 *
 * Replaces the Polarion rich page table the widget renderers produced. The columns are rendered from plain
 * REST values here, so the page needs none of Polarion's own stylesheets.
 */
export default function ItemsTable<T>({
  columns,
  items,
  rowKey,
  renderSelection,
  unavailableMessage,
  selectAll,
  loading,
  emptyMessage,
  footer,
}: Readonly<ItemsTableProps<T>>) {
  return (
    <div className="items-table-wrapper">
      <table className="items-table">
        <thead>
          <tr className="table-header-row">
            <th className="select-column">
              {selectAll ? (
                <input
                  type="checkbox"
                  className="select-all"
                  aria-label="Select all items on this page"
                  checked={selectAll.checked}
                  onChange={(event) => selectAll.onChange(event.target.checked)}
                />
              ) : null}
            </th>
            {columns.map((column) => (
              <th key={column.key}>{column.label}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {items.length === 0 ? (
            <tr className="table-content-row">
              <td className="table-empty-cell" colSpan={columns.length + 1}>
                {loading ? 'Loading...' : emptyMessage}
              </td>
            </tr>
          ) : (
            items.map((item, index) => {
              const unavailable = unavailableMessage(item);
              return (
                <tr className="table-content-row" key={rowKey(item, index)}>
                  {unavailable ? (
                    <td className="table-not-readable-cell" colSpan={columns.length + 1}>
                      {unavailable}
                    </td>
                  ) : (
                    <>
                      <td className="select-column">{renderSelection(item)}</td>
                      {columns.map((column) => (
                        <td key={column.key}>{column.render(item)}</td>
                      ))}
                    </>
                  )}
                </tr>
              );
            })
          )}
        </tbody>
      </table>
      {footer}
    </div>
  );
}

/** An enumeration cell: Polarion's icon for the option, if it has one, plus its name. */
export function EnumCell({ option }: Readonly<{ option?: { name: string; iconUrl?: string | null } | null }>) {
  if (!option) {
    return null;
  }
  return (
    <span className="enum-cell">
      {/* The icon is served by Polarion. It is decorative: the name is always rendered next to it, so a
          missing icon (no Polarion behind the page, as under Vitest) costs nothing. */}
      {option.iconUrl ? <img src={option.iconUrl} alt="" /> : null}
      {option.name}
    </span>
  );
}
