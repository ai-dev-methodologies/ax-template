package com.ax.template.authblueprint.practices;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.io.InputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.yaml.snakeyaml.Yaml;

/**
 * Shared, injectable DDD-guard predicate engine (ralplan re-verification CM2 / AM2).
 *
 * <p>Each guard's core predicate lives here as a pure function over its inputs — NOT
 * hardcoded to {@code com.ax.template.authblueprint} or {@code src/main/java}. The
 * production guard tests ({@code DddDecompositionTierZero/TierOne/Heuristics}) AND the
 * non-vacuity proof ({@code DddDecompositionViolationFixtureTest}) both call THIS code,
 * so the fixture proof exercises the real predicates rather than a copy that could drift.
 *
 * <p>AM1 fix: {@link #repoTargetMap(Path)} extracts the first generic type argument of an
 * interface's {@code extends ... <T, …>} clause regardless of the base-interface name and
 * tolerant of line breaks, so a member repository extending a custom base (e.g.
 * {@code BaseRepo<FooMember, Long>}) or with a line-broken type parameter is no longer a
 * false negative for HG-AGG-REPO / HG-ANTI-GODSERVICE-TX.
 */
final class DddRules {

    private DddRules() {}

    // Single shared import of the whole authblueprint bytecode tree. ArchUnit's full-tree
    // import is memory-heavy; sharing ONE JavaClasses across all four DDD test classes (rather
    // than a static per class) keeps the testPractices fork's heap footprint bounded.
    private static volatile JavaClasses authblueprint;

    static JavaClasses authblueprint() {
        JavaClasses local = authblueprint;
        if (local == null) {
            synchronized (DddRules.class) {
                local = authblueprint;
                if (local == null) {
                    local = new ClassFileImporter()
                            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                            .importPackages("com.ax.template.authblueprint");
                    authblueprint = local;
                }
            }
        }
        return local;
    }

    static final Set<String> BANNED_TOPLEVEL =
            Set.of("controllers", "services", "repositories", "routes", "models");
    static final Set<String> KERNEL = Set.of("common", "observability");
    static final Set<String> MUTATORS =
            Set.of("save", "saveAndFlush", "saveAll", "delete", "deleteById", "deleteAll");
    static final String AGG_ROOT = "com.ax.template.authblueprint.common.AggregateRoot";
    static final String AGG_MEMBER = "com.ax.template.authblueprint.common.AggregateMember";
    static final String ENTITY = "jakarta.persistence.Entity";
    static final String TRANSACTIONAL = "org.springframework.transaction.annotation.Transactional";

    /** Parsed allowlist surface (CM1: all three carve sets). */
    record Allowlist(Set<String> pairs, Set<String> godService, Set<String> stateMutators) {}

    // ── package/feature helpers ────────────────────────────────────────────────────
    static String basePackage(JavaClasses classes) {
        String base = null;
        for (JavaClass c : classes) {
            String pkg = c.getPackageName();
            base = (base == null) ? pkg : commonPrefix(base, pkg);
        }
        return base == null ? "" : base;
    }

