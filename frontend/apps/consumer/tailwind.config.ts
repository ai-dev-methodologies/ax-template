import type { Config } from 'tailwindcss';
import rootConfig from '../../tailwind.config';

// Reuse the SHARED theme (darkMode, colors mapped to the same --background /
// --foreground / --ax-status-* CSS variables, borderRadius keyed to --radius,
// keyframes, the tailwindcss-animate plugin). Two app-specific deltas:
//   - `content` globs: this app's src plus the shared catalog packages so their
//     utility classes are never purged.
//   - a `font-display` family (Quicksand via --font-display) for the playful
//     consumer-delight wordmark. The shared config only ships sans + mono.
const config: Config = {
  ...rootConfig,
  content: [
    './src/**/*.{ts,tsx}',
    '../../packages/**/*.{ts,tsx}',
  ],
  theme: {
    ...rootConfig.theme,
    extend: {
      ...rootConfig.theme?.extend,
      fontFamily: {
        ...rootConfig.theme?.extend?.fontFamily,
        display: ['var(--font-display)', 'var(--font-sans)', 'ui-sans-serif', 'sans-serif'],
      },
    },
  },
};

export default config;
