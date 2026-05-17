---
evidence:
  - source_type: external
    citation: "React 19 documentation — Forms. react.dev."
    url: "https://react.dev/reference/react-dom/components/form"
---

// LoginForm.tsx — L2 feature block
// evidence: anchored to React 19 docs (frontmatter above)
import React from 'react';

export function LoginForm() {
  return <form aria-label="Login form"><input type="email" /><input type="password" /><button type="submit">Login</button></form>;
}
