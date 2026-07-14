---
snapshot_id: spring-dependency-management
source: "https://docs.spring.io/dependency-management-plugin/docs/current/reference/html/"
fetched_at: "2026-07-14T00:00:00Z"
version_observed: "as published, fetched 2026-07-14"
via: curl
tier: 3
bytes: 24973
sha: "e15796cc31193a3d59db159ac70232bf3ae579c46765562de2e464d4e026dd74"
---

# spring dependency management — upstream snapshot

Source: https://docs.spring.io/dependency-management-plugin/docs/current/reference/html/
Fetched: 2026-07-14

Dependency Management Plugin
Dark Theme

# Dependency Management Plugin
Andy Wilkinson

version 1.1.7
Table of Contents
1. Introduction
2. Requirements
3. Getting Started
4. Dependency Management Configuration
4.1. Dependency Management DSL
4.1.1. Dependency Sets
4.1.2. Exclusions
4.2. Importing a Maven Bom
4.2.1. Importing Multiple Boms
4.2.2. Overriding Versions in a Bom
Changing the Value of a Version Property
Overriding the Dependency Management
4.2.3. Configuring the Dependency Management Resolution Strategy
4.3. Dependency Management for Specific Configurations
5. Accessing Properties from Imported Boms
6. Maven Exclusions
6.1. Disabling Maven exclusions
7. Pom generation
7.1. Disabling the customization of a generated pom
7.2. Configuring your own pom
8. Working with the Managed Versions
8.1. Dependency Management Task
8.2. Programmatic access
A Gradle plugin that provides Maven-like dependency management and exclusions

## 1. Introduction
Based on the configured dependency management metadata, the Dependency Management Plugin will control the versions of your project’s direct and transitive dependencies and will honour any exclusions declared in the poms of your project’s dependencies.

## 2. Requirements
The Plugin has the following requirements:
Gradle 6.x (6.8 or later), 7.x, or 8.x.
Gradle 6.7 and earlier are not supported.
Java 8 or later

## 3. Getting Started
The plugin is available in the Gradle Plugin Portal and can be applied like this:
Groovy
plugins {
 id "io.spring.dependency-management" version <>
}
Kotlin
plugins {
 id("io.spring.dependency-management") version <>
}
If you prefer, the plugin is also available from Maven Central and JCenter.
Snapshots are available from repo.spring.io/plugins-snapshot and can be used as shown in the following example:
Groovy
buildscript {
 repositories {
 maven { url 'https://repo.spring.io/plugins-snapshot' }
 }
 dependencies {
 classpath 'io.spring.gradle:dependency-management-plugin:<>'
 }
}

apply plugin: "io.spring.dependency-management"
Kotlin
buildscript {
 repositories {
 maven {
 url = uri("https://repo.spring.io/plugins-snapshot")
 }
 }
 dependencies {
 classpath("io.spring.gradle:dependency-management-plugin:<>")
 }
}

apply(plugin = "io.spring.dependency-management")
With this basic configuration in place, you’re ready to configure the project’s dependency management and declare its dependencies.

## 4. Dependency Management Configuration
You have two options for configuring the plugin’s dependency management:
Use the plugin’s DSL to configure dependency management directly
Import one or more existing Maven boms.
Dependency management can be applied to every configuration (the default) or to one or more specific configurations.

### 4.1. Dependency Management DSL
The DSL allows you to declare dependency management using a : separated string to configure the coordinates of the managed dependency, as shown in the following example:
Groovy
dependencyManagement {
 dependencies {
 dependency 'org.springframework:spring-core:6.0.10'
 }
}
Kotlin
dependencyManagement {
 dependencies {
 dependency("org.springframework:spring-core:6.0.10")
 }
}
Alternatively, you can use a map with group, name, and version entries, as shown in the following example:
Groovy
dependencyManagement {
 dependencies {
 dependency group:'org.springframework', name:'spring-core', version:'6.0.10'
 }
}
Kotlin
dependencyManagement {
 dependencies {
 dependency(mapOf(
 "group" to "org.springframework",
 "name" to "spring-core",
 "version" to "6.0.10"
 ))
 }
}
With either syntax, this configuration will cause all dependencies (direct or transitive) on spring-core to have the version 6.0.10.
When dependency management is in place, you can declare a dependency without a version, as shown in the following example:
Groovy
dependencies {
 implementation 'org.springframework:spring-core'
}
Kotlin
dependencies {
 implementation("org.springframework:spring-core")
}

