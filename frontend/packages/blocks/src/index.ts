// @ax/blocks — codified 21st.dev composed blocks, shared across per-persona apps.
//
// Each block is also reachable by subpath (e.g. `@ax/blocks/form-field`) via the
// package "exports" map; this barrel gives a single named-export surface. The
// interfaces-card primitives are re-exported under an `Interfaces*` prefix so they
// do not collide with the @ax/ui Card family.

export { default as StatusBadge } from './status-badge';
export { Button04 } from './animated-arrow-button';
export { Component as CategoryBarChart } from './category-bar-chart';
export { PrimeButton } from './prime-button';
export { AnimatedBadge } from './animated-badge';
export { ImageSwiper } from './image-swiper';
export { AnimatedFeatureCard } from './animated-feature-card';
export { default as SocialButton } from './social-button';
export { default as AutoLayoutCard } from './auto-layout-card';
export { Component as SplitText } from './split-text-effect';
export { ImageCarouselHero } from './ai-image-generator-hero';
export { default as CyberneticGridShader } from './cybernetic-grid-shader';
export { AuroraHero } from './futurastic-hero-section';
export {
  Card as InterfacesCard,
  CardHeader as InterfacesCardHeader,
  CardTitle as InterfacesCardTitle,
  CardDescription as InterfacesCardDescription,
  CardContent as InterfacesCardContent,
} from './interfaces-card';
export { default as TextField } from './form-field';
export { default as DataGrid, GridStatus } from './data-grid';
export { default as AvatarGroup } from './avatar-group';
export { default as CodeSnippet } from './code-snippet';
