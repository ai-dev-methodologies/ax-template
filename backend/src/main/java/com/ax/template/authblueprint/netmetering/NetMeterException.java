package com.ax.template.authblueprint.netmetering;

import org.springframework.http.HttpStatus;

/**
 * Domain exception for signed-dual-register net metering. status + RFC 9457 type + machine-readable code.
 */
public class NetMeterException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private NetMeterException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static NetMeterException notFound() {
        return new NetMeterException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Net meter not found");
    }

    public static NetMeterException duplicateMeter() {
        return new NetMeterException(HttpStatus.CONFLICT,
            "urn:problem:netmeter-duplicate", "NETMETER_DUPLICATE",
            "A net meter with this meter key already exists");
    }

    /** NETM-DIRECTION-001 — a reading below its direction's cumulative (each direction is value-monotone). */
    public static NetMeterException notMonotone() {
        return new NetMeterException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:netmeter-not-monotone", "NETMETER_NOT_MONOTONE",
            "A reading must be >= its direction's current cumulative; each direction register is value-monotone");
    }

    /** NETM-DIRECTION-001 — a reading value outside the valid non-negative range. */
    public static NetMeterException invalidReading() {
        return new NetMeterException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:netmeter-invalid-reading", "NETMETER_INVALID_READING",
            "Reading value must be non-negative");
    }

    /** NETM-NET-001 — the recorded derived net diverged from the independent chain recompute (a defect). */
    public static NetMeterException netMismatch() {
        return new NetMeterException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:netmeter-net-mismatch", "NETMETER_NET_MISMATCH",
            "The derived net diverged from the independent Σimport − Σexport recompute");
    }

    /** NETM-PERIOD-001 — a reading backdated into a closed period, or a re-close at/before the latest boundary. */
    public static NetMeterException periodClosed() {
        return new NetMeterException(HttpStatus.CONFLICT,
            "urn:problem:netmeter-period-closed", "NETMETER_PERIOD_CLOSED",
            "A closed billing period is immutable; the boundary must move strictly forward and a reading "
                + "cannot be backdated at or before the latest closed boundary");
    }
}
