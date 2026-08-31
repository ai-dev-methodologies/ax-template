<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd"><html xmlns="http://www.w3.org/1999/xhtml"><head><meta http-equiv="Content-Type" content="text/html; charset=us-ascii" /><meta http-equiv="Content-Type" content="text/html; charset=us-ascii" /><title>JEP 395: Records</title><link rel="shortcut icon" href="/images/nanoduke.ico" /><link rel="stylesheet" type="text/css" href="/page.css" /><script type="text/javascript" src="/page.js"><noscript></noscript></script><script src="https://cdn.usefathom.com/script.js" data-site="KCYJJPZX" defer="yes"></script><style type="text/css" xml:space="preserve">
      TABLE { border-collapse: collapse; padding: 0px; margin: 1em 0 1em 2em; }
      TR:first-child TH, TR:first-child TD { padding-top: 0; }
      TH, TD { padding: 0px; padding-top: .5ex; vertical-align: baseline; text-align: left; }
      TD + TD, TH + TH { padding-left: 1em; }
      TD:first-child, TH:first-child, TD.jep { text-align: right; }
      TABLE.head TD:first-child { font-style: italic; padding-left: 2em; white-space: nowrap; }
      PRE { padding-left: 2em; margin: 1ex 0; font-size: inherit; }
      TABLE PRE { padding-left: 0; margin: 0; }
      TABLE.jeps TD:first-child + TD,
      TABLE.jeps TD:first-child + TD + TD { padding-left: .5em; }
      TABLE.jeps TD:first-child,
      TABLE.jeps TD:first-child + TD,
      TABLE.jeps TD:first-child + TD + TD { font-size: smaller; }
      TABLE.jeps TD.cl { font-size: smaller; padding-right: 0; text-align: right; }
      TABLE.jeps TD.cm { font-size: smaller; padding-left: .1em; padding-right: .1em; }
      TABLE.jeps TD.cr { font-size: smaller; padding-left: 0; }
      TABLE.jeps TD.z { padding-left: 0; padding-right: 0; }
      TABLE.head TD { padding-top: 0; }
    </style></head><body><div id="main"><h1>JEP 395: Records</h1><table class="head"><tr><td>Owner</td><td>Gavin Bierman</td></tr><tr><td>Type</td><td>Feature</td></tr><tr><td>Scope</td><td>SE</td></tr><tr><td>Status</td><td>Closed&#8201;/&#8201;Delivered</td></tr><tr><td>Release</td><td>16</td></tr><tr><td>Component</td><td>specification&#8201;/&#8201;language</td></tr><tr><td>Discussion</td><td>amber dash dev at openjdk dot java dot net</td></tr><tr><td>Relates to</td><td><a href="359">JEP 359: Records (Preview)</a></td></tr><tr><td></td><td><a href="384">JEP 384: Records (Second Preview)</a></td></tr><tr><td>Reviewed by</td><td>Alex Buckley, Brian Goetz</td></tr><tr><td>Endorsed by</td><td>Brian Goetz</td></tr><tr><td>Created</td><td>2020/06/08 16:07</td></tr><tr><td>Updated</td><td>2024/02/03 01:28</td></tr><tr><td>Issue</td><td><a href="https://bugs.openjdk.org/browse/JDK-8246771">8246771</a></td></tr></table><div class="markdown"><h2 id="Summary">Summary</h2>
