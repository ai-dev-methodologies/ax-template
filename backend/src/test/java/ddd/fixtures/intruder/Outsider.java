package ddd.fixtures.intruder;
import ddd.fixtures.widget.WidgetPart;
/** FIXTURE — deliberately violates HG-AGG-MEMBER-ENCAP (references a member of another feature). */
public class Outsider { WidgetPart leaked; }
