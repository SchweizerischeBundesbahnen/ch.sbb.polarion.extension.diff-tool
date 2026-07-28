// Runs the test suite inside the pinned Playwright Docker image (Linux) so screenshots match the
// committed references - wrapping the long `docker run ...` command behind an npm script. Used by
// test:docker / test:update:docker / test:coverage:docker.
import { spawnSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const uiDir = resolve(dirname(fileURLToPath(import.meta.url)), '..');

// Which inner npm script to run in the container (default: test).
const script = process.argv[2] || 'test';

// Anything after the script name is forwarded to the inner npm run, so flags reach vitest inside the
// container - e.g. `-- --exclude **/*.visual.test.tsx`, which the Maven -DskipVisualJsTests profile
// appends. Single-quoted so globs are passed through literally instead of being expanded by the
// container's shell against /work.
const forwarded = process.argv
  .slice(3)
  .map((argument) => `'${argument.replaceAll("'", `'\\''`)}'`)
  .join(' ');

// Pin the image to the installed Playwright version so the container's browser + system deps match.
let playwrightVersion;
try {
  const pkg = JSON.parse(readFileSync(resolve(uiDir, 'node_modules/playwright/package.json'), 'utf8'));
  playwrightVersion = pkg.version;
} catch {
  console.error('Cannot read node_modules/playwright - run `npm install` first.');
  process.exit(1);
}
const image = `mcr.microsoft.com/playwright:v${playwrightVersion}-jammy`;

const args = [
  'run',
  '--rm',
  // Marks this run as THE reference environment for the pixel comparisons. The committed screenshots
  // are locked to this image, so the visual tests assert only when this is set (see vitest.config.ts);
  // anywhere else they skip instead of failing on the host's font metrics.
  '-e',
  'PIXEL_REFERENCES=1',
  '-v',
  `${uiDir}:/work`,
  // Shadow node_modules so the container's Linux install does not overwrite host binaries.
  '-v',
  '/work/node_modules',
  '-w',
  '/work',
  image,
  'bash',
  '-c',
  // `--` so npm forwards the flags to the script (vitest) instead of consuming them itself.
  `npm ci && npm run ${script}${forwarded ? ` -- ${forwarded}` : ''}`,
];

console.log(`> docker ${args.join(' ')}`);
const result = spawnSync('docker', args, { stdio: 'inherit' });
if (result.error) {
  console.error(`Failed to launch docker: ${result.error.message}. Is Docker installed and running?`);
  process.exit(1);
}
process.exit(result.status ?? 1);
