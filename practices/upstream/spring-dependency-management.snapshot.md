<!DOCTYPE html>
<html lang="en">
<script src="https://cdn.cookielaw.org/scripttemplates/otSDKStub.js" data-domain-script="018ee325-b3a7-7753-937b-b8b3e643b1a7"></script><script>function OptanonWrapper() {}</script><script>function setGTM(w, d, s, l, i) { w[l] = w[l] || []; w[l].push({ "gtm.start": new Date().getTime(), event: "gtm.js"}); var f = d.getElementsByTagName(s)[0], j = d.createElement(s), dl = l != "dataLayer" ? "&l=" + l : ""; j.async = true; j.src = "https://www.googletagmanager.com/gtm.js?id=" + i + dl; f.parentNode.insertBefore(j, f); } if (document.cookie.indexOf("OptanonConsent") > -1 && document.cookie.indexOf("groups=") > -1) { setGTM(window, document, "script", "dataLayer", "GTM-W8CQ8TL"); } else { waitForOnetrustActiveGroups(); } var timer; function waitForOnetrustActiveGroups() { if (document.cookie.indexOf("OptanonConsent") > -1 && document.cookie.indexOf("groups=") > -1) { clearTimeout(timer); setGTM(window, document, "script", "dataLayer", "GTM-W8CQ8TL"); } else { timer = setTimeout(waitForOnetrustActiveGroups, 250); }}</script>
<meta charset="UTF-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<meta name="generator" content="Asciidoctor 2.0.10">
<meta name="author" content="Andy Wilkinson">
<title>Dependency Management Plugin</title>
<link rel="stylesheet" href="css/site.css">
<script src="js/setup.js"></script><script defer src="js/site.js"></script>

</head>
<body class="book toc2 toc-left"><div id="banner-container" class="container" role="banner">
  <div id="banner" class="contained" role="banner">
    <div id="switch-theme">
      <input type="checkbox" id="switch-theme-checkbox" />
      <label for="switch-theme-checkbox">Dark Theme</label>
    </div>
  </div>
</div>
<div id="tocbar-container" class="container" role="navigation">
  <div id="tocbar" class="contained" role="navigation">
    <button id="toggle-toc"></button>
  </div>
</div>
<div id="main-container" class="container">
  <div id="main" class="contained">
    <div id="doc" class="doc">
<div id="header">
<h1>Dependency Management Plugin</h1>
<div class="details">
<span id="author" class="author">Andy Wilkinson</span><br>
<span id="revnumber">version 1.1.7</span>
</div>
<div id="toc" class="toc2">
<div id="toctitle">Table of Contents</div>
<ul class="sectlevel1">
<li><a href="#introduction">1. Introduction</a></li>
<li><a href="#requirements">2. Requirements</a></li>
<li><a href="#getting-started">3. Getting Started</a></li>
<li><a href="#dependency-management-configuration">4. Dependency Management Configuration</a>
<ul class="sectlevel2">
<li><a href="#dependency-management-configuration-dsl">4.1. Dependency Management DSL</a>
<ul class="sectlevel3">
<li><a href="#dependency-management-configuration-dsl-dependency-sets">4.1.1. Dependency Sets</a></li>
<li><a href="#dependency-management-configuration-dsl-exclusions">4.1.2. Exclusions</a></li>
</ul>
</li>
<li><a href="#dependency-management-configuration-bom-import">4.2. Importing a Maven Bom</a>
<ul class="sectlevel3">
<li><a href="#dependency-management-configuration-bom-import-multiple">4.2.1. Importing Multiple Boms</a></li>
<li><a href="#dependency-management-configuration-bom-import-override">4.2.2. Overriding Versions in a Bom</a>
<ul class="sectlevel4">
<li><a href="#dependency-management-configuration-bom-import-override-property">Changing the Value of a Version Property</a></li>
<li><a href="#dependency-management-configuration-bom-import-override-dependency-management">Overriding the Dependency Management</a></li>
</ul>
</li>
<li><a href="#dependency-management-configuration-import-bom-resolution-strategy">4.2.3. Configuring the Dependency Management Resolution Strategy</a></li>
</ul>
</li>
<li><a href="#dependency-management-configuration-specific">4.3. Dependency Management for Specific Configurations</a></li>
</ul>
</li>
<li><a href="#accessing-properties">5. Accessing Properties from Imported Boms</a></li>
<li><a href="#maven-exclusions">6. Maven Exclusions</a>
<ul class="sectlevel2">
<li><a href="#maven-exclusions-disabling">6.1. Disabling Maven exclusions</a></li>
</ul>
</li>
<li><a href="#pom-generation">7. Pom generation</a>
<ul class="sectlevel2">
<li><a href="#pom-generation-disabling">7.1. Disabling the customization of a generated pom</a></li>
<li><a href="#pom-generation-manual">7.2. Configuring your own pom</a></li>
</ul>
</li>
<li><a href="#working-with-managed-versions">8. Working with the Managed Versions</a>
<ul class="sectlevel2">
<li><a href="#working-with-managed-versions-dependency-management-task">8.1. Dependency Management Task</a></li>
<li><a href="#working-with-managed-versions-programmatic-access">8.2. Programmatic access</a></li>
</ul>
</li>
</ul>
</div>
</div>
<div id="content">
<div id="preamble">
<div class="sectionbody">
<div id="abstract" class="paragraph">
<p>A Gradle plugin that provides Maven-like dependency management and exclusions</p>
</div>
</div>
</div>
<div class="sect1">
<h2 id="introduction"><a class="anchor" href="#introduction"></a>1. Introduction</h2>
<div class="sectionbody">
<div class="paragraph">
<p>Based on the configured dependency management metadata, the Dependency Management Plugin will control the versions of your project&#8217;s direct and transitive dependencies and will honour any exclusions declared in the poms of your project&#8217;s dependencies.</p>
</div>
</div>
</div>
<div class="sect1">
<h2 id="requirements"><a class="anchor" href="#requirements"></a>2. Requirements</h2>
<div class="sectionbody">
<div class="paragraph">
<p>The Plugin has the following requirements:</p>
</div>
<div class="ulist">
<ul>
<li>
<p>Gradle 6.x (6.8 or later), 7.x, or 8.x.
Gradle 6.7 and earlier are not supported.</p>
</li>
<li>
<p>Java 8 or later</p>
</li>
</ul>
</div>
</div>
</div>
<div class="sect1">
<h2 id="getting-started"><a class="anchor" href="#getting-started"></a>3. Getting Started</h2>
<div class="sectionbody">
<div class="paragraph">
<p>The plugin is <a href="https://plugins.gradle.org/plugin/io.spring.dependency-management">available in the Gradle Plugin Portal</a> and can be applied like this:</p>
</div>
<div class="listingblock primary">
<div class="title">Groovy</div>
<div class="content">
<pre class="highlight"><code class="language-groovy" data-lang="groovy">plugins {
    id "io.spring.dependency-management" version &lt;&lt;version&gt;&gt;
}
</code></pre>
</div>
</div>
<div class="listingblock secondary">
<div class="title">Kotlin</div>
<div class="content">
<pre class="highlight"><code class="language-kotlin" data-lang="kotlin">plugins {
    id("io.spring.dependency-management") version &lt;&lt;version&gt;&gt;
}
</code></pre>
</div>
</div>
<div class="paragraph">
<p>If you prefer, the plugin is also available from Maven Central and JCenter.</p>
</div>
<div class="paragraph">
<p>Snapshots are available from <a href="https://repo.spring.io/plugins-snapshot" class="bare">repo.spring.io/plugins-snapshot</a> and can be used as shown in the following example:</p>
</div>
<div class="listingblock primary">
<div class="title">Groovy</div>
<div class="content">
<pre class="highlight"><code class="language-groovy" data-lang="groovy">buildscript {
    repositories {
        maven { url 'https://repo.spring.io/plugins-snapshot' }
    }
    dependencies {
        classpath 'io.spring.gradle:dependency-management-plugin:&lt;&lt;snapshot-version&gt;&gt;'
    }
}