    static String commonPrefix(String a, String b) {
        String[] as = a.split("\\.");
        String[] bs = b.split("\\.");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < Math.min(as.length, bs.length); i++) {
            if (as[i].equals(bs[i])) {
                if (out.length() > 0) {
                    out.append('.');
                }
                out.append(as[i]);
            } else {
                break;
            }
        }
        return out.toString();
    }

    static String featureOf(JavaClass c, String base) {
        String pkg = c.getPackageName();
        if (pkg.equals(base) || !pkg.startsWith(base + ".")) {
            return "";
        }
        String rest = pkg.substring(base.length() + 1);
        int dot = rest.indexOf('.');
        return dot < 0 ? rest : rest.substring(0, dot);
    }

    /** Simple name of an entity's own aggregate root: root() if a member, else itself. */
    static String ownRootName(JavaClass c) {
        if (c.isAnnotatedWith(AGG_MEMBER)) {
            JavaAnnotation<JavaClass> ann = c.getAnnotationOfType(AGG_MEMBER);
            Optional<Object> root = ann.get("root");
            if (root.isPresent() && root.get() instanceof JavaClass rootClass) {
                return rootClass.getSimpleName();
            }
        }
        return c.getSimpleName();
    }

    // ── allowlist + repo-map loaders ────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    static Allowlist loadAllowlist(Path yaml) {
        Set<String> pairs = new HashSet<>();
        Set<String> god = new HashSet<>();
        Set<String> state = new HashSet<>();
        if (yaml == null || !Files.exists(yaml)) {
            return new Allowlist(pairs, god, state);
        }
        try (InputStream in = Files.newInputStream(yaml)) {
            Map<String, Object> doc = new Yaml().load(in);
            if (doc == null) {
                return new Allowlist(pairs, god, state);
            }
            Object ex = doc.get("exceptions");
            if (ex instanceof List<?> list) {
                for (Object entry : list) {
                    if (entry instanceof Map<?, ?> m) {
                        Object from = m.get("from");
                        Object to = m.get("to");
                        if (from != null && to != null) {
                            pairs.add(from + "->" + to);
                        }
                    }
                }
            }
            addStrings(doc.get("governed_god_service"), god);
            addStrings(doc.get("governed_state_mutators"), state);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read allowlist " + yaml + ": " + e, e);
        }
        return new Allowlist(pairs, god, state);
    }

    private static void addStrings(Object v, Set<String> out) {
        if (v instanceof List<?> list) {
            for (Object e : list) {
                if (e != null) {
                    out.add(e.toString());
                }
            }
        }
    }

    /**
     * interface simple name -> first generic type argument simple name, for ANY interface
     * whose {@code extends} clause carries a generic. Base-interface-name agnostic and
     * line-break tolerant (AM1). Callers filter targets to actual entities/members.
     */
    static Map<String, String> repoTargetMap(Path srcRoot) {
        Map<String, String> map = new HashMap<>();
        // (?s) DOTALL so [^{] / .*? span newlines; first type arg after `extends … <`.
        Pattern p = Pattern.compile(
                "(?s)\\binterface\\s+(\\w+)\\b[^{]*?\\bextends\\b[^{]*?<\\s*([A-Za-z_][A-Za-z0-9_.]*)\\s*[,>]");
        try (Stream<Path> walk = Files.walk(srcRoot)) {
            for (Path f : (Iterable<Path>) walk.filter(x -> x.toString().endsWith(".java"))::iterator) {
                String src = Files.readString(f);
                Matcher m = p.matcher(src);
                while (m.find()) {
                    String iface = m.group(1);
                    String target = m.group(2);
                    int dot = target.lastIndexOf('.');
                    if (dot >= 0) {
                        target = target.substring(dot + 1);
                    }
                    // keep the FIRST extends-generic per interface name
                    map.putIfAbsent(iface, target);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("repoTargetMap scan failed", e);
        }
        return map;
    }

    // ── TIER-0 predicates ───────────────────────────────────────────────────────────
    static Set<String> topLevelTech(JavaClasses classes) {
        String base = basePackage(classes);
        Set<String> v = new TreeSet<>();
        for (JavaClass c : classes) {
            if (BANNED_TOPLEVEL.contains(featureOf(c, base))) {
                v.add(base + "." + featureOf(c, base));
            }
        }
        return v;
    }

    static Set<String> kernelFeatureDep(JavaClasses classes) {
        String base = basePackage(classes);
        Set<String> v = new TreeSet<>();
        for (JavaClass c : classes) {
            String feature = featureOf(c, base);
            if (!KERNEL.contains(feature)) {
                continue;
            }
            for (Dependency dep : c.getDirectDependenciesFromSelf()) {
                String tf = featureOf(dep.getTargetClass(), base);
                if (!tf.isEmpty() && !KERNEL.contains(tf) && !tf.equals(feature)) {
                    v.add(c.getName() + " -> " + dep.getTargetClass().getName() + " (feature '" + tf + "')");
                }
            }
        }
        return v;
    }

    static Set<String> featIsolation(JavaClasses classes, Allowlist allow) {
        String base = basePackage(classes);
        Set<String> v = new TreeSet<>();
        for (JavaClass c : classes) {
            String feature = featureOf(c, base);
            if (feature.isEmpty() || KERNEL.contains(feature)) {
                continue;
            }
            for (Dependency dep : c.getDirectDependenciesFromSelf()) {
                JavaClass target = dep.getTargetClass();
                String tf = featureOf(target, base);
                if (tf.isEmpty() || tf.equals(feature) || KERNEL.contains(tf)) {
                    continue;
                }
                boolean isEntity = target.isAnnotatedWith(ENTITY);
                boolean isRepo = target.getSimpleName().endsWith("Repository");
                if (!isEntity && !isRepo) {
                    continue;
                }
                if (allow.pairs().contains(c.getName() + "->" + target.getName())) {
                    continue;
                }
                v.add(c.getName() + " -> " + target.getName() + " (" + (isEntity ? "@Entity" : "*Repository") + ")");
            }
        }
        return v;
    }

    static Set<String> antiSplitEndpoint(JavaClasses classes) {
        Set<String> v = new TreeSet<>();
        for (JavaClass c : classes) {
            String n = c.getSimpleName();
            if (n.endsWith("Controller") && n.matches("^(Create|List|Get).*Controller$")) {
                v.add(c.getName());
            }
        }
        return v;
    }

    // ── TIER-1 structural predicates ────────────────────────────────────────────────
    /** member entity -> has a repository (any interface whose first extends-generic is that member). */
    static Set<String> memberRepo(JavaClasses classes, Path srcRoot, Allowlist allow) {
        Map<String, String> memberFqn = new HashMap<>();
        for (JavaClass c : classes) {
            if (c.isAnnotatedWith(AGG_MEMBER)) {
                memberFqn.put(c.getSimpleName(), c.getName());
            }
        }
        Map<String, String> repoTarget = repoTargetMap(srcRoot); // iface -> target simple
        // also resolve repo FQN from source: scan interface files for package + name
        Map<String, String> repoFqn = interfaceFqnMap(srcRoot);
        Set<String> v = new TreeSet<>();
        for (Map.Entry<String, String> e : repoTarget.entrySet()) {
            String iface = e.getKey();
            String target = e.getValue();
            if (!memberFqn.containsKey(target)) {
                continue;
            }
            String repoFq = repoFqn.getOrDefault(iface, iface);
            String pair = repoFq + "->" + memberFqn.get(target);
            if (allow.pairs().contains(pair)) {
                continue;
            }
            // emit "<repoFqn> -> <memberFqn> (member <Target>)" — a parseable FQN pair + readable suffix
            v.add(repoFq + " -> " + memberFqn.get(target) + " (member " + target + ")");
        }
        return v;
    }

    /** interface simple name -> FQN (package + name), from source. */
    static Map<String, String> interfaceFqnMap(Path srcRoot) {
        Map<String, String> map = new HashMap<>();
        Pattern pkgRe = Pattern.compile("(?m)^package\\s+([\\w.]+)\\s*;");
        Pattern ifRe = Pattern.compile("\\binterface\\s+(\\w+)\\b");
        try (Stream<Path> walk = Files.walk(srcRoot)) {
            for (Path f : (Iterable<Path>) walk.filter(x -> x.toString().endsWith(".java"))::iterator) {
                String src = Files.readString(f);
                Matcher pm = pkgRe.matcher(src);
                String pkg = pm.find() ? pm.group(1) : "";
                Matcher im = ifRe.matcher(src);
                if (im.find()) {
                    map.put(im.group(1), pkg.isEmpty() ? im.group(1) : pkg + "." + im.group(1));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("interfaceFqnMap scan failed", e);
        }
        return map;
    }

    static Set<String> aggRef(JavaClasses classes, Allowlist allow) {
        Set<String> v = new TreeSet<>();
        for (JavaClass c : classes) {
            if (!c.isAnnotatedWith(ENTITY)) {
                continue;
            }
            String ownRoot = ownRootName(c);
            for (JavaField field : c.getFields()) {
                // field.getType() keeps the GENERIC type so a collection element type (e.g.
                // @OneToMany List<OtherRoot>) is included; field.getRawType() would erase it to List
                // and silently miss cross-aggregate pointers held in collections (spec §6 requires
                // "@OneToMany/@ManyToMany 컬렉션 element 타입 해석").
                for (JavaClass involved : field.getType().getAllInvolvedRawTypes()) {
                    if (!involved.isAnnotatedWith(ENTITY)) {
                        continue;
                    }
                    String target = involved.getSimpleName();
                    if (target.equals(c.getSimpleName()) || target.equals(ownRoot)
                            || ownRootName(involved).equals(c.getSimpleName())) {
                        continue;
                    }
                    if (allow.pairs().contains(c.getName() + "->" + involved.getName())) {
                        continue;
                    }
                    v.add(c.getName() + " -> " + involved.getName() + " (cross-aggregate object pointer)");
                }
            }
        }
        return v;
    }

    static Set<String> memberEncap(JavaClasses classes) {
        String base = basePackage(classes);
        Set<String> v = new TreeSet<>();
        for (JavaClass c : classes) {
            if (!c.isAnnotatedWith(AGG_MEMBER)) {
                continue;
            }
            String mf = featureOf(c, base);
            c.getDirectDependenciesToSelf().forEach(dep -> {
                String of = featureOf(dep.getOriginClass(), base);
                if (!of.equals(mf) && !of.isEmpty() && !KERNEL.contains(of)) {
                    v.add(dep.getOriginClass().getName() + " -> " + c.getName()
                            + " (member referenced from feature '" + of + "')");
                }
            });
        }
        return v;
    }

    // ── TIER-1 heuristic predicates ─────────────────────────────────────────────────
    static Set<String> godService(JavaClasses classes, Path srcRoot, Allowlist allow) {
        Map<String, String> repoTarget = repoTargetMap(srcRoot);
        Set<String> rootEntities = new TreeSet<>();
        for (JavaClass c : classes) {
            if (c.isAnnotatedWith(AGG_ROOT)) {
                rootEntities.add(c.getSimpleName());
            }
        }
        Set<String> v = new TreeSet<>();
        for (JavaClass c : classes) {
            for (JavaMethod method : c.getMethods()) {
                if (!method.isAnnotatedWith(TRANSACTIONAL)) {
                    continue;
                }
                Set<String> mutatedRoots = new TreeSet<>();
                for (JavaMethodCall call : method.getMethodCallsFromSelf()) {
                    if (!MUTATORS.contains(call.getName())) {
                        continue;
                    }
                    String entity = repoTarget.get(call.getTargetOwner().getSimpleName());
                    if (entity != null && rootEntities.contains(entity)) {
                        mutatedRoots.add(entity);
                    }
                }
                if (mutatedRoots.size() >= 2) {
                    String key = c.getName() + "#" + method.getName();
                    if (!allow.godService().contains(key)) {
                        v.add(key + " directly mutates " + mutatedRoots);
                    }
                }
            }
        }
        return v;
    }

    static Set<String> stateMutator(JavaClasses classes, Allowlist allow) {
        Set<String> stateMachineNames = new HashSet<>();
        for (JavaClass c : classes) {
            if (c.getSimpleName().endsWith("StateMachine")) {
                stateMachineNames.add(c.getSimpleName());
            }
        }
        Set<String> v = new TreeSet<>();
        for (JavaClass c : classes) {
            if (!c.isAnnotatedWith(ENTITY)) {
                continue;
            }
            if (!stateMachineNames.contains(c.getSimpleName() + "StateMachine")) {
                continue;
            }
            for (JavaMethod m : c.getMethods()) {
                if (!m.getName().equals("setStatus") && !m.getName().equals("setState")) {
                    continue;
                }
                final JavaClass entity = c;
                m.getCallsOfSelf().forEach(call -> {
                    JavaClass caller = call.getOriginOwner();
                    String cn = caller.getName();
                    if (cn.equals(entity.getName()) || caller.getSimpleName().endsWith("StateMachine")) {
                        return;
                    }
                    String key = cn + " -> " + entity.getName() + "#" + m.getName();
                    if (!allow.stateMutators().contains(key)) {
                        v.add(key);
                    }
                });
            }
        }
        return v;
    }
}
