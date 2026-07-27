interface RoleCheckboxGroupProps {
  title: string;
  /** All roles that exist and can be granted, in display order. */
  roles: string[];
  /** The subset currently granted. */
  selected: string[];
  onToggle: (role: string, checked: boolean) => void;
  /** Shown instead of the list when there are no roles at all. */
  emptyMessage: string;
}

/**
 * One column of role checkboxes, replacing the table of rows that authorization.jsp used to render from
 * a RolesUtils scriptlet.
 *
 * The layout (a vertical stack) is set on the wrapping `.role-group` in App.css, never on the labels:
 * generic's control CSS styles the admin checkbox through
 * `.standard-admin-page label:has(input[type='checkbox'])` at specificity 0,2,2, which would override a
 * label rule of our own.
 */
export default function RoleCheckboxGroup({ title, roles, selected, onToggle, emptyMessage }: RoleCheckboxGroupProps) {
  return (
    <div className="role-group">
      <h2>{title}</h2>
      {roles.length === 0 ? (
        <span className="no-roles">{emptyMessage}</span>
      ) : (
        roles.map((role) => (
          <label key={role}>
            <input
              type="checkbox"
              checked={selected.includes(role)}
              onChange={(event) => onToggle(role, event.target.checked)}
            />
            {role}
          </label>
        ))
      )}
    </div>
  );
}
