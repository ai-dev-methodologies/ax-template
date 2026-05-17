package com.ax.template.authblueprint.practices;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("PRACTICES")
@Tag("PRACTICES-QUALITY-002")
class QualityUtilityClassShapeTest {

    @Test
    void practices_QUALITY_002_piiRedactorIsAnUninstantiableUtility() {
        // A utility class — one whose every member is static — must enforce its shape:
        //   - final (cannot be subclassed to add instance state)
        //   - single, private no-arg constructor (cannot be instantiated)
        // PiiRedactor is the project's archetypal utility class. Effective Java Item 4.
        Class<?> type = PiiRedactor.class;
        assertThat(Modifier.isFinal(type.getModifiers()))
                .as("utility class %s must be final", type.getSimpleName())
                .isTrue();

        Constructor<?>[] ctors = type.getDeclaredConstructors();
        assertThat(ctors)
                .as("utility class %s must have exactly one constructor", type.getSimpleName())
                .hasSize(1);
        Constructor<?> ctor = ctors[0];
        assertThat(ctor.getParameterCount())
                .as("the sole constructor of %s must take no arguments", type.getSimpleName())
                .isZero();
        assertThat(Modifier.isPrivate(ctor.getModifiers()))
                .as("the sole constructor of %s must be private", type.getSimpleName())
                .isTrue();
    }
}
