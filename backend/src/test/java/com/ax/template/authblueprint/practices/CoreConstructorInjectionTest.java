package com.ax.template.authblueprint.practices;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("PRACTICES")
@Tag("PRACTICES-CORE-001")
class CoreConstructorInjectionTest {

    @Test
    void practices_CORE_001_constructorInjectedDependencyIsFinal() throws Exception {
        var field = ConstructorInjectedService.class.getDeclaredField("parents");
        assertThat(Modifier.isFinal(field.getModifiers()))
                .as("constructor-injected dependency must be `final` to forbid post-construction mutation")
                .isTrue();
        // And the class must declare a single-arg constructor matching the field type.
        var ctors = ConstructorInjectedService.class.getDeclaredConstructors();
        assertThat(ctors).hasSize(1);
        assertThat(ctors[0].getParameterTypes()).containsExactly(ParentRepository.class);
    }

    @Test
    void practices_CORE_001_fieldInjectedDependencyIsNotFinal() throws Exception {
        // The anti-pattern: the same dependency lives on a non-final field, requiring
        // reflection to set in a non-Spring context and inviting silent re-assignment.
        var field = FieldInjectedService.class.getDeclaredField("parents");
        assertThat(Modifier.isFinal(field.getModifiers()))
                .as("field-injected dependency is necessarily non-final — anti-pattern signal")
                .isFalse();
    }
}
