---
title: Sample rule for react-substance fixture (clause 1 violation)
impact: LOW
impactDescription: ""
verification:
  type: review
  status: manual
  notes: "Reviewer checks that the sample pattern below is followed exactly as written."
---

## Sample rule for react-substance fixture (clause 1 violation)

### Correct

```tsx
function Example() {
  const value = 1
  return <div>{value}</div>
}
```

Reference: https://react.dev/learn
