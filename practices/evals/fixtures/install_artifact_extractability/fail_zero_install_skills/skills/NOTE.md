# fail_zero_install_skills (fixture)

This tree has a `skills/` directory and NO `skills/ax-install-*` directory in it.
That is the whole fixture — there is deliberately no SKILL.md to lint.

Before P2-109 the guard printed "no skills/ax-install-* directories found --
nothing to check" and exited 0 on exactly this shape, which meant DELETING every
install skill in the repo satisfied the guard. A census-based check whose census
is allowed to be empty measures nothing; the empty census IS the failure. The
guard now reports `NO_INSTALL_SKILLS` and exits 1.

This file exists only so the `skills/` directory itself is a tracked, non-empty
path. Its name deliberately does not match the `ax-install-*` glob, and it is not
a SKILL.md, so it contributes no markers.