apply plugin: "io.spring.dependency-management"
</code></pre>
</div>
</div>
<div class="listingblock secondary">
<div class="title">Kotlin</div>
<div class="content">
<pre class="highlight"><code class="language-kotlin" data-lang="kotlin">buildscript {
  repositories {
    maven {
      url = uri("https://repo.spring.io/plugins-snapshot")
    }
  }
  dependencies {
    classpath("io.spring.gradle:dependency-management-plugin:&lt;&lt;snapshot-version&gt;&gt;")
  }
}

apply(plugin = "io.spring.dependency-management")
</code></pre>
</div>
</div>
<div class="paragraph">
<p>With this basic configuration in place, you&#8217;re ready to configure the project&#8217;s dependency management and declare its dependencies.</p>
</div>
</div>
</div>
<div class="sect1">
<h2 id="dependency-management-configuration"><a class="anchor" href="#dependency-management-configuration"></a>4. Dependency Management Configuration</h2>
<div class="sectionbody">
<div class="paragraph">
<p>You have two options for configuring the plugin&#8217;s dependency management:</p>
</div>
<div class="olist arabic">
<ol class="arabic">
<li>
<p>Use the plugin&#8217;s DSL to configure dependency management directly</p>
</li>
<li>
<p>Import one or more existing Maven boms.</p>
</li>
</ol>
</div>
<div class="paragraph">
<p>Dependency management can be applied to every configuration (the default) or to one or more specific configurations.</p>
</div>
<div class="sect2">
<h3 id="dependency-management-configuration-dsl"><a class="anchor" href="#dependency-management-configuration-dsl"></a>4.1. Dependency Management DSL</h3>
<div class="paragraph">
<p>The DSL allows you to declare dependency management using a <code>:</code> separated string to configure the coordinates of the managed dependency, as shown in the following example:</p>
</div>
<div class="listingblock primary">
<div class="title">Groovy</div>
<div class="content">
<pre class="highlight"><code class="language-groovy" data-lang="groovy">dependencyManagement {
    dependencies {
        dependency 'org.springframework:spring-core:6.0.10'
    }
}
</code></pre>
</div>
</div>
<div class="listingblock secondary">
<div class="title">Kotlin</div>
<div class="content">
<pre class="highlight"><code class="language-groovy" data-lang="groovy">dependencyManagement {
    dependencies {
        dependency("org.springframework:spring-core:6.0.10")
    }
}
</code></pre>
</div>
</div>
<div class="paragraph">
<p>Alternatively, you can use a map with <code>group</code>, <code>name</code>, and <code>version</code> entries, as shown in the following example:</p>
</div>
<div class="listingblock primary">
<div class="title">Groovy</div>
<div class="content">
<pre class="highlight"><code class="language-groovy" data-lang="groovy">dependencyManagement {
    dependencies {
        dependency group:'org.springframework', name:'spring-core', version:'6.0.10'
    }
}
</code></pre>
</div>
</div>
<div class="listingblock secondary">
<div class="title">Kotlin</div>
<div class="content">
<pre class="highlight"><code class="language-groovy" data-lang="groovy">dependencyManagement {
    dependencies {
        dependency(mapOf(
            "group" to "org.springframework",
            "name" to "spring-core",
            "version" to "6.0.10"
        ))
    }
}
</code></pre>
</div>
</div>
<div class="paragraph">
<p>With either syntax, this configuration will cause all dependencies (direct or transitive) on <code>spring-core</code> to have the version <code>6.0.10</code>.
When dependency management is in place, you can declare a dependency without a version, as shown in the following example:</p>
</div>
<div class="listingblock primary">
<div class="title">Groovy</div>
<div class="content">
<pre class="highlight"><code class="language-groovy" data-lang="groovy">dependencies {
    implementation 'org.springframework:spring-core'
}
</code></pre>
</div>
</div>
<div class="listingblock secondary">
<div class="title">Kotlin</div>
<div class="content">
<pre class="highlight"><code class="language-kotlin" data-lang="kotlin">dependencies {
    implementation("org.springframework:spring-core")
}
</code></pre>
</div>
</div>
<div class="sect3">
<h4 id="dependency-management-configuration-dsl-dependency-sets"><a class="anchor" href="#dependency-management-configuration-dsl-dependency-sets"></a>4.1.1. Dependency Sets</h4>
<div class="paragraph">
<p>When you want to provide dependency management for multiple modules with the same group and version you should use a dependency set.
Using a dependency set removes the need to specify the same group and version multiple times, as shown in the following example:</p>
</div>
<div class="listingblock primary">
<div class="title">Groovy</div>
<div class="content">
<pre class="highlight"><code class="language-groovy" data-lang="groovy">dependencyManagement {
     dependencies {
          dependencySet(group:'org.slf4j', version: '2.0.7') {
               entry 'slf4j-api'
               entry 'slf4j-simple'
          }
     }
}
</code></pre>
</div>
</div>
<div class="listingblock secondary">
<div class="title">Kotlin</div>
<div class="content">
<pre class="highlight"><code class="language-kotlin" data-lang="kotlin">dependencyManagement {
    dependencies {
        dependencySet("org.slf4j:2.0.7") {
            entry("slf4j-api")
            entry("slf4j-simple")
        }
    }
}
</code></pre>
</div>
</div>
</div>
<div class="sect3">
<h4 id="dependency-management-configuration-dsl-exclusions"><a class="anchor" href="#dependency-management-configuration-dsl-exclusions"></a>4.1.2. Exclusions</h4>
<div class="paragraph">
<p>You can also use the DSL to declare exclusions.
The two main advantages of using this mechanism are that they will be included in the <code>&lt;dependencyManagement&gt;</code> of your project&#8217;s <a href="#pom-generation">generated pom</a> and that they will be applied using <a href="#maven-exclusions">Maven&#8217;s exclusion semantics</a>.</p>
</div>
<div class="paragraph">
<p>An exclusion can be declared on individual dependencies, as shown in the following example:</p>
</div>
<div class="listingblock primary">
<div class="title">Groovy</div>
<div class="content">
<pre class="highlight"><code class="language-groovy" data-lang="groovy">dependencyManagement {
    dependencies {
        dependency('org.apache.activemq:activemq-spring:5.18.1') {
            exclude 'commons-logging:commons-logging'
        }
    }
}
</code></pre>
</div>
</div>
<div class="listingblock secondary">
<div class="title">Kotlin</div>
<div class="content">
<pre class="highlight"><code class="language-kotlin" data-lang="kotlin">dependencyManagement {
    dependencies {
        dependency("org.apache.activemq:activemq-spring:5.18.1") {
            exclude("commons-logging:commons-logging")
        }
    }
}
</code></pre>
</div>
</div>
<div class="paragraph">
<p>An exclusion can also be declared on an entry in a dependency set, as shown in the following example:</p>
</div>
<div class="listingblock primary">
<div class="title">Groovy</div>
<div class="content">
<pre class="highlight"><code class="language-groovy" data-lang="groovy">dependencyManagement {
    dependencies {
        dependencySet(group:'org.apache.activemq', version: '5.18.1') {
            entry('activemq-spring') {
                exclude group: 'commons-logging', name: 'commons-logging'
            }
        }
    }
}
</code></pre>
</div>
</div>
<div class="listingblock secondary">
<div class="title">Kotlin</div>
<div class="content">
<pre class="highlight"><code class="language-groovy" data-lang="groovy">dependencyManagement {
    dependencies {
        dependencySet("org.apache.activemq:5.18.1") {
            entry("activemq-spring") {
                exclude(mapOf("group" to "commons-logging", "name" to "commons-logging"))
            }
        }
    }
}
</code></pre>
</div>
</div>
<div class="paragraph">
<p>As shown in the two examples above, an exclusion can be identified using a string in the form <code>'group:name'</code> or a map with <code>group</code> and <code>name</code> entries.</p>
</div>
<div class="admonitionblock note">
<table>
<tr>
<td class="icon">
<i class="fa icon-note" title="Note"></i>
</td>
<td class="content">
Gradle does not provide an API for accessing a dependency&#8217;s classifier during resolution.
Unfortunately, this means that dependency management-based exclusions will not work when a classifier is involved.
</td>
</tr>
</table>
</div>
</div>
</div>
<div class="sect2">
<h3 id="dependency-management-configuration-bom-import"><a class="anchor" href="#dependency-management-configuration-bom-import"></a>4.2. Importing a Maven Bom</h3>
<div class="paragraph">
<p>The plugin also allows you to import an existing Maven bom to utilise its dependency management, as shown in the following example:</p>
</div>
<div class="listingblock primary">
<div class="title">Groovy</div>
<div class="content">
<pre class="highlight"><code class="language-groovy" data-lang="groovy">dependencyManagement {
     imports {
          mavenBom 'org.springframework.boot:spring-boot-dependencies:3.1.1'
     }
}

