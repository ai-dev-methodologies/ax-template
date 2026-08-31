<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd"><html xmlns="http://www.w3.org/1999/xhtml"><head><meta http-equiv="Content-Type" content="text/html; charset=us-ascii" /><meta http-equiv="Content-Type" content="text/html; charset=us-ascii" /><title>JEP 409: Sealed Classes</title><link rel="shortcut icon" href="/images/nanoduke.ico" /><link rel="stylesheet" type="text/css" href="/page.css" /><script type="text/javascript" src="/page.js"><noscript></noscript></script><script src="https://cdn.usefathom.com/script.js" data-site="KCYJJPZX" defer="yes"></script><style type="text/css" xml:space="preserve">
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
    </style></head><body><div id="main"><h1>JEP 409: Sealed Classes</h1><table class="head"><tr><td>Owner</td><td>Gavin Bierman</td></tr><tr><td>Type</td><td>Feature</td></tr><tr><td>Scope</td><td>SE</td></tr><tr><td>Status</td><td>Closed&#8201;/&#8201;Delivered</td></tr><tr><td>Release</td><td>17</td></tr><tr><td>Component</td><td>specification&#8201;/&#8201;language</td></tr><tr><td>Discussion</td><td>amber dash dev at openjdk dot java dot net</td></tr><tr><td>Relates to</td><td><a href="360">JEP 360: Sealed Classes (Preview)</a></td></tr><tr><td></td><td><a href="397">JEP 397: Sealed Classes (Second Preview)</a></td></tr><tr><td>Reviewed by</td><td>Alex Buckley</td></tr><tr><td>Endorsed by</td><td>Brian Goetz</td></tr><tr><td>Created</td><td>2021/01/27 13:40</td></tr><tr><td>Updated</td><td>2024/01/03 01:48</td></tr><tr><td>Issue</td><td><a href="https://bugs.openjdk.org/browse/JDK-8260514">8260514</a></td></tr></table><div class="markdown"><h2 id="Summary">Summary</h2>
<p>Enhance the Java programming language with
<a href="https://cr.openjdk.java.net/~briangoetz/amber/datum.html">sealed classes and interfaces</a>.
Sealed classes and interfaces restrict which other classes or interfaces may extend or
implement them.</p>
<h2 id="History">History</h2>
<p>Sealed Classes were proposed by <a href="https://openjdk.java.net/jeps/360">JEP 360</a> and
delivered in <a href="https://openjdk.java.net/projects/jdk/15">JDK 15</a> as a
<a href="https://openjdk.java.net/jeps/12">preview feature</a>. They were proposed again, with refinements,
by <a href="https://openjdk.java.net/jeps/397">JEP 397</a> and delivered in
<a href="https://openjdk.java.net/projects/jdk/16">JDK 16</a> as a preview feature.
This JEP proposes to finalize Sealed Classes in JDK 17, with no changes from JDK 16.</p>
<h2 id="Goals">Goals</h2>
<ul>
<li>
<p>Allow the author of a class or interface to control which code is responsible
for implementing it.</p>
</li>
<li>
<p>Provide a more declarative way than access modifiers to restrict the use of a
superclass.</p>
</li>
<li>
<p>Support future directions in <a href="https://cr.openjdk.java.net/~briangoetz/amber/pattern-match.html">pattern matching</a> by
providing a foundation for the exhaustive analysis of patterns.</p>
</li>
</ul>
<h2 id="Non-Goals">Non-Goals</h2>
<ul>
<li>
<p>It is not a goal to provide new forms of access control such as "friends".</p>
</li>
<li>
<p>It is not a goal to change <code>final</code> in any way.</p>
</li>
</ul>
<h2 id="Motivation">Motivation</h2>
<p>The object-oriented data model of inheritance hierarchies of classes and
interfaces has proven to be highly effective in modeling the real-world data
processed by modern applications. This expressiveness is an important aspect of
the Java language.</p>
<p>There are, however, cases where such expressiveness can usefully be tamed. For
example, Java supports <em>enum classes</em> to model the situation where a given class
has only a fixed number of instances. In the following code, an enum class lists
a fixed set of planets. They are the only values of the class, therefore you can
switch over them exhaustively &#8212; without having to write a <code>default</code> clause:</p>
<pre><code>enum Planet { MERCURY, VENUS, EARTH }

