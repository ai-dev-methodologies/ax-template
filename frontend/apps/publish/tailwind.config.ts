import type { Config } from 'tailwindcss';
import rootConfig from '../../tailwind.config';

// Reuse the SHARED theme (darkMode, colors mapped to the same --background /
// --foreground / --ax-status-* CSS variables, borderRadius keyed to --radius,
// keyframes, the tailwindcss-animate plugin). Two app-specific deltas:
//   - `content` globs: this app's src plus the shared catalog packages so their
//     utility classes are never purged.
//   - a `font-display` family (a high-contrast SERIF via --font-display) for the
//     editorial-luxury masthead + headlines. The shared config ships sans + mono
//     only; this persona's identity is the serif display at extreme scale.
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
        display: ['var(--font-display)', 'ui-serif', 'Georgia', 'serif'],
      },
    },
  },
};

export default config;
