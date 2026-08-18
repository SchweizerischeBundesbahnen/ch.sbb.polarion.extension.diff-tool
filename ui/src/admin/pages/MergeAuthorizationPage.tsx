import { useMemo } from 'react';
import { AuthorizationSettings, createAuthorizationService } from '@sbb-polarion/react-sbb-polarion';
import type { AuthorizationService } from '@sbb-polarion/react-sbb-polarion';
import { sendRequest } from '../../services/useRemote';

/** The `authorization` named setting, whose single "Default" configuration this page edits. */
const FEATURE_NAME = 'authorization';

/** Sorted, for a stable checkbox order: both role lookups end in ISecurityService, whose
 *  getGlobalRoles / getContextRoles return unordered collections. */
function sorted(roles: string[] | undefined): string[] {
  return [...(roles ?? [])].sort((left, right) => left.localeCompare(right));
}

/**
 * Which roles may merge. The page itself is react-sbb-polarion's `AuthorizationSettings`, shared with
 * every other extension that grants a permission to a set of roles; what stays here is the title, which
 * setting to read and write, and the Quick Help text.
 *
 * The service talks to generic's own endpoints - `/roles`, from the RolesInternalController that
 * DiffToolRestApplication registers, plus the single-setting endpoints of `authorization`. This extension
 * used to carry its own equivalent of all of that: a `/roles` endpoint, a RolesModel, a RolesUtils, the
 * role checkbox columns and the toolbar wiring.
 *
 * One thing is deliberately gone with the move: the "saved by a different version of the extension"
 * banner. A role setting is two lists of role names, so there is no schema that can go stale, and since
 * the timestamp is stamped at save time the banner appeared after every plugin upgrade, asked the
 * administrator to check something that cannot have changed, and could only be dismissed by saving
 * again. RSP dropped it for exactly this page.
 */
export default function MergeAuthorizationPage() {
  const service: AuthorizationService = useMemo(() => {
    const generic = createAuthorizationService(sendRequest, FEATURE_NAME);
    return {
      ...generic,
      loadRoles: async (scope: string) => {
        const roles = await generic.loadRoles(scope);
        return { globalRoles: sorted(roles.globalRoles), projectRoles: sorted(roles.projectRoles) };
      },
    };
  }, []);

  return (
    <AuthorizationSettings
      title="Merge Authorization"
      service={service}
      quickHelp={
        <>
          <h3>Permissions</h3>
          <p>The diffing functionality is unrestricted and available to all users.</p>
          <p>
            On the other hand, the merging functionality can be restricted or permitted for specific global or project
            roles.
          </p>
          <p>By default, only users with the global admin role have permission to merge.</p>
          <p>
            Additionally, project administrators can configure merging permissions based on the needs of their specific
            project, allowing for more granular control.
          </p>
        </>
      }
    />
  );
}