Planet p = ...
switch (p) {
  case MERCURY: ...
  case VENUS: ...
  case EARTH: ...
}</code></pre>
<p>Using enum classes to model fixed sets of values is often helpful, but sometimes
we want to model a fixed set of <em>kinds</em> of values. We can do this by using a
class hierarchy not as a mechanism for code inheritance and reuse but, rather,
as a way to list kinds of values. Building on our planetary example, we might
model the kinds of values in the astronomical domain as follows:</p>
<pre><code>interface Celestial { ... }
final class Planet implements Celestial { ... }
final class Star   implements Celestial { ... }
final class Comet  implements Celestial { ... }</code></pre>
<p>This hierarchy does not, however, reflect the important domain knowledge that
there are only three kinds of celestial objects in our model. In these
situations, restricting the set of subclasses or subinterfaces can streamline
the modeling.</p>
<p>Consider another example: In a graphics library, the author of a class <code>Shape</code>
may intend that only particular classes can extend <code>Shape</code>, since much of the
library's work involves handling each kind of shape in the appropriate way. The
author is interested in the clarity of code that handles known subclasses of
<code>Shape</code>, and not interested in writing code to defend against unknown
subclasses of <code>Shape</code>. Allowing arbitrary classes to extend <code>Shape</code>, and thus
inherit its code for reuse, is not a goal in this case. Unfortunately, Java
assumes that code reuse is always a goal: If <code>Shape</code> can be extended at all,
then it can be extended by any number of classes. It would be helpful to relax
this assumption so that an author can declare a class hierarchy that is not open
for extension by arbitrary classes. Code reuse would still be possible within
such a closed class hierarchy, but not beyond.</p>
<p>Java developers are familiar with the idea of restricting the set of subclasses
because it often crops up in API design. The language provides limited tools in
this area: Either make a class <code>final</code>, so it has zero subclasses, or make the
class or its constructor package-private, so it can only have subclasses in the
same package. An example of a package-private superclass
<a href="https://github.com/openjdk/jdk/tree/master/src/java.base/share/classes/java/lang">appears in the JDK</a>:</p>
<pre><code>package java.lang;

abstract class AbstractStringBuilder { ... }
public final class StringBuffer  extends AbstractStringBuilder { ... }
public final class StringBuilder extends AbstractStringBuilder { ... }</code></pre>
<p>The package-private approach is useful when the goal is code reuse, such as
having the subclasses of <code>AbstractStringBuilder</code> share its code for <code>append</code>.
However, the approach is useless when the goal is modeling alternatives, since
user code cannot access the key abstraction &#8212; the superclass &#8212; in order to
<code>switch</code> over it. Allowing users to access the superclass without also allowing
them to extend it cannot be specified without resorting to brittle tricks
involving non-<code>public</code> constructors &#8212; which do not work for interfaces. In a
graphics library that declares <code>Shape</code> and its subclasses, it would be
unfortunate if only one package could access <code>Shape</code>.</p>
<p>In summary, it should be possible for a superclass to be widely <em>accessible</em>
(since it represents an important abstraction for users) but not widely
<em>extensible</em> (since its subclasses should be restricted to those known to the
author). The author of such a superclass should be able to express that it is
co-developed with a given set of subclasses, both to document intent for the
reader and to allow enforcement by the Java compiler. At the same time, the
superclass should not unduly constrain its subclasses by, e.g., forcing them to
be <code>final</code> or preventing them from defining their own state.</p>
<h2 id="Description">Description</h2>
<p>A <em>sealed</em> class or interface can be extended or implemented only by those
classes and interfaces permitted to do so.</p>
<p>A class is sealed by applying the <code>sealed</code> modifier to its declaration. Then,
after any <code>extends</code> and <code>implements</code> clauses, the <code>permits</code> clause specifies the
classes that are permitted to extend the sealed class. For example, the
following declaration of <code>Shape</code> specifies three permitted subclasses:</p>
<pre><code>package com.example.geometry;

