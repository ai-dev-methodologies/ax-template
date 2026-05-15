package com.ax.template.authblueprint.practices;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@Tag("PRACTICES")
@Tag("PRACTICES-PERS-002")
class PersistenceEntityGraphTest {

    @Autowired
    private EntityManager em;

    @Autowired
    private ParentRepository parents;

    @BeforeEach
    void seed() {
        Statistics stats = statistics();
        stats.setStatisticsEnabled(true);
        for (int i = 0; i < 3; i++) {
            Parent p = new Parent();
            p.setName("p" + i);
            for (int j = 0; j < 2; j++) {
                Child c = new Child();
                c.setLabel("c" + i + "_" + j);
                p.addChild(c);
            }
            em.persist(p);
        }
        em.flush();
        em.clear();
        stats.clear();
    }

    @Test
    void practices_PERS_002_entityGraphAnnotationAvoidsNPlusOne() {
        var list = parents.findAllWithChildrenViaEntityGraph();
        list.forEach(p -> p.getChildren().size());
        long statements = statistics().getPrepareStatementCount();
        assertThat(statements)
                .as("@EntityGraph annotation-driven fetch must produce a single statement")
                .isEqualTo(1);
    }

    private Statistics statistics() {
        return em.unwrap(Session.class).getSessionFactory().getStatistics();
    }
}