<p>Enhance the Java programming language with <a href="http://cr.openjdk.java.net/~briangoetz/amber/datum.html">records</a>, which are classes
that act as transparent carriers for immutable data. Records can be thought of
as <em>nominal tuples</em>.</p>
<h2 id="History">History</h2>
<p>Records were proposed by <a href="https://openjdk.java.net/jeps/359">JEP 359</a>
and delivered in <a href="https://openjdk.java.net/projects/jdk/14">JDK 14</a> as a
<a href="http://openjdk.java.net/jeps/12">preview feature</a>.</p>
<p>In response to feedback, the design was refined by
<a href="https://openjdk.java.net/jeps/384">JEP 384</a> and delivered in
<a href="https://openjdk.java.net/projects/jdk/15">JDK 15</a> as a
preview feature for a second time. The refinements for the second preview were as follows:</p>
<ul>
<li>
<p>In the first preview, canonical constructors were required to be <code>public</code>.
In the second preview, if the canonical constructor is
implicitly declared then its access modifier is the same as the record class; if
the canonical constructor is explicitly declared then its access modifier must provide
at least as much access as the record class.</p>
</li>
<li>
<p>The meaning of the <code>@Override</code> annotation was extended to include
the case where the annotated method is an explicitly declared accessor method for
a record component.</p>
</li>
<li>
<p>To enforce the intended use of compact constructors, it became a compile-time
error to assign to any of the instance fields in the constructor body.</p>
</li>
<li>
<p>The ability to declare local record classes, local enum classes, and local interfaces
was introduced.</p>
</li>
</ul>
<p>This JEP proposes to finalize the feature in JDK 16, with the following refinement:</p>
<ul>
<li>Relax the longstanding restriction whereby an inner class
cannot declare a member that is explicitly or implicitly static. This will become legal
and, in particular, will allow an inner class to declare a member that is a record class.</li>
</ul>
<p>Additional refinements may be incorporated based on further feedback.</p>
<h2 id="Goals">Goals</h2>
<ul>
<li>
<p>Devise an object-oriented construct that expresses a simple aggregation of
values.</p>
</li>
<li>
<p>Help developers to focus on modeling immutable data rather than extensible
behavior.</p>
</li>
<li>
<p>Automatically implement data-driven methods such as <code>equals</code> and accessors.</p>
</li>
<li>
<p>Preserve long-standing Java principles such as nominal typing and migration
compatibility.</p>
</li>
</ul>
<h2 id="Non-Goals">Non-Goals</h2>
<ul>
<li>
<p>While records do offer improved concision when declaring data carrier classes,
it is not a goal to declare a "war on boilerplate". In particular, it is not a
goal to address the problems of mutable classes which use the JavaBeans naming
conventions.</p>
</li>
<li>
<p>It is not a goal to add features such as properties or annotation-driven code
generation, which are often proposed to streamline the declaration of classes
for "Plain Old Java Objects".</p>
</li>
</ul>
<h2 id="Motivation">Motivation</h2>
<p>It is a common complaint that "Java is too verbose" or has "too much ceremony".
Some of the worst offenders are classes that are nothing more than immutable
<em>data carriers</em> for a handful of values. Properly writing such a data-carrier
class involves a lot of low-value, repetitive, error-prone code: constructors,
accessors, <code>equals</code>, <code>hashCode</code>, <code>toString</code>, etc. For example, a class to carry
x and y coordinates inevitably ends up like this:</p>
<pre><code>class Point {
    private final int x;
    private final int y;

    Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    int x() { return x; }
    int y() { return y; }

    public boolean equals(Object o) {
        if (!(o instanceof Point)) return false;
        Point other = (Point) o;
        return other.x == x &amp;&amp; other.y == y;
    }

    public int hashCode() {
        return Objects.hash(x, y);
    }

    public String toString() {
        return String.format("Point[x=%d, y=%d]", x, y);
    }
}</code></pre>
<p>Developers are sometimes tempted to cut corners by omitting methods such as
<code>equals</code>, leading to surprising behavior or poor debuggability, or by pressing
an alternate but not entirely appropriate class into service because it has the
"right shape" and they don't want to declare yet another class.</p>
<p>IDEs help us to <em>write</em> most of the code in a data-carrier class, but don't do
anything to help the <em>reader</em> distill the design intent of "I'm a data carrier
for <code>x</code> and <code>y</code>" from the dozens of lines of boilerplate.  Writing Java code
that models a handful of values should be easier to write, to read, and to
verify as correct.</p>
<p>While it is superficially tempting to treat records as primarily being about
boilerplate reduction, we instead choose a more semantic goal: <em>modeling data as
data</em>.  (If the semantics are right, the boilerplate will take care of itself.)
It should be easy and concise to declare data-carrier classes that <em>by default</em>
make their data immutable and provide idiomatic implementations of methods that
produce and consume the data.</p>
<h2 id="Description">Description</h2>
<p><em>Record classes</em> are a new kind of class in the Java language. Record classes
help to model plain data aggregates with less ceremony than normal classes.</p>
<p>The declaration of a record class primarily consists of a declaration of its
<em>state</em>; the record class then commits to an API that matches that state. This
means that record classes give up a freedom that classes usually enjoy
&#8212; the ability to decouple a class's API from its internal representation &#8212; but,
in return, record class declarations become significantly more concise.</p>
<p>More precisely, a record class declaration consists of a name, optional type
parameters, a header, and a body. The header
lists the <em>components</em> of the record class, which are the variables that make up its
state. (This list of components is sometimes referred to as the <em>state
description</em>.) For example:</p>
<pre><code>record Point(int x, int y) { }</code></pre>
<p>Because record classes make the semantic claim of being transparent carriers for
their data, a record class acquires many standard members automatically:</p>
<ul>
<li>
<p>For each component in the header, two members: a <code>public</code> accessor method with
the same name and return type as the component, and a <code>private</code> <code>final</code> field
with the same type as the component;</p>
</li>
<li>
<p>A <em>canonical constructor</em> whose signature is the same as the header, and which
assigns each private field to the corresponding argument from a <code>new</code>
expression which instantiates the record;</p>
</li>
<li>
<p><code>equals</code> and <code>hashCode</code> methods which ensure that two record values are equal
if they are of the same type and contain equal component values; and</p>
</li>
<li>
<p>A <code>toString</code> method that returns a string representation of all the record
components, along with their names.</p>
</li>
</ul>
<p>In other words, the header of a record class describes its state, i.e., the
types and names of its components, and the API is derived mechanically and
completely from that state description. The API includes protocols for
construction, member access, equality, and display. (We expect a future version
to support deconstruction patterns to allow powerful pattern matching.)</p>
<h3 id="Constructors-for-record-classes">Constructors for record classes</h3>
<p>The rules for constructors in a record class are different than in a normal
class. A normal class without any constructor declarations is automatically
given a
<a href="https://docs.oracle.com/javase/specs/jls/se14/html/jls-8.html#jls-8.8.9"><em>default constructor</em></a>.
In contrast, a record class without any constructor declarations is automatically
given a <em>canonical constructor</em> that assigns all the private fields to the
corresponding arguments of the <code>new</code> expression which instantiated the record.
For example, the record declared earlier &#8212; <code>record Point(int x, int y) { }</code> &#8212;
is compiled as if it were:</p>
<pre><code>record Point(int x, int y) {
    // Implicitly declared fields
    private final int x;
    private final int y;

    // Other implicit declarations elided ...

    // Implicitly declared canonical constructor
    Point(int x, int y) {
        this.x = x;
        this.y = y;
    }
}</code></pre>
<p>The canonical constructor may be declared explicitly with a list of formal
parameters which match the record header, as shown above.  It may also be
declared more compactly, by eliding the list of formal parameters.  In such a
<em>compact canonical constructor</em> the parameters are declared implicitly, and the
private fields corresponding to record components cannot be assigned in the
body but are automatically assigned to the corresponding formal parameter
(<code>this.x = x;</code>) at the end of the constructor. The compact form helps
developers focus on validating and normalizing parameters without the tedious
work of assigning parameters to fields.</p>
<p>For example, here is a compact canonical constructor that validates its implicit
formal parameters:</p>
<pre><code>record Range(int lo, int hi) {
    Range {
        if (lo &gt; hi)  // referring here to the implicit constructor parameters
            throw new IllegalArgumentException(String.format("(%d,%d)", lo, hi));
    }
}</code></pre>
<p>Here is a compact canonical constructor that normalizes its formal parameters:</p>
<pre><code>record Rational(int num, int denom) {
    Rational {
        int gcd = gcd(num, denom);
        num /= gcd;
        denom /= gcd;
    }
}</code></pre>
<p>This declaration is equivalent to the conventional constructor form:</p>
<pre><code>record Rational(int num, int denom) {
    Rational(int num, int demon) {
        // Normalization
        int gcd = gcd(num, denom);
        num /= gcd;
        denom /= gcd;
        // Initialization
        this.num = num;
        this.denom = denom;
    }
}</code></pre>
<p>Record classes with implicitly declared constructors and methods satisfy
important, and intuitive, semantic properties. For example, consider a record
class <code>R</code> declared as follows:</p>
<pre><code>record R(T1 c1, ..., Tn cn){ }</code></pre>
<p>If an instance <code>r1</code> of <code>R</code> is copied in the following way:</p>
<pre><code>R r2 = new R(r1.c1(), r1.c2(), ..., r1.cn());</code></pre>
<p>then, assuming <code>r1</code> is not the null reference, it is <em>always</em> the case that the
expression <code>r1.equals(r2)</code> will evaluate to <code>true</code>. Explicitly declared accessor
and <code>equals</code> methods should respect this invariant. However, it is not generally
possible for a compiler to check that explicitly declared methods respect this
invariant.</p>
<p>As an example, the following declaration of a record class should be considered
bad style because its accessor methods "silently" adjust the state of a record
instance, and the invariant above is not satisfied:</p>
<pre><code>record SmallPoint(int x, int y) {
  public int x() { return this.x &lt; 100 ? this.x : 100; }
  public int y() { return this.y &lt; 100 ? this.y : 100; }
}</code></pre>
<p>In addition, for all record classes the implicitly declared <code>equals</code> method is implemented so that it is
reflexive and that it behaves consistently with <code>hashCode</code> for record classes
that have floating point components. Again, explicitly declared <code>equals</code> and
<code>hashCode</code> methods should behave similarly.</p>
<h3 id="Rules-for-record-classes">Rules for record classes</h3>
<p>There are numerous restrictions on the declaration of a record class in
comparison to a normal class:</p>
<ul>
<li>
<p>A record class declaration does not have an <code>extends</code> clause. The superclass
of a record class is always <code>java.lang.Record</code>, similar to how the superclass of
an enum class is always <code>java.lang.Enum</code>. Even though a normal class can explicitly
extend its implicit superclass <code>Object</code>, a record cannot explicitly extend any
class, even its implicit superclass <code>Record</code>.</p>
</li>
<li>
<p>A record class is implicitly <code>final</code>, and cannot be <code>abstract</code>. These
restrictions emphasize that the API of a record class is defined solely by its
state description, and cannot be enhanced later by another class.</p>
</li>
<li>
<p>The fields derived from the record components are <code>final</code>. This restriction
embodies an <em>immutable by default</em> policy that is widely applicable for
data-carrier classes.</p>
</li>
<li>
<p>A record class cannot explicitly declare instance fields, and cannot contain
instance initializers. These restrictions ensure that the record header alone
defines the state of a record value.</p>
</li>
<li>
<p>Any explicit declarations of a member that would otherwise be automatically
derived must match the type of the automatically derived member exactly,
disregarding any annotations on the explicit declaration. Any explicit
implementation of accessors or the <code>equals</code> or <code>hashCode</code> methods should be
careful to preserve the semantic invariants of the record class.</p>
</li>
<li>
<p>A record class cannot declare <code>native</code> methods. If a record class could
declare a <code>native</code> method then the behavior of the record class would by
definition depend on external state rather than the record class's explicit
state. No class with native methods is likely to be a good candidate for
migration to a record.</p>
</li>
</ul>
<p>Beyond the restrictions above, a record class behaves like a normal class:</p>
<ul>
<li>
<p>Instances of record classes are created using a <code>new</code> expression.</p>
</li>
<li>
<p>A record class can be declared top level or nested, and can be generic.</p>
</li>
<li>
<p>A record class can declare <code>static</code> methods, fields, and initializers.</p>
</li>
<li>
<p>A record class can declare instance methods.</p>
</li>
<li>
<p>A record class can implement interfaces. A record class cannot specify a
superclass since that would mean inherited state, beyond the state described in
the header.  A record class can, however, freely specify superinterfaces and
declare instance methods to implement them. Just as for classes, an interface
can usefully characterize the behavior of many records. The behavior may be
domain-independent (e.g., <code>Comparable</code>) or domain-specific, in which case
records can be part of a <em>sealed</em> hierarchy which captures the domain (see
below).</p>
</li>
<li>
<p>A record class can declare nested types, including nested record classes. If a
record class is itself nested, then it is implicitly static; this avoids an
immediately enclosing instance which would silently add state to the record
class.</p>
</li>
<li>
<p>A record class, and the components in its header, may be decorated with
annotations. Any annotations on the record components are propagated to the
automatically derived fields, methods, and constructor parameters, according to
the set of applicable targets for the annotation. Type annotations on the types
of record components are also propagated to the corresponding type uses in the
automatically derived members.</p>
</li>
<li>
<p>Instances of record classes can be serialized and deserialized. However, the
process cannot be customized by providing <code>writeObject</code>, <code>readObject</code>,
<code>readObjectNoData</code>, <code>writeExternal</code>, or <code>readExternal</code> methods. The components
of a record class govern serialization, while the canonical constructor of a
record class governs deserialization.</p>
</li>
</ul>
<h3 id="Local-record-classes">Local record classes</h3>
<p>A program that produces and consumes instances of a record class is likely to
deal with many intermediate values that are themselves simple groups of
variables. It will often be convenient to declare record classes to model those
intermediate values. One option is to declare "helper" record classes that are static
and nested, much as many programs declare helper classes today. A more
convenient option would be to declare a record <em>inside a method</em>, close to the
code which manipulates the variables. Accordingly we define <em>local
record classes</em>, akin to the existing construct of
<a href="https://docs.oracle.com/javase/specs/jls/se14/html/jls-14.html#jls-14.3">local classes</a>.</p>
<p>In the following example, the aggregation of a merchant and a monthly sales
figure is modeled with a local record class, <code>MerchantSales</code>. Using this record class
improves the readability of the stream operations which follow:</p>
<pre><code>List&lt;Merchant&gt; findTopMerchants(List&lt;Merchant&gt; merchants, int month) {
    // Local record
    record MerchantSales(Merchant merchant, double sales) {}

    return merchants.stream()
        .map(merchant -&gt; new MerchantSales(merchant, computeSales(merchant, month)))
        .sorted((m1, m2) -&gt; Double.compare(m2.sales(), m1.sales()))
        .map(MerchantSales::merchant)
        .collect(toList());
}</code></pre>
<p>Local record classes are a particular case of nested record classes. Like nested
record classes, local record classes are <em>implicitly static</em>. This means that
their own methods cannot access any variables of the enclosing method; in turn,
this avoids capturing an immediately enclosing instance which would silently add
state to the record class. The fact that local record classes are implicitly
static is in contrast to local classes, which are not implicitly static. In
fact, local classes are never static &#8212; implicitly or explicitly &#8212; and can
always access variables in the enclosing method.</p>
<h3 id="Local-enum-classes-and-local-interfaces">Local enum classes and local interfaces</h3>
<p>The addition of local record classes is an opportunity to add other kinds of
implicitly-static local declarations.</p>
<p>Nested enum classes and nested interfaces are already implicitly static, so
for consistency we define local enum classes and local interfaces, which are
also implicitly static.</p>
<h3 id="Static-members-of-inner-classes">Static members of inner classes</h3>
<p>It is
<a href="https://docs.oracle.com/javase/specs/jls/se14/html/jls-8.html#jls-8.1.3">currently specified</a>
to be a compile-time error if an inner class declares a member that is explicitly
or implicitly static, unless the member is a constant variable. This means that,
for example, an inner class cannot declare a record class member, since nested
record classes are implicitly static.</p>
<p>We relax this restriction in order to allow an inner class
to declare members that are either explicitly or implicitly static. In
particular, this allows an inner class to declare a static member that is a
record class.</p>
<h3 id="Annotations-on-record-components">Annotations on record components</h3>
<p>Record components have multiple roles in record declarations.  A record
component is a first-class concept, but each component also corresponds to a
field of the same name and type, an accessor method of the same name and return
type, and a formal parameter of the canonical constructor of the same name and
type.</p>
<p>This raises the question: When a component is annotated, what actually is being
annotated?  The answer is, "all of the elements to which this particular
annotation is applicable."  This enables classes that use annotations on their
fields, constructor parameters, or accessor methods to be migrated to records
without having to redundantly declare these members.  For example, a class such
as the following</p>
<pre><code>public final class Card {
    private final @MyAnno Rank rank;
    private final @MyAnno Suit suit;
    @MyAnno Rank rank() { return this.rank; }
    @MyAnno Suit suit() { return this.suit; }
    ...
}</code></pre>
<p>can be migrated to the equivalent, and considerably more readable, record declaration:</p>
<pre><code>public record Card(@MyAnno Rank rank, @MyAnno Suit suit) { ... }</code></pre>
<p>The applicability of an annotation is declared using a <code>@Target</code> meta-annotation.
Consider the following:</p>
<pre><code>@Target(ElementType.FIELD)
    public @interface I1 {...}</code></pre>
