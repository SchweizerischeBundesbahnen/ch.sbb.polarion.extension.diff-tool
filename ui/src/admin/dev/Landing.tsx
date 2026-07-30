import { useEffect, useState } from 'react';
import { PageLayout, SearchableSelect, getCookie, getScope, setCookie } from '@grigoriev/react-sbb-polarion';
import { FEATURES } from '../../features';
import { sendRequest } from '../../services/useRemote';
import type { ProjectInfo } from '../types';

/** Where the dev scope selection is remembered, so it survives a reload and a hop between features. */
const DEV_SCOPE_COOKIE = 'diff-tool-dev-scope';

/**
 * The scope to start on: an explicit `scope` query parameter wins - that is how a feature page's own
 * "Overview" back link returns here - then the last selection from the cookie, then global.
 */
function initialScope(): string {
  if (new URLSearchParams(window.location.search).has('scope')) {
    return getScope();
  }
  return getCookie(DEV_SCOPE_COOKIE) ?? '';
}

/**
 * Development-only overview, shown when no (or an unknown) `?feature=` is given. Polarion always opens a
 * page with an explicit feature id, `embedded=true` and a `scope`, so this is never reached there - it
 * exists to make `vite dev` navigable. Excluded from the coverage gate as dev scaffolding.
 *
 * It carries the scope, which is the point of it: every admin page here is scope-sensitive - the
 * configuration names, the roles offered, the queue settings are all read per scope - and without a
 * picker the only way to reach a project scope in dev was to hand-edit the URL. Pick a project and every
 * feature link below gets `scope=project/<id>/`; the choice is remembered in a cookie.
 *
 * The projects come from this extension's own `/projects` endpoint through `sendRequest`, not from
 * Polarion's platform REST API as in the sibling extensions - so there is no second auth path to keep
 * working (the session in Polarion, the bearer token in dev) and no JSON:API shape to unpick. Those
 * extensions have no projects endpoint of their own; this one does, because Project Duplication needs it.
 */
export default function Landing() {
  const [projects, setProjects] = useState<ProjectInfo[]>([]);
  const [scope, setScope] = useState<string>(initialScope);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const response = await sendRequest({ method: 'GET', url: '/projects' });
        if (!response.ok) {
          throw new Error(`HTTP ${response.status}`);
        }
        const loaded = (await response.json()) as ProjectInfo[];
        if (!cancelled) {
          setProjects(loaded);
          setError(null);
        }
      } catch {
        if (!cancelled) {
          setError(
            'Could not load the projects. In dev, point VITE_BASE_URL at a Polarion and set ' +
              'VITE_BEARER_TOKEN in ui/.env.development.local, then restart the dev server.',
          );
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  // Remembered so a later bare `?` - no scope parameter - comes back to the same project.
  useEffect(() => {
    setCookie(DEV_SCOPE_COOKIE, scope);
  }, [scope]);

  const scopeOptions = [
    { id: '', name: 'Repository (global scope)' },
    ...projects.map((project) => ({
      id: `project/${project.id}/`,
      name: project.name ? `${project.name} (${project.id})` : project.id,
    })),
  ];

  /** No `projectId` parameter: every page here derives the project from the scope. */
  const linkFor = (featureId: string): string => {
    const params = new URLSearchParams({ feature: featureId });
    if (scope) {
      params.set('scope', scope);
    }
    return `?${params.toString()}`;
  };

  return (
    <PageLayout title="Diff Tool Administration">
      <div className="landing-scope">
        <label htmlFor="dev-scope">Project scope:</label>
        <SearchableSelect id="dev-scope" value={scope} onChange={setScope} options={scopeOptions} placeholder="" />
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      <p>Pick an admin page:</p>
      <ul className="landing-features">
        {FEATURES.map((feature) => (
          <li key={feature.id}>
            {/* No `embedded` parameter on purpose: dev navigation is not embedded, so the pages show
                their "Overview" back link, which returns here with the scope in the URL. */}
            <a href={linkFor(feature.id)}>{feature.label}</a>
          </li>
        ))}
      </ul>
    </PageLayout>
  );
}
