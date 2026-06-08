package ddd.fixtures.widget;
import org.springframework.transaction.annotation.Transactional;
/** FIXTURE — deliberately violates HG-ANTI-GODSERVICE-TX (one @Transactional mutates 2 roots). */
public class FixtureGodService {
    private WidgetRootRepo widgetRepo;
    private GadgetRootRepo gadgetRepo;

    @Transactional
    public void mutateTwoAggregates(WidgetRoot w, GadgetRoot g) {
        widgetRepo.save(w);
        gadgetRepo.save(g);
    }
}