<p>This declares the annotation <code>@I1</code> that it is applicable to field
declarations. We can declare that an annotation is applicable to more than one
declaration; for example:</p>
<pre><code>@Target({ElementType.FIELD, ElementType.METHOD})
    public @interface I2 {...}</code></pre>
<p>This declares an annotation <code>@I2</code> that it is applicable to both field
declarations and method declarations.</p>
<p>Returning to annotations on a record component, these annotations appear at the
corresponding program points where they are applicable. In other words, the
propagation is under the control of the developer using the <code>@Target</code>
meta-annotation. The propagation rules are systematic and intuitive, and all
that apply are followed:</p>
<ul>
<li>
<p>If an annotation on a record component is applicable to a field declaration,
then the annotation appears on the corresponding private field.</p>
</li>
<li>
<p>If an annotation on a record component is applicable to a method declaration,
then the annotation appears on the corresponding accessor method.</p>
</li>
<li>
<p>If an annotation on a record component is applicable to a formal parameter,
then the annotation appears on the corresponding formal parameter of the canonical
constructor if one is not declared explicitly, or else to the corresponding formal
parameter of the compact constructor if one is declared explicitly.</p>
</li>
<li>
<p>If an annotation on a record component is applicable to a type, the annotation
will be propagated to all of the following:</p>
<ul>
<li>the type of the corresponding field</li>
<li>the return type of the corresponding accessor method</li>
<li>the type of the corresponding formal parameter of the canonical constructor</li>
<li>the type of the record component (which is accessible at runtime via reflection)</li>
</ul>
</li>
</ul>
<p>If a public accessor method or (non-compact) canonical constructor is declared
explicitly, then it only has the annotations which appear on it directly;
nothing is propagated from the corresponding record component to these members.</p>
<p>A declaration annotation on a record component will not be amongst those associated
with the record component at run time via the
<a href="#Reflection-API">reflection API</a> unless the annotation is meta-annotated with
<code>@Target(RECORD_COMPONENT)</code>.</p>
<h3 id="Compatibility-and-migration">Compatibility and migration</h3>
<p>The abstract class <code>java.lang.Record</code> is the common superclass of all record
classes. Every Java source file implicitly imports the <code>java.lang.Record</code> class,
as well as all other types in the <code>java.lang</code> package, regardless of whether you
enable or disable preview features. However, if your application imports
another class named <code>Record</code> from a different package, you might get a compiler
error.</p>
<p>Consider the following class declaration of <code>com.myapp.Record</code>:</p>
<pre><code>package com.myapp;

