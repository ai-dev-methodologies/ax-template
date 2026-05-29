package com.ax.template.authblueprint.common;

/**
 * Cross-cutting 400 signal raised by {@link OffsetPageSupport#clamp(int, int, int)}
 * when an offset-pagination request is out of range (PAGE-LIMIT-001:
 * {@code page < 0}, {@code size} outside {@code [1, maxSize]}, or a {@code maxSize}
 * above the absolute ceiling) — see specs/pagination-l0.yaml.
 *
 * <h2>Why a TYPED exception, not {@code IllegalArgumentException}</h2>
 * {@code clamp} originally threw {@link IllegalArgumentException}. Under the reference
 * {@code SecurityConfig} an unmapped exception falls through to {@code /error}, which
 * re-enters the filter chain and is caught by {@code anyRequest().denyAll()} — so an
 * out-of-range page size surfaced as a misleading {@code 403}, exactly the trap IDW3
 * personas hit. A narrow, typed exception lets {@link GlobalProblemDetailAdvice}
 * register an {@code @ExceptionHandler} that maps ONLY this signal to
 * {@code 400 application/problem+json} (code {@code PAGE_SIZE_INVALID}) without a broad
 * {@code IllegalArgumentException → 400} handler that would mask genuine programming
 * bugs (e.g. an internal {@code IllegalArgumentException} from misuse of an API).
 *
 * <p>Framework-light: a plain unchecked exception with no Spring annotation. The
 * {@code 400} mapping lives in {@link GlobalProblemDetailAdvice} (the
 * {@code LOWEST_PRECEDENCE} fallback), so a domain-local handler can still override it.
 */
public class InvalidPageRequestException extends RuntimeException {

    public InvalidPageRequestException(String message) {
        super(message);
    }
}
