export default (phase, { defaultConfig }) => {
  if (phase === "phase-development-server") {
    return {
      /* development only config options here */
    }
  }

  return {
    output: 'export',
    basePath: '/polarion/diff-tool-app/ui/app',
    distDir: '../src/main/resources/webapp/diff-tool-app/app'
  }
}