public class Record {
    public String greeting;
    public Record(String greeting) {
        this.greeting = greeting;
    }
}</code></pre>
<p>The following example, <code>org.example.MyappPackageExample</code>, imports
<code>com.myapp.Record</code> with a wildcard but doesn't compile:</p>
<pre><code>package org.example;
import com.myapp.*;

public class MyappPackageExample {
    public static void main(String[] args) {
       Record r = new Record("Hello world!");
    }
}</code></pre>
<p>The compiler generates an error message similar to the following:</p>
<pre><code>./org/example/MyappPackageExample.java:6: error: reference to Record is ambiguous
       Record r = new Record("Hello world!");
       ^
  both class com.myapp.Record in com.myapp and class java.lang.Record in java.lang match

./org/example/MyappPackageExample.java:6: error: reference to Record is ambiguous
       Record r = new Record("Hello world!");
                      ^
  both class com.myapp.Record in com.myapp and class java.lang.Record in java.lang match</code></pre>
<p>Both <code>Record</code> in the <code>com.myapp</code> package and <code>Record</code> in the <code>java.lang</code> package
are imported with wildcards. Consequently, neither class takes precedence, and
the compiler generates an error message when it encounters the use of the simple
name <code>Record</code>.</p>
<p>To enable this example to compile, the <code>import</code> statement can be changed so that
it imports the fully qualified name of <code>Record</code>:</p>
<pre><code>import com.myapp.Record;</code></pre>
<p>The introduction of classes in the <code>java.lang</code> package is rare but sometimes
necessary.  Previous examples are <code>Enum</code> in Java&#160;5, <code>Module</code> in
Java&#160;9, and <code>Record</code> in Java&#160;14.</p>
<h3 id="Java-grammar">Java grammar</h3>
<pre><code>RecordDeclaration:
  {ClassModifier} `record` TypeIdentifier [TypeParameters]
    RecordHeader [SuperInterfaces] RecordBody