dependencies {
     implementation 'org.springframework.integration:spring-integration-core'
}
</code></pre>
</div>
</div>
<div class="listingblock secondary">
<div class="title">Kotlin</div>
<div class="content">
<pre class="highlight"><code class="language-kotlin" data-lang="kotlin">dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.1.1")
    }
}

dependencies {
    implementation("org.springframework.integration:spring-integration-core")
}
</code></pre>
</div>
</div>
<div class="paragraph">
<p>This configuration will apply the <a href="https://docs.spring.io/spring-boot/docs/3.1.1/reference/html/dependency-versions.html#appendix.dependency-versions">versions in <code>spring-boot-dependencies</code></a> to the project&#8217;s dependencies:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlight"><code class="language-shell" data-lang="shell">$ gradle dependencies --configuration compileClasspath

&gt; Task :dependencies

------------------------------------------------------------
Root project
------------------------------------------------------------

compileClasspath - Compile classpath for source set 'main'.
\--- org.springframework.integration:spring-integration-core -&gt; 6.1.1
     +--- org.springframework:spring-aop:6.0.10
     |    +--- org.springframework:spring-beans:6.0.10
     |    |    \--- org.springframework:spring-core:6.0.10
     |    |         \--- org.springframework:spring-jcl:6.0.10
     |    \--- org.springframework:spring-core:6.0.10 (*)
     +--- org.springframework:spring-context:6.0.10
     |    +--- org.springframework:spring-aop:6.0.10 (*)
     |    +--- org.springframework:spring-beans:6.0.10 (*)
     |    +--- org.springframework:spring-core:6.0.10 (*)
     |    \--- org.springframework:spring-expression:6.0.10
     |         \--- org.springframework:spring-core:6.0.10 (*)
     +--- org.springframework:spring-messaging:6.0.10
     |    +--- org.springframework:spring-beans:6.0.10 (*)
     |    \--- org.springframework:spring-core:6.0.10 (*)
     +--- org.springframework:spring-tx:6.0.10
     |    +--- org.springframework:spring-beans:6.0.10 (*)
     |    \--- org.springframework:spring-core:6.0.10 (*)
     +--- org.springframework.retry:spring-retry:2.0.2
     +--- io.projectreactor:reactor-core:3.5.7
     |    \--- org.reactivestreams:reactive-streams:1.0.4
     \--- io.micrometer:micrometer-observation:1.11.1
          \--- io.micrometer:micrometer-commons:1.11.1</code></pre>