public abstract sealed class Shape
    permits Circle, Rectangle, Square { ... }</code></pre>
<p>The classes specified by <code>permits</code> must be located near the superclass: either
in the same module (if the superclass is in a named module) or in the same
package (if the superclass is in the unnamed module). For example, in the
following declaration of <code>Shape</code> its permitted subclasses are all located in
different packages of the same named module:</p>
<pre><code>package com.example.geometry;

public abstract sealed class Shape 
    permits com.example.polar.Circle,
            com.example.quad.Rectangle,
            com.example.quad.simple.Square { ... }</code></pre>
<p>When the permitted subclasses are small in size and number, it may be convenient
to declare them in the same source file as the sealed class. When they are
declared in this way, the <code>sealed</code> class may omit the <code>permits</code> clause and the
Java compiler will infer the permitted subclasses from the declarations in the
source file. (The subclasses may be auxiliary or nested classes.) For example,
if the following code is found in <code>Root.java</code> then the sealed class
<code>Root</code> is inferred to have three permitted subclasses:</p>
<pre><code>abstract sealed class Root { ... 
    final class A extends Root { ... }
    final class B extends Root { ... }
    final class C extends Root { ... }
}</code></pre>
<p>Classes specified by <code>permits</code> must have a canonical name, otherwise a
compile-time error is reported. This means that anonymous classes and local
classes cannot be permitted subtypes of a sealed class.</p>
<p>A sealed class imposes three constraints on its permitted subclasses:</p>
<ol>
<li>
<p>The sealed class and its permitted subclasses must belong to the same module,
and, if declared in an unnamed module, to the same package.</p>
</li>
<li>
<p>Every permitted subclass must directly extend the sealed class.</p>
</li>
<li>
<p>Every permitted subclass must use a modifier to describe how it propagates
the sealing initiated by its superclass:</p>
<ul>
<li>
<p>A permitted subclass may be declared <code>final</code> to prevent its part of the
class hierarchy from being extended further.
(<a href="https://docs.oracle.com/javase/specs/jls/se16/html/jls-8.html#jls-8.10">Record classes</a> are implicitly declared <code>final</code>.)</p>
</li>
<li>
<p>A permitted subclass may be declared <code>sealed</code> to allow its part of the
hierarchy to be extended further than envisaged by its sealed superclass,
but in a restricted fashion.</p>
</li>
<li>
<p>A permitted subclass may be declared <code>non-sealed</code> so that its part of the
hierarchy reverts to being open for extension by unknown subclasses. A
sealed class cannot prevent its permitted subclasses from doing this.
(The modifier <code>non-sealed</code> is the first <a href="https://openjdk.java.net/jeps/8223002">hyphenated keyword</a>
proposed for Java.)</p>
</li>
</ul>
</li>
</ol>
<p>As an example of the third constraint, <code>Circle</code> and <code>Square</code> may be <code>final</code>
while <code>Rectangle</code> is <code>sealed</code> and we add a new subclass, <code>WeirdShape</code>, that is
<code>non-sealed</code>:</p>
<pre><code>package com.example.geometry;

public abstract sealed class Shape
    permits Circle, Rectangle, Square, WeirdShape { ... }

public final class Circle extends Shape { ... }

public sealed class Rectangle extends Shape 
    permits TransparentRectangle, FilledRectangle { ... }
public final class TransparentRectangle extends Rectangle { ... }
public final class FilledRectangle extends Rectangle { ... }

public final class Square extends Shape { ... }