RecordHeader:
 `(` [RecordComponentList] `)`

RecordComponentList:
 RecordComponent { `,` RecordComponent}

RecordComponent:
 {Annotation} UnannType Identifier
 VariableArityRecordComponent

VariableArityRecordComponent:
 {Annotation} UnannType {Annotation} `...` Identifier

RecordBody:
  `{` {RecordBodyDeclaration} `}`

RecordBodyDeclaration:
  ClassBodyDeclaration
  CompactConstructorDeclaration

CompactConstructorDeclaration:
  {ConstructorModifier} SimpleTypeName ConstructorBody</code></pre>
<h3 id="Class-file-representation">Class-file representation</h3>
<p>The <code>class</code> file of a record uses a <code>Record</code> attribute to store information
about the record's components:</p>
<pre><code>Record_attribute {
    u2 attribute_name_index;
    u4 attribute_length;
    u2 components_count;
    record_component_info components[components_count];
}

record_component_info {
    u2 name_index;
    u2 descriptor_index;
    u2 attributes_count;
    attribute_info attributes[attributes_count];
}</code></pre>
<p>If the record component has a generic signature that is different from the
erased descriptor then there must be a <code>Signature</code> attribute in the
<code>record_component_info</code> structure.</p>
<h3 id="Reflection-API">Reflection API</h3>
<p>We add two public methods to <code>java.lang.Class</code>:</p>
<ul>
<li>
<p><code>RecordComponent[] getRecordComponents()</code> &#8212; Returns an array of
<code>java.lang.reflect.RecordComponent</code> objects. The elements of this array
correspond to the record's components, in the same order as they appear in the
record declaration. Additional information can be extracted from each element of
the array, including its name, annotations, and accessor method.</p>
</li>
<li>
<p><code>boolean isRecord()</code> &#8212; Returns true if the given class was declared as a
record.  (Compare with <code>isEnum</code>.)</p>
</li>
</ul>
<h2 id="Alternatives">Alternatives</h2>
<p>Record classes can be considered a nominal form of <em>tuples</em>. Instead of record
classes, we could implement structural tuples. However, while tuples might offer
a lightweight means of expressing some aggregates, the result is often inferior
aggregates:</p>
<ul>
<li>
<p>A central aspect of Java's design philosophy is that <em>names matter</em>. Classes
and their members have meaningful names, while tuples and tuple components do
not. That is, a <code>Person</code> record class with components <code>firstName</code> and <code>lastName</code> is
clearer and safer than an anonymous tuple of two strings.</p>
</li>
<li>
<p>Classes allow for state validation through their constructors; tuples typically
do not. Some data aggregates (such as numeric ranges) have invariants that, if
enforced by the constructor, can thereafter be relied upon. Tuples do not offer
this ability.</p>
</li>
<li>
<p>Classes can have behavior that is based on their state; co-locating the state
and behavior makes the behavior more discoverable and easier to access. Tuples,
being raw data, offer no such facility.</p>
</li>
</ul>
<h2 id="Dependencies">Dependencies</h2>
<p>Record classes work well with another feature currently in preview, namely <em>sealed
classes</em> (<a href="https://openjdk.java.net/jeps/360">JEP 360</a>). For example, a family
of record classes can be explicitly declared to implement the same sealed
interface:</p>
<pre><code>package com.example.expression;

