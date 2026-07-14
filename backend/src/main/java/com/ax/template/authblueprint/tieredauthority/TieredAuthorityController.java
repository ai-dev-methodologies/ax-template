package com.ax.template.authblueprint.tieredauthority;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * amount-tiered-authority-l0 thin controller (전결 규정). Config mutation is ROLE_ADMIN-only;
 * reads and decisions are any authenticated caller. Decider identity/level: this is a generic
 * composition-kit primitive — {@code deciderLevel} is a request field, not derived from an org
 * chart (business roles ≠ security principals, per docs/NEW-DOMAIN-CHECKLIST.md §0). A
 * fork-receiver wires a real org-chart→level resolution; the catalog's guarantee is the
 * fail-closed comparison + the immutable snapshot, not the level's provenance.
 */
@RestController
public class TieredAuthorityController {

    public record BandReq(@NotNull BigDecimal minAmount, BigDecimal maxAmount, int minDeciderLevel) {}
    public record CreateTableReq(@NotEmpty @Size(max = 50) List<BandReq> bands) {}
    public record DecideReq(@NotNull BigDecimal amount, int deciderLevel, @Size(max = 500) String outcome) {}

    public record BandDto(UUID id, BigDecimal minAmount, BigDecimal maxAmount, int minDeciderLevel) {
        static BandDto of(AuthorityTierBand b) {
            return new BandDto(b.getId(), b.getMinAmount(), b.getMaxAmount(), b.getMinDeciderLevel());
        }
    }
    public record TableDto(UUID id, int tableVersion, String createdBy, Instant createdAt, List<BandDto> bands) {}
    public record DecisionDto(UUID id, UUID tableId, int tableVersion, BigDecimal amount,
                              BigDecimal bandMinAmount, BigDecimal bandMaxAmount, int bandMinDeciderLevel,
                              int deciderLevel, String outcome, String decidedBy, Instant decidedAt) {
        static DecisionDto of(TieredDecisionRecord d) {
            return new DecisionDto(d.getId(), d.getTableId(), d.getTableVersion(), d.getAmount(),
                d.getBandMinAmount(), d.getBandMaxAmount(), d.getBandMinDeciderLevel(),
                d.getDeciderLevel(), d.getOutcome(), d.getDecidedBy(), d.getDecidedAt());
        }
    }

    private final TieredAuthorityService service;

    public TieredAuthorityController(TieredAuthorityService service) {
        this.service = service;
    }

    @PostMapping("/api/tiered-authority/config")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<TableDto> createTable(@Valid @RequestBody CreateTableReq req, Authentication auth) {
        List<TieredAuthorityService.BandInput> inputs = req.bands().stream()
            .map(b -> new TieredAuthorityService.BandInput(b.minAmount(), b.maxAmount(), b.minDeciderLevel()))
            .toList();
        AuthorityTierTable table = service.createTable(inputs, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(toTableDto(table));
    }

    @GetMapping("/api/tiered-authority/config")
    public TableDto currentTable() {
        return toTableDto(service.currentTable());
    }

    @PostMapping("/api/tiered-authority/decisions")
    public ResponseEntity<DecisionDto> decide(@Valid @RequestBody DecideReq req, Authentication auth) {
        TieredDecisionRecord record = service.decide(req.amount(), req.deciderLevel(), req.outcome(), auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
            .location(URI.create("/api/tiered-authority/decisions/" + record.getId()))
            .body(DecisionDto.of(record));
    }

    @GetMapping("/api/tiered-authority/decisions/{id}")
    public DecisionDto decision(@PathVariable UUID id) {
        return DecisionDto.of(service.decision(id));
    }

    private TableDto toTableDto(AuthorityTierTable table) {
        List<BandDto> bands = service.bandsFor(table.getId()).stream().map(BandDto::of).toList();
        return new TableDto(table.getId(), table.getTableVersion(), table.getCreatedBy(),
            table.getCreatedAt(), bands);
    }

    @ExceptionHandler(TieredAuthorityException.class)
    public ResponseEntity<ProblemDetail> handle(TieredAuthorityException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
