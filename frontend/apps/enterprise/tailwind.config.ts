import type { Config } from 'tailwindcss';
import rootConfig from '../../tailwind.config';

// Reuse the SHARED theme (darkMode, colors mapped to the same --background /
// --foreground / --ax-status-* CSS variables, borderRadius keyed to --radius,
// font families, keyframes, the tailwindcss-animate plugin). Only the `content`
// globs are app-specific: this app's src plus the shared catalog packages so
// their utility classes are never purged.
const config: Config = {
  ...rootConfig,
  content: [
    './src/**/*.{ts,tsx}',
    '../../packages/**/*.{ts,tsx}',
  ],
};

export default config;
