import type { Config } from 'tailwindcss';
import rootConfig from '../../tailwind.config';

// Reuse the SHARED theme verbatim (darkMode: ['class'], colors mapped to the
// same --background / --foreground / --ax-status-* CSS variables, borderRadius
// keyed to --radius, the font-sans / font-mono families already wired to
// --font-sans / --font-mono, keyframes, the tailwindcss-animate plugin). The
// only app-specific delta is the `content` globs: this app's src plus the shared
// catalog packages so their utility classes are never purged.
//
// The developer-tool persona supplies --font-sans (a grotesk) and --font-mono (a
// code monospace) through next/font in layout.tsx; the dense 14px base + dark
// shell live in globals.css under .ax-devtool. No fontFamily override is needed
// here because the shared config already keys both families to those variables.
const config: Config = {
  ...rootConfig,
  content: [
    './src/**/*.{ts,tsx}',
    '../../packages/**/*.{ts,tsx}',
  ],
};

export default config;
