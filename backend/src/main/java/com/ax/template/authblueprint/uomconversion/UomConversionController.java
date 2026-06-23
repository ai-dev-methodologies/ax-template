package com.ax.template.authblueprint.uomconversion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * dimensional-uom-conversion-l0 thin controller. The acting principal is ALWAYS the authenticated
 * caller (caller-authentication-only-no-userid-param). Delegates to {@link UomConversionService}.
 */
@RestController
public class UomConversionController {

    public record RegisterMaterialReq(@NotBlank @Size(max = 200) String materialRef) {}
    public record RecordPropertyReq(@NotNull Dimension fromDimension,
                                    @NotNull Dimension toDimension,
                                    @NotNull @Positive BigDecimal factor) {}
    public record ConvertReq(@NotNull @PositiveOrZero BigDecimal fromQuantity,
                             @NotBlank @Size(max = 20) String fromUnit,
                             @NotBlank @Size(max = 20) String toUnit,
                             Long materialVersion) {}

    public record MaterialDto(UUID id, String materialRef, long currentVersion, Instant createdAt) {
        static MaterialDto of(Material m) {
            return new MaterialDto(m.getId(), m.getMaterialRef(), m.getCurrentVersion(), m.getCreatedAt());
        }
    }
    public record PropertyDto(UUID id, long version, Dimension fromDimension, Dimension toDimension,
                              BigDecimal factor, Instant recordedAt) {
        static PropertyDto of(MaterialProperty p) {
            return new PropertyDto(p.getId(), p.getVersion(), p.getFromDimension(), p.getToDimension(),
                p.getFactor(), p.getRecordedAt());
        }
    }
    public record ConversionDto(UUID id, UUID materialId, BigDecimal fromQuantity, Unit fromUnit, Unit toUnit,
                                Dimension fromDimension, Dimension toDimension, Conversion.Mode mode,
                                BigDecimal factor, long materialVersion, int resultScale, BigDecimal toQuantity,
                                Instant occurredAt) {
        static ConversionDto of(Conversion c) {
            return new ConversionDto(c.getId(), c.getMaterialId(), c.getFromQuantity(), c.getFromUnit(),
                c.getToUnit(), c.getFromDimension(), c.getToDimension(), c.getMode(), c.getFactor(),
                c.getMaterialVersion(), c.getResultScale(), c.getToQuantity(), c.getOccurredAt());
        }
    }

    private final UomConversionService service;

    public UomConversionController(UomConversionService service) {
        this.service = service;
    }

    @PostMapping("/api/uom-conversion/materials")
    public ResponseEntity<MaterialDto> register(@Valid @RequestBody RegisterMaterialReq req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(MaterialDto.of(service.registerMaterial(req.materialRef())));
    }

    /** UOMCONV-MATERIAL/VERSION-001 — append a new immutable bridging-property version. */
    @PostMapping("/api/uom-conversion/materials/{id}/properties")
    public ResponseEntity<PropertyDto> recordProperty(@PathVariable UUID id, @Valid @RequestBody RecordPropertyReq req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(PropertyDto.of(
            service.recordProperty(id, req.fromDimension(), req.toDimension(), req.factor())));
    }

    @GetMapping("/api/uom-conversion/materials/{id}")
    public MaterialDto getMaterial(@PathVariable UUID id) {
        return MaterialDto.of(service.getMaterial(id));
    }

    @GetMapping("/api/uom-conversion/materials/{id}/properties")
    public List<PropertyDto> properties(@PathVariable UUID id) {
        return service.properties(id).stream().map(PropertyDto::of).toList();
    }

    /** UOMCONV-COMPAT-001 — convert; cross-dimension cites the material (and optionally a pinned version). */
    @PostMapping("/api/uom-conversion/materials/{id}/conversions")
    public ConversionDto convert(@PathVariable UUID id, @Valid @RequestBody ConvertReq req, Authentication auth) {
        Conversion c = req.materialVersion() == null
            ? service.convert(id, req.fromQuantity(), req.fromUnit(), req.toUnit(), auth.getName())
            : service.convertAtVersion(id, req.fromQuantity(), req.fromUnit(), req.toUnit(),
                req.materialVersion(), auth.getName());
        return ConversionDto.of(c);
    }

    @GetMapping("/api/uom-conversion/conversions/{id}")
    public ConversionDto getConversion(@PathVariable UUID id) {
        return ConversionDto.of(service.getConversion(id));
    }

    @ExceptionHandler(UomConversionException.class)
    public ResponseEntity<ProblemDetail> handle(UomConversionException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
