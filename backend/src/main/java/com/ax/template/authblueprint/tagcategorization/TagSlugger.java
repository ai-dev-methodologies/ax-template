package com.ax.template.authblueprint.tagcategorization;

import java.text.Normalizer;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * URL-safe slug generator.
 *
 * <p>Trace: TAG-CRUD-001. Strategy:
 * <ol>
 *   <li>Unicode-NFKD normalize, strip combining marks (handles accented Latin)</li>
 *   <li>Lowercase</li>
 *   <li>Replace any non-{@code [a-z0-9]} run with a single hyphen</li>
 *   <li>Trim leading / trailing hyphens</li>
 *   <li>Truncate to 64 chars</li>
 *   <li>If the result is empty (e.g. input was pure Korean / CJK), fall back to
 *       {@code "tag-" + 8-char UUID prefix} so the row still has a unique slug</li>
 * </ol>
 *
 * <p>The Korean fallback is intentional — a full Korean→romanization library would
 * pull a heavy dependency for a small percentage of use cases. Fork-receivers needing
 * proper romanization (e.g. 'kpop' from '케이팝') can swap this utility.
 */
public final class TagSlugger {

    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");
    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final int MAX_LENGTH = 64;

    private TagSlugger() {}

    public static String slugify(String name) {
        if (name == null) {
            return fallback();
        }
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFKD);
        String stripped = COMBINING_MARKS.matcher(normalized).replaceAll("");
        String lower = stripped.toLowerCase();
        String hyphenated = NON_ALNUM.matcher(lower).replaceAll("-");
        String trimmed = trimHyphens(hyphenated);
        if (trimmed.isEmpty()) {
            return fallback();
        }
        return trimmed.length() <= MAX_LENGTH ? trimmed : trimmed.substring(0, MAX_LENGTH);
    }

    private static String fallback() {
        return "tag-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String trimHyphens(String s) {
        int start = 0;
        int end = s.length();
        while (start < end && s.charAt(start) == '-') start++;
        while (end > start && s.charAt(end - 1) == '-') end--;
        return s.substring(start, end);
    }
}