</div>
</div>
<div class="paragraph">
<p>It&#8217;s provided a version of <code>6.1.1</code> for the <code>spring-integration-core</code> dependency.</p>
</div>
<div class="sect3">
<h4 id="dependency-management-configuration-bom-import-multiple"><a class="anchor" href="#dependency-management-configuration-bom-import-multiple"></a>4.2.1. Importing Multiple Boms</h4>
<div class="paragraph">
<p>If you import more than one bom, the order in which the boms are imported can be important.
The boms are processed in the order in which they are imported.
If multiple boms provide dependency management for the same dependency, the dependency management from the last bom will be used.</p>
</div>
</div>
<div class="sect3">
<h4 id="dependency-management-configuration-bom-import-override"><a class="anchor" href="#dependency-management-configuration-bom-import-override"></a>4.2.2. Overriding Versions in a Bom</h4>
<div class="paragraph">
<p>If you want to deviate slightly from the dependency management provided by a bom, it can be useful to be able to override a particular managed version.
There are two ways to do this:</p>
</div>
<div class="olist arabic">
<ol class="arabic">
<li>
<p>Change the value of a version property</p>
</li>
<li>
<p>Override the dependency management</p>
</li>
</ol>
</div>
<div class="sect4">
<h5 id="dependency-management-configuration-bom-import-override-property"><a class="anchor" href="#dependency-management-configuration-bom-import-override-property"></a>Changing the Value of a Version Property</h5>
<div class="paragraph">
<p>If the bom has been written to use properties for its versions then you can override the version by providing a different value for the relevant version property.</p>
</div>
<div class="admonitionblock note">
<table>
<tr>
<td class="icon">
<i class="fa icon-note" title="Note"></i>
</td>
<td class="content">
You should only use this approach if you do not intend to <a href="#pom-generation">generate and publish a Maven pom</a> for your project as it will result in a pom that does not override the version.
</td>
</tr>
</table>
</div>
<div class="paragraph">
<p>Building on the example above, the Spring IO Platform bom that is used contains a property named <code>spring.version</code>.
This property determines the version of all of the Spring Framework modules and, by default, its value is <code>4.0.6.RELEASE</code>.</p>
</div>
<div class="paragraph">
<p>A property can be overridden as part of importing a bom, as shown in the following example:</p>
</div>
<div class="listingblock primary">
<div class="title">Groovy</div>
<div class="content">
<pre class="highlight"><code class="language-groovy" data-lang="groovy">dependencyManagement {
    imports {
        mavenBom('org.springframework.boot:spring-boot-dependencies:3.1.1') {
            bomProperty 'spring-framework.version', '6.0.9'
        }
    }
}
</code></pre>
</div>
</div>
<div class="listingblock secondary">
<div class="title">Kotlin</div>
<div class="content">
<pre class="highlight"><code class="language-kotlin" data-lang="kotlin">dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.1.1") {
            bomProperty("spring-framework.version", "6.0.9")
        }
    }
}
</code></pre>
</div>
</div>
<div class="paragraph">
<p>You can also use a map, as shown in the following example:</p>
</div>
<div class="listingblock primary">
<div class="title">Groovy</div>
<div class="content">
<pre class="highlight"><code class="language-groovy" data-lang="groovy">dependencyManagement {
    imports {
        mavenBom('org.springframework.boot:spring-boot-dependencies:3.1.1') {
            bomProperties([
                'spring-framework.version': '6.0.9'
            ])
        }
    }
}
</code></pre>
</div>
</div>
<div class="listingblock secondary">
<div class="title">Kotlin</div>
<div class="content">
<pre class="highlight"><code class="language-groovy" data-lang="groovy">dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.1.1") {
            bomProperties(mapOf(
                "spring-framework.version" to "6.0.9"
            ))
        }
    }
}
</code></pre>
</div>
</div>
<div class="paragraph">
<p>Alternatively, the property can also be overridden using a project&#8217;s properties configured via any of the mechanisms that Gradle provides.
You may choose to configure it in your <code>build.gradle</code> script, as shown in the following example:</p>
</div>
<div class="listingblock primary">
<div class="title">Groovy</div>
<div class="content">
<pre class="highlight"><code class="language-groovy" data-lang="groovy">ext['spring-framework.version'] = '6.0.9'
</code></pre>
</div>
</div>
<div class="listingblock secondary">
<div class="title">Kotlin</div>
<div class="content">
<pre class="highlight"><code class="language-kotlin" data-lang="kotlin">ext["spring-framework.version"] = "6.0.9"
</code></pre>
</div>
</div>
<div class="paragraph">
<p>Or in <code>gradle.properties</code></p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlight"><code>spring-framework.version=6.0.9</code></pre>
</div>
</div>
<div class="paragraph">
<p>Wherever you configure it, the version of any Spring Framework modules will now match the value of the property:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlight"><code class="language-shell" data-lang="shell">$ gradle dependencies --configuration compileClasspath