#### 4.1.1. Dependency Sets
When you want to provide dependency management for multiple modules with the same group and version you should use a dependency set.
Using a dependency set removes the need to specify the same group and version multiple times, as shown in the following example:
Groovy
dependencyManagement {
 dependencies {
 dependencySet(group:'org.slf4j', version: '2.0.7') {
 entry 'slf4j-api'
 entry 'slf4j-simple'
 }
 }
}
Kotlin
dependencyManagement {
 dependencies {
 dependencySet("org.slf4j:2.0.7") {
 entry("slf4j-api")
 entry("slf4j-simple")
 }
 }
}

#### 4.1.2. Exclusions
You can also use the DSL to declare exclusions.
The two main advantages of using this mechanism are that they will be included in the of your project’s generated pom and that they will be applied using Maven’s exclusion semantics.
An exclusion can be declared on individual dependencies, as shown in the following example:
Groovy
dependencyManagement {
 dependencies {
 dependency('org.apache.activemq:activemq-spring:5.18.1') {
 exclude 'commons-logging:commons-logging'
 }
 }
}
Kotlin
dependencyManagement {
 dependencies {
 dependency("org.apache.activemq:activemq-spring:5.18.1") {
 exclude("commons-logging:commons-logging")
 }
 }
}
An exclusion can also be declared on an entry in a dependency set, as shown in the following example:
Groovy
dependencyManagement {
 dependencies {
 dependencySet(group:'org.apache.activemq', version: '5.18.1') {
 entry('activemq-spring') {
 exclude group: 'commons-logging', name: 'commons-logging'
 }
 }
 }
}
Kotlin
dependencyManagement {
 dependencies {
 dependencySet("org.apache.activemq:5.18.1") {
 entry("activemq-spring") {
 exclude(mapOf("group" to "commons-logging", "name" to "commons-logging"))
 }
 }
 }
}
As shown in the two examples above, an exclusion can be identified using a string in the form 'group:name' or a map with group and name entries.
Gradle does not provide an API for accessing a dependency’s classifier during resolution.
Unfortunately, this means that dependency management-based exclusions will not work when a classifier is involved.

### 4.2. Importing a Maven Bom
The plugin also allows you to import an existing Maven bom to utilise its dependency management, as shown in the following example:
Groovy
dependencyManagement {
 imports {
 mavenBom 'org.springframework.boot:spring-boot-dependencies:3.1.1'
 }
}

dependencies {
 implementation 'org.springframework.integration:spring-integration-core'
}
Kotlin
dependencyManagement {
 imports {
 mavenBom("org.springframework.boot:spring-boot-dependencies:3.1.1")
 }
}

dependencies {
 implementation("org.springframework.integration:spring-integration-core")
}
This configuration will apply the versions in spring-boot-dependencies to the project’s dependencies:
$ gradle dependencies --configuration compileClasspath

> Task :dependencies

------------------------------------------------------------
Root project
------------------------------------------------------------

compileClasspath - Compile classpath for source set 'main'.
\--- org.springframework.integration:spring-integration-core -> 6.1.1
 +--- org.springframework:spring-aop:6.0.10
 | +--- org.springframework:spring-beans:6.0.10
 | | \--- org.springframework:spring-core:6.0.10
 | | \--- org.springframework:spring-jcl:6.0.10
 | \--- org.springframework:spring-core:6.0.10 (*)
 +--- org.springframework:spring-context:6.0.10
 | +--- org.springframework:spring-aop:6.0.10 (*)
 | +--- org.springframework:spring-beans:6.0.10 (*)
 | +--- org.springframework:spring-core:6.0.10 (*)
 | \--- org.springframework:spring-expression:6.0.10
 | \--- org.springframework:spring-core:6.0.10 (*)
 +--- org.springframework:spring-messaging:6.0.10
 | +--- org.springframework:spring-beans:6.0.10 (*)
 | \--- org.springframework:spring-core:6.0.10 (*)
 +--- org.springframework:spring-tx:6.0.10
 | +--- org.springframework:spring-beans:6.0.10 (*)
 | \--- org.springframework:spring-core:6.0.10 (*)
 +--- org.springframework.retry:spring-retry:2.0.2
 +--- io.projectreactor:reactor-core:3.5.7
 | \--- org.reactivestreams:reactive-streams:1.0.4
 \--- io.micrometer:micrometer-observation:1.11.1
 \--- io.micrometer:micrometer-commons:1.11.1
