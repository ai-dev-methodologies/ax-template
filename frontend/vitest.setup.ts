import '@testing-library/jest-dom/vitest';
import { afterEach } from 'vitest';
import { cleanup } from '@testing-library/react';

// React Testing Library does NOT auto-unmount between tests unless vitest's
// `test.globals` is true. Without it, rendered trees accumulate in the jsdom
// document and a second render of the same component throws
// `Found multiple elements...`. FDW1 (frontend dogfood) hit this in all 3
// persona test suites — each re-added `afterEach(cleanup)` locally. Register it
// once here so every fork's component test gets isolation for free. FMW1.
afterEach(() => {
  cleanup();
});
