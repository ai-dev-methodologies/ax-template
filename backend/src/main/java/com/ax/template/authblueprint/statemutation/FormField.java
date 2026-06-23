package com.ax.template.authblueprint.statemutation;

/**
 * state-conditional-mutability-l0 editable fields (STATEMUTATION-AUTHORITY-001). The per-(state,
 * field) mutable-set in {@link StateFieldPolicy} is keyed on these. TITLE and BODY are the form
 * content frozen on submission; REVIEWER_NOTE stays editable in SUBMITTED so a reviewer can annotate
 * a frozen submission. A bare field name string is never trusted — the controller binds to this enum.
 */
public enum FormField {
    TITLE,
    BODY,
    REVIEWER_NOTE
}
