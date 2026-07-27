import { useCallback, useEffect, useRef, useState } from 'react';
import {
  ConfigurationButtons,
  ConfigurationsPane,
  type ConfigurationsPaneHandle,
  PageLayout,
  type Revision,
  RevisionsTable,
  getProjectIdFromScope,
  getScope,
} from '@grigoriev/react-sbb-polarion';
import { toast } from 'sonner';
import useRemote from '../../services/useRemote';
import useSettings from '../../services/useSettings';
import FieldsTransferList from '../components/FieldsTransferList';
import MultiSearchableSelect, { type MultiOption } from '../components/MultiSearchableSelect';
import type { DiffField, DiffModel, LinkRole, WorkItemField, WorkItemStatus } from '../types';
import { fieldId } from '../types';

/**
 * The form's own state. Every list is required here - applyModel normalises the wire model's optional
 * arrays once, on the way in, so nothing downstream needs a `?? []` fallback.
 */
interface DiffSettings {
  diffFields: DiffField[];
  statusesToIgnore: string[];
  hyperlinkRoles: string[];
  linkedWorkItemRoles: string[];
  /** Build that wrote the stored settings, used only for the newer-version warning. */
  bundleTimestamp?: string;
}

const EMPTY: DiffSettings = { diffFields: [], statusesToIgnore: [], hyperlinkRoles: [], linkedWorkItemRoles: [] };

const NO_LISTS: ProjectData = { fields: [], statuses: [], hyperlinkRoles: [], linkedWorkItemRoles: [] };

/** Selecting these fields reveals the corresponding role multiselect, as in the legacy page. */
const HYPERLINKS_FIELD = 'hyperlinks';
const LINKED_WORK_ITEMS_FIELD = 'linkedWorkItems';

interface ProjectData {
  fields: WorkItemField[];
  statuses: WorkItemStatus[];
  hyperlinkRoles: LinkRole[];
  linkedWorkItemRoles: LinkRole[];
}

/** True when the two field sets differ, ignoring order. */
function fieldsDiffer(left: DiffField[], right: DiffField[] = []): boolean {
  if (left.length !== right.length) {
    return true;
  }
  const rightIds = new Set(right.map(fieldId));
  return left.some((field) => !rightIds.has(fieldId(field)));
}

