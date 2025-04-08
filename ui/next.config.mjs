export default (phase, { defaultConfig }) => {
  if (phase === "phase-development-server") {
    if (process.env.PLAYWRIGHT_TESTS === 'true') {
      return {};
    } else {
      return {
        /* development only config options here */
        /* proxy to Polarion resources */
        rewrites: async () => [
          {
            source: '/polarion/icons/:path*',
            destination: `${process.env.NEXT_PUBLIC_BASE_URL}/polarion/icons/:path*`,
          },
          {
            source: '/polarion/ria/:path*',
            destination: `${process.env.NEXT_PUBLIC_BASE_URL}/polarion/ria/:path*`,
          },
          {
            source: '/polarion/wiki/:path*',
            destination: `${process.env.NEXT_PUBLIC_BASE_URL}/polarion/wiki/:path*`,
          },
        ]
      }
    }
  }

  return {
    output: 'export',
    basePath: '/polarion/diff-tool-app/ui/app',
    distDir: '../src/main/resources/webapp/diff-tool-app/app'
  }
}
