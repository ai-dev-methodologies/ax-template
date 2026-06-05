import type { Config } from 'tailwindcss';
import rootConfig from '../../tailwind.config';

// Reuse the SHARED theme verbatim (darkMode: ['class'], colors mapped to the
// same --background / --foreground / --ax-status-* CSS variables, borderRadius
// keyed to --radius, the font-sans / font-mono families already wired to
// --font-sans / --font-mono, keyframes, the tailwindcss-animate plugin). The
// only app-specific delta is the `content` globs: this app's src plus the shared
// catalog packages so their utility classes are never purged.
//
// The playful-creator persona supplies --font-display + --font-sans (a chunky
// rounded display + a friendly rounded body) through next/font in layout.tsx;
// the vibrant studio shell + 16px base + layered colorful elevation live in
// globals.css under .ax-studio. No fontFamily override is needed here because the
// shared config already keys font-sans to --font-sans; the display face is used
// through the .ax-display utility (font-family: var(--font-display)).
const config: Config = {
  ...rootConfig,
  content: [
    './src/**/*.{ts,tsx}',
    '../../packages/**/*.{ts,tsx}',
  ],
};

export default config;