&gt; Task :dependencies

------------------------------------------------------------
Root project
------------------------------------------------------------

compileClasspath - Compile classpath for source set 'main'.
\--- org.springframework.integration:spring-integration-core -&gt; 6.1.1
     +--- org.springframework:spring-aop:6.0.10 -&gt; 6.0.9
     |    +--- org.springframework:spring-beans:6.0.9
     |    |    \--- org.springframework:spring-core:6.0.9
     |    |         \--- org.springframework:spring-jcl:6.0.9
     |    \--- org.springframework:spring-core:6.0.9 (*)
     +--- org.springframework:spring-context:6.0.10 -&gt; 6.0.9
     |    +--- org.springframework:spring-aop:6.0.9 (*)
     |    +--- org.springframework:spring-beans:6.0.9 (*)
     |    +--- org.springframework:spring-core:6.0.9 (*)
     |    \--- org.springframework:spring-expression:6.0.9
     |         \--- org.springframework:spring-core:6.0.9 (*)
     +--- org.springframework:spring-messaging:6.0.10 -&gt; 6.0.9
     |    +--- org.springframework:spring-beans:6.0.9 (*)
     |    \--- org.springframework:spring-core:6.0.9 (*)
     +--- org.springframework:spring-tx:6.0.10 -&gt; 6.0.9
     |    +--- org.springframework:spring-beans:6.0.9 (*)
     |    \--- org.springframework:spring-core:6.0.9 (*)
     +--- org.springframework.retry:spring-retry:2.0.2
     +--- io.projectreactor:reactor-core:3.5.7
     |    \--- org.reactivestreams:reactive-streams:1.0.4
     \--- io.micrometer:micrometer-observation:1.11.1
          \--- io.micrometer:micrometer-commons:1.11.1</code></pre>
</div>
</div>
</div>
<div class="sect4">
<h5 id="dependency-management-configuration-bom-import-override-dependency-management"><a class="anchor" href="#dependency-management-configuration-bom-import-override-dependency-management"></a>Overriding the Dependency Management</h5>
<div class="paragraph">
<p>If the bom that you have imported does not use properties, or you want the override to be honoured in the Maven pom that&#8217;s generated for your Gradle project, you should use dependency management to perform the override.
For example, if you&#8217;re using <code>spring-boot-dependencies</code>, you can override its version of HikariCP and have that override apply to the generated pom, as shown in the following example:</p>
</div>
<div class="listingblock primary">
<div class="title">Groovy</div>
<div class="content">
<pre class="highlight"><code class="language-groovy" data-lang="groovy">dependencyManagement {
    imports {
        mavenBom 'org.springframework.boot:spring-boot-dependencies:3.1.1'
    }
    dependencies {
        dependency 'com.zaxxer:HikariCP:5.0.0'
    }
}
</code></pre>
</div>
</div>
<div class="listingblock secondary">
<div class="title">Kotlin</div>
<div class="content">
<pre class="highlight"><code class="language-kotlin" data-lang="kotlin">dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.1.1")
    }
    dependencies {
        dependency("com.zaxxer:HikariCP:5.0.0")
    }
}
</code></pre>
</div>
</div>
<div class="paragraph">
<p>This will produce the following <code>&lt;dependencyManagement&gt;</code> in the generated pom file:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlight"><code class="language-xml" data-lang="xml">&lt;dependencyManagement&gt;
    &lt;dependencies&gt;
        &lt;dependency&gt;
            &lt;groupId&gt;org.springframework.boot&lt;/groupId&gt;
            &lt;artifactId&gt;spring-boot-dependencies&lt;/artifactId&gt;
            &lt;version&gt;3.1.1&lt;/version&gt;
            &lt;scope&gt;import&lt;/scope&gt;
            &lt;type&gt;pom&lt;/type&gt;
        &lt;/dependency&gt;
        &lt;dependency&gt;
            &lt;groupId&gt;com.zaxxer&lt;/groupId&gt;
            &lt;artifactId&gt;HikariCP&lt;/artifactId&gt;
            &lt;version&gt;5.0.0&lt;/version&gt;
        &lt;/dependency&gt;
    &lt;/dependencies&gt;