public non-sealed class WeirdShape extends Shape { ... }</code></pre>
<p>Even though the <code>WeirdShape</code> is open to extension by unknown classes, all
instances of those subclasses are also instances of <code>WeirdShape</code>.  Therefore
code written to test whether an instance of <code>Shape</code> is either a <code>Circle</code>, a
<code>Rectangle</code>, a <code>Square</code>, or a <code>WeirdShape</code> remains exhaustive.</p>
<p>Exactly one of the modifiers <code>final</code>, <code>sealed</code>, and <code>non-sealed</code> must be used by
each permitted subclass. It is not possible for a class to be both <code>sealed</code>
(implying subclasses) and <code>final</code> (implying no subclasses),  or both
<code>non-sealed</code> (implying subclasses) and <code>final</code> (implying no subclasses), or both
<code>sealed</code> (implying restricted subclasses) and <code>non-sealed</code> (implying
unrestricted subclasses).</p>
<p>(The <code>final</code> modifier can be considered a special case of sealing, where
extension/implementation is prohibited completely. That is, <code>final</code> is
conceptually equivalent to <code>sealed</code> plus a <code>permits</code> clause which specifies
nothing, though such a <code>permits</code> clause cannot be written.)</p>
<p>A class which is <code>sealed</code> or <code>non-sealed</code> may be <code>abstract</code>, and have <code>abstract</code>
members. A <code>sealed</code> class may permit subclasses which are <code>abstract</code>, providing
they are then <code>sealed</code> or <code>non-sealed</code>, rather than <code>final</code>.</p>
<p>It is a compile-time error if any class extends a <code>sealed</code> class but is not
permitted to do so.</p>
<h3 id="Class-accessibility">Class accessibility</h3>
<p>Because <code>extends</code> and <code>permits</code> clauses make use of class names, a permitted
subclass and its sealed superclass must be accessible to each other. However,
permitted subclasses need not have the same accessibility as each other, or as
the sealed class. In particular, a subclass may be less accessible than the
sealed class. This means that, in a future release when pattern matching is
supported by switches, some code will not be able to exhaustively <code>switch</code> over
the subclasses unless a <code>default</code> clause (or other total pattern) is used. Java
compilers will be encouraged to detect when <code>switch</code> is not as exhaustive as its
original author imagined it would be, and customize the error message to
recommend a <code>default</code> clause.</p>
<h3 id="Sealed-interfaces">Sealed interfaces</h3>
<p>As for classes, an interface can be sealed by applying the <code>sealed</code> modifier to
the interface. After any <code>extends</code> clause to specify superinterfaces, the
implementing classes and subinterfaces are specified with a <code>permits</code> clause.
For example, the planetary example from above can be rewritten as follows:</p>
<pre><code>sealed interface Celestial 
    permits Planet, Star, Comet { ... }

final class Planet implements Celestial { ... }
final class Star   implements Celestial { ... }
final class Comet  implements Celestial { ... }</code></pre>
<p>Here is another classic example of a class hierarchy where there is a known set
of subclasses: modeling mathematical expressions.</p>
<pre><code>package com.example.expression;

public sealed interface Expr
    permits ConstantExpr, PlusExpr, TimesExpr, NegExpr { ... }

public final class ConstantExpr implements Expr { ... }
public final class PlusExpr     implements Expr { ... }
public final class TimesExpr    implements Expr { ... }
public final class NegExpr      implements Expr { ... }</code></pre>
<h3 id="Sealing-and-record-classes">Sealing and record classes</h3>
<p>Sealed classes work well with <a href="https://docs.oracle.com/javase/specs/jls/se16/html/jls-8.html#jls-8.10">record classes</a>.  Record classes are
implicitly <code>final</code>, so a sealed hierarchy of record classes is slightly more
concise than the example above:</p>
<pre><code>package com.example.expression;

public sealed interface Expr
    permits ConstantExpr, PlusExpr, TimesExpr, NegExpr { ... }

public record ConstantExpr(int i)       implements Expr { ... }
public record PlusExpr(Expr a, Expr b)  implements Expr { ... }
public record TimesExpr(Expr a, Expr b) implements Expr { ... }
public record NegExpr(Expr e)           implements Expr { ... }</code></pre>
<p>The combination of sealed classes and record classes is sometimes referred to
as <a href="https://en.wikipedia.org/wiki/Algebraic_data_type"><em>algebraic data types</em></a>:
Record classes allow us to express <em>product types</em>, and sealed classes allow
us to express <em>sum types</em>.</p>
<h3 id="Sealed-classes-and-conversions">Sealed classes and conversions</h3>
<p>A cast expression converts a value to a type.  A type <code>instanceof</code> expression
tests a value against a type. Java is extremely permissive about the types that
are allowed in these kinds of expressions. For example:</p>
<pre><code>interface I {}
class C {} // does not implement I