export default function DiffConfigurationsPage() {
  const scope = getScope();
  const projectId = getProjectIdFromScope(scope);
  const { sendRequest } = useRemote();
  const settings = useSettings<DiffModel>('diff');

  const [data, setData] = useState<ProjectData | null>(null);
  const [model, setModel] = useState<DiffSettings>(EMPTY);
  const [selectedConfiguration, setSelectedConfiguration] = useState<string | null>(null);
  const [editingName, setEditingName] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [newerVersion, setNewerVersion] = useState(false);
  const [revisionsShown, setRevisionsShown] = useState(false);
  const [revisionsToken, setRevisionsToken] = useState(0);
  const [currentBundleTimestamp, setCurrentBundleTimestamp] = useState<string | undefined>(undefined);
  const paneRef = useRef<ConfigurationsPaneHandle>(null);

  // The four project lists the controls are built from. Loaded once - they do not depend on which
  // configuration is selected.
  useEffect(() => {
    if (!projectId) {
      setLoadError('This page must be opened in a project scope.');
      return;
    }
    let cancelled = false;
    const list = <T,>(what: string) =>
      sendRequest({ method: 'GET', url: `/projects/${encodeURIComponent(projectId)}/${what}` }).then(
        async (response) => {
          if (!response.ok) {
            throw new Error(`Loading ${what} failed (HTTP ${response.status})`);
          }
          return (await response.json()) as T[];
        },
      );

    (async () => {
      try {
        const [fields, statuses, hyperlinkRoles, linkedWorkItemRoles, info] = await Promise.all([
          list<WorkItemField>('workitem-fields'),
          list<WorkItemStatus>('workitem-statuses'),
          list<LinkRole>('hyperlink-roles'),
          list<LinkRole>('linked-workitem-roles'),
          sendRequest({ method: 'GET', url: '/extension/info' }),
        ]);
        if (cancelled) {
          return;
        }
        setCurrentBundleTimestamp(
          info.ok ? ((await info.json())?.version?.bundleBuildTimestamp as string | undefined) : undefined,
        );
        setData({
          fields: fields,
          statuses: statuses,
          hyperlinkRoles: hyperlinkRoles,
          linkedWorkItemRoles: linkedWorkItemRoles,
        });
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
  }, [sendRequest, projectId]);

  const applyModel = useCallback((loaded: DiffModel) => {
    setModel({
      diffFields: loaded.diffFields ?? [],
      statusesToIgnore: loaded.statusesToIgnore ?? [],
      hyperlinkRoles: loaded.hyperlinkRoles ?? [],
      linkedWorkItemRoles: loaded.linkedWorkItemRoles ?? [],
      bundleTimestamp: loaded.bundleTimestamp,
    });
  }, []);

  /**
   * When the stored settings were written by a different build of the extension, warn if the default
   * field set has changed since - the configuration may be missing newly added fields.
   *
   * Evaluated in an effect rather than inside applyModel because the two inputs arrive independently:
   * ConfigurationsPane can deliver the stored content before the /extension/info response lands, and
   * comparing against a not-yet-known timestamp made the warning depend on network ordering.
   *
   * (The legacy check additionally compared the two field arrays by object identity, which is never
   * equal, so it warned on every build change whether or not anything had actually changed.)
   */
  useEffect(() => {
    const storedTimestamp = model.bundleTimestamp;
    if (!storedTimestamp || !currentBundleTimestamp || storedTimestamp === currentBundleTimestamp) {
      setNewerVersion(false);
      return;
    }
    let cancelled = false;
    settings
      .loadDefaultContent()
      .then((defaults) => {
        if (!cancelled) {
          setNewerVersion(fieldsDiffer(model.diffFields, defaults.diffFields));
        }
      })
      .catch(() => {
        if (!cancelled) {
          setNewerVersion(false);
        }
      });
    return () => {
      cancelled = true;
    };
    // Deliberately keyed on the stored timestamp, not the whole model: editing fields in the form must
    // not re-trigger the comparison.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [model.bundleTimestamp, currentBundleTimestamp, settings]);

  const save = async () => {
    if (!selectedConfiguration) {
      return;
    }
    try {
      await settings.saveContent(selectedConfiguration, scope, {
        diffFields: model.diffFields,
        statusesToIgnore: model.statusesToIgnore,
        hyperlinkRoles: model.hyperlinkRoles,
        linkedWorkItemRoles: model.linkedWorkItemRoles,
      });
      setNewerVersion(false);
      setRevisionsToken((token) => token + 1);
      paneRef.current?.reloadNames();
      toast.success('Settings saved');
    } catch (error) {
      toast.error(`Settings not saved: ${(error as Error).message}`);
    }
  };

  const reload = async () => {
    if (!selectedConfiguration) {
      return;
    }
    try {
      applyModel(await settings.loadContent(selectedConfiguration, scope));
      setLoadError(null);
    } catch (error) {
      setLoadError((error as Error).message);
    }
  };

  const revertToDefault = async () => {
    if (!window.confirm('Are you sure you want to return the default value?')) {
      return;
    }
    try {
      // Fills the form only; nothing is persisted until Save, as before.
      applyModel(await settings.loadDefaultContent());
      toast.info('Reverted to default values. Save to apply.');
    } catch (error) {
      toast.error((error as Error).message);
    }
  };

  const revertToRevision = async (revision: Revision) => {
    if (!selectedConfiguration) {
      return;
    }
    try {
      applyModel(await settings.loadContent(selectedConfiguration, scope, revision.name));
      toast.info(`Loaded revision ${revision.name}. Save to apply.`);
    } catch (error) {
      toast.error((error as Error).message);
    }
  };

  // One nullish check for the whole payload instead of four.
  const lists = data ?? NO_LISTS;

  const statusOptions: MultiOption[] = lists.statuses.map((status) => ({
    value: status.id,
    label: status.wiTypeName ? `${status.name} [${status.id} - ${status.wiTypeName}]` : `${status.name} [${status.id}]`,
    icon: status.iconUrl,
  }));

  const hyperlinkRoleOptions: MultiOption[] = lists.hyperlinkRoles.map((role) => ({
    value: role.combinedId ?? role.id,
    label: `[${role.workItemTypeName}] ${role.name}`,
  }));

  const linkedWorkItemRoleOptions: MultiOption[] = lists.linkedWorkItemRoles.map((role) => ({
    value: role.id,
    label: role.name,
  }));

  const hasField = (key: string) => model.diffFields.some((field) => field.key === key);

  return (
    <PageLayout title="Diff Configurations">
      {loadError && <div className="alert alert-error">{loadError}</div>}
      {newerVersion && (
        <div className="alert alert-warning">
          A newer version of the extension provides a different set of default fields. Review this configuration and
          save it again.
        </div>
      )}

      <ConfigurationsPane<DiffModel>
        ref={paneRef}
        scope={scope}
        service={settings}
        cookieKey="selected-configuration-diff"
        label="configuration"
        onContentLoaded={applyModel}
        onSelectedChange={setSelectedConfiguration}
        onEditingNameChange={setEditingName}
      />

      <div className={`diff-settings${editingName ? ' dimmed' : ''}`}>
        <FieldsTransferList
          fields={lists.fields}
          selected={model.diffFields}
          onChange={(diffFields) => setModel((current) => ({ ...current, diffFields: diffFields }))}
        />

        <div className="diff-role-settings">
          <div className="diff-role-column">
            <label htmlFor="statuses-to-ignore">
              Statuses of WorkItems in a source document to ignore when diffing:
            </label>
            <MultiSearchableSelect
              id="statuses-to-ignore"
              options={statusOptions}
              selected={model.statusesToIgnore}
              onChange={(statuses) => setModel((current) => ({ ...current, statusesToIgnore: statuses }))}
              placeholder="Select statuses to ignore..."
            />
          </div>

          {hasField(HYPERLINKS_FIELD) && (
            <div className="diff-role-column" id="hyperlink-settings-container">
              <label htmlFor="hyperlink-roles">Hyperlink roles to diff and merge</label>
              <MultiSearchableSelect
                id="hyperlink-roles"
                options={hyperlinkRoleOptions}
                selected={model.hyperlinkRoles}
                onChange={(roles) => setModel((current) => ({ ...current, hyperlinkRoles: roles }))}
                placeholder="Select hyperlink roles..."
              />
            </div>
          )}

          {hasField(LINKED_WORK_ITEMS_FIELD) && (
            <div className="diff-role-column" id="linked-workitem-settings-container">
              <label htmlFor="linked-workitem-roles">Roles of linked WorkItems to diff and merge</label>
              <MultiSearchableSelect
                id="linked-workitem-roles"
                options={linkedWorkItemRoleOptions}
                selected={model.linkedWorkItemRoles}
                onChange={(roles) => setModel((current) => ({ ...current, linkedWorkItemRoles: roles }))}
                placeholder="Select linked WorkItem roles..."
              />
            </div>
          )}
        </div>
      </div>

      <ConfigurationButtons
        onSave={() => void save()}
        onCancel={() => {
          if (window.confirm('Are you sure you want to cancel editing and revert all changes made?')) {
            void reload();
          }
        }}
        onRevertToDefault={() => void revertToDefault()}
        onToggleRevisions={() => setRevisionsShown((shown) => !shown)}
        revisionsShown={revisionsShown}
      />

      {revisionsShown && selectedConfiguration && (
        <RevisionsTable
          name={selectedConfiguration}
          scope={scope}
          reloadToken={revisionsToken}
          loadRevisions={settings.loadRevisions}
          onRevert={(revision) => void revertToRevision(revision)}
        />
      )}
    </PageLayout>
  );
}