&lt;/dependencyManagement&gt;</code></pre>
</div>
</div>
<div class="paragraph">
<p>The dependency management for HikariCP that&#8217;s declared directly in the pom takes precedence over any dependency management for it in <code>spring-boot-dependencies</code> that&#8217;s been imported.</p>
</div>
<div class="paragraph">
<p>You can also override the dependency management by declaring a dependency and configuring it with the desired version, as shown in the following example:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlight"><code>dependencies {
    implementation("com.zaxxer:HikariCP:5.0.0")
}</code></pre>
</div>
</div>
<div class="paragraph">
<p>This will cause any dependency (direct or transitive) on <code>com.zaxxer:HikariCP</code> in the <code>implementation</code> configuration to use version <code>5.0.0</code>, overriding any dependency management that may exist.
If you do not want a project&#8217;s dependencies to override its dependency management, this behavior can be disabled using <code>overriddenByDependencies</code>, as shown in the following example:</p>
</div>
<div class="listingblock primary">
<div class="title">Groovy</div>
<div class="content">
<pre class="highlight"><code class="language-groovy" data-lang="groovy">dependencyManagement {
    overriddenByDependencies = false
}
</code></pre>
</div>
</div>
<div class="listingblock secondary">
<div class="title">Kotlin</div>
<div class="content">
<pre class="highlight"><code class="language-kotlin" data-lang="kotlin">dependencyManagement {
    overriddenByDependencies(false)
}
</code></pre>
</div>
</div>
</div>
</div>
<div class="sect3">
<h4 id="dependency-management-configuration-import-bom-resolution-strategy"><a class="anchor" href="#dependency-management-configuration-import-bom-resolution-strategy"></a>4.2.3. Configuring the Dependency Management Resolution Strategy</h4>
<div class="paragraph">
<p>The plugin uses separate, detached configurations for its internal dependency resolution.
You can configure the resolution strategy for these configurations using a closure.
If you&#8217;re using a snapshot, you may want to disable the caching of an imported bom by configuring Gradle to cache changing modules for zero seconds, as shown in the following example:</p>
</div>
<div class="listingblock primary">
<div class="title">Groovy</div>
<div class="content">
<pre class="highlight"><code class="language-groovy" data-lang="groovy">dependencyManagement {
    resolutionStrategy {
        cacheChangingModulesFor 0, 'seconds'
    }
}
</code></pre>
</div>
</div>
<div class="listingblock secondary">
<div class="title">Kotlin</div>
<div class="content">
<pre class="highlight"><code class="language-kotlin" data-lang="kotlin">dependencyManagement {
    resolutionStrategy {
        cacheChangingModulesFor(0, TimeUnit.SECONDS)
    }
}
</code></pre>
</div>
</div>
</div>
</div>
<div class="sect2">
<h3 id="dependency-management-configuration-specific"><a class="anchor" href="#dependency-management-configuration-specific"></a>4.3. Dependency Management for Specific Configurations</h3>
<div class="paragraph">
<p>To target dependency management at a single configuration, you nest the dependency management within a block named after the configuration, such as <code>implementation</code> as shown in the following example:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlight"><code>dependencyManagement {
     implementation {
          dependencies {
               // …
          }
          imports {
               // …
          }
     }
}</code></pre>
</div>
</div>
<div class="paragraph">
<p>To target dependency management at multiple configurations, you use <code>configurations</code> to list the configurations to which the dependency management should be applied, as shown in the following example:</p>
</div>
<div class="listingblock primary">
<div class="title">Groovy</div>
<div class="content">
<pre class="highlight"><code class="language-groovy" data-lang="groovy">dependencyManagement {
     configurations(implementation, custom) {
          dependencies {
               …
          }
          imports {
               …
          }
     }
}
</code></pre>
</div>
</div>
<div class="listingblock secondary">
<div class="title">Kotlin</div>
<div class="content">
<pre class="highlight"><code class="language-kotlin" data-lang="kotlin">dependencyManagement {
    configurations {
        listOf("implementation", "custom").forEach {configName -&gt;
            getByName(configName) {
                dependencies {
                    …
                }
                imports {
                    …
                }
            }
        }

    }
}
</code></pre>
</div>
</div>
</div>
</div>
</div>
<div class="sect1">
<h2 id="accessing-properties"><a class="anchor" href="#accessing-properties"></a>5. Accessing Properties from Imported Boms</h2>
<div class="sectionbody">
<div class="paragraph">
<p>The plugin makes all of the properties from imported boms available for use in your Gradle build.
Properties from both global dependency management and configuration-specific dependency management can be accessed.
A property named <code>spring.version</code> from global dependency management can be accessed as shown in the following example:</p>
</div>
<div class="listingblock primary">
<div class="title">Groovy</div>
<div class="content">
<pre class="highlight"><code class="language-groovy" data-lang="groovy">dependencyManagement.importedProperties['spring-framework.version']
</code></pre>
</div>
</div>
<div class="listingblock secondary">
<div class="title">Kotlin</div>
<div class="content">
<pre class="highlight"><code class="language-kotlin" data-lang="kotlin">dependencyManagement.importedProperties["spring-framework.version"]
</code></pre>
</div>
</div>
<div class="paragraph">
<p>The same property from the implementation configuration&#8217;s dependency management can be accessed as shown in the following example:</p>
</div>
<div class="listingblock primary">
<div class="title">Groovy</div>
<div class="content">
<pre class="highlight"><code class="language-groovy" data-lang="groovy">dependencyManagement.implementation.importedProperties['spring-framework.version']
</code></pre>
</div>
</div>
<div class="admonitionblock note">
<table>
<tr>
<td class="icon">
<i class="fa icon-note" title="Note"></i>
</td>
<td class="content">
Accessing imported properties for a specific configuration is not currently supported when using the Kotlin DSL.
</td>
</tr>
</table>
</div>
</div>
</div>
<div class="sect1">
<h2 id="maven-exclusions"><a class="anchor" href="#maven-exclusions"></a>6. Maven Exclusions</h2>
<div class="sectionbody">
<div class="paragraph">
<p>While Gradle can consume dependencies described with a Maven pom file, Gradle does not honour Maven&#8217;s semantics when it is using the pom to build the dependency graph.
A notable difference that results from this is in how exclusions are handled.
This is best illustrated with an example.</p>
</div>
<div class="paragraph">
<p>Consider a Maven artifact, <code>exclusion-example</code>, that declares a dependency on <code>org.springframework:spring-core</code> in its pom with an exclusion for <code>org.springframework:spring-jcl</code>, as illustrated in the following example:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlight"><code class="language-xml" data-lang="xml">&lt;dependency&gt;
    &lt;groupId&gt;org.springframework&lt;/groupId&gt;
    &lt;artifactId&gt;spring-core&lt;/artifactId&gt;
    &lt;version&gt;6.0.10&lt;/version&gt;
    &lt;exclusions&gt;
        &lt;exclusion&gt;
            &lt;groupId&gt;org.springframework&lt;/groupId&gt;
            &lt;artifactId&gt;spring-jcl&lt;/artifactId&gt;
        &lt;/exclusion&gt;
    &lt;/exclusions&gt;
