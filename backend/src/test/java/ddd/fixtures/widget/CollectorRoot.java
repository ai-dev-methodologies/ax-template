package ddd.fixtures.widget;
import jakarta.persistence.Entity;
import java.util.List;
import com.ax.template.authblueprint.common.AggregateRoot;
/** FIXTURE — deliberately violates HG-AGG-REF via a COLLECTION of another aggregate's root. */
@AggregateRoot
@Entity
public class CollectorRoot { Long id; List<WidgetRoot> manyOtherAggregates; }
