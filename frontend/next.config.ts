import path from 'path';
import type { NextConfig } from 'next';

const nextConfig: NextConfig = {
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
  webpack(config) {
    // Allow importing ax-template templates from outside the Next.js src root.
    // Used by L4 demo pages (editor/page.tsx) that reference L1/L2 templates directly.
    // process.cwd() = frontend/ when `npm run build` is executed from frontend/.
    const templatesDir = path.resolve(process.cwd(), '../templates');

    config.resolve.alias = {
      ...(typeof config.resolve.alias === 'object' && !Array.isArray(config.resolve.alias)
        ? config.resolve.alias
        : {}),
      '@templates': templatesDir,
    };

    // When webpack processes files in ../templates/, node_modules resolution
    // climbs the directory tree and misses frontend/node_modules (a sibling, not ancestor).
    // Adding it explicitly makes @tiptap/*, clsx, tailwind-merge, etc. resolvable.
    config.resolve.modules = [
      ...(Array.isArray(config.resolve.modules) ? config.resolve.modules : ['node_modules']),
      path.resolve(process.cwd(), 'node_modules'),
    ];

    return config;
  },
};

export default nextConfig;
