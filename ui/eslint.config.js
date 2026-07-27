import js from '@eslint/js';
import prettier from 'eslint-config-prettier';
import reactHooks from 'eslint-plugin-react-hooks';
import globals from 'globals';
import tseslint from 'typescript-eslint';

// Mirrors react-sbb-polarion's eslint.config.js so the React apps lint identically, plus a block for
// the diff/merge viewer, which is still plain JS/JSX (see the coverage note in vitest.config.ts): the
// typescript-eslint rule sets are scoped to **/*.{ts,tsx} and never see it.
export default tseslint.config(
  {
    ignores: [
      'dist',
      'node',
      'node_modules',
      'coverage',
      '.vite',
      'test/expected',
      'test/__diff__',
      'test/__screenshots__',
      '.vitest-attachments',
      'playwright-report',
      'test-results',
    ],
  },
  // TypeScript + React sources.
  {
    files: ['**/*.{ts,tsx}'],
    extends: [js.configs.recommended, ...tseslint.configs.recommended],
    plugins: { 'react-hooks': reactHooks },
    languageOptions: {
      ecmaVersion: 2022,
      globals: { ...globals.browser, ...globals.node },
    },
    rules: {
      // eslint-plugin-react-hooks recommended set (declared explicitly - the plugin's shipped flat
      // config uses a legacy string-array `plugins` key that ESLint 10 rejects when spread directly).
      'react-hooks/rules-of-hooks': 'error',
      'react-hooks/exhaustive-deps': 'warn',
    },
  },
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
  // Plain JS/ESM tooling (this config, vite.config.js, the docker-test wrapper).
  {
    files: ['*.{js,mjs}', 'scripts/**/*.{js,mjs}'],
    extends: [js.configs.recommended],
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: 'module',
      globals: { ...globals.node },
    },
  },
  // Keep last: turns off ESLint rules that would conflict with Prettier's formatting.
  prettier,
);