It’s provided a version of 6.1.1 for the spring-integration-core dependency.

#### 4.2.1. Importing Multiple Boms
If you import more than one bom, the order in which the boms are imported can be important.
The boms are processed in the order in which they are imported.
If multiple boms provide dependency management for the same dependency, the dependency management from the last bom will be used.

#### 4.2.2. Overriding Versions in a Bom
If you want to deviate slightly from the dependency management provided by a bom, it can be useful to be able to override a particular managed version.
There are two ways to do this:
Change the value of a version property
Override the dependency management

##### Changing the Value of a Version Property
If the bom has been written to use properties for its versions then you can override the version by providing a different value for the relevant version property.
You should only use this approach if you do not intend to generate and publish a Maven pom for your project as it will result in a pom that does not override the version.
Building on the example above, the Spring IO Platform bom that is used contains a property named spring.version.
This property determines the version of all of the Spring Framework modules and, by default, its value is 4.0.6.RELEASE.
A property can be overridden as part of importing a bom, as shown in the following example:
Groovy
dependencyManagement {
 imports {
 mavenBom('org.springframework.boot:spring-boot-dependencies:3.1.1') {
 bomProperty 'spring-framework.version', '6.0.9'
 }
 }
}
Kotlin
dependencyManagement {
 imports {
 mavenBom("org.springframework.boot:spring-boot-dependencies:3.1.1") {
 bomProperty("spring-framework.version", "6.0.9")
 }
 }
}
You can also use a map, as shown in the following example:
Groovy
dependencyManagement {
 imports {
 mavenBom('org.springframework.boot:spring-boot-dependencies:3.1.1') {
 bomProperties([
 'spring-framework.version': '6.0.9'
 ])
 }
 }
}
Kotlin
dependencyManagement {
 imports {
 mavenBom("org.springframework.boot:spring-boot-dependencies:3.1.1") {
 bomProperties(mapOf(
 "spring-framework.version" to "6.0.9"
 ))
 }
 }
}
Alternatively, the property can also be overridden using a project’s properties configured via any of the mechanisms that Gradle provides.
You may choose to configure it in your build.gradle script, as shown in the following example:
Groovy
ext['spring-framework.version'] = '6.0.9'
Kotlin
ext["spring-framework.version"] = "6.0.9"
Or in gradle.properties
spring-framework.version=6.0.9
Wherever you configure it, the version of any Spring Framework modules will now match the value of the property:
$ gradle dependencies --configuration compileClasspath

> Task :dependencies

------------------------------------------------------------
Root project
------------------------------------------------------------

compileClasspath - Compile classpath for source set 'main'.
\--- org.springframework.integration:spring-integration-core -> 6.1.1
 +--- org.springframework:spring-aop:6.0.10 -> 6.0.9
 | +--- org.springframework:spring-beans:6.0.9
 | | \--- org.springframework:spring-core:6.0.9
 | | \--- org.springframework:spring-jcl:6.0.9
 | \--- org.springframework:spring-core:6.0.9 (*)
 +--- org.springframework:spring-context:6.0.10 -> 6.0.9
 | +--- org.springframework:spring-aop:6.0.9 (*)
 | +--- org.springframework:spring-beans:6.0.9 (*)
 | +--- org.springframework:spring-core:6.0.9 (*)
 | \--- org.springframework:spring-expression:6.0.9
 | \--- org.springframework:spring-core:6.0.9 (*)
 +--- org.springframework:spring-messaging:6.0.10 -> 6.0.9
 | +--- org.springframework:spring-beans:6.0.9 (*)
 | \--- org.springframework:spring-core:6.0.9 (*)
 +--- org.springframework:spring-tx:6.0.10 -> 6.0.9
 | +--- org.springframework:spring-beans:6.0.9 (*)
 | \--- org.springframework:spring-core:6.0.9 (*)
 +--- org.springframework.retry:spring-retry:2.0.2
 +--- io.projectreactor:reactor-core:3.5.7
 | \--- org.reactivestreams:reactive-streams:1.0.4
 \--- io.micrometer:micrometer-observation:1.11.1
 \--- io.micrometer:micrometer-commons:1.11.1

