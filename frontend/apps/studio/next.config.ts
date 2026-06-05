import type { NextConfig } from 'next';

const nextConfig: NextConfig = {
  // Hide the Next.js dev-mode build indicator (bottom-left badge) — a dev-only
  // artifact, never present in production; removing the visual noise.
  devIndicators: false,
  // Allow the home-mac Tailscale IP to load the dev server + HMR over the tailnet.
  allowedDevOrigins: ['100.112.5.105'],
  // The shared catalog packages ship raw TS source (package.json "exports" point
  // at .ts/.tsx), so Next must transpile them.
  transpilePackages: ['@ax/ui', '@ax/blocks', '@ax/core'],
  typescript: {
    ignoreBuildErrors: false,
  },
  eslint: {
    ignoreDuringBuilds: false,
  },
  async rewrites() {
    return [
      {
        source: '/api/:path*',
        destination: 'http://localhost:8080/api/:path*',
      },
    ];
  },
};

export default nextConfig;
