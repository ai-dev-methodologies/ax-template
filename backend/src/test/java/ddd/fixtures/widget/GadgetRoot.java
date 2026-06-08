package ddd.fixtures.widget;
import jakarta.persistence.Entity;
import com.ax.template.authblueprint.common.AggregateRoot;
/** FIXTURE — deliberately violates HG-AGG-REF (object pointer to ANOTHER aggregate root). */
@AggregateRoot
@Entity
public class GadgetRoot { Long id; WidgetRoot otherAggregate; }
