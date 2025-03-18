export default (phase, { defaultConfig }) => {
  if (phase === "phase-development-server") {
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
      ],

    }
  }

  const nextConfig = {
    output: 'export',
    basePath: '/polarion/diff-tool-app/ui/app',
    distDir: '../src/main/resources/webapp/diff-tool-app/app'
  };

  if (process.env.CYPRESS === 'true' && process.env.NEXT_PUBLIC_COVERAGE === 'true') {
    nextConfig.webpack = (config, { dev, isServer }) => {
      // Only add instrumentation in non-production environments
      if (!isServer) {
        console.log('Enabling code coverage for Cypress...');
        config.module.rules.push({
          test: /\.(js|jsx|ts|tsx)$/,
          exclude: [
            /node_modules/,
            /\.next\//,
            /cypress\//,
            /.spec./,
            /.test./,
          ],
          use: {
            loader: 'babel-loader',
            options: {
              presets: ['next/babel'],
              plugins: ['istanbul']
            }
          }
        });
      }

      // If you have existing webpack config, merge it here

      return config;
    };
  }

  return nextConfig;
}
