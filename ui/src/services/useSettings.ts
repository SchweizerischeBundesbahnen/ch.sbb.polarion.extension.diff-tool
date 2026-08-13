import type { Revision, SettingName } from '@grigoriev/react-sbb-polarion';
// The module-level request function, not the useRemote() wrapper: the service is built once per feature
// outside of any render, so calling something named use* here would (rightly) trip rules-of-hooks.
import { sendRequest } from './useRemote';

/**
 * The generic framework's named-settings REST API, for one feature (`diff`, `authorization`,
 * `executionQueue`). One hook serves every settings page.
 *
 * The returned object is structurally a react-sbb-polarion `ConfigurationsService<T>`, so it can be
 * handed to `ConfigurationsPane` as `service` and its `loadRevisions` to `RevisionsTable` directly.
 */
export const DEFAULT_CONFIGURATION = 'Default';

async function readJson<T>(response: Response, what: string): Promise<T> {
  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new Error(body?.errorMessage || body?.message || `${what} failed (HTTP ${response.status})`);
  }
  return (await response.json()) as T;
}

async function expectOk(response: Response, what: string): Promise<void> {
  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new Error(body?.errorMessage || body?.message || `${what} failed (HTTP ${response.status})`);
  }
}

export interface SettingsService<T> {
  loadConfigurationNames: (scope: string) => Promise<SettingName[]>;
  loadContent: (name: string, scope: string, revision?: string) => Promise<T>;
  loadDefaultContent: () => Promise<T>;
  saveContent: (name: string, scope: string, content: T) => Promise<void>;
  createConfiguration: (name: string, scope: string) => Promise<void>;
  renameConfiguration: (name: string, scope: string, newName: string) => Promise<void>;
  deleteConfiguration: (name: string, scope: string) => Promise<void>;
  loadRevisions: (name: string, scope: string) => Promise<Revision[]>;
}

// Cached per feature so the service object - and every function on it - keeps a stable identity across
// renders. react-sbb-polarion's RevisionsTable and ConfigurationsPane list the injected callbacks in
// their effect dependencies, so a fresh identity per render would refetch on every keystroke or
// checkbox toggle in the page hosting them.
const services = new Map<string, SettingsService<unknown>>();

function createService<T>(feature: string): SettingsService<T> {
  const base = `/settings/${encodeURIComponent(feature)}`;
  const named = (name: string) => `${base}/names/${encodeURIComponent(name)}`;
  const scoped = (scope: string) => `scope=${encodeURIComponent(scope)}`;

  return {
    loadConfigurationNames: async (scope) =>
      readJson<SettingName[]>(
        await sendRequest({ method: 'GET', url: `${base}/names?${scoped(scope)}` }),
        'Loading configuration names',
      ),

    loadContent: async (name, scope, revision) => {
      const revisionParam = revision ? `&revision=${encodeURIComponent(revision)}` : '';
      return readJson<T>(
        await sendRequest({ method: 'GET', url: `${named(name)}/content?${scoped(scope)}${revisionParam}` }),
        'Loading configuration',
      );
    },

    // Scope-independent by design in the generic API: the defaults come from the settings class itself.
    loadDefaultContent: async () =>
      readJson<T>(await sendRequest({ method: 'GET', url: `${base}/default-content` }), 'Loading default values'),

    saveContent: async (name, scope, content) =>
      expectOk(
        await sendRequest({
          method: 'PUT',
          url: `${named(name)}/content?${scoped(scope)}`,
          contentType: 'application/json',
          body: JSON.stringify(content),
        }),
        'Saving configuration',
      ),

    // An empty body makes the backend seed the setting with its default values.
    createConfiguration: async (name, scope) =>
      expectOk(
        await sendRequest({
          method: 'PUT',
          url: `${named(name)}/content?${scoped(scope)}`,
          contentType: 'application/json',
          body: '',
        }),
        'Creating configuration',
      ),

    // The new name is the request body, as a bare JSON string.
    renameConfiguration: async (name, scope, newName) =>
      expectOk(
        await sendRequest({
          method: 'POST',
          url: `${named(name)}?${scoped(scope)}`,
          contentType: 'application/json',
          body: JSON.stringify(newName),
        }),
        'Renaming configuration',
      ),

    deleteConfiguration: async (name, scope) =>
      expectOk(
        await sendRequest({ method: 'DELETE', url: `${named(name)}?${scoped(scope)}` }),
        'Deleting configuration',
      ),

    loadRevisions: async (name, scope) =>
      readJson<Revision[]>(
        await sendRequest({ method: 'GET', url: `${named(name)}/revisions?${scoped(scope)}` }),
        'Loading revisions',
      ),
  };
}

export default function useSettings<T>(feature: string): SettingsService<T> {
  let service = services.get(feature);
  if (!service) {
    service = createService<unknown>(feature);
    services.set(feature, service);
  }
  return service as SettingsService<T>;
}
