package com.ax.template.authblueprint.practices;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Tag("PRACTICES")
@Tag("PRACTICES-CORE-002")
class CoreAopFinalClassTest {

    @Autowired
    private ProxiedService proxiedService;

    @Test
    void practices_CORE_002_nonFinalServiceIsCglibProxied() {
        assertThat(AopUtils.isAopProxy(proxiedService))
                .as("Spring must wrap the bean in a CGLIB proxy so @Transactional advice runs")
                .isTrue();
        assertThat(Modifier.isFinal(ProxiedService.class.getModifiers()))
                .as("the bean class must not be final; CGLIB cannot subclass a final type")
                .isFalse();
        for (var m : ProxiedService.class.getDeclaredMethods()) {
            if (java.lang.reflect.Modifier.isPublic(m.getModifiers())) {
                assertThat(Modifier.isFinal(m.getModifiers()))
                        .as("public method `%s` must not be final; CGLIB cannot override final methods", m.getName())
                        .isFalse();
            }
        }
    }

    @Test
    void practices_CORE_002_proxyAdviceFiresOnPublicEntry() {
        // Calling the proxied bean from outside opens a transaction.
        assertThat(proxiedService.isInsideTransaction())
                .as("@Transactional must propagate when invoked through the proxy")
                .isTrue();
    }
}
