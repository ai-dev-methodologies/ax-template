package com.ax.template.authblueprint.queryguard;

import org.springframework.http.HttpStatus;

/**
 * Domain exception for query-field-allowlist. status + RFC 9457 type + machine-readable code.
 * Every rejection NAMES the offending field/direction/operator in the message so the 422 detail
 * is actionable (QUERY-ALLOWLIST-SORT/FILTER/KEYSTONE-001) — never a silent ignore or pass-through.
 */
public class QueryGuardException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private QueryGuardException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    /** QUERY-ALLOWLIST-SORT-001 — the sort field is not in the resource's sortable allowlist. */
    public static QueryGuardException notSortable(String field, java.util.Collection<String> allowed) {
        return new QueryGuardException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:query-field-not-sortable", "QUERY_FIELD_NOT_SORTABLE",
            "Field '" + field + "' is not sortable on this resource; sortable fields: " + allowed);
    }

    /** QUERY-ALLOWLIST-SORT-001 — the sort direction is outside the closed set {asc, desc}. */
    public static QueryGuardException directionInvalid(String direction) {
        return new QueryGuardException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:query-direction-invalid", "QUERY_DIRECTION_INVALID",
            "Sort direction '" + direction + "' is invalid; allowed directions: [asc, desc]");
    }

    /** QUERY-ALLOWLIST-FILTER-001 — the filter field is not in the resource's filterable allowlist. */
    public static QueryGuardException notFilterable(String field, java.util.Collection<String> allowed) {
        return new QueryGuardException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:query-field-not-filterable", "QUERY_FIELD_NOT_FILTERABLE",
            "Field '" + field + "' is not filterable on this resource; filterable fields: " + allowed);
    }

    /** QUERY-ALLOWLIST-FILTER-001 — the filter operator is outside the closed safe set. */
    public static QueryGuardException operatorInvalid(String operator) {
        return new QueryGuardException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:query-operator-invalid", "QUERY_OPERATOR_INVALID",
            "Operator '" + operator + "' is invalid; allowed operators: [eq, ne, gt, gte, lt, lte, like]");
    }

    /** QUERY-ALLOWLIST-FILTER-001 — a malformed filter expression (not field:op:value). */
    public static QueryGuardException filterMalformed(String expr) {
        return new QueryGuardException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:query-filter-malformed", "QUERY_FILTER_MALFORMED",
            "Filter '" + expr + "' is malformed; expected field:operator:value");
    }

    /** QUERY-ALLOWLIST-FILTER-001 — the filter value does not coerce to the field's type. */
    public static QueryGuardException valueInvalid(String value) {
        return new QueryGuardException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:query-value-invalid", "QUERY_VALUE_INVALID",
            "Filter value '" + value + "' is invalid for the target field's type");
    }
}