&lt;/dependency&gt;</code></pre>
</div>
</div>
<div class="paragraph">
<p>If we have a Maven project, <code>consumer</code>, that depends on <code>exclusion-example</code> and <code>org.springframework:spring-beans</code> the exclusion in <code>exclusion-example</code> prevents a transitive dependency on <code>org.springframework:spring-jcl</code>.
This can be seen in the following output from <code>mvn dependency:tree</code>:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlight"><code>+- com.example:exclusion-example:jar:1.0:compile
|  \- org.springframework:spring-core:jar:6.0.10:compile
\- org.springframework:spring-beans:jar:6.0.10:compile</code></pre>
</div>
</div>
<div class="paragraph">
<p>If we create a similar project in Gradle the dependencies are different as the exclusion of <code>org.springframework:spring-jcl</code> is not honored.
This can be seen in the following output from <code>gradle dependencies</code>:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlight"><code>+--- com.example:exclusion-example:1.0
|    \--- org.springframework:spring-core:6.0.10
|         \--- org.springframework:spring-jcl:6.0.10
\--- org.springframework:spring-beans:6.0.10
     \--- org.springframework:spring-core:6.0.10 (*)</code></pre>
</div>
</div>
<div class="paragraph">
<p>Despite <code>exclusion-example</code> excluding <code>spring-jcl</code> from its <code>spring-core</code> dependency, <code>spring-core</code> has still pulled in <code>spring-jcl</code>.</p>
</div>
<div class="paragraph">
<p>The dependency management plugin improves Gradle&#8217;s handling of exclusions that have been declared in a Maven pom by honoring Maven&#8217;s semantics for those exclusions.
This applies to exclusions declared in a project&#8217;s dependencies that have a Maven pom and exclusions declared in imported Maven boms.</p>
</div>
<div class="sect2">
<h3 id="maven-exclusions-disabling"><a class="anchor" href="#maven-exclusions-disabling"></a>6.1. Disabling Maven exclusions</h3>
<div class="paragraph">
<p>The plugin&#8217;s support for applying Maven&#8217;s exclusion semantics can be disabled by setting <code>applyMavenExclusions</code> to false, as shown in the following example:</p>
</div>
<div class="listingblock primary">
<div class="title">Groovy</div>
<div class="content">
<pre class="highlight"><code class="language-groovy" data-lang="groovy">dependencyManagement {
    applyMavenExclusions = false
}
</code></pre>
</div>
</div>
<div class="listingblock secondary">
<div class="title">Kotlin</div>
<div class="content">
<pre class="highlight"><code class="language-kotlin" data-lang="kotlin">dependencyManagement {
    applyMavenExclusions(false)
}
</code></pre>
</div>
</div>
</div>
</div>
</div>
<div class="sect1">
<h2 id="pom-generation"><a class="anchor" href="#pom-generation"></a>7. Pom generation</h2>
<div class="sectionbody">
<div class="paragraph">
<p>Gradle&#8217;s <code>maven-publish</code> plugin automatically generates a pom file that describes the published artifact.
The dependency management plugin will automatically include any global dependency management, i.e. dependency management that does not target a specific configuration, in the <code>&lt;dependencyManagement&gt;</code> section of the generated pom file.
For example, the following dependency management configuration:</p>
</div>
<div class="listingblock primary">
<div class="title">Groovy</div>
<div class="content">
<pre class="highlight"><code class="language-groovy" data-lang="groovy">dependencyManagement {
    imports {
        mavenBom 'com.example:bom:1.0'
    }
    dependencies {
        dependency 'com.example:dependency:1.5'
    }
}
</code></pre>
</div>
</div>
<div class="listingblock secondary">
<div class="title">Kotlin</div>
<div class="content">
<pre class="highlight"><code class="language-kotlin" data-lang="kotlin">dependencyManagement {
    imports {
        mavenBom("com.example:bom:1.0")
    }
    dependencies {
        dependency("com.example:dependency:1.5")
    }
}
</code></pre>
</div>
</div>
<div class="paragraph">
<p>Will result in the following <code>&lt;dependencyManagement&gt;</code> in the generated pom file:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlight"><code class="language-xml" data-lang="xml">&lt;dependencyManagement&gt;
     &lt;dependencies&gt;
          &lt;dependency&gt;
               &lt;groupId&gt;com.example&lt;/groupId&gt;
               &lt;artifactId&gt;bom&lt;/artifactId&gt;
               &lt;version&gt;1.0&lt;/version&gt;
               &lt;scope&gt;import&lt;/scope&gt;
               &lt;type&gt;pom&lt;/type&gt;
          &lt;dependency&gt;
          &lt;dependency&gt;
               &lt;groupId&gt;com.example&lt;/groupId&gt;
               &lt;artifactId&gt;dependency&lt;/artifactId&gt;
               &lt;version&gt;1.5&lt;/version&gt;
          &lt;/dependency&gt;
     &lt;dependencies&gt;
&lt;/dependencyManagement&gt;</code></pre>
</div>
</div>
<div class="sect2">
<h3 id="pom-generation-disabling"><a class="anchor" href="#pom-generation-disabling"></a>7.1. Disabling the customization of a generated pom</h3>
<div class="paragraph">
<p>If you prefer to have complete control over your project&#8217;s generated pom, you can disable the plugin&#8217;s customization by setting <code>enabled</code> to false, as shown in the following example:</p>
</div>
<div class="listingblock primary">
<div class="title">Groovy</div>
<div class="content">
<pre class="highlight"><code class="language-groovy" data-lang="groovy">dependencyManagement {
    generatedPomCustomization {
        enabled = false
    }
}
</code></pre>
</div>
</div>
<div class="listingblock secondary">
<div class="title">Kotlin</div>
<div class="content">
<pre class="highlight"><code class="language-kotlin" data-lang="kotlin">dependencyManagement {
    generatedPomCustomization {
        enabled(false)
    }
}
</code></pre>
</div>
</div>
</div>
<div class="sect2">
<h3 id="pom-generation-manual"><a class="anchor" href="#pom-generation-manual"></a>7.2. Configuring your own pom</h3>
<div class="paragraph">
<p>If your build creates a pom outside of Gradle&#8217;s standard <code>maven-publish</code> mechanism you can still configure its dependency management by using the <code>pomConfigurer</code> from <code>dependencyManagement</code>:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlight"><code>dependencyManagement.pomConfigurer.configurePom(yourPom)</code></pre>
</div>
</div>
</div>
</div>
</div>
<div class="sect1">
<h2 id="working-with-managed-versions"><a class="anchor" href="#working-with-managed-versions"></a>8. Working with the Managed Versions</h2>
<div class="sectionbody">
<div class="sect2">
<h3 id="working-with-managed-versions-dependency-management-task"><a class="anchor" href="#working-with-managed-versions-dependency-management-task"></a>8.1. Dependency Management Task</h3>
<div class="paragraph">
<p>The plugin provides a task, <code>dependencyManagement</code>, that will output a report of the project&#8217;s dependency management, as shown in the following example:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlight"><code class="language-shell" data-lang="shell">$  gradle dependencyManagement