public sealed interface Expr
    permits ConstantExpr, PlusExpr, TimesExpr, NegExpr {...}

public record ConstantExpr(int i)       implements Expr {...}
public record PlusExpr(Expr a, Expr b)  implements Expr {...}
public record TimesExpr(Expr a, Expr b) implements Expr {...}
public record NegExpr(Expr e)           implements Expr {...}</code></pre>
<p>The combination of record classes and sealed classes is sometimes referred to as
<a href="https://en.wikipedia.org/wiki/Algebraic_data_type"><em>algebraic data types</em></a>.
Record classes allow us to express <em>products</em>, and sealed classes allow us to
express <em>sums</em>.</p>
<p>In addition to the combination of record classes and sealed classes, record
classes lend themselves naturally to <a href="https://cr.openjdk.java.net/~briangoetz/amber/pattern-match.html">pattern matching</a>.  Because
record classes couple their API to their state description, we will eventually
be able to derive deconstruction patterns for record classes as well, and use
sealed type information to determine exhaustiveness in <code>switch</code> expressions with
type patterns or deconstruction patterns.</p>
</div></div><div id="sidebar"><div id="openjdk-sidebar-logo"><a href="/"><img alt="OpenJDK logo" width="91" height="25" src="/images/openjdk2.svg" /></a></div><div class="links"><div class="link"><a href="/install/">Installing</a></div><div class="link"><a href="/guide/#contributing-to-an-openjdk-project">Contributing</a></div><div class="link"><a href="/guide/#reviewing-and-sponsoring-a-change">Sponsoring</a></div><div class="link"><a href="/guide/">Developers' Guide</a></div><div class="link"><a href="/groups/vulnerability/report">Vulnerabilities</a></div><div class="link"><a href="https://jdk.java.net">JDK GA/EA Builds</a></div></div><div class="links"><div class="links"><a href="https://mail.openjdk.org">Mailing lists</a></div><div class="link"><a href="https://wiki.openjdk.org">Wiki</a>
                      &#183; <a href="/irc">IRC</a></div><div class="link"><a href="https://mastodon.social/@openjdk" rel="me">Mastodon</a></div><div class="link"><a href="https://bsky.app/profile/openjdk.org">Bluesky</a></div></div><div class="links"><div class="links"><a href="/bylaws">Bylaws</a> &#183; <a href="/census">Census</a></div><div class="link"><a href="/legal/">Legal</a> &#183; <a href="/legal/ai">AI</a></div></div><div class="links"><div class="links"><a href="/workshop"><b>Workshop</b></a></div></div><div class="links"><div class="links"><a href="/jeps/0"><b>JEP Process</b></a></div></div><div class="links"><div class="about">Source code</div><div class="link"><a href="https://github.com/openjdk/">GitHub</a></div><div class="link"><a href="https://hg.openjdk.org">Mercurial</a></div></div><div class="links"><div class="about">Tools</div><div class="link"><a href="http://git-scm.org/">Git</a></div><div class="link"><a href="/jtreg/">jtreg harness</a></div></div><div class="links"><div class="about">Groups</div><div class="link"><a href="/groups/">(overview</a>,
      <a href="/groups/archive">archive</a>)</div><div class="link"><a href="/groups/adoption">Adoption</a></div><div class="link"><a href="/groups/build">Build</a></div><div class="link"><a href="/groups/client-libs">Client Libraries</a></div><div class="link"><a href="/groups/csr">Compatibility &amp; Specification Review</a></div><div class="link"><a href="/groups/compiler">Compiler</a></div><div class="link"><a href="/groups/conformance">Conformance</a></div><div class="link"><a href="/groups/core-libs">Core Libraries</a></div><div class="link"><a href="/groups/gb">Governing Board</a></div><div class="link"><a href="/groups/hotspot">HotSpot</a></div><div class="link"><a href="/groups/ide-support">IDE Tooling &amp; Support</a></div><div class="link"><a href="/groups/i18n">Internationalization</a></div><div class="link"><a href="/groups/members">Members</a></div><div class="link"><a href="/groups/net">Networking</a></div><div class="link"><a href="/groups/porters">Porters</a></div><div class="link"><a href="/groups/quality">Quality</a></div><div class="link"><a href="/groups/security">Security</a></div><div class="link"><a href="/groups/serviceability">Serviceability</a></div><div class="link"><a href="/groups/vulnerability">Vulnerability</a></div><div class="link"><a href="/groups/web">Web</a></div></div><div class="links"><div class="about">Projects</div><div class="link">(<a href="/projects/">overview</a>,
      <a href="/projects/archive">archive</a>)</div><div class="link"><a href="/projects/amber">Amber</a></div><div class="link"><a href="/projects/babylon">Babylon</a></div><div class="link"><a href="/projects/brisbane">Brisbane</a></div><div class="link"><a href="/projects/crac">CRaC</a></div><div class="link"><a href="/projects/code-tools">Code Tools</a></div><div class="link"><a href="/projects/coin">Coin</a></div><div class="link"><a href="/projects/cvmi">Common VM Interface</a></div><div class="link"><a href="/projects/detroit">Detroit</a></div><div class="link"><a href="/projects/guide">Developers' Guide</a></div><div class="link"><a href="/projects/duke">Duke</a></div><div class="link"><a href="/projects/icedtea">IcedTea</a></div><div class="link"><a href="/projects/jdk8u">JDK 8 Updates</a></div><div class="link"><a href="/projects/jdk9">JDK 9</a></div><div class="link"><a href="/projects/jdk">JDK</a>
      (&#8230;,
       <a href="/projects/jdk/26">26</a>,
       <a href="/projects/jdk/27">27</a>,
       <a href="/projects/jdk/28">28</a>)</div><div class="link"><a href="/projects/jdk-updates">JDK Updates</a></div><div class="link"><a href="/projects/jmc">JMC</a></div><div class="link"><a href="/projects/jigsaw">Jigsaw</a></div><div class="link"><a href="/projects/lanai">Lanai</a></div><div class="link"><a href="/projects/leyden">Leyden</a></div><div class="link"><a href="/projects/lilliput">Lilliput</a></div><div class="link"><a href="/projects/loom">Loom</a></div><div class="link"><a href="/projects/jmm">Memory Model Update</a></div><div class="link"><a href="/projects/mlvm">Multi-Language VM</a></div><div class="link"><a href="/projects/nashorn">Nashorn</a></div><div class="link"><a href="/projects/nio">New I/O</a></div><div class="link"><a href="/projects/openjfx">OpenJFX</a></div><div class="link"><a href="/projects/panama">Panama</a></div><div class="link"><a href="/projects/aarch32-port">Port: AArch32</a></div><div class="link"><a href="/projects/aarch64-port">Port: AArch64</a></div><div class="link"><a href="/projects/bsd-port">Port: BSD</a></div><div class="link"><a href="/projects/haiku-port">Port: Haiku</a></div><div class="link"><a href="/projects/mips-port">Port: MIPS</a></div><div class="link"><a href="/projects/mobile">Port: Mobile</a></div><div class="link"><a href="/projects/ppc-aix-port">Port: PowerPC/AIX</a></div><div class="link"><a href="/projects/riscv-port">Port: RISC-V</a></div><div class="link"><a href="/projects/s390x-port">Port: s390x</a></div><div class="link"><a href="/projects/sctp">SCTP</a></div><div class="link"><a href="/projects/shenandoah">Shenandoah</a></div><div class="link"><a href="/projects/skara">Skara</a></div><div class="link"><a href="/projects/sumatra">Sumatra</a></div><div class="link"><a href="/projects/tsan">Tsan</a></div><div class="link"><a href="/projects/valhalla">Valhalla</a></div><div class="link"><a href="/projects/wakefield">Wakefield</a></div><div class="link"><a href="/projects/zero">Zero</a></div><div class="link"><a href="/projects/zgc">ZGC</a></div></div><div class="buttons"><a href="https://oracle.com"><img alt="Oracle logo" width="100" height="13" src="/images/oracle.svg" /></a></div></div><div id="footer">

        &#169; 2026 Oracle Corporation and/or its affiliates
        <br /><a href="/legal/tou/">Terms of Use</a>
        &#183;
        
            License: <a href="/legal/gplv2+ce.html">GPLv2</a>
        &#183; <a href="https://www.oracle.com/us/legal/privacy/">Privacy</a>
        &#183; <a href="https://openjdk.org/legal/openjdk-trademark-notice.html">Trademarks</a></div><script type="text/javascript"  src="/yzlOw9/77m/2I7/PfCttw/Ju9LmNND3twXG8b3OS/Zxs-fQE/VT5ZdVR/kQUwB"></script></body></html>
