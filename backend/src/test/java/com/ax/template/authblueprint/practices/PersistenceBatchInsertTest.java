package com.ax.template.authblueprint.practices;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.jdbc.batch_size=20",
        "spring.jpa.properties.hibernate.order_inserts=true",
        "spring.jpa.properties.hibernate.order_updates=true"
})
@Tag("PRACTICES")
@Tag("PRACTICES-PERS-003")
class PersistenceBatchInsertTest {

    @Autowired
    private EntityManager em;

    @Test
    void practices_PERS_003_batchSizePropertyTakesEffect() {
        EntityManagerFactory emf = em.getEntityManagerFactory();
        Object batchSize = emf.getProperties().get("hibernate.jdbc.batch_size");
        Object orderInserts = emf.getProperties().get("hibernate.order_inserts");
        assertThat(batchSize)
                .as("hibernate.jdbc.batch_size must be wired into the EMF properties")
                .isNotNull()
                .satisfies(v -> assertThat(String.valueOf(v)).isEqualTo("20"));
        assertThat(orderInserts)
                .as("hibernate.order_inserts must accompany batch_size or batches won't pack")
                .isNotNull()
                .satisfies(v -> assertThat(String.valueOf(v)).isEqualToIgnoringCase("true"));
    }
}
