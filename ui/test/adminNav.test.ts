import { describe, expect, it } from 'vitest';
import { nodeForFeature } from '../src/services/adminNav';

// This extension's feature->admin-node mapping. The generic node-switching logic (retargetNodeHash,
// pendingTarget) and the DOM wrappers live in @sbb-polarion/react-sbb-polarion and are tested there.

describe('nodeForFeature', () => {
  it('maps every documentation article to the single documentation node', () => {
    for (const id of ['quick-start', 'user-guide', 'configuration', 'velocity-api']) {
      expect(nodeForFeature(id)).toBe('documentation');
    }
  });

  it('keeps About as its own node', () => {
    expect(nodeForFeature('about')).toBe('about');
  });

  it('returns null for features no admin node opens', () => {
    expect(nodeForFeature('diff-configurations')).toBeNull();
    expect(nodeForFeature('merge-authorization')).toBeNull();
  });
});