void test (C c) {
    if (c instanceof I) 
        System.out.println("It's an I");
}</code></pre>
<p>This program is legal even though it is currently not possible for a <code>C</code> object to
implement the interface <code>I</code>. Of course, as the program evolves, it might be:</p>
<pre><code>...
class B extends C implements I {}

test(new B()); 
// Prints "It's an I"</code></pre>
<p>The type conversion rules capture a notion of <em>open extensibility</em>. The Java
type system does not assume a closed world. Classes and interfaces can be
extended at some future time, and casting conversions compile to runtime tests,
so we can safely be flexible.</p>
<p>However, at the other end of the spectrum the conversion rules do address the
case where a class can definitely not be extended, i.e., when it is a <code>final</code>
class.</p>
<pre><code>interface I {}
final class C {}

void test (C c) {
    if (c instanceof I)     // Compile-time error!
        System.out.println("It's an I");
}</code></pre>
<p>The method <code>test</code> fails to compile, since the compiler knows that there can be
no subclass of <code>C</code>, so since <code>C</code> does not implement <code>I</code> then it is never
possible for a <code>C</code> value to implement <code>I</code>. This is a compile-time error.</p>
<p>What if <code>C</code> is not <code>final</code>, but <code>sealed</code>? Its direct subclasses
are explicitly enumerated, and &#8212; by the definition of being <code>sealed</code> &#8212; in the same
module, so we expect the compiler to look to see if it can spot a
similar compile-time error. Consider the following code:</p>
<pre><code>interface I {}
sealed class C permits D {}
final class D extends C {}

void test (C c) {
    if (c instanceof I)     // Compile-time error!
        System.out.println("It's an I");
}</code></pre>
<p>Class <code>C</code> does not implement <code>I</code>, and is not <code>final</code>, so by the existing rules we
might conclude that a conversion is possible. <code>C</code> is <code>sealed</code>, however, and there
is one permitted direct subclass of <code>C</code>, namely <code>D</code>. By the definition of sealed
types, <code>D</code> must be either <code>final</code>, <code>sealed</code>, or <code>non-sealed</code>. In this example,
all the direct subclasses of <code>C</code> are <code>final</code> and do not implement <code>I</code>. This
program should therefore be rejected, since there cannot be a subtype of <code>C</code>
that implements <code>I</code>.</p>
<p>In contrast, consider a similar program where one of the direct subclasses of
the sealed class is <code>non-sealed</code>:</p>
<pre><code>interface I {}
sealed class C permits D, E {}
non-sealed class D extends C {}
final class E extends C {}

void test (C c) {
    if (c instanceof I) 
        System.out.println("It's an I");
}</code></pre>
<p>This is type-correct, since it is possible for a subtype of the <code>non-sealed</code>
type <code>D</code> to implement <code>I</code>.</p>
<p>Consequently, supporting <code>sealed</code> classes leads to a change in the definition of
<a href="https://docs.oracle.com/javase/specs/jls/se15/html/jls-5.html#jls-5.1.6.1">narrowing reference conversion</a>
to navigate sealed hierarchies to determine at compile time which
conversions are not possible.</p>
<h3 id="Sealed-classes-in-the-JDK">Sealed classes in the JDK</h3>
<p>An example of how sealed classes might be used in the JDK is in the
<code>java.lang.constant</code> package that models
<a href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/constant/package-summary.html">descriptors for JVM entities</a>:</p>
<pre><code>package java.lang.constant;

public sealed interface ConstantDesc
    permits String, Integer, Float, Long, Double,
            ClassDesc, MethodTypeDesc, DynamicConstantDesc { ... }

// ClassDesc is designed for subclassing by JDK classes only
public sealed interface ClassDesc extends ConstantDesc
    permits PrimitiveClassDescImpl, ReferenceClassDescImpl { ... }
final class PrimitiveClassDescImpl implements ClassDesc { ... }
final class ReferenceClassDescImpl implements ClassDesc { ... } 

