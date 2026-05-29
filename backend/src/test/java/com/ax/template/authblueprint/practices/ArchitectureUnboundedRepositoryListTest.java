package com.ax.template.authblueprint.practices;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * IMW1-C unbounded-repository-list guard (IDW1 dogfood 2026-05-29).
 *
 * <p>The IDW1 dogfood produced a Spring Data repository method that returned a raw
 * {@code java.util.List} with NO {@code Pageable} parameter — a mechanical violation of
 * pagination-l0 PAGE-LIMIT-001 (every list read must be bounded) — and it slipped past all
 * 49 prior guards because none of them inspected repository method signatures.
 *
 * <p>This guard mechanically flags the canonical full-table-scan signature: a method DECLARED
 * on a {@code *Repository} interface whose name starts with {@code findAll} and which returns
 * a collection type ({@code List}/{@code Collection}/{@code Set}/{@code Iterable}/{@code Stream})
 * WITHOUT a {@link org.springframework.data.domain.Pageable} parameter.
 *
 * <p><b>Predicate scope (deliberately narrow to stay false-positive-free).</b> We flag only the
 * {@code findAll*} prefix — the unambiguous "return the whole table" idiom — not arbitrary
 * {@code findBy<predicate>} finders, which are bounded by their query predicate (a FK, a unique
 * key, a small enum status) and are legitimately small. Spring Data's own inherited
 * {@code JpaRepository.findAll()} is not flagged: it is not <em>declared</em> on the concrete
 * interface, and ArchUnit's {@code getMethods()} returns declared methods only.
 *
 * <p><b>Allowlist.</b> The two demo methods on {@code ParentRepository}
 * ({@code findAllWithChildren} / {@code findAllWithChildrenViaEntityGraph}) are deliberate
 * N+1 / {@code @EntityGraph} fetch-join demonstrations on a tiny demo table in the demo
 * {@code practices} package; they predate this guard and are intentionally unbounded for
 * teaching purposes. They are explicitly allowlisted by fully-qualified name so the predicate
 * itself stays strict for every real domain.
 */
@Tag("PRACTICES")
@Tag("PRACTICES-TEST-005")
class ArchitectureUnboundedRepositoryListTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.ax.template.authblueprint");

    private static final String PAGEABLE = "org.springframework.data.domain.Pageable";

    /** Collection-shaped return types that, unbounded, can stream an entire table into memory. */
    private static final Set<String> COLLECTION_RETURN_TYPES = Set.of(
            "java.util.List",
            "java.util.Collection",
            "java.util.Set",
            "java.lang.Iterable",
            "java.util.stream.Stream");

    /**
     * Demo fetch-join methods that are intentionally unbounded (N+1 / @EntityGraph teaching
     * fixtures on a tiny demo table). Matched by fully-qualified owner + method name so the
     * predicate stays strict everywhere else.
     */
    private static final Set<String> ALLOWLIST = Set.of(
            "com.ax.template.authblueprint.practices.ParentRepository.findAllWithChildren",
            "com.ax.template.authblueprint.practices.ParentRepository.findAllWithChildrenViaEntityGraph");

    @Test
    void practices_TEST_005_repositoryFindAllListReturnsCarryPageable() {
        ArchRule rule = methods()
                .that(new DeclaredOnRepositoryInterface())
                .should(new BeBoundedFindAllListReturn())
                .allowEmptyShould(true);
        rule.check(CLASSES);
    }

    /** True for methods declared directly on an interface whose simple name ends with "Repository". */
    private static final class DeclaredOnRepositoryInterface
            extends com.tngtech.archunit.base.DescribedPredicate<JavaMethod> {

        DeclaredOnRepositoryInterface() {
            super("declared on a *Repository interface");
        }

        @Override
        public boolean test(JavaMethod method) {
            JavaClass owner = method.getOwner();
            return owner.isInterface() && owner.getSimpleName().endsWith("Repository");
        }
    }

    /**
     * Satisfied UNLESS the method is the unbounded full-table-scan signature:
     * name starts with {@code findAll}, returns a collection type, and has no {@code Pageable}.
     */
    private static final class BeBoundedFindAllListReturn extends ArchCondition<JavaMethod> {

        BeBoundedFindAllListReturn() {
            super("be a bounded read: findAll* methods returning a collection must accept a "
                    + "org.springframework.data.domain.Pageable");
        }

        @Override
        public void check(JavaMethod method, ConditionEvents events) {
            boolean isFindAll = method.getName().startsWith("findAll");
            boolean returnsCollection =
                    COLLECTION_RETURN_TYPES.contains(method.getRawReturnType().getFullName());
            boolean hasPageable = method.getRawParameterTypes().stream()
                    .anyMatch(p -> PAGEABLE.equals(p.getFullName()));
            String fqName = method.getOwner().getFullName() + "." + method.getName();
            boolean allowlisted = ALLOWLIST.contains(fqName);

            boolean violation = isFindAll && returnsCollection && !hasPageable && !allowlisted;

            if (violation) {
                String message = String.format(
                        "%s returns an unbounded %s with no Pageable parameter "
                                + "(violates pagination-l0 PAGE-LIMIT-001). Add a "
                                + "org.springframework.data.domain.Pageable parameter and return Page<T>, "
                                + "or rename to a bounded findBy<predicate> finder. [%s]",
                        fqName,
                        method.getRawReturnType().getSimpleName(),
                        method.getSourceCodeLocation());
                events.add(SimpleConditionEvent.violated(method, message));
            }
        }
    }
}