##### Overriding the Dependency Management
If the bom that you have imported does not use properties, or you want the override to be honoured in the Maven pom that’s generated for your Gradle project, you should use dependency management to perform the override.
For example, if you’re using spring-boot-dependencies, you can override its version of HikariCP and have that override apply to the generated pom, as shown in the following example:
Groovy
dependencyManagement {
 imports {
 mavenBom 'org.springframework.boot:spring-boot-dependencies:3.1.1'
 }
 dependencies {
 dependency 'com.zaxxer:HikariCP:5.0.0'
 }
}
Kotlin
dependencyManagement {
 imports {
 mavenBom("org.springframework.boot:spring-boot-dependencies:3.1.1")
 }
 dependencies {
 dependency("com.zaxxer:HikariCP:5.0.0")
 }
}
This will produce the following in the generated pom file:
org.springframework.boot
 spring-boot-dependencies
 3.1.1
 import
 pom
 
 
 com.zaxxer
 HikariCP
 5.0.0
The dependency management for HikariCP that’s declared directly in the pom takes precedence over any dependency management for it in spring-boot-dependencies that’s been imported.
You can also override the dependency management by declaring a dependency and configuring it with the desired version, as shown in the following example:
dependencies {
 implementation("com.zaxxer:HikariCP:5.0.0")
}
This will cause any dependency (direct or transitive) on com.zaxxer:HikariCP in the implementation configuration to use version 5.0.0, overriding any dependency management that may exist.
If you do not want a project’s dependencies to override its dependency management, this behavior can be disabled using overriddenByDependencies, as shown in the following example:
Groovy
dependencyManagement {
 overriddenByDependencies = false
}
Kotlin
dependencyManagement {
 overriddenByDependencies(false)
}

#### 4.2.3. Configuring the Dependency Management Resolution Strategy
The plugin uses separate, detached configurations for its internal dependency resolution.
You can configure the resolution strategy for these configurations using a closure.
If you’re using a snapshot, you may want to disable the caching of an imported bom by configuring Gradle to cache changing modules for zero seconds, as shown in the following example:
Groovy
dependencyManagement {
 resolutionStrategy {
 cacheChangingModulesFor 0, 'seconds'
 }
}
Kotlin
dependencyManagement {
 resolutionStrategy {
 cacheChangingModulesFor(0, TimeUnit.SECONDS)
 }
}

