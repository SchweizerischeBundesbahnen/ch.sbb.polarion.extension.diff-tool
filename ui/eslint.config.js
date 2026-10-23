import js from '@eslint/js';
import { polarionEslintConfig } from '@sbb-polarion/react-sbb-polarion/eslint-config';
import reactHooks from 'eslint-plugin-react-hooks';
import globals from 'globals';

// The shared setup of the SBB Polarion React apps (TypeScript, React hooks, jsx-a11y, Prettier), plus
// what is this extension's own: the diff/merge viewer, which is still plain JS/JSX (see the coverage note
// in vitest.config.ts) and which the shared TypeScript block never sees, the E2E specs and the tooling.
export default polarionEslintConfig({
  appFiles: ['src/**/*.{js,jsx,ts,tsx}'],
  ignores: [
    'node',
    '.vite',
    'test/expected',
    'test/__diff__',
    'test/__screenshots__',
    '.vitest',
    'playwright-report',
    'test-results',
  ],
  configs: [
    // The diff/merge viewer plus the shells and page entries: browser JS/JSX, React hook rules apply.
    {
      files: ['src/**/*.{js,jsx}'],
      extends: [js.configs.recommended],
      plugins: { 'react-hooks': reactHooks },
      languageOptions: {
        ecmaVersion: 2022,
        sourceType: 'module',
        globals: { ...globals.browser },
        parserOptions: { ecmaFeatures: { jsx: true } },
      },
      rules: {
        'react-hooks/rules-of-hooks': 'error',
        'react-hooks/exhaustive-deps': 'warn',
      },
    },
    // Playwright end-to-end specs.
    {
      files: ['e2e/**/*.js'],
      extends: [js.configs.recommended],
      languageOptions: {
        ecmaVersion: 2022,
        sourceType: 'module',
        globals: { ...globals.browser, ...globals.node },
      },
      rules: {
        // The expected-HTML assertions are large blocks of markup pasted into template literals, where
        // the carried-over \" escapes are redundant but harmless (inside backticks \" and " are the same
        // character). That is fixture data, not code: the rule has no fixer, and hand-stripping ~700
        // backslashes inside assertions risks silently changing what is asserted.
        'no-useless-escape': 'off',
      },
    },
    // Plain JS/ESM tooling (this config, vite.config.js, the Node scripts in scripts/).
    {
      files: ['*.{js,mjs}', 'scripts/**/*.{js,mjs}'],
      extends: [js.configs.recommended],
      languageOptions: {
        ecmaVersion: 2022,
        sourceType: 'module',
        globals: { ...globals.node },
      },
    },
  ],
});
