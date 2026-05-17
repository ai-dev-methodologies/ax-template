package com.ax.template.authblueprint.practices;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag("PRACTICES")
@Tag("PRACTICES-WEB-003")
class WebSpecificMappingMethodsTest {

    private static final List<Class<? extends java.lang.annotation.Annotation>> METHOD_SPECIFIC = List.of(
            GetMapping.class, PostMapping.class, PutMapping.class, DeleteMapping.class, PatchMapping.class);

    @Test
    void practices_WEB_003_handlerMethodsUseHttpMethodSpecificMappingAnnotations() {
        // @RequestMapping(method = RequestMethod.POST) is the verbose, error-prone form
        // — forgetting `method = ...` silently exposes a handler on every HTTP verb. The
        // method-specific shortcuts (@GetMapping, @PostMapping, etc.) make the verb
        // mandatory and self-documenting at the call site.
        List<String> offenders = java.util.stream.Stream.of(PracticesDemoController.class.getDeclaredMethods())
                .filter(m -> m.getDeclaredAnnotations().length > 0)
                .filter(WebSpecificMappingMethodsTest::usesGenericRequestMapping)
                .map(Method::getName)
                .toList();
        assertThat(offenders)
                .as("handler methods must use method-specific mapping annotations (@GetMapping, @PostMapping, ...) — not @RequestMapping(method=...)")
                .isEmpty();
    }

    private static boolean usesGenericRequestMapping(Method m) {
        boolean hasGenericMapping = m.isAnnotationPresent(RequestMapping.class);
        boolean hasSpecificMapping = METHOD_SPECIFIC.stream().anyMatch(m::isAnnotationPresent);
        // @RequestMapping at method level is the anti-pattern; class-level usage on the
        // controller is fine (already covered by WebProducesContractTest).
        return hasGenericMapping && !hasSpecificMapping;
    }
}