// MethodTypeDesc is designed for subclassing by JDK classes only
public sealed interface MethodTypeDesc extends ConstantDesc
    permits MethodTypeDescImpl { ... }
final class MethodTypeDescImpl implements MethodTypeDesc { ... }

// DynamicConstantDesc is designed for subclassing by user code
public non-sealed abstract class DynamicConstantDesc implements ConstantDesc { ... }</code></pre>
<h3 id="Sealed-classes-and-pattern-matching">Sealed classes and pattern matching</h3>
<p>A significant benefit of sealed classes will be realized in <a href="https://openjdk.java.net/jeps/406">JEP 406</a>,
which proposes to extend <code>switch</code> with pattern matching. Instead of inspecting
an instance of a sealed class with <code>if</code>-<code>else</code> chains, user code will be able to
use a <code>switch</code> enhanced with patterns. The use of sealed classes will allow
the Java compiler to check that the patterns are exhaustive.</p>
<p>For example, consider this code using the <code>sealed</code> hierarchy declared earlier:</p>
<pre><code>Shape rotate(Shape shape, double angle) {
        if (shape instanceof Circle) return shape;
        else if (shape instanceof Rectangle) return shape;
        else if (shape instanceof Square) return shape;
        else throw new IncompatibleClassChangeError();
}</code></pre>
<p>The Java compiler cannot ensure that the <code>instanceof</code> tests cover all the
permitted subclasses of <code>Shape</code>. The final <code>else</code> clause is actually
unreachable, but this cannot be verified by the compiler. More importantly, no
compile-time error message would be issued if the <code>instanceof Rectangle</code> test
was omitted.</p>
<p>In contrast, with pattern matching for <code>switch</code>
(<a href="https://openjdk.java.net/jeps/406">JEP 406</a>)the compiler can confirm that
every permitted subclass of <code>Shape</code> is covered, so no <code>default</code> clause or other
total pattern is needed. The compiler will, moreover, issue an error message if
any of the three cases is missing:</p>
<pre><code>Shape rotate(Shape shape, double angle) {
    return switch (shape) {   // pattern matching switch
        case Circle c    -&gt; c; 
        case Rectangle r -&gt; shape.rotate(angle);
        case Square s    -&gt; shape.rotate(angle);
        // no default needed!
    }
}</code></pre>
<h3 id="Java-grammar">Java grammar</h3>
<p>The grammar for class declarations is amended to the following:</p>
<pre><code>NormalClassDeclaration:
  {ClassModifier} class TypeIdentifier [TypeParameters]
    [Superclass] [Superinterfaces] [PermittedSubclasses] ClassBody

ClassModifier:
  (one of)
  Annotation public protected private
  abstract static sealed final non-sealed strictfp

PermittedSubclasses:
  permits ClassTypeList

ClassTypeList:
  ClassType {, ClassType}</code></pre>
