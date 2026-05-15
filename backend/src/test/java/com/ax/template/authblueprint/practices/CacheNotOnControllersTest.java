package com.ax.template.authblueprint.practices;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.RestController;

@Tag("PRACTICES")
@Tag("PRACTICES-CACHE-003")
class CacheNotOnControllersTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.ax.template.authblueprint.practices");

    @Test
    void practices_CACHE_003_cacheAnnotationsAreForbiddenOnControllers() {
        // Caching at the controller layer caches the *entire* HTTP response — including
        // request-derived state like the principal, the locale, and any headers Spring
        // injects. The next request for the same path can be served from another user's
        // response. Caching belongs at the service layer where the inputs are explicit
        // method arguments, not the implicit HTTP context.
        ArchRule rule = classes()
                .that().areAnnotatedWith(RestController.class)
                .should().notBeAnnotatedWith(Cacheable.class)
                .andShould().notBeAnnotatedWith(CachePut.class)
                .andShould().notBeAnnotatedWith(CacheEvict.class)
                .allowEmptyShould(true);
        rule.check(CLASSES);
    }
}
