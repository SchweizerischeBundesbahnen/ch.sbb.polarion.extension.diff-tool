import { PageLayout, getScope } from '@grigoriev/react-sbb-polarion';
import { FEATURES } from '../../features';

/**
 * Development-only overview, shown when no (or an unknown) `?feature=` is given. Polarion always opens a
 * page with an explicit feature id and `embedded=true`, so this is never reached there - it just makes
 * `vite dev` navigable. Excluded from the coverage gate as dev scaffolding.
 */
export default function Landing() {
  const scope = getScope();
  const scopeParam = scope ? `&scope=${encodeURIComponent(scope)}` : '';

  return (
    <PageLayout title="Diff Tool Administration">
      <p>Pick an admin page:</p>
      <ul className="landing-features">
        {FEATURES.map((feature) => (
          <li key={feature.id}>
            <a href={`?feature=${feature.id}${scopeParam}`}>{feature.label}</a>
          </li>
        ))}
      </ul>
    </PageLayout>
  );
}
