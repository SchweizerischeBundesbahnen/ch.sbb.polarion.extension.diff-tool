import { useCallback, useEffect, useState } from 'react';
import {
  ConfigurationButtons,
  PageLayout,
  type Revision,
  RevisionsTable,
  getScope,
} from '@grigoriev/react-sbb-polarion';
import { toast } from 'sonner';
import useRemote from '../../services/useRemote';
import useSettings, { DEFAULT_CONFIGURATION } from '../../services/useSettings';
import RoleCheckboxGroup from '../components/RoleCheckboxGroup';

/** The `authorization` named setting. A single "Default" configuration, as the legacy page had. */
interface AuthorizationModel {
  globalRoles?: string[];
  projectRoles?: string[];
  bundleTimestamp?: string;
}

interface AvailableRoles {
  globalRoles: string[];
  projectRoles: string[];
}

/** Sorted for a stable checkbox order: getContextRoles returns an unordered Set server-side. */
function sorted(roles: string[] | undefined): string[] {
  return [...(roles ?? [])].sort((left, right) => left.localeCompare(right));
}

export default function MergeAuthorizationPage() {
  const scope = getScope();
  const { sendRequest } = useRemote();
  const settings = useSettings<AuthorizationModel>('authorization');

  const [available, setAvailable] = useState<AvailableRoles>({ globalRoles: [], projectRoles: [] });
  const [granted, setGranted] = useState<AuthorizationModel>({ globalRoles: [], projectRoles: [] });
  const [loadError, setLoadError] = useState<string | null>(null);
  const [newerVersion, setNewerVersion] = useState(false);
  const [revisionsShown, setRevisionsShown] = useState(false);
  const [revisionsToken, setRevisionsToken] = useState(0);

  const applySettings = useCallback((model: AuthorizationModel, currentBundleTimestamp?: string) => {
    setGranted({ globalRoles: model.globalRoles ?? [], projectRoles: model.projectRoles ?? [] });
    if (currentBundleTimestamp !== undefined) {
      // Matches the legacy notification: the stored setting was written by a different build of the
      // extension than the one currently deployed.
      setNewerVersion(!!model.bundleTimestamp && model.bundleTimestamp !== currentBundleTimestamp);
    }
  }, []);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const [rolesResponse, infoResponse] = await Promise.all([
          sendRequest({ method: 'GET', url: `/roles?scope=${encodeURIComponent(scope)}` }),
          sendRequest({ method: 'GET', url: '/extension/info' }),
        ]);
        if (!rolesResponse.ok) {
          throw new Error(`Loading roles failed (HTTP ${rolesResponse.status})`);
        }
        const roles: AvailableRoles = await rolesResponse.json();
        const bundleTimestamp = infoResponse.ok
          ? ((await infoResponse.json())?.version?.bundleBuildTimestamp as string | undefined)
          : undefined;
        const model = await settings.loadContent(DEFAULT_CONFIGURATION, scope);
        if (cancelled) {
          return;
        }
        setAvailable({ globalRoles: sorted(roles.globalRoles), projectRoles: sorted(roles.projectRoles) });
        applySettings(model, bundleTimestamp ?? '');
        setLoadError(null);
      } catch (error) {
        if (!cancelled) {
          setLoadError((error as Error).message);
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [sendRequest, settings, scope, applySettings]);

  const toggle = (group: 'globalRoles' | 'projectRoles') => (role: string, checked: boolean) =>
    setGranted((current) => {
      const roles = new Set(current[group] ?? []);
      if (checked) {
        roles.add(role);
      } else {
        roles.delete(role);
      }
      return { ...current, [group]: [...roles] };
    });

  const reload = async () => {
    try {
      applySettings(await settings.loadContent(DEFAULT_CONFIGURATION, scope));
      setLoadError(null);
    } catch (error) {
      setLoadError((error as Error).message);
    }
  };

  const handleSave = async () => {
    try {
      await settings.saveContent(DEFAULT_CONFIGURATION, scope, {
        globalRoles: granted.globalRoles ?? [],
        projectRoles: granted.projectRoles ?? [],
      });
      setNewerVersion(false);
      setRevisionsToken((token) => token + 1);
      toast.success('Settings saved');
    } catch (error) {
      toast.error(`Settings not saved: ${(error as Error).message}`);
    }
  };

  const handleCancel = () => {
    if (window.confirm('Are you sure you want to cancel editing and revert all changes made?')) {
      void reload();
    }
  };

  const handleRevertToDefault = async () => {
    if (!window.confirm('Are you sure you want to return the default values?')) {
      return;
    }
    try {
      // Loads the defaults into the form only; nothing is persisted until Save, as before.
      applySettings(await settings.loadDefaultContent());
      toast.info('Reverted to default values. Save to apply.');
    } catch (error) {
      toast.error((error as Error).message);
    }
  };

  const handleRevertToRevision = async (revision: Revision) => {
    try {
      applySettings(await settings.loadContent(DEFAULT_CONFIGURATION, scope, revision.name));
      toast.info(`Loaded revision ${revision.name}. Save to apply.`);
    } catch (error) {
      toast.error((error as Error).message);
    }
  };

  return (
    <PageLayout title="Merge Authorization">
      {loadError && <div className="alert alert-error">{loadError}</div>}
      {newerVersion && (
        <div className="alert alert-warning">
          These settings were saved by a different version of the extension. Review and save them again.
        </div>
      )}

      <div className="role-groups">
        <RoleCheckboxGroup
          title="Global Roles"
          roles={available.globalRoles}
          selected={granted.globalRoles ?? []}
          onToggle={toggle('globalRoles')}
          emptyMessage="No global roles defined"
        />
        <RoleCheckboxGroup
          title="Project Roles"
          roles={available.projectRoles}
          selected={granted.projectRoles ?? []}
          onToggle={toggle('projectRoles')}
          emptyMessage="No project roles in this scope"
        />
      </div>

      <ConfigurationButtons
        onSave={() => void handleSave()}
        onCancel={handleCancel}
        onRevertToDefault={() => void handleRevertToDefault()}
        onToggleRevisions={() => setRevisionsShown((shown) => !shown)}
        revisionsShown={revisionsShown}
      />

      {revisionsShown && (
        <RevisionsTable
          name={DEFAULT_CONFIGURATION}
          scope={scope}
          reloadToken={revisionsToken}
          loadRevisions={settings.loadRevisions}
          onRevert={(revision) => void handleRevertToRevision(revision)}
        />
      )}

      <div className="quick-help">
        <h2>Quick Help</h2>
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
      </div>
    </PageLayout>
  );
}