<h3 id="JVM-support-for-sealed-classes">JVM support for sealed classes</h3>
<p>The Java Virtual Machine recognizes <code>sealed</code> classes and interfaces at runtime,
and prevents extension by unauthorized subclasses and subinterfaces.</p>
<p>Although <code>sealed</code> is a class modifier, there is no <code>ACC_SEALED</code> flag in the
<code>ClassFile</code> structure. Instead, the <code>class</code> file of a sealed class has a
<code>PermittedSubclasses</code> attribute which implicitly indicates the <code>sealed</code> modifier
and explicitly specifies the permitted subclasses:</p>
<pre><code>PermittedSubclasses_attribute {
    u2 attribute_name_index;
    u4 attribute_length;
    u2 number_of_classes;
    u2 classes[number_of_classes];
}</code></pre>
<p>The list of permitted subclasses is mandatory. Even when the permitted
subclasses are inferred by the compiler, those inferred subclasses are
explicitly included in the <code>PermittedSubclasses</code> attribute.</p>
<p>The <code>class</code> file of a permitted subclass carries no new attributes.</p>
<p>When the JVM attempts to define a class whose superclass or superinterface has a
<code>PermittedSubclasses</code> attribute, the class being defined must be named by the
attribute. Otherwise, an <code>IncompatibleClassChangeError</code> is thrown.</p>
<h3 id="Reflection-API">Reflection API</h3>
<p>We add the following <code>public</code> methods to <code>java.lang.Class</code>:</p>
<ul>
<li><code>Class&lt;?&gt;[] getPermittedSubclasses()</code></li>
<li><code>boolean isSealed()</code></li>
</ul>
<p>The method <code>getPermittedSubclasses()</code> returns an array containing
<code>java.lang.Class</code> objects representing the permitted subclasses of the class, if
the class is sealed.  It returns an empty array if the class is not sealed.</p>
<p>The method <code>isSealed</code> returns true if the given class or interface is sealed.
(Compare with <code>isEnum</code>.)</p>
<h2 id="Future-Work">Future Work</h2>
<p>A common pattern, especially when writing APIs, is to define a public type as an
interface and implement it with a single private class. With sealed classes this
can be expressed more precisely, as a sealed public interface with a single
permitted private implementation. Thus the type is widely accessible but the
implementation is not, and cannot be extended in any way.</p>
<pre><code>public sealed interface Foo permits MyFooImpl { } 
private final class MyFooImpl implements Foo { }</code></pre>
<p>A clumsiness of this approach is that implementation methods that accept <code>Foo</code>
objects require explicit casts, for example:</p>
<pre><code>void m(Foo f) { 
    MyFooImpl mfi = (MyFooImpl) f;
    ...
}</code></pre>
<p>The cast here seems unnecessary, since we know it should always succeed. Yet
there is an implicit semantic assumption in the cast, which is that the class
<code>MyFooImpl</code> is the only implementation of <code>Foo</code>. There is no way for the author
to capture this intuition so that it can be checked at compile time. Should, in
time, <code>Foo</code> permit an additional implementation, this cast would remain
type-correct but may fail at runtime. In other words, the semantic assumption
would be broken but the compiler cannot alert the developer of that fact.</p>
<p>With the precision of sealed hierarchies it may be worth providing developers
with the means to express such semantic assumptions, and for the compiler to
check them. This could be achieved by adding a new form of reference conversion
for assignment contexts which allows the conversion of a sealed supertype to its
only subtype, for example:</p>
<pre><code>MyFooImpl mfi = f; // Allowed because the compiler sees that MyFooImpl
                   // is the only permitted subtype of Foo.
                   // (A synthetic cast would be added for safety.)</code></pre>
<p>Alternatively, we could provide a new form of cast, for example:</p>
<pre><code>MyFooImpl mfi = (total MyFooImpl) f;</code></pre>
<p>In both cases, should the interface <code>Foo</code> be changed to permit another
implementation, then both would cause compile-time errors upon recompilation.</p>
<h2 id="Alternatives">Alternatives</h2>
<p>Some languages have direct support for
<a href="https://en.wikipedia.org/wiki/Algebraic_data_type">algebraic data types</a>
(ADTs), such as Haskell's <code>data</code> feature.  It would be possible to express ADTs
more directly and in a manner familiar to Java developers through a variant of
the <code>enum</code> feature, where a sum of products could be defined in a single
declaration. However, this would not support all the desired use cases, such as
those where sums range over classes in more than one compilation unit, or sums
that range over classes that are not products.</p>
<p>The <code>permits</code> clause allows a sealed class, such as the <code>Shape</code> class shown
earlier, to be accessible-for-invocation by code in any module, but
accessible-for-implementation by code in only the same module as the sealed
class (or same package if in the unnamed module). This makes the type system
more expressive than the access-control system. With access control alone, if
<code>Shape</code> is accessible-for-invocation by code in any module (because its package
is exported), then <code>Shape</code> is also accessible-for-implementation in any module;
and if <code>Shape</code> is not accessible-for-implementation in any other module, then
<code>Shape</code> is also not accessible-for-invocation in any other module.</p>
<h2 id="Dependencies">Dependencies</h2>
<p>Sealed classes do not depend on any other JEPs. As mentioned earlier, <a href="https://openjdk.java.net/jeps/406">JEP 406</a>
proposes to extend <code>switch</code> with pattern matching, and builds on sealed classes to
improve the exhaustiveness checking of <code>switch</code>.</p>
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