### 4.3. Dependency Management for Specific Configurations
To target dependency management at a single configuration, you nest the dependency management within a block named after the configuration, such as implementation as shown in the following example:
dependencyManagement {
 implementation {
 dependencies {
 // …
 }
 imports {
 // …
 }
 }
}
To target dependency management at multiple configurations, you use configurations to list the configurations to which the dependency management should be applied, as shown in the following example:
Groovy
dependencyManagement {
 configurations(implementation, custom) {
 dependencies {
 …
 }
 imports {
 …
 }
 }
}
Kotlin
dependencyManagement {
 configurations {
 listOf("implementation", "custom").forEach {configName ->
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

## 5. Accessing Properties from Imported Boms
The plugin makes all of the properties from imported boms available for use in your Gradle build.
Properties from both global dependency management and configuration-specific dependency management can be accessed.
A property named spring.version from global dependency management can be accessed as shown in the following example:
Groovy
dependencyManagement.importedProperties['spring-framework.version']
Kotlin
dependencyManagement.importedProperties["spring-framework.version"]
The same property from the implementation configuration’s dependency management can be accessed as shown in the following example:
Groovy
dependencyManagement.implementation.importedProperties['spring-framework.version']
Accessing imported properties for a specific configuration is not currently supported when using the Kotlin DSL.

## 6. Maven Exclusions
While Gradle can consume dependencies described with a Maven pom file, Gradle does not honour Maven’s semantics when it is using the pom to build the dependency graph.
A notable difference that results from this is in how exclusions are handled.
This is best illustrated with an example.
Consider a Maven artifact, exclusion-example, that declares a dependency on org.springframework:spring-core in its pom with an exclusion for org.springframework:spring-jcl, as illustrated in the following example:
org.springframework
 spring-core
 6.0.10
 
 
 org.springframework
 spring-jcl
If we have a Maven project, consumer, that depends on exclusion-example and org.springframework:spring-beans the exclusion in exclusion-example prevents a transitive dependency on org.springframework:spring-jcl.
This can be seen in the following output from mvn dependency:tree:
+- com.example:exclusion-example:jar:1.0:compile
| \- org.springframework:spring-core:jar:6.0.10:compile
\- org.springframework:spring-beans:jar:6.0.10:compile
If we create a similar project in Gradle the dependencies are different as the exclusion of org.springframework:spring-jcl is not honored.
This can be seen in the following output from gradle dependencies:
+--- com.example:exclusion-example:1.0
| \--- org.springframework:spring-core:6.0.10
| \--- org.springframework:spring-jcl:6.0.10
\--- org.springframework:spring-beans:6.0.10
 \--- org.springframework:spring-core:6.0.10 (*)
Despite exclusion-example excluding spring-jcl from its spring-core dependency, spring-core has still pulled in spring-jcl.
The dependency management plugin improves Gradle’s handling of exclusions that have been declared in a Maven pom by honoring Maven’s semantics for those exclusions.
This applies to exclusions declared in a project’s dependencies that have a Maven pom and exclusions declared in imported Maven boms.

### 6.1. Disabling Maven exclusions
The plugin’s support for applying Maven’s exclusion semantics can be disabled by setting applyMavenExclusions to false, as shown in the following example:
Groovy
dependencyManagement {
 applyMavenExclusions = false
}
Kotlin
dependencyManagement {
 applyMavenExclusions(false)
}

## 7. Pom generation
Gradle’s maven-publish plugin automatically generates a pom file that describes the published artifact.
The dependency management plugin will automatically include any global dependency management, i.e. dependency management that does not target a specific configuration, in the section of the generated pom file.
For example, the following dependency management configuration:
Groovy
dependencyManagement {
 imports {
 mavenBom 'com.example:bom:1.0'
 }
 dependencies {
 dependency 'com.example:dependency:1.5'
 }
}
Kotlin
dependencyManagement {
 imports {
 mavenBom("com.example:bom:1.0")
 }
 dependencies {
 dependency("com.example:dependency:1.5")
 }
}
Will result in the following in the generated pom file:
com.example
 bom
 1.0
 import
 pom
 
 
 com.example
 dependency
 1.5

### 7.1. Disabling the customization of a generated pom
If you prefer to have complete control over your project’s generated pom, you can disable the plugin’s customization by setting enabled to false, as shown in the following example:
Groovy
dependencyManagement {
 generatedPomCustomization {
 enabled = false
 }
}
Kotlin
dependencyManagement {
 generatedPomCustomization {
 enabled(false)
 }
}

### 7.2. Configuring your own pom
If your build creates a pom outside of Gradle’s standard maven-publish mechanism you can still configure its dependency management by using the pomConfigurer from dependencyManagement:
dependencyManagement.pomConfigurer.configurePom(yourPom)

## 8. Working with the Managed Versions

### 8.1. Dependency Management Task
The plugin provides a task, dependencyManagement, that will output a report of the project’s dependency management, as shown in the following example:
$ gradle dependencyManagement

> Task :dependencyManagement

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
No configuration-specific dependency management
This report is produced by a project with the following dependency management:
dependencyManagement {
 dependencies {
 dependency 'org.springframework:spring-core:6.0.10'
 }
 testImplementation {
 dependencies {
 dependency 'org.springframework:spring-beans:6.0.10'
 }
 }
}

### 8.2. Programmatic access
The plugin provides an API for accessing the versions provided by the configured dependency management.
The managed versions from global dependency management are available from dependencyManagement.managedVersions, as shown in the following example:
Groovy
def managedVersions = dependencyManagement.managedVersions
Kotlin
val managedVersions = dependencyManagement.managedVersions
Managed versions from configuration-specific dependency management are available from dependencyManagement..managedVersions, as shown in the following example for the implementation configuratation:
Groovy
def managedVersions = dependencyManagement.implementation.managedVersions
Kotlin
dependencyManagement.getManagedVersionsForConfiguration(configurations.getByName("implementation"))
The managed versions are of map of groupId:artifactId to version, as shown in the following example for accessing the version of org.springframework:spring-core:
Groovy
def springCoreVersion = managedVersions['org.springframework:spring-core']
Kotlin
val springCoreVersion = managedVersions["org.springframework:spring-core"]
Version 1.1.7

Last updated 2024-12-17 08:57:33 UTC
