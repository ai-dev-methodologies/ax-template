package com.ax.template.authblueprint.orderquantization;

import com.ax.template.authblueprint.common.PageEnvelope;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

/**
 * order-multiple-quantization-l0 thin controller. The acting principal is ALWAYS the authenticated
 * caller (no userId path/body param). Delegates to {@link OrderQuantizationService}.
 */
@RestController
public class OrderQuantizationController {

    /** required >= 0 (positive-or-zero); moq / multiple positivity is enforced in the service (422). */
    public record QuantizeReq(@NotBlank @Size(max = 200) String itemRef,
                              @NotNull @PositiveOrZero Long required,
                              @NotNull Long moq,
                              @NotNull Long orderMultiple) {}

    public record QuantizationDto(UUID id, String itemRef, long required, long moq, long orderMultiple,
                                  long orderQuantity, long overage, Instant createdAt) {
        static QuantizationDto of(OrderQuantization q) {
            return new QuantizationDto(q.getId(), q.getItemRef(), q.getRequiredQuantity(), q.getMoq(),
                q.getOrderMultiple(), q.getOrderQuantity(), q.getOverage(), q.getCreatedAt());
        }
    }

    private final OrderQuantizationService service;

    public OrderQuantizationController(OrderQuantizationService service) {
        this.service = service;
    }

    /** ORDERQUANT-QUANTIZE/OVERAGE/BASIS-001 — quantize a required net quantity up to the lot constraint. */
    @PostMapping("/api/order-quantization/quantizations")
    public ResponseEntity<QuantizationDto> quantize(@Valid @RequestBody QuantizeReq req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(QuantizationDto.of(
            service.quantize(req.itemRef(), req.required(), req.moq(), req.orderMultiple())));
    }

    @GetMapping("/api/order-quantization/quantizations/{id}")
    public QuantizationDto get(@PathVariable UUID id) {
        return QuantizationDto.of(service.get(id));
    }

    @GetMapping("/api/order-quantization/quantizations")
    public PageEnvelope<QuantizationDto> list(@RequestParam String itemRef,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return PageEnvelope.from(service.listForItem(itemRef, page, size), QuantizationDto::of);
    }

    @ExceptionHandler(OrderQuantizationException.class)
    public ResponseEntity<ProblemDetail> handle(OrderQuantizationException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
