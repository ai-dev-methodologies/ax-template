package ddd.fixtures.widget;
import jakarta.persistence.Entity;
import com.ax.template.authblueprint.common.AggregateMember;
/** FIXTURE — a member of the WidgetRoot aggregate. */
@AggregateMember(root = WidgetRoot.class)
@Entity
public class WidgetPart { Long id; }
