package com.ax.template.authblueprint.softdelete;

/**
 * A soft-delete lifecycle conflict mapped to 409 by {@link SoftDeleteAdvice}, carrying a stable
 * machine {@code code}:
 * <ul>
 *   <li>{@code SOFT_DELETE_UNIQUE_CONFLICT} — a new live row duplicates a live natural key (UNIQUE-001);</li>
 *   <li>{@code SOFT_DELETE_WINDOW_EXPIRED} — restore past the recovery window (RESTORE-001);</li>
 *   <li>{@code SOFT_DELETE_NOT_DELETED} — restore of a row that is not tombstoned (RESTORE-001).</li>
 * </ul>
 */
public class SoftDeleteConflictException extends RuntimeException {

    public static final String UNIQUE_CONFLICT = "SOFT_DELETE_UNIQUE_CONFLICT";
    public static final String WINDOW_EXPIRED = "SOFT_DELETE_WINDOW_EXPIRED";
    public static final String NOT_DELETED = "SOFT_DELETE_NOT_DELETED";

    private final String code;

    public SoftDeleteConflictException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
