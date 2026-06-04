import { describe, it, expect } from "vitest";
import { readFileSync } from "fs";
import { join } from "path";

// Durable WCAG 1.4.3 gate for the showcase design tokens. axe-core at component level cannot resolve
// `bg-[var(--ax-status-*-bg)]` arbitrary Tailwind values to RGB, so token contrast slips past every
// other gate (eslint/tsc/next-build/axe). This test computes the real fg-on-bg ratio for the LIGHT
// theme from globals.css and fails if any pill / muted text pair drops below 4.5:1 (normal 12px text).
// The audit found warning at 3.16:1; this locks the fix.

const MIN = 4.5;

// --- OKLCH -> linear sRGB (Bjorn Ottosson) ---
function oklchToLinSrgb(L: number, C: number, Hdeg: number): [number, number, number] {
  const h = (Hdeg * Math.PI) / 180;
  const a = C * Math.cos(h);
  const b = C * Math.sin(h);
  const l_ = L + 0.3963377774 * a + 0.2158037573 * b;
  const m_ = L - 0.1055613458 * a - 0.0638541728 * b;
  const s_ = L - 0.0894841775 * a - 1.291485548 * b;
  const l = l_ ** 3;
  const m = m_ ** 3;
  const s = s_ ** 3;
  const clamp = (v: number) => Math.min(1, Math.max(0, v));
  return [
    clamp(4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s),
    clamp(-1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s),
    clamp(-0.0041960863 * l - 0.7034186147 * m + 1.707614701 * s),
  ];
}

// --- HSL -> linear sRGB ---
function hslToLinSrgb(H: number, S: number, Lp: number): [number, number, number] {
  const s = S / 100;
  const l = Lp / 100;
  const f = (n: number) => {
    const k = (n + H / 30) % 12;
    return l - s * Math.min(l, 1 - l) * Math.max(-1, Math.min(k - 3, 9 - k, 1));
  };
  const lin = (c: number) => (c <= 0.04045 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4);
  return [lin(f(0)), lin(f(8)), lin(f(4))];
}

const lum = ([r, g, b]: [number, number, number]) => 0.2126 * r + 0.7152 * g + 0.0722 * b;
const contrast = (ya: number, yb: number) => {
  const hi = Math.max(ya, yb);
  const lo = Math.min(ya, yb);
  return (hi + 0.05) / (lo + 0.05);
};

const css = readFileSync(join(process.cwd(), "src/app/globals.css"), "utf8");

// Slice each theme block independently: LIGHT is the first `:root {…}`, DARK is `.dark {…}`. Both are
// tested — the developer-tool persona renders in dark mode, so a dark-token regression must also fail.
function blockAfter(marker: string): string {
  const start = css.indexOf(marker);
  if (start < 0) throw new Error(`block ${marker} not found in globals.css`);
  return css.slice(start, css.indexOf("}", start));
}
const THEMES: Array<{ name: string; css: string }> = [
  { name: "LIGHT", css: blockAfter(":root") },
  { name: "DARK", css: blockAfter(".dark") },
];

function oklchVar(block: string, name: string): [number, number, number] {
  const m = block.match(new RegExp(`--${name}:\\s*oklch\\(([\\d.]+)%\\s+([\\d.]+)\\s+([\\d.]+)\\)`));
  if (!m) throw new Error(`token --${name} not found / not oklch`);
  return oklchToLinSrgb(Number(m[1]) / 100, Number(m[2]), Number(m[3]));
}
function hslVar(block: string, name: string): [number, number, number] {
  const m = block.match(new RegExp(`--${name}:\\s*([\\d.]+)\\s+([\\d.]+)%\\s+([\\d.]+)%`));
  if (!m) throw new Error(`token --${name} not found / not hsl`);
  return hslToLinSrgb(Number(m[1]), Number(m[2]), Number(m[3]));
}

const STATUSES = ["success", "danger", "warning", "info", "attention", "neutral", "accent"];

for (const theme of THEMES) {
  describe(`globals.css ${theme.name} tokens meet WCAG 1.4.3 (4.5:1) for 12px pill/muted text`, () => {
    for (const s of STATUSES) {
      it(`--ax-status-${s}-fg on -bg >= ${MIN}:1`, () => {
        const ratio = contrast(lum(oklchVar(theme.css, `ax-status-${s}-fg`)), lum(oklchVar(theme.css, `ax-status-${s}-bg`)));
        expect(ratio).toBeGreaterThanOrEqual(MIN);
      });
    }

    it(`--muted-foreground on --muted >= ${MIN}:1 (data-grid neutral pill + avatar initials/+N)`, () => {
      const ratio = contrast(lum(hslVar(theme.css, "muted-foreground")), lum(hslVar(theme.css, "muted")));
      expect(ratio).toBeGreaterThanOrEqual(MIN);
    });
  });
}

describe("contrast math sanity", () => {
  it("#767676 on white computes to the canonical 4.54:1", () => {
    const g = 0x76 / 255;
    const glin = g <= 0.04045 ? g / 12.92 : ((g + 0.055) / 1.055) ** 2.4;
    expect(contrast(1, lum([glin, glin, glin]))).toBeCloseTo(4.54, 1);
  });
});
