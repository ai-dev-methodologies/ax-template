/**
 * @ax/eslint-plugin-ax
 *
 * Custom ESLint plugin pairing with the practices-react/ catalog.
 * Each rule name matches the corresponding practices-react/rules/<id>.md file.
 */

import reactAsyncParallel from "./rules/react-async-parallel.js";

const plugin = {
  meta: {
    name: "@ax/eslint-plugin-ax",
    version: "0.0.1",
  },
  rules: {
    "react-async-parallel": reactAsyncParallel,
  },
  configs: {},
};

// ESLint 9 flat-config recommended preset
plugin.configs.recommended = {
  plugins: { ax: plugin },
  rules: {
    "ax/react-async-parallel": "warn",
  },
};

export default plugin;
