package com.ax.template.authblueprint.billing;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * R21 billing-domain ArchUnit checks.
 * <p>
 * Trace:
 * <ul>
 *   <li>BILLING-STATE-001 — only {@link SubscriptionStateMachine} may
 *       call {@link Subscription#setStatus(SubscriptionStatus)}. The setter is
 *       package-private, so any class outside the {@code billing} package
 *       cannot reach it; this test pins the in-package constraint.</li>
 *   <li>BILLING-BOUNDARY-001 — {@code billing} package must not import
 *       any class from {@code payment}, and vice versa.</li>
 * </ul>
 */
@Tag("BILLING")
class BillingArchitectureTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages("com.ax.template.authblueprint");

    @Test
    @Tag("BILLING-STATE-001")
    void onlyStateMachineMutatesSubscriptionStatus() {
        // Subscription.setStatus(...) is package-private. The only legal caller
        // inside the billing package is SubscriptionStateMachine. Verify nobody
        // else inside the package depends on the entity's status mutator field
        // through reflective or direct setter calls.
        ArchRule rule = noClasses()
            .that().resideInAPackage("..billing..")
            .and().doNotHaveSimpleName("SubscriptionStateMachine")
            .and().doNotHaveSimpleName("Subscription")
            .should().callMethod(Subscription.class, "setStatus", SubscriptionStatus.class)
            .allowEmptyShould(true);
        rule.check(CLASSES);
    }

    @Test
    @Tag("BILLING-BOUNDARY-001")
    void billingMustNotImportPayment() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..billing..")
            .should().dependOnClassesThat().resideInAPackage("..payment..")
            .allowEmptyShould(true);
        rule.check(CLASSES);
    }

    @Test
    @Tag("BILLING-BOUNDARY-001")
    void paymentMustNotImportBilling() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..payment..")
            .should().dependOnClassesThat().resideInAPackage("..billing..")
            .allowEmptyShould(true);
        rule.check(CLASSES);
    }
}
