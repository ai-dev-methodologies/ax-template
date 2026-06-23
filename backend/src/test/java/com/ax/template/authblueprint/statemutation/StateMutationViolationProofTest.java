package com.ax.template.authblueprint.statemutation;

import jakarta.persistence.Column;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIOLATION proof for state-conditional-mutability-l0. Structural assertions a deliberate break
 * cannot pass silently: the authority is a SINGLE declared EnumMap (no field-name if-scatter in the
 * edit path), the mutable-set tightens monotonically DRAFT ⊇ SUBMITTED ⊇ APPROVED ⊇ LOCKED, the
 * governed transitions are append-only one-per-(form,seq) with no public setter, the form carries
 * @Version + the @Check backstops, NO delete path exists anywhere in the domain, mutators are
 * package-sealed, the write path uses the PESSIMISTIC_WRITE finder and re-checks state under it,
 * and the migration carries the same backstops.
 */
@Tag("STATEMUTATION")
class StateMutationViolationProofTest {

    // ── STATEMUTATION-AUTHORITY/DECLARED-001 — the authority is a declared table, not an if-scatter ──
    @Test @Tag("STATEMUTATION-AUTHORITY-001") @Tag("STATEMUTATION-DECLARED-001")
    void violation_authorityIsADeclaredTable_notAnIfScatter() throws Exception {
        // the declared sets are exactly what the policy table reports
        assertThat(StateFieldPolicy.mutableFields(FormState.DRAFT))
            .containsExactlyInAnyOrder(FormField.TITLE, FormField.BODY, FormField.REVIEWER_NOTE);
        assertThat(StateFieldPolicy.mutableFields(FormState.SUBMITTED)).containsExactly(FormField.REVIEWER_NOTE);
        assertThat(StateFieldPolicy.mutableFields(FormState.APPROVED)).isEmpty();
        assertThat(StateFieldPolicy.mutableFields(FormState.LOCKED)).isEmpty();

        // the declared sets are immutable — a caller cannot widen the policy at runtime
        Set<FormField> draft = StateFieldPolicy.mutableFields(FormState.DRAFT);
        try {
            draft.add(FormField.TITLE);
            org.junit.jupiter.api.Assertions.fail("the declared mutable-set must be immutable");
        } catch (UnsupportedOperationException expected) { /* immutable view — correct */ }

        // the SERVICE edit path consults the declared table (isMutable / StateFieldPolicy), not a
        // field-name if/switch scatter — the authority lives in ONE place.
        String svc = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "statemutation", "StateMutationService.java"));
        assertThat(svc).as("the edit path consults the declared StateFieldPolicy table")
            .contains("StateFieldPolicy.isMutable(");
        assertThat(svc).as("no per-field-name if-scatter in the service edit path")
            .doesNotContain("equals(\"title\")").doesNotContain("equals(\"body\")")
            .doesNotContain("== FormField.TITLE").doesNotContain("== FormField.BODY");
    }

    // ── STATEMUTATION-MONOTONE-001 — the mutable-set tightens monotonically along the forward lifecycle ──
    @Test @Tag("STATEMUTATION-MONOTONE-001")
    void violation_mutableSetTightensMonotonically() {
        assertThat(StateFieldPolicy.isMonotoneForward(FormState.DRAFT, FormState.SUBMITTED))
            .as("DRAFT ⊇ SUBMITTED").isTrue();
        assertThat(StateFieldPolicy.isMonotoneForward(FormState.SUBMITTED, FormState.APPROVED))
            .as("SUBMITTED ⊇ APPROVED").isTrue();
        assertThat(StateFieldPolicy.isMonotoneForward(FormState.APPROVED, FormState.LOCKED))
            .as("APPROVED ⊇ LOCKED").isTrue();
        // and the chain is a strict tightening into the empty set
        assertThat(StateFieldPolicy.mutableFields(FormState.DRAFT))
            .containsAll(StateFieldPolicy.mutableFields(FormState.SUBMITTED));
        assertThat(StateFieldPolicy.mutableFields(FormState.SUBMITTED))
            .containsAll(StateFieldPolicy.mutableFields(FormState.APPROVED));
        // a hypothetical re-open (DRAFT after SUBMITTED) WIDENS — so it is NOT monotone-forward,
        // which is exactly why a widening must be a recorded REOPEN, never a FORWARD edge.
        assertThat(StateFieldPolicy.isMonotoneForward(FormState.SUBMITTED, FormState.DRAFT))
            .as("re-opening widens — not a forward-monotone edge").isFalse();
    }

    // ── STATEMUTATION-MONOTONE-001 — transitions append-only, one per (form, seq), no public setter ──
    @Test @Tag("STATEMUTATION-MONOTONE-001")
    void violation_transitionsAppendOnly_uniquePerSeq() throws Exception {
        for (Method m : FormTransition.class.getMethods()) {
            assertThat(m.getName()).as("FormTransition must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "formId", "seq", "fromState", "toState", "kind", "reason",
                                     "actor", "occurredAt"}) {
            Column col = FormTransition.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("FormTransition." + f + " must be immutable").isFalse();
        }
        jakarta.persistence.Table table = FormTransition.class.getAnnotation(jakarta.persistence.Table.class);
        assertThat(table.uniqueConstraints()[0].columnNames()).containsExactly("form_id", "seq");
    }

    // ── @Check backstops; mutators sealed; @Version; immutable identity; NO delete path ──
    @Test @Tag("STATEMUTATION-AUTHORITY-001") @Tag("STATEMUTATION-MONOTONE-001")
    void violation_noDeletePath_checkBackstops_mutatorsSealed_versioned() throws Exception {
        for (Method m : GovernedFormRepository.class.getDeclaredMethods()) {
            assertThat(m.getName()).doesNotContain("delete");
        }
        for (String src : new String[]{"StateMutationService", "StateMutationController", "GovernedFormStateMachine"}) {
            String text = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
                "com", "ax", "template", "authblueprint", "statemutation", src + ".java"));
            assertThat(text).as(src + " must contain no delete call — forms are locked, never removed")
                .doesNotContain(".delete(").doesNotContain("deleteBy");
        }

        Check check = GovernedForm.class.getAnnotation(Check.class);
        String c = check.constraints().replaceAll("\\s+", " ");
        assertThat(c).contains("state <> 'LOCKED' OR locked_at IS NOT NULL");
        assertThat(c).contains("last_edited_at IS NULL OR last_edited_field IS NOT NULL");

        // sole-mutator hooks are package-private — never public setters
        for (String hook : new String[]{"applyEdit", "setState", "markLocked"}) {
            Method m = java.util.Arrays.stream(GovernedForm.class.getDeclaredMethods())
                .filter(x -> x.getName().equals(hook)).findFirst().orElseThrow();
            assertThat(Modifier.isPublic(m.getModifiers()))
                .as("GovernedForm." + hook + " must be package-private").isFalse();
        }
        // no PUBLIC setter at all on the root
        for (Method m : GovernedForm.class.getMethods()) {
            assertThat(m.getName()).as("GovernedForm must expose no public setter").doesNotStartWith("set");
        }

        for (String f : new String[]{"id", "owner", "createdAt"}) {
            Column col = GovernedForm.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col.updatable()).as("GovernedForm." + f + " must be immutable").isFalse();
        }
        assertThat(GovernedForm.class.getDeclaredField("version").isAnnotationPresent(Version.class)).isTrue();
    }

    // ── STATEMUTATION-TOCTOU-001 — the write path uses the PESSIMISTIC_WRITE finder + re-checks under it ──
    @Test @Tag("STATEMUTATION-TOCTOU-001")
    void violation_lockedFinder_andUnderLockRecheck() throws Exception {
        Method locked = GovernedFormRepository.class.getMethod("findByIdForUpdate", java.util.UUID.class);
        org.springframework.data.jpa.repository.Lock lock =
            locked.getAnnotation(org.springframework.data.jpa.repository.Lock.class);
        assertThat(lock).isNotNull();
        assertThat(lock.value()).isEqualTo(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);

        String svc = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "statemutation", "StateMutationService.java"));
        for (String method : new String[]{"public GovernedForm editField(", "public GovernedForm transition("}) {
            int start = svc.indexOf(method);
            assertThat(start).as(method + " must exist").isPositive();
            String body = svc.substring(start, svc.indexOf("\n    }", start));
            assertThat(body).as(method + " must take the form row lock").contains("findByIdForUpdate");
        }
        // the edit re-checks the authority against the CURRENT (under-lock) state — the line ordering
        // is findByIdForUpdate BEFORE the isMutable check so the check sees the advanced state (CWE-367).
        int editAt = svc.indexOf("public GovernedForm editField(");
        String editBody = svc.substring(editAt, svc.indexOf("\n    }", editAt));
        assertThat(editBody.indexOf("findByIdForUpdate")).as("lock is taken before the authority check")
            .isLessThan(editBody.indexOf("StateFieldPolicy.isMutable("));
    }

    // ── the migration carries the same backstops ──
    @Test @Tag("STATEMUTATION-AUTHORITY-001") @Tag("STATEMUTATION-MONOTONE-001")
    void violation_migrationCarriesTheSameBackstops() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V062__create_statemutation.sql")) {
            assertThat(in).as("V062__create_statemutation.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).contains("state <> 'LOCKED' OR locked_at IS NOT NULL");
            assertThat(sql).contains("last_edited_at IS NULL OR last_edited_field IS NOT NULL");
            assertThat(sql).contains("UNIQUE INDEX uq_form_transition_seq");
            assertThat(sql).contains("(form_id, seq)");
        }
        // every FormField is represented exactly once across the union of declared sets ⊆ DRAFT (the top)
        EnumSet<FormField> all = EnumSet.allOf(FormField.class);
        assertThat(StateFieldPolicy.mutableFields(FormState.DRAFT)).containsAll(all);
    }
}
