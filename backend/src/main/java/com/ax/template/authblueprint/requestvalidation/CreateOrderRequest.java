package com.ax.template.authblueprint.requestvalidation;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * request-validation-l0 reference command — exercises every validation facet declaratively:
 *
 * <ul>
 *   <li>VALIDATION-SCHEMA-001 — standard constraints on every field, triggered by {@code @Valid};</li>
 *   <li>VALIDATION-TYPE-001 — {@code amount} uses {@link StrictNumericDeserializer} (no
 *       string→number coercion); {@code priority} is a strict enum (an unlisted token is
 *       rejected); unknown/typo'd fields are collected by the {@link JsonAnySetter any-setter}
 *       and rejected DECLARATIVELY by {@link #isNoUnknownFields()} — surfaced through the SAME
 *       {@code errors[]} array (code {@code AssertTrue}), with NO global Jackson change;</li>
 *   <li>VALIDATION-CONSTRAINT-001 — built-in constraints + the class-level cross-field
 *       {@link DateRange};</li>
 *   <li>VALIDATION-NESTED-001 — {@code @Valid} cascades into {@link Address} and into every
 *       {@code List<@Valid LineItem>} element.</li>
 * </ul>
 *
 * <p>A class (not a record) so a {@link JsonAnySetter} instance method can deterministically
 * capture unknown properties — record-component any-setters are not reliably honored. Accessor
 * names mirror the record style ({@code customer()}, {@code items()}, …) so callers are unchanged.
 * Spec: specs/request-validation-l0.yaml.
 */
@DateRange
public final class CreateOrderRequest {

    @NotBlank @Size(max = 80)
    private final String customer;

    @NotNull @Positive
    private final BigDecimal amount;

    @NotNull
    private final Priority priority;

    @NotNull
    private final LocalDate startDate;

    @NotNull
    private final LocalDate endDate;

    @NotNull @Valid
    private final Address address;

    @NotEmpty
    private final List<@Valid LineItem> items;

    /** VALIDATION-TYPE-001: any property not matching a declared field lands here, not dropped. */
    private final Map<String, Object> unknownFields = new HashMap<>();

    @JsonCreator
    public CreateOrderRequest(
            @JsonProperty("customer") String customer,
            @JsonProperty("amount") @JsonDeserialize(using = StrictNumericDeserializer.class) BigDecimal amount,
            @JsonProperty("priority") Priority priority,
            @JsonProperty("startDate") LocalDate startDate,
            @JsonProperty("endDate") LocalDate endDate,
            @JsonProperty("address") Address address,
            @JsonProperty("items") List<LineItem> items) {
        this.customer = customer;
        this.amount = amount;
        this.priority = priority;
        this.startDate = startDate;
        this.endDate = endDate;
        this.address = address;
        this.items = items;
    }

    @JsonAnySetter
    private void collectUnknown(String name, Object value) {
        unknownFields.put(name, value);
    }

    /**
     * VALIDATION-TYPE-001 reject-unknown, surfaced through the SAME {@code errors[]} array as
     * every other constraint (code {@code AssertTrue}) — a typo'd field fails validation instead
     * of being silently swallowed.
     */
    @AssertTrue(message = "unknown fields are not permitted")
    @JsonIgnore
    public boolean isNoUnknownFields() {
        return unknownFields.isEmpty();
    }

    public String customer() {
        return customer;
    }

    public BigDecimal amount() {
        return amount;
    }

    public Priority priority() {
        return priority;
    }

    public LocalDate startDate() {
        return startDate;
    }

    public LocalDate endDate() {
        return endDate;
    }

    public Address address() {
        return address;
    }

    public List<LineItem> items() {
        return items;
    }

    /** Strict enum — Jackson rejects an unlisted token by default (VALIDATION-TYPE-001). */
    public enum Priority {
        LOW, NORMAL, HIGH
    }

    /** Nested object — cascaded via {@code @Valid address} (VALIDATION-NESTED-001). */
    public record Address(
            @NotBlank @Pattern(regexp = "\\d{5}", message = "postalCode must be exactly 5 digits") String postalCode,
            @NotBlank String city) {}

    /** Collection element — every violating element reported with its index (VALIDATION-NESTED-001). */
    public record LineItem(
            @NotBlank String sku,
            @Positive int quantity) {}
}