&gt; Task :dependencyManagement

------------------------------------------------------------
Root project
------------------------------------------------------------

global - Default dependency management for all configurations
    org.springframework:spring-core 6.0.10

annotationProcessor - Dependency management for the annotationProcessor configuration
No configuration-specific dependency management

apiElements - Dependency management for the apiElements configuration
No configuration-specific dependency management

archives - Dependency management for the archives configuration
No configuration-specific dependency management

compile - Dependency management for the compile configuration
No configuration-specific dependency management

compileClasspath - Dependency management for the compileClasspath configuration
No configuration-specific dependency management

compileOnly - Dependency management for the compileOnly configuration
No configuration-specific dependency management

default - Dependency management for the default configuration
No configuration-specific dependency management

implementation - Dependency management for the implementation configuration
No configuration-specific dependency management

runtime - Dependency management for the runtime configuration
No configuration-specific dependency management

runtimeClasspath - Dependency management for the runtimeClasspath configuration
No configuration-specific dependency management

runtimeElements - Dependency management for the runtimeElements configuration
No configuration-specific dependency management

runtimeOnly - Dependency management for the runtimeOnly configuration
No configuration-specific dependency management

testAnnotationProcessor - Dependency management for the testAnnotationProcessor configuration
No configuration-specific dependency management

testCompile - Dependency management for the testCompile configuration
No configuration-specific dependency management

testCompileClasspath - Dependency management for the testCompileClasspath configuration
    org.springframework:spring-beans 6.0.10
    org.springframework:spring-core 6.0.10

testCompileOnly - Dependency management for the testCompileOnly configuration
No configuration-specific dependency management

testImplementation - Dependency management for the testImplementation configuration
    org.springframework:spring-beans 6.0.10
    org.springframework:spring-core 6.0.10

testRuntime - Dependency management for the testRuntime configuration
No configuration-specific dependency management

testRuntimeClasspath - Dependency management for the testRuntimeClasspath configuration
    org.springframework:spring-beans 6.0.10
    org.springframework:spring-core 6.0.10

testRuntimeOnly - Dependency management for the testRuntimeOnly configuration
No configuration-specific dependency management</code></pre>
</div>
</div>
<div class="paragraph">
<p>This report is produced by a project with the following dependency management:</p>
</div>
<div class="listingblock">
<div class="content">
<pre class="highlight"><code class="language-groovy" data-lang="groovy">dependencyManagement {
    dependencies {
        dependency 'org.springframework:spring-core:6.0.10'
    }
    testImplementation {
        dependencies {
            dependency 'org.springframework:spring-beans:6.0.10'
        }
    }
}
</code></pre>
</div>
</div>
</div>
<div class="sect2">
<h3 id="working-with-managed-versions-programmatic-access"><a class="anchor" href="#working-with-managed-versions-programmatic-access"></a>8.2. Programmatic access</h3>
<div class="paragraph">
<p>The plugin provides an API for accessing the versions provided by the configured dependency management.
The managed versions from global dependency management are available from <code>dependencyManagement.managedVersions</code>, as shown in the following example:</p>
</div>
<div class="listingblock primary">
<div class="title">Groovy</div>
<div class="content">
<pre class="highlight"><code class="language-groovy" data-lang="groovy">def managedVersions = dependencyManagement.managedVersions
</code></pre>
</div>
</div>
<div class="listingblock secondary">
<div class="title">Kotlin</div>
<div class="content">
<pre class="highlight"><code class="language-groovy" data-lang="groovy">val managedVersions = dependencyManagement.managedVersions
</code></pre>
</div>
</div>
<div class="paragraph">
<p>Managed versions from configuration-specific dependency management are available from <code>dependencyManagement.&lt;configuration&gt;.managedVersions</code>, as shown in the following example for the <code>implementation</code> configuratation:</p>
</div>
<div class="listingblock primary">
<div class="title">Groovy</div>
<div class="content">
<pre class="highlight"><code class="language-groovy" data-lang="groovy">def managedVersions = dependencyManagement.implementation.managedVersions
</code></pre>
</div>
</div>
<div class="listingblock secondary">
<div class="title">Kotlin</div>
<div class="content">
<pre class="highlight"><code class="language-kotlin" data-lang="kotlin">dependencyManagement.getManagedVersionsForConfiguration(configurations.getByName("implementation"))
</code></pre>
</div>
</div>
<div class="paragraph">
<p>The managed versions are of map of <code>groupId:artifactId</code> to <code>version</code>, as shown in the following example for accessing the version of <code>org.springframework:spring-core</code>:</p>
</div>
<div class="listingblock primary">
<div class="title">Groovy</div>
<div class="content">
<pre class="highlight"><code class="language-groovy" data-lang="groovy">def springCoreVersion = managedVersions['org.springframework:spring-core']
</code></pre>
</div>
</div>
<div class="listingblock secondary">
<div class="title">Kotlin</div>
<div class="content">
<pre class="highlight"><code class="language-kotlin" data-lang="kotlin">val springCoreVersion = managedVersions["org.springframework:spring-core"]
</code></pre>
</div>
</div>
</div>
</div>
</div>
</div>
<div id="footer">
<div id="footer-text">
Version 1.1.7<br>
Last updated 2024-12-17 08:57:33 UTC
</div>
</div>
</div>
  </div>
</div>
</body>
</html>