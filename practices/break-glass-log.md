# Break-Glass Log (P3 §Axis 3)

Per `practices/DECISIONS-P3.md` §Break-glass procedure: every `[break-glass]`-prefixed PR
MUST add an entry here in the same commit. Override without this artifact is a Methodology
violation.

Schema per entry:

```
## YYYY-MM-DD — <one-line reason>
- PR: #NNN
- Maintainer: <name>
- Guard(s) bypassed: <spec_ref | substance | time_decay | evidence>
- Reason: <concrete failure mode the bypass averts>
- Planned remediation: <what gets fixed in the follow-up PR>
- Follow-up PR target date: YYYY-MM-DD (≤ 14 days from this entry)
- Resolution date: <filled when the follow-up PR lands>
```

---

(no entries yet)
