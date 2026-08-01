# gradle-toolchains — upstream snapshot (2026-08-01 refresh, append-only)

**Source URL(s):** https://docs.gradle.org/current/userguide/toolchains.html (re-fetched 2026-08-01; every pre-existing section below the divider is preserved verbatim)
**HTTP status:** 200
**Fetched at:** 2026-08-01T02:23:44Z
**Extractor invocation:** `practices/scripts/snapshot-extract.sh https://docs.gradle.org/current/userguide/toolchains.html`
**Fetch receipt:** `practices/upstream/_FETCH-RECEIPTS.yaml` id `r115`
**Body SHA-256 (below the `---` divider, header excluded):** 5624b74675cb72e706c6e3a3c2f1eb1f135a1e81c3ba12cbd861b1d86b976e94

---

---
snapshot_id: gradle-toolchains
source: "https://docs.gradle.org/current/userguide/toolchains.html"
fetched_at: "2026-07-14T00:00:00Z"
version_observed: "as published, fetched 2026-07-14"
via: curl
tier: 3
bytes: 32157
sha: "41abfac6dc96d413d01f72c70084f21f48de0696243c026eedc7f7f16b57674f"
---

# gradle toolchains — upstream snapshot

Source: https://docs.gradle.org/current/userguide/toolchains.html
Fetched: 2026-07-14

Toolchains for JVM projects

# Toolchains for JVM projects
version 9.6.1
On this Page
Toolchains for projects
1. Java toolchains
2. The --release flag
3. Source and Target compatibility
4. Environment variables (JAVA_HOME)
5. IDE settings
Combining toolchains
Comparison table for setting project toolchains
Toolchains for tasks
Integration with tasks relying on a Java executable or Java home
Using Java toolchains
Selecting toolchains by vendor
Selecting toolchains that support GraalVM native image
Selecting toolchains by virtual machine implementation
Configuring toolchain specifications
Auto-detection of installed toolchains
How to disable auto-detection
Auto-provisioning
Toolchain Download Repositories
Viewing and debugging toolchains
Disabling auto provisioning
Removing an auto-provisioned toolchain
Custom toolchain locations
Toolchain installations precedence
Toolchains for plugin authors
Toolchains limitations
Working on multiple projects can require interacting with multiple versions of the Java language.
Even within a single project different parts of the codebase may be fixed to a particular language level due to backward compatibility requirements.
This means different versions of the same tools (a toolchain) must be installed and managed on each machine that builds the project.
A Java toolchain is a set of tools to build and run Java projects, which is usually provided by the environment via local JRE or JDK installations.
Compile tasks may use javac as their compiler, test and exec tasks may use the java command while javadoc will be used to generate documentation.
By default, Gradle uses the same Java toolchain for running Gradle itself and building JVM projects.
However, this may only sometimes be desirable.
Building projects with different Java versions on different developer machines and CI servers may lead to unexpected issues.
Additionally, you may want to build a project using a Java version that is not supported for running Gradle.
In order to improve reproducibility of the builds and make build requirements clearer, Gradle allows configuring toolchains on both project and task levels.
You can also control the JVM used to run Gradle itself using the Daemon JVM criteria.

## Toolchains for projects
Gradle provides multiple ways to configure the Java version used for compiling and running your project.
The five primary mechanisms are:
Java toolchains
The --release flag
Source and Target compatibility
Environment variables (JAVA_HOME)
IDE settings
These settings are not mutually exclusive, and advanced users may need to combine them in specific scenarios.

### 1. Java toolchains
To configure a toolchain for your project, declare the desired Java language version in the java extension block:
build.gradle.kts
java {
 toolchain {
 languageVersion = JavaLanguageVersion.of(17)
 }
}
build.gradle
java {
 toolchain {
 languageVersion = JavaLanguageVersion.of(17)
 }
}
The java block is flexible and supports additional configuration options.
You can learn more in Using Java toolchains.

### 2. The --release flag
For strict cross-compilation, the --release flag is recommended instead of sourceCompatibility and targetCompatibility:
tasks.withType().configureEach {
 options.release = 8
}
tasks.withType(JavaCompile).configureEach {
 options.release = 8
}
This flag prevents accidental use of newer APIs that are not available in the specified version.
However, it does not control which JDK is used—only how the compiler treats source code.
This method can be combined with toolchains if you need both a specific JDK and strict cross-compilation.

### 3. Source and Target compatibility
Setting sourceCompatibility and targetCompatibility tells the Java compiler to produce bytecode compatible with a specific Java version but does not enforce which JDK Gradle itself runs with:
java {
 sourceCompatibility = JavaVersion.VERSION_1_8
 targetCompatibility = JavaVersion.VERSION_1_8
}
java {
 sourceCompatibility = JavaVersion.VERSION_1_8
 targetCompatibility = JavaVersion.VERSION_1_8
}
This does not guarantee the correct JDK is used and may cause issues when APIs have been backported to older Java versions.
You should only use this method in cases where you need backward compatibility but cannot use toolchains.

### 4. Environment variables (JAVA_HOME)
You can influence which JDK Gradle uses by setting the JAVA_HOME environment variable:
export JAVA_HOME=/path/to/java17
This sets a default JDK for all Java-based tools on your system, including Gradle and Maven.
This does not override Gradle’s toolchain support or other project-specific configurations.
This approach is useful for legacy projects that do not use toolchains and expect a specific JDK to be active in the environment.
However, since JAVA_HOME applies globally, it cannot be used to specify different JDK versions for different projects.
It is more reliable to use toolchains, which allow setting the Java version at the project level.

### 5. IDE settings
Most modern IDEs allow you to configure the JVM used to run Gradle when working with a project.
This setting affects how Gradle itself is executed inside the IDE, but not how your code is compiled—unless the build does not explicitly specify a toolchain.
If your build does not define a Java toolchain, Gradle may fall back to using the Java version defined by the IDE settings. This can lead to unintended and non-reproducible behavior, especially if different team members use different IDE configurations.
You should change the IDE’s Gradle JVM setting to align with the JVM used on the command line (JAVA_HOME or the system’s default Java installation) —ensuring consistent behavior across environments (e.g., when running tests or tasks from the IDE vs the terminal).
You should also change the IDE’s Gradle JVM setting if the IDE emits a warning/error when the JVM is not set or does not match with JAVA_HOME.

#### IntelliJ IDEA
To configure the Gradle JVM:
Open Settings (Preferences) > Build, Execution, Deployment > Gradle.
Set Gradle JVM to the desired JDK.

#### Eclipse
To configure the Gradle JVM:
Open Preferences > Gradle > Gradle JDK.
Select the appropriate JDK.
Some IDEs also allow you to configure the Gradle Daemon JVM in the same settings screen.
Be careful not to confuse it with the toolchain or project JVM—make sure you’re selecting the correct one.

### Combining toolchains
In some cases, you may want to:
Use a specific JDK version for compilation (toolchains).
Ensure that the compiled bytecode is compatible with an older Java version (--release or targetCompatibility).
For example, to compile with Java 17 but produce Java 11 bytecode:
build.gradle.kts
java {
 toolchain {
 languageVersion = JavaLanguageVersion.of(17)
 }
}

tasks.withType().configureEach {
 options.release = 11
}
build.gradle
java {
 toolchain {
 languageVersion = JavaLanguageVersion.of(17)
 }
}

tasks.withType(JavaCompile).configureEach {
 options.release = 11
}

### Comparison table for setting project toolchains
Method
Ensures Correct JDK?
Auto Downloads JDK?
Prevents Accidental API Use?
Java toolchains
✅ Yes
✅ Yes
❌ No
--release flag
❌ No
❌ No
✅ Yes
Source & Target compatibility
❌ No
❌ No
❌ No
Environment variables (JAVA_HOME)
✅ Yes (but only globally)
❌ No
❌ No
IDE settings
✅ Yes (inside the IDE)
❌ No
❌ No
Recommendation:
For most users: Use Java toolchains (toolchain.languageVersion).
For strict compatibility enforcement: Use the --release flag.
For advanced cases: Combine toolchains and --release.
Avoid sourceCompatibility and targetCompatibility unless necessary.
Use JAVA_HOME only if you need a default system-wide JDK version.
Use IDE settings if you want Gradle to match your IDE’s JDK version.

## Toolchains for tasks
In case you want to tweak which toolchain is used for a specific task, you can specify the exact tool a task is using.
For example, the Test task exposes a JavaLauncher property that defines which java executable to use for launching the tests.
In the example below, we configure all java compilation tasks to use Java 8.
Additionally, we introduce a new Test task that will run our unit tests using a JDK 17.
list/build.gradle.kts
tasks.withType().configureEach {
 javaCompiler = javaToolchains.compilerFor {
 languageVersion = JavaLanguageVersion.of(8)
 }
}

tasks.register("testsOn17") {
 javaLauncher = javaToolchains.launcherFor {
 languageVersion = JavaLanguageVersion.of(17)
 }
}
list/build.gradle
tasks.withType(JavaCompile).configureEach {
 javaCompiler = javaToolchains.compilerFor {
 languageVersion = JavaLanguageVersion.of(8)
 }
}

tasks.register('testsOn17', Test) {
 javaLauncher = javaToolchains.launcherFor {
 languageVersion = JavaLanguageVersion.of(17)
 }
}
In addition, in the application subproject, we add another Java execution task to run our application with JDK 17.
application/build.gradle.kts
tasks.register("runOn17") {
 javaLauncher = javaToolchains.launcherFor {
 languageVersion = JavaLanguageVersion.of(17)
 }

 classpath = sourceSets["main"].runtimeClasspath
 mainClass = application.mainClass
}
application/build.gradle
tasks.register('runOn17', JavaExec) {
 javaLauncher = javaToolchains.launcherFor {
 languageVersion = JavaLanguageVersion.of(17)
 }

 classpath = sourceSets.main.runtimeClasspath
 mainClass = application.mainClass
}
Depending on the task, a JRE might be enough while for other tasks (e.g. compilation), a JDK is required.
By default, Gradle prefers installed JDKs over JREs if they can satisfy the requirements.
Toolchains tool providers can be obtained from the javaToolchains extension.
Three tools are available:
A JavaCompiler which is the tool used by the JavaCompile task
A JavaLauncher which is the tool used by the JavaExec or Test tasks
A JavadocTool which is the tool used by the Javadoc task

### Integration with tasks relying on a Java executable or Java home
Any task that can be configured with a path to a Java executable, or a Java home location, can benefit from toolchains.
While you will not be able to wire a toolchain tool directly, they all have the metadata that gives access to their full path or to the path of the Java installation they belong to.
For example, you can configure the java executable for a task as follows:
build.gradle.kts
val launcher = javaToolchains.launcherFor {
 languageVersion = JavaLanguageVersion.of(11)
}

tasks.sampleTask {
 javaExecutable = launcher.map { it.executablePath }
}
build.gradle
def launcher = javaToolchains.launcherFor {
 languageVersion = JavaLanguageVersion.of(11)
}

tasks.named('sampleTask') {
 javaExecutable = launcher.map { it.executablePath }
}
As another example, you can configure the Java Home for a task as follows:
build.gradle.kts
val launcher = javaToolchains.launcherFor {
 languageVersion = JavaLanguageVersion.of(11)
}

tasks.anotherSampleTask {
 javaHome = launcher.map { it.metadata.installationPath }
}
build.gradle
def launcher = javaToolchains.launcherFor {
 languageVersion = JavaLanguageVersion.of(11)
}

tasks.named('anotherSampleTask') {
 javaHome = launcher.map { it.metadata.installationPath }
}
If you require a path to a specific tool such as Java compiler, you can obtain it as follows:
build.gradle.kts
val compiler = javaToolchains.compilerFor {
 languageVersion = JavaLanguageVersion.of(11)
}

tasks.yetAnotherSampleTask {
 javaCompilerExecutable = compiler.map { it.executablePath }
}
build.gradle
def compiler = javaToolchains.compilerFor {
 languageVersion = JavaLanguageVersion.of(11)
}

tasks.named('yetAnotherSampleTask') {
 javaCompilerExecutable = compiler.map { it.executablePath }
}
The examples above use tasks with RegularFileProperty and DirectoryProperty properties which allow lazy configuration.
Doing respectively launcher.get().executablePath, launcher.get().metadata.installationPath or compiler.get().executablePath instead will give you the full path for the given toolchain but note that this may realize (and provision) a toolchain eagerly.

## Using Java toolchains
Using Java toolchains allows Gradle to automatically download and manage the required JDK version for your build. It ensures that the correct Java version is used for both compilation and execution.
You can define what toolchain to use for a project by stating the Java language version in the java extension block:
build.gradle.kts
java {
 toolchain {
 languageVersion = JavaLanguageVersion.of(17)
 }
}
build.gradle
java {
 toolchain {
 languageVersion = JavaLanguageVersion.of(17)
 }
}
Executing the build (e.g. using gradle check) will now handle several things for you and others running your build:
Gradle configures all compile, test and javadoc tasks to use the defined toolchain.
Gradle detects locally installed toolchains.
Gradle chooses a toolchain matching the requirements (any Java 17 toolchain for the example above).
If no matching toolchain is found, Gradle can automatically download a matching one based on the configured toolchain download repositories.
Toolchain support is available in the Java plugins and for the tasks they define.
For the Groovy plugin, compilation is supported but not yet Groovydoc generation.
For the Scala plugin, compilation and Scaladoc generation are supported.

### Selecting toolchains by vendor
In case your build has specific requirements from the used JRE/JDK, you may want to define the vendor for the toolchain as well.
JvmVendorSpec has a list of well-known JVM vendors recognized by Gradle.
The advantage is that Gradle can handle any inconsistencies across JDK versions in how exactly the JVM encodes the vendor information.
build.gradle.kts
java {
 toolchain {
 languageVersion = JavaLanguageVersion.of(11)
 vendor = JvmVendorSpec.ADOPTIUM
 }
}
build.gradle
java {
 toolchain {
 languageVersion = JavaLanguageVersion.of(11)
 vendor = JvmVendorSpec.ADOPTIUM
 }
}
If the vendor you want to target is not a known vendor, you can still restrict the toolchain to those matching the java.vendor system property of the available toolchains.
The following snippet uses filtering to include a subset of available toolchains.
This example only includes toolchains whose java.vendor property contains the given match string.
The matching is done in a case-insensitive manner.
build.gradle.kts
java {
 toolchain {
 languageVersion = JavaLanguageVersion.of(11)
 vendor = JvmVendorSpec.matching("customString")
 }
}
build.gradle
java {
 toolchain {
 languageVersion = JavaLanguageVersion.of(11)
 vendor = JvmVendorSpec.matching("customString")
 }
}

### Selecting toolchains that support GraalVM native image
If your project needs a toolchain with GraalVM Native Image capability, you can configure the spec to request it:
build.gradle.kts
java {
 toolchain {
 languageVersion = JavaLanguageVersion.of(21)
 nativeImageCapable = true
 }
}
build.gradle
java {
 toolchain {
 languageVersion = JavaLanguageVersion.of(21)
 nativeImageCapable = true
 }
}
Leaving that value unconfigured or set to false will not restrict the toolchain selection based on the Native Image capability.
That means that a Native Image capable JDK can be selected if it matches the other criteria.

### Selecting toolchains by virtual machine implementation
If your project requires a specific implementation, you can filter based on the implementation as well.
Currently available implementations to choose from are:
VENDOR_SPECIFIC
Acts as a placeholder and matches any implementation from any vendor (e.g. hotspot, zulu, …​)
J9
Matches only virtual machine implementations using the OpenJ9/IBM J9 runtime engine.
For example, to use an IBM JVM, distributed via AdoptOpenJDK,
you can specify the filter as shown in the example below.
build.gradle.kts
java {
 toolchain {
 languageVersion = JavaLanguageVersion.of(11)
 vendor = JvmVendorSpec.IBM
 implementation = JvmImplementation.J9
 }
}
build.gradle
java {
 toolchain {
 languageVersion = JavaLanguageVersion.of(11)
 vendor = JvmVendorSpec.IBM
 implementation = JvmImplementation.J9
 }
}
The Java major version, the vendor (if specified) and implementation (if specified) will be tracked as an input for compilation and test execution.

### Configuring toolchain specifications
Gradle allows configuring multiple properties that affect the selection of a toolchain, such as language version or vendor.
Even though these properties can be configured independently, the configuration must follow certain rules in order to form a valid specification.
A JavaToolchainSpec is considered valid in two cases:
when no properties have been set, i.e. the specification is empty;
when languageVersion has been set, optionally followed by setting any other property.
In other words, if a vendor or an implementation are specified, they must be accompanied by the language version.
Gradle distinguishes between toolchain specifications that configure the language version and the ones that do not.
A specification without a language version, in most cases, would be treated as a one that selects the toolchain of the current build.
Usage of invalid instances of JavaToolchainSpec results in a build error since Gradle 8.0.

## Auto-detection of installed toolchains
By default, Gradle automatically detects local JRE/JDK installations so no further configuration is required by the user.
The following is a list of common package managers, tools, and locations that are supported by the JVM auto-detection.
JVM auto-detection knows how to work with:
Operation-system specific locations: Linux, macOS, Windows
Conventional Environment Variable: JAVA_HOME
Package Managers: Asdf-vm, Jabba, SDKMAN!
Maven Toolchain specifications
IntelliJ IDEA installations
Among the set of all detected JRE/JDK installations, one will be picked according to the Toolchain Precedence Rules.
Whether you are using toolchain auto-detection or you are configuring Custom toolchain locations, installations that are non-existing or without a bin/java executable will be ignored with a warning, but they won’t generate an error.

### How to disable auto-detection
In order to disable auto-detection, you can use the org.gradle.java.installations.auto-detect Gradle property:
Either start Gradle using -Dorg.gradle.java.installations.auto-detect=false
Or put org.gradle.java.installations.auto-detect=false into your gradle.properties file.

## Auto-provisioning
If Gradle can’t find a locally available toolchain that matches the requirements of the build, it can automatically download one (as long as a toolchain download repository has been configured; for detail, see relevant section).
Gradle installs the downloaded JDKs in the Gradle User Home.
Gradle only downloads JDK versions for GA releases.
There is no support for downloading early access versions.
Once installed in the Gradle User Home, a provisioned JDK becomes one of the JDKs visible to auto-detection and can be used by any subsequent builds, just like any other JDK installed on the system.
Since auto-provisioning only kicks in when auto-detection fails to find a matching JDK, auto-provisioning can only download new JDKs and is in no way involved in updating any of the already installed ones.
None of the auto-provisioned JDKs will ever be revisited and automatically updated by auto-provisioning, even if there is a newer minor version available for them.

### Toolchain Download Repositories
Toolchain download repository definitions are added to a build by applying specific settings plugins.
For details on writing such plugins, consult the Toolchain Resolver Plugins page.
One example of a toolchain resolver plugin is the Foojay Toolchains Plugin, based on the foojay Disco API.
It even has a convention variant, which automatically takes care of all the needed configuration, just by being applied:
settings.gradle.kts
plugins {
 id("org.gradle.toolchains.foojay-resolver-convention").version("1.0.0")
}
settings.gradle
plugins {
 id 'org.gradle.toolchains.foojay-resolver-convention' version '1.0.0'
}
For advanced or highly specific configurations, a custom toolchain resolver plugin should be used.
In general, when applying toolchain resolver plugins, the toolchain download resolvers provided by them also need to be configured.
Let’s illustrate with an example.
Consider two toolchain resolver plugins applied by the build:
One is the Foojay plugin mentioned above, which downloads toolchains via the FoojayToolchainResolver it provides.
The other contains a FICTITIOUS resolver named MadeUpResolver.
The following example uses these toolchain resolvers in a build via the toolchainManagement block in the settings file:
settings.gradle.kts
toolchainManagement {
 jvm { (1)
 javaRepositories {
 repository("foojay") { (2)
 resolverClass = org.gradle.toolchains.foojay.FoojayToolchainResolver::class.java
 }
 repository("made_up") { (3)
 resolverClass = MadeUpResolver::class.java
 credentials {
 username = "user"
 password = "password"
 }
 authentication {
 create("digest")
 } (4)
 }
 }
 }
}
settings.gradle
toolchainManagement {
 jvm { (1)
 javaRepositories {
 repository('foojay') { (2)
 resolverClass = org.gradle.toolchains.foojay.FoojayToolchainResolver
 }
 repository('made_up') { (3)
 resolverClass = MadeUpResolver
 credentials {
 username = "user"
 password = "password"
 }
 authentication {
 digest(BasicAuthentication)
 } (4)
 }
 }
 }
}
1
In the toolchainManagement block, the jvm block contains configuration for Java toolchains.
2
The javaRepositories block defines named Java toolchain repository configurations.
Use the resolverClass property to link these configurations to plugins.
3
Toolchain declaration order matters.
Gradle downloads from the first repository that provides a match, starting with the first repository in the list.
4
You can configure toolchain repositories with the same set of authentication and authorization options used for dependency management.
The jvm block in toolchainManagement only resolves after applying a toolchain resolver plugin.

### Viewing and debugging toolchains
Gradle can display the list of all detected toolchains including their metadata.
For example, to show all toolchains of a project, run:
$ ./gradlew -q javaToolchains
> gradle -q javaToolchains

 + Options
 | Auto-detection: Enabled
 | Auto-download: Enabled

 + AdoptOpenJDK 1.8.0_242
 | Location: /Users/username/myJavaInstalls/8.0.242.hs-adpt/jre
 | Language Version: 8
 | Vendor: AdoptOpenJDK
 | Architecture: x86_64
 | Is JDK: false
 | Detected by: Gradle property 'org.gradle.java.installations.paths'

 + Microsoft JDK 16.0.2+7
 | Location: /Users/username/.sdkman/candidates/java/16.0.2.7.1-ms
 | Language Version: 16
 | Vendor: Microsoft
 | Architecture: aarch64
 | Is JDK: true
 | Detected by: SDKMAN!

 + OpenJDK 15-ea
 | Location: /Users/user/customJdks/15.ea.21-open
 | Language Version: 15
 | Vendor: AdoptOpenJDK
 | Architecture: x86_64
 | Is JDK: true
 | Detected by: environment variable 'JDK16'

 + Oracle JDK 1.7.0_80
 | Location: /Library/Java/JavaVirtualMachines/jdk1.7.0_80.jdk/Contents/Home/jre
 | Language Version: 7
 | Vendor: Oracle
 | Architecture: x86_64
 | Is JDK: false
 | Detected by: MacOS java_home
This can help to debug which toolchains are available to the build, how they are detected and what kind of metadata Gradle knows about those toolchains.

### Disabling auto provisioning
In order to disable auto-provisioning, you can use the org.gradle.java.installations.auto-download Gradle property:
Either start Gradle using -Dorg.gradle.java.installations.auto-download=false
Or put org.gradle.java.installations.auto-download=false into a gradle.properties file.
After disabling the auto provisioning, ensure that the specified JRE/JDK version in the build file is already installed locally.
Then, stop the Gradle daemon so that it can be reinitialized for the next build.
You can use the ./gradlew --stop command to stop the daemon process.

### Removing an auto-provisioned toolchain
When removing an auto-provisioned toolchain is necessary, remove the relevant toolchain located in the /jdks directory within the Gradle User Home.
The Gradle Daemon caches information about your project, including configuration details such as toolchain paths or versions. Changes to a project’s toolchain configuration might only occur once the Gradle Daemon is restarted. It is recommended to stop the Gradle Daemon to ensure that Gradle updates the configuration for subsequent builds.

## Custom toolchain locations
If auto-detecting local toolchains is not sufficient or disabled, there are additional ways you can let Gradle know about installed toolchains.
If your setup already provides environment variables pointing to installed JVMs, you can also let Gradle know about which environment variables to take into account.
Assuming the environment variables JDK8 and JRE17 point to valid java installations, the following instructs Gradle to resolve those environment variables and consider those installations when looking for a matching toolchain.
org.gradle.java.installations.fromEnv=JDK8,JRE17
Additionally, you can provide a comma-separated list of paths to specific installations using the org.gradle.java.installations.paths property.
For example, using the following in your gradle.properties will let Gradle know which directories to look at when detecting toolchains.
Gradle will treat these directories as possible installations but will not descend into any nested directories.
org.gradle.java.installations.paths=/custom/path/jdk1.8,/shared/jre11
Gradle does not prioritize custom toolchains over auto-detected toolchains.
If you enable auto-detection in your build, custom toolchains extend the set of toolchain locations.
Gradle picks a toolchain according to the precedence rules.

## Toolchain installations precedence
Gradle will sort all the JDK/JRE installations matching the toolchain specification of the build and will pick the first one.
Sorting is done based on the following rules:
the installation currently running Gradle is preferred over any other
JDK installations are preferred over JRE ones
certain vendors take precedence over others; their ordering (from the highest priority to lowest):
ADOPTIUM
ADOPTOPENJDK
AMAZON
APPLE
AZUL
BELLSOFT
GRAAL_VM
HEWLETT_PACKARD
IBM
JETBRAINS
MICROSOFT
ORACLE
SAP
TENCENT
everything else
higher major versions take precedence over lower ones
higher minor versions take precedence over lower ones
installation paths take precedence according to their lexicographic ordering (last resort criteria for deterministically deciding
between installations of the same type, from the same vendor and with the same version)
All these rules are applied as multilevel sorting criteria, in the order shown.
Let’s illustrate with an example.
A toolchain specification requests Java version 17.
Gradle detects the following matching installations:
Oracle JRE v17.0.1
Oracle JDK v17.0.0
Microsoft JDK 17.0.0
Microsoft JRE 17.0.1
Microsoft JDK 17.0.1
Assume that Gradle runs on a major Java version other than 17.
Otherwise, that installation would have priority.
When we apply the above rules to sort this set we will end up with following ordering:
Microsoft JDK 17.0.1
Microsoft JDK 17.0.0
Oracle JDK v17.0.0
Microsoft JRE v17.0.1
Oracle JRE v17.0.1
Gradle prefers JDKs over JREs, so the JREs come last.
Gradle prefers the Microsoft vendor over Oracle, so the Microsoft installations come first.
Gradle prefers higher version numbers, so JDK 17.0.1 comes before JDK 17.0.0.
So Gradle picks the first match in this order: Microsoft JDK 17.0.1.

## Toolchains for plugin authors
When creating a plugin or a task that uses toolchains, it is essential to provide sensible defaults and allow users to override them.
For JVM projects, it is usually safe to assume that the java plugin has been applied to the project.
The java plugin is automatically applied for the core Groovy and Scala plugins, as well as for the Kotlin plugin.
In such a case, using the toolchain defined via the java extension as a default value for the tool property is appropriate.
This way, the users will need to configure the toolchain only once on the project level.
The example below showcases how to use the default toolchain as convention while allowing users to individually configure the toolchain per task.
build.gradle.kts
abstract class CustomTaskUsingToolchains : DefaultTask() {

 @get:Nested
 abstract val launcher: Property (1)

 init {
 val toolchain = project.extensions.getByType().toolchain (2)
 val defaultLauncher = javaToolchainService.launcherFor(toolchain) (3)
 launcher.convention(defaultLauncher) (4)
 }

 @TaskAction
 fun showConfiguredToolchain() {
 println(launcher.get().executablePath)
 println(launcher.get().metadata.installationPath)
 }

 @get:Inject
 protected abstract val javaToolchainService: JavaToolchainService
}
build.gradle
abstract class CustomTaskUsingToolchains extends DefaultTask {

 @Nested
 abstract Property getLauncher() (1)

 CustomTaskUsingToolchains() {
 def toolchain = project.extensions.getByType(JavaPluginExtension.class).toolchain (2)
 Provider defaultLauncher = getJavaToolchainService().launcherFor(toolchain) (3)
 launcher.convention(defaultLauncher) (4)
 }

 @TaskAction
 def showConfiguredToolchain() {
 println launcher.get().executablePath
 println launcher.get().metadata.installationPath
 }

 @Inject
 protected abstract JavaToolchainService getJavaToolchainService()
}
1
We declare a JavaLauncher property on the task.
The property must be marked as a @Nested input to make sure the task is responsive to toolchain changes.
2
We obtain the toolchain spec from the java extension to use it as a default.
3
Using the JavaToolchainService we get a provider of the JavaLauncher that matches the toolchain.
4
Finally, we wire the launcher provider as a convention for our property.
In a project where the java plugin was applied, we can use the task as follows:
build.gradle.kts
plugins {
 java
}

java {
 toolchain { (1)
 languageVersion = JavaLanguageVersion.of(8)
 }
}

tasks.register("showDefaultToolchain") (2)

tasks.register("showCustomToolchain") {
 launcher = javaToolchains.launcherFor { (3)
 languageVersion = JavaLanguageVersion.of(17)
 }
}
build.gradle
plugins {
 id 'java'
}

java {
 toolchain { (1)
 languageVersion = JavaLanguageVersion.of(8)
 }
}

tasks.register('showDefaultToolchain', CustomTaskUsingToolchains) (2)

tasks.register('showCustomToolchain', CustomTaskUsingToolchains) {
 launcher = javaToolchains.launcherFor { (3)
 languageVersion = JavaLanguageVersion.of(17)
 }
}
1
The toolchain defined on the java extension is used by default to resolve the launcher.
2
The custom task without additional configuration will use the default Java 8 toolchain.
3
The other task overrides the value of the launcher by selecting a different toolchain using javaToolchains service.
When a task needs access to toolchains without the java plugin being applied the toolchain service can be used directly.
If an unconfigured toolchain spec is provided to the service, it will always return a tool provider for the toolchain that is running Gradle.
This can be achieved by passing an empty lambda when requesting a tool: javaToolchainService.launcherFor({}).
You can find more details on defining custom tasks in the Authoring tasks documentation.

## Toolchains limitations
Gradle may detect toolchains incorrectly when it’s running in a JVM compiled against musl, an alternative implementation of the C standard library.
JVMs compiled against musl can sometimes override the LD_LIBRARY_PATH environment variable to control dynamic library resolution.
This can influence forked java processes launched by Gradle, resulting in unexpected behavior.
As a consequence, using multiple java toolchains is discouraged in environments with the musl library.
This is the case in most Alpine distributions — consider using another distribution, like Ubuntu, instead.
If you are using a single toolchain, the JVM running Gradle, to build and run your application, you can safely ignore this limitation.

---

## Upstream refresh 2026-08-01 (verbatim extractor output)

Source: https://docs.gradle.org/current/userguide/toolchains.html
HTTP status: 200 · extracted bytes: 39375 · sha256: 321f0d646fd82182a066025229c9aaa47a9862925a2cdd089c184e3a8b229130
Extractor: `practices/scripts/snapshot-extract.sh` (curl -> deterministic HTML->text; no model in the loop)
Fetch receipt: `practices/upstream/_FETCH-RECEIPTS.yaml` id `r115`

Everything above this divider is the previous snapshot, preserved byte-for-byte (append-only:
history is recorded, never rewritten). The block below is the UNMODIFIED extractor output for
the 2026-08-01 re-fetch of the same URL — it is the current upstream text, and any citation that
claims to quote this source verbatim must match it.

Toolchains for JVM projects =8.0 or nightly, or nightly-release, or current, or RC) --> Gradle User Manual Theme Build Tool Releases Features 9.0.0 Highlights 8.0.0 Highlights Gradle vs Maven Learn User Manual DPE University YouTube Channel Events and Webinars Support Community Slack Community Forums Professional Services News Newsletter Blog Gradle Technologies Develocity® Build Scan® DPE.org Careers About Contact Us Gradle Fellowship GitHub Gradle User Manual Getting Started Gradle Releases All Releases Release Notes Installing Gradle Upgrading Gradle Within versions 9.x.y To version 9.0.0 Within versions 8.x From version 7.x to 8.0 From version 6.x to 7.0 From version 5.x to 6.0 From version 4.x to 5.0 Migrating to Gradle from Maven from Ant Compatibility Notes Gradle's Feature Lifecycle Gradle Fundamentals Learning Gradle Basics 1. Core Concepts 2. Wrapper Basics 3. CLI Basics 4. Settings File Basics 5. Build File Basics 6. Dependencies Basics 7. Tasks Basics 8. Caching Basics 9. Plugins Basics 10. Build Scan Basics Writing Build Scripts 1. Anatomy of a Gradle Build 2. Structuring Multi-Project Builds 3. Gradle Build Lifecycle 4. Writing Build Scripts 5. Gradle Managed Types 6. Declaring Dependencies 7. Creating and Registering Tasks 8. Working with Plugins Creating Plugins 1. Plugin Introduction 2. Pre-Compiled Script Plugins 3. Binary Plugins 4. Developing Binary Plugins 5. Testing Binary Plugins 6. Publishing Binary Plugins Gradle Tutorials Beginner Tutorial 1. Initializing the Project 2. Running Tasks 3. Understanding Dependencies 4. Applying Plugins 5. Exploring Incremental Builds 6. Enabling the Build Cache Intermediate Tutorial 1. Initializing the Project 2. Understanding the Build Lifecycle 3. Multi-Project Builds 4. Writing the Settings File 5. Writing a Build Script 6. Writing Tasks 7. Writing Plugins Advanced Tutorial 1. Initializing the Project 2. Adding an Extension 3. Creating a Custom Task 4. Writing a Unit Test 5. Adding a DataFlow Action 6. Writing a Functional Test 7. Using a Consumer Project 8. Publish the Plugin Gradle Best Practices Introduction Best Practices Best Practices Index General Best Practices Best Practices for Structuring Builds Best Practices for Dependencies Best Practices for Tasks Best Practices for Performance Best Practices for Security Best Practices for Testing Gradle DSLs and APIs Groovy Groovy DSL Primer Groovy DSL Reference Kotlin Kotlin DSL Primer Kotlin DSL Reference Migration from Groovy General Java API Public APIs Default Script Imports Gradle Reference Runtime and Configuration Command-Line Interface Logging and Output Gradle Wrapper Gradle Daemon Gradle Directories Build Configuration Build Lifecycle Build Scan Continuous Builds File System Watching Core Plugins Core Plugins JVM Plugins Java Plugin Java Library Plugin Java Platform Plugin Groovy Plugin Scala Plugin ANTLR Plugin JVM Test Suite Plugin Test Report Aggregation Plugin Native Plugins C++ Application Plugin C++ Library Plugin C++ Unit Test Plugin Swift Application Plugin Swift Library Plugin XCTest Plugin Packaging/Distribution Plugins Java Application Plugin WAR Plugin EAR Plugin Maven Publish Plugin Ivy Publish Plugin Distribution Plugin Java Library Distribution Plugin Code Analysis Plugins Checkstyle Plugin PMD Plugin JaCoCo Plugin JaCoCo Report Aggregation Plugin CodeNarc Plugin IDE Integration Plugins Eclipse Plugin IntelliJ IDEA Plugin Visual Studio Plugin Xcode Plugin Utility Plugins Base Plugin Build Init Plugin Signing Plugin Java Gradle Plugin Project Report Plugin Build Dashboard Plugin Tasks Understanding Tasks Controlling Task Execution Organizing Tasks Implementing Custom Tasks Lazy Configuration Parallel Task Execution Advanced Task Development Shared Build Services Task Configuration Avoidance Plugins Introduction to Plugins Precompiled Script Plugins Convention Plugins Binary Plugins Testing Plugins Preparing to Publish Publishing Plugins Reporting Plugin Problems Initialization Scripts & Init Plugins Testing with TestKit Dependencies Getting Started Learning the Basics 1. Declaring Dependencies 2. Dependency Configurations 3. Declaring Repositories 4. Centralizing Dependencies 5. Dependency Constraints and Conflict Resolution Declaring Dependencies Declaring Dependencies Viewing Dependencies Declaring Versions and Ranges Declaring Dependency Constraints Creating Dependency Configurations Gradle Distribution-Specific Dependencies Declaring Repositories Declaring Repositories Centralizing Repository Declarations Repository Types Metadata Formats Supported Protocols Filtering Repository Content Centralizing Dependencies Platforms (BOMs) Version Catalogs Using Catalogs with Platforms Controlling Dependency Resolution Consistent Dependency Resolution Resolving Specific Artifacts Capabilities Variants and Attributes Artifact Views Artifact Transforms Managing Dependencies Locking Versions Using Resolution Rules Modifying Dependency Metadata Caching Dependencies Advanced Concepts 1. Dependency Resolution 2. Graph Resolution 3. Variant Selection 4. Artifact Resolution Publishing Libraries Setting up Publishing Understanding Gradle Module Metadata Signing Artifacts Customizing Publishing Gradle Managed Types Lazy vs Eager Evaluation Properties and Providers Collections Services and Service Injection Dataflow Actions Working with Files Platforms JVM Builds Building Java & JVM projects Testing Java & JVM projects Java Toolchains Toolchains for JVM projects Toolchain Resolver Plugins Managing Dependencies C++ Builds Building C++ projects Testing C++ projects Building Native Software Swift Builds Building Swift projects Testing Swift projects Other Topics Using Ant from Gradle Gradle on CI Introduction CI/CD Systems GitHub Actions GitLab CI Jenkins TeamCity Travis CI Structuring Gradle Builds Organizing Projects Multi-Project Builds Sharing Build Logic Composite Builds Configuration on Demand Optimizing Gradle Builds Improving Performance Build Cache Enabling and Configuring Why use the Build Cache? Understanding the Impact Learning Basic Concepts Caching Java Project Caching Android Project Debugging Caching Issues Troubleshooting Configuration Cache How it Works Enabling and Configuring Requirements for your Build Logic Debugging and Troubleshooting Status Isolated Projects Securing Gradle Builds Supply Chain Security Verifying Dependencies IDE & Tool Integration Third-party Tools APIs Tooling API Test Reporting API How-To-Guides Structuring Builds Convert a Single-Project Build to Multi-Project Dependency Management How to Downgrade Transitive Dependencies How to Upgrade Transitive Dependencies How to Exclude Transitive Dependencies How to Prevent Accidental or Eager Dependency Upgrades How to Align Dependency Versions How to Share Outputs Between Projects How to Resolve Specific Artifacts from a Module Dependency How to Use a Local Fork of a Module Dependency How to Fix Version Catalog Problems How to Create Feature Variants of a Library More Resources Licenses Single Page Version Toolchains for JVM projects version 9.6.1 On this Page Toolchains for projects 1. Java toolchains 2. The --release flag 3. Source and Target compatibility 4. Environment variables ( JAVA_HOME ) 5. IDE settings Combining toolchains Comparison table for setting project toolchains Toolchains for tasks Integration with tasks relying on a Java executable or Java home Using Java toolchains Selecting toolchains by vendor Selecting toolchains that support GraalVM native image Selecting toolchains by virtual machine implementation Configuring toolchain specifications Auto-detection of installed toolchains How to disable auto-detection Auto-provisioning Toolchain Download Repositories Viewing and debugging toolchains Disabling auto provisioning Removing an auto-provisioned toolchain Custom toolchain locations Toolchain installations precedence Toolchains for plugin authors Toolchains limitations Working on multiple projects can require interacting with multiple versions of the Java language. Even within a single project different parts of the codebase may be fixed to a particular language level due to backward compatibility requirements. This means different versions of the same tools (a toolchain) must be installed and managed on each machine that builds the project. A Java toolchain is a set of tools to build and run Java projects, which is usually provided by the environment via local JRE or JDK installations. Compile tasks may use javac as their compiler, test and exec tasks may use the java command while javadoc will be used to generate documentation. By default, Gradle uses the same Java toolchain for running Gradle itself and building JVM projects. However, this may only sometimes be desirable. Building projects with different Java versions on different developer machines and CI servers may lead to unexpected issues. Additionally, you may want to build a project using a Java version that is not supported for running Gradle. In order to improve reproducibility of the builds and make build requirements clearer, Gradle allows configuring toolchains on both project and task levels. You can also control the JVM used to run Gradle itself using the Daemon JVM criteria . Toolchains for projects Gradle provides multiple ways to configure the Java version used for compiling and running your project. The five primary mechanisms are: Java toolchains The --release flag Source and Target compatibility Environment variables ( JAVA_HOME ) IDE settings These settings are not mutually exclusive , and advanced users may need to combine them in specific scenarios. 1. Java toolchains To configure a toolchain for your project, declare the desired Java language version in the java extension block: build.gradle.kts java { toolchain { languageVersion = JavaLanguageVersion.of(17) } } build.gradle java { toolchain { languageVersion = JavaLanguageVersion.of(17) } } The java block is flexible and supports additional configuration options. You can learn more in Using Java toolchains . 2. The --release flag For strict cross-compilation, the --release flag is recommended instead of sourceCompatibility and targetCompatibility : tasks.withType<JavaCompile>().configureEach { options.release = 8 } tasks.withType(JavaCompile).configureEach { options.release = 8 } This flag prevents accidental use of newer APIs that are not available in the specified version. However, it does not control which JDK is used—only how the compiler treats source code. This method can be combined with toolchains if you need both a specific JDK and strict cross-compilation . 3. Source and Target compatibility Setting sourceCompatibility and targetCompatibility tells the Java compiler to produce bytecode compatible with a specific Java version but does not enforce which JDK Gradle itself runs with: java { sourceCompatibility = JavaVersion.VERSION_1_8 targetCompatibility = JavaVersion.VERSION_1_8 } java { sourceCompatibility = JavaVersion.VERSION_1_8 targetCompatibility = JavaVersion.VERSION_1_8 } This does not guarantee the correct JDK is used and may cause issues when APIs have been backported to older Java versions. You should only use this method in cases where you need backward compatibility but cannot use toolchains . 4. Environment variables ( JAVA_HOME ) You can influence which JDK Gradle uses by setting the JAVA_HOME environment variable: export JAVA_HOME=/path/to/java17 This sets a default JDK for all Java-based tools on your system, including Gradle and Maven. This does not override Gradle’s toolchain support or other project-specific configurations. This approach is useful for legacy projects that do not use toolchains and expect a specific JDK to be active in the environment. However, since JAVA_HOME applies globally, it cannot be used to specify different JDK versions for different projects. It is more reliable to use toolchains , which allow setting the Java version at the project level. 5. IDE settings Most modern IDEs allow you to configure the JVM used to run Gradle when working with a project. This setting affects how Gradle itself is executed inside the IDE, but not how your code is compiled—unless the build does not explicitly specify a toolchain. If your build does not define a Java toolchain, Gradle may fall back to using the Java version defined by the IDE settings. This can lead to unintended and non-reproducible behavior, especially if different team members use different IDE configurations. You should change the IDE’s Gradle JVM setting to align with the JVM used on the command line ( JAVA_HOME or the system’s default Java installation) —ensuring consistent behavior across environments (e.g., when running tests or tasks from the IDE vs the terminal). You should also change the IDE’s Gradle JVM setting if the IDE emits a warning/error when the JVM is not set or does not match with JAVA_HOME . IntelliJ IDEA To configure the Gradle JVM: Open Settings (Preferences) > Build, Execution, Deployment > Gradle . Set Gradle JVM to the desired JDK. Eclipse To configure the Gradle JVM: Open Preferences > Gradle > Gradle JDK . Select the appropriate JDK. Some IDEs also allow you to configure the Gradle Daemon JVM in the same settings screen. Be careful not to confuse it with the toolchain or project JVM— make sure you’re selecting the correct one. Combining toolchains In some cases, you may want to: Use a specific JDK version for compilation ( toolchains ). Ensure that the compiled bytecode is compatible with an older Java version ( --release or targetCompatibility ). For example, to compile with Java 17 but produce Java 11 bytecode: build.gradle.kts java { toolchain { languageVersion = JavaLanguageVersion.of(17) } } tasks.withType<JavaCompile>().configureEach { options.release = 11 } build.gradle java { toolchain { languageVersion = JavaLanguageVersion.of(17) } } tasks.withType(JavaCompile).configureEach { options.release = 11 } Comparison table for setting project toolchains Method Ensures Correct JDK? Auto Downloads JDK? Prevents Accidental API Use? Java toolchains ✅ Yes ✅ Yes ❌ No --release flag ❌ No ❌ No ✅ Yes Source & Target compatibility ❌ No ❌ No ❌ No Environment variables ( JAVA_HOME ) ✅ Yes (but only globally) ❌ No ❌ No IDE settings ✅ Yes (inside the IDE) ❌ No ❌ No Recommendation: For most users: Use Java toolchains ( toolchain.languageVersion ). For strict compatibility enforcement: Use the --release flag. For advanced cases: Combine toolchains and --release . Avoid sourceCompatibility and targetCompatibility unless necessary. Use JAVA_HOME only if you need a default system-wide JDK version. Use IDE settings if you want Gradle to match your IDE’s JDK version. Toolchains for tasks In case you want to tweak which toolchain is used for a specific task, you can specify the exact tool a task is using. For example, the Test task exposes a JavaLauncher property that defines which java executable to use for launching the tests. In the example below, we configure all java compilation tasks to use Java 8. Additionally, we introduce a new Test task that will run our unit tests using a JDK 17. list/build.gradle.kts tasks.withType<JavaCompile>().configureEach { javaCompiler = javaToolchains.compilerFor { languageVersion = JavaLanguageVersion.of(8) } } tasks.register<Test>("testsOn17") { javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(17) } } list/build.gradle tasks.withType(JavaCompile).configureEach { javaCompiler = javaToolchains.compilerFor { languageVersion = JavaLanguageVersion.of(8) } } tasks.register('testsOn17', Test) { javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(17) } } In addition, in the application subproject, we add another Java execution task to run our application with JDK 17. application/build.gradle.kts tasks.register<JavaExec>("runOn17") { javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(17) } classpath = sourceSets["main"].runtimeClasspath mainClass = application.mainClass } application/build.gradle tasks.register('runOn17', JavaExec) { javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(17) } classpath = sourceSets.main.runtimeClasspath mainClass = application.mainClass } Depending on the task, a JRE might be enough while for other tasks (e.g. compilation), a JDK is required. By default, Gradle prefers installed JDKs over JREs if they can satisfy the requirements. Toolchains tool providers can be obtained from the javaToolchains extension. Three tools are available: A JavaCompiler which is the tool used by the JavaCompile task A JavaLauncher which is the tool used by the JavaExec or Test tasks A JavadocTool which is the tool used by the Javadoc task Integration with tasks relying on a Java executable or Java home Any task that can be configured with a path to a Java executable, or a Java home location, can benefit from toolchains. While you will not be able to wire a toolchain tool directly, they all have the metadata that gives access to their full path or to the path of the Java installation they belong to. For example, you can configure the java executable for a task as follows: build.gradle.kts val launcher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(11) } tasks.sampleTask { javaExecutable = launcher.map { it.executablePath } } build.gradle def launcher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(11) } tasks.named('sampleTask') { javaExecutable = launcher.map { it.executablePath } } As another example, you can configure the Java Home for a task as follows: build.gradle.kts val launcher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(11) } tasks.anotherSampleTask { javaHome = launcher.map { it.metadata.installationPath } } build.gradle def launcher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(11) } tasks.named('anotherSampleTask') { javaHome = launcher.map { it.metadata.installationPath } } If you require a path to a specific tool such as Java compiler, you can obtain it as follows: build.gradle.kts val compiler = javaToolchains.compilerFor { languageVersion = JavaLanguageVersion.of(11) } tasks.yetAnotherSampleTask { javaCompilerExecutable = compiler.map { it.executablePath } } build.gradle def compiler = javaToolchains.compilerFor { languageVersion = JavaLanguageVersion.of(11) } tasks.named('yetAnotherSampleTask') { javaCompilerExecutable = compiler.map { it.executablePath } } The examples above use tasks with RegularFileProperty and DirectoryProperty properties which allow lazy configuration. Doing respectively launcher.get().executablePath , launcher.get().metadata.installationPath or compiler.get().executablePath instead will give you the full path for the given toolchain but note that this may realize (and provision) a toolchain eagerly. Using Java toolchains Using Java toolchains allows Gradle to automatically download and manage the required JDK version for your build. It ensures that the correct Java version is used for both compilation and execution. You can define what toolchain to use for a project by stating the Java language version in the java extension block: build.gradle.kts java { toolchain { languageVersion = JavaLanguageVersion.of(17) } } build.gradle java { toolchain { languageVersion = JavaLanguageVersion.of(17) } } Executing the build (e.g. using gradle check ) will now handle several things for you and others running your build: Gradle configures all compile, test and javadoc tasks to use the defined toolchain. Gradle detects locally installed toolchains . Gradle chooses a toolchain matching the requirements (any Java 17 toolchain for the example above). If no matching toolchain is found, Gradle can automatically download a matching one based on the configured toolchain download repositories . Toolchain support is available in the Java plugins and for the tasks they define. For the Groovy plugin, compilation is supported but not yet Groovydoc generation. For the Scala plugin, compilation and Scaladoc generation are supported. Selecting toolchains by vendor In case your build has specific requirements from the used JRE/JDK, you may want to define the vendor for the toolchain as well. JvmVendorSpec has a list of well-known JVM vendors recognized by Gradle. The advantage is that Gradle can handle any inconsistencies across JDK versions in how exactly the JVM encodes the vendor information. build.gradle.kts java { toolchain { languageVersion = JavaLanguageVersion.of(11) vendor = JvmVendorSpec.ADOPTIUM } } build.gradle java { toolchain { languageVersion = JavaLanguageVersion.of(11) vendor = JvmVendorSpec.ADOPTIUM } } If the vendor you want to target is not a known vendor, you can still restrict the toolchain to those matching the java.vendor system property of the available toolchains. The following snippet uses filtering to include a subset of available toolchains. This example only includes toolchains whose java.vendor property contains the given match string. The matching is done in a case-insensitive manner. build.gradle.kts java { toolchain { languageVersion = JavaLanguageVersion.of(11) vendor = JvmVendorSpec.matching("customString") } } build.gradle java { toolchain { languageVersion = JavaLanguageVersion.of(11) vendor = JvmVendorSpec.matching("customString") } } Selecting toolchains that support GraalVM native image If your project needs a toolchain with GraalVM Native Image capability , you can configure the spec to request it: build.gradle.kts java { toolchain { languageVersion = JavaLanguageVersion.of(21) nativeImageCapable = true } } build.gradle java { toolchain { languageVersion = JavaLanguageVersion.of(21) nativeImageCapable = true } } Leaving that value unconfigured or set to false will not restrict the toolchain selection based on the Native Image capability. That means that a Native Image capable JDK can be selected if it matches the other criteria. Selecting toolchains by virtual machine implementation If your project requires a specific implementation, you can filter based on the implementation as well. Currently available implementations to choose from are: VENDOR_SPECIFIC Acts as a placeholder and matches any implementation from any vendor (e.g. hotspot, zulu, …​) J9 Matches only virtual machine implementations using the OpenJ9/IBM J9 runtime engine. For example, to use an IBM JVM, distributed via AdoptOpenJDK , you can specify the filter as shown in the example below. build.gradle.kts java { toolchain { languageVersion = JavaLanguageVersion.of(11) vendor = JvmVendorSpec.IBM implementation = JvmImplementation.J9 } } build.gradle java { toolchain { languageVersion = JavaLanguageVersion.of(11) vendor = JvmVendorSpec.IBM implementation = JvmImplementation.J9 } } The Java major version, the vendor (if specified) and implementation (if specified) will be tracked as an input for compilation and test execution. Configuring toolchain specifications Gradle allows configuring multiple properties that affect the selection of a toolchain, such as language version or vendor. Even though these properties can be configured independently, the configuration must follow certain rules in order to form a valid specification. A JavaToolchainSpec is considered valid in two cases: when no properties have been set, i.e. the specification is empty ; when languageVersion has been set, optionally followed by setting any other property. In other words, if a vendor or an implementation are specified, they must be accompanied by the language version. Gradle distinguishes between toolchain specifications that configure the language version and the ones that do not. A specification without a language version, in most cases, would be treated as a one that selects the toolchain of the current build. Usage of invalid instances of JavaToolchainSpec results in a build error since Gradle 8.0. Auto-detection of installed toolchains By default, Gradle automatically detects local JRE/JDK installations so no further configuration is required by the user. The following is a list of common package managers, tools, and locations that are supported by the JVM auto-detection. JVM auto-detection knows how to work with: Operation-system specific locations: Linux, macOS, Windows Conventional Environment Variable: JAVA_HOME Package Managers: Asdf-vm , Jabba , SDKMAN! Maven Toolchain specifications IntelliJ IDEA installations Among the set of all detected JRE/JDK installations, one will be picked according to the Toolchain Precedence Rules . Whether you are using toolchain auto-detection or you are configuring Custom toolchain locations , installations that are non-existing or without a bin/java executable will be ignored with a warning, but they won’t generate an error. How to disable auto-detection In order to disable auto-detection, you can use the org.gradle.java.installations.auto-detect Gradle property: Either start Gradle using -Dorg.gradle.java.installations.auto-detect=false Or put org.gradle.java.installations.auto-detect=false into your gradle.properties file. Auto-provisioning If Gradle can’t find a locally available toolchain that matches the requirements of the build, it can automatically download one (as long as a toolchain download repository has been configured; for detail, see relevant section ). Gradle installs the downloaded JDKs in the Gradle User Home . Gradle only downloads JDK versions for GA releases. There is no support for downloading early access versions. Once installed in the Gradle User Home , a provisioned JDK becomes one of the JDKs visible to auto-detection and can be used by any subsequent builds, just like any other JDK installed on the system. Since auto-provisioning only kicks in when auto-detection fails to find a matching JDK, auto-provisioning can only download new JDKs and is in no way involved in updating any of the already installed ones. None of the auto-provisioned JDKs will ever be revisited and automatically updated by auto-provisioning, even if there is a newer minor version available for them. Toolchain Download Repositories Toolchain download repository definitions are added to a build by applying specific settings plugins. For details on writing such plugins, consult the Toolchain Resolver Plugins page. One example of a toolchain resolver plugin is the Foojay Toolchains Plugin , based on the foojay Disco API . It even has a convention variant, which automatically takes care of all the needed configuration, just by being applied: settings.gradle.kts plugins { id("org.gradle.toolchains.foojay-resolver-convention").version("1.0.0") } settings.gradle plugins { id 'org.gradle.toolchains.foojay-resolver-convention' version '1.0.0' } For advanced or highly specific configurations, a custom toolchain resolver plugin should be used. In general, when applying toolchain resolver plugins, the toolchain download resolvers provided by them also need to be configured. Let’s illustrate with an example. Consider two toolchain resolver plugins applied by the build: One is the Foojay plugin mentioned above, which downloads toolchains via the FoojayToolchainResolver it provides. The other contains a FICTITIOUS resolver named MadeUpResolver . The following example uses these toolchain resolvers in a build via the toolchainManagement block in the settings file: settings.gradle.kts toolchainManagement { jvm { (1) javaRepositories { repository("foojay") { (2) resolverClass = org.gradle.toolchains.foojay.FoojayToolchainResolver::class.java } repository("made_up") { (3) resolverClass = MadeUpResolver::class.java credentials { username = "user" password = "password" } authentication { create<DigestAuthentication>("digest") } (4) } } } } settings.gradle toolchainManagement { jvm { (1) javaRepositories { repository('foojay') { (2) resolverClass = org.gradle.toolchains.foojay.FoojayToolchainResolver } repository('made_up') { (3) resolverClass = MadeUpResolver credentials { username = "user" password = "password" } authentication { digest(BasicAuthentication) } (4) } } } } 1 In the toolchainManagement block, the jvm block contains configuration for Java toolchains. 2 The javaRepositories block defines named Java toolchain repository configurations. Use the resolverClass property to link these configurations to plugins. 3 Toolchain declaration order matters. Gradle downloads from the first repository that provides a match, starting with the first repository in the list. 4 You can configure toolchain repositories with the same set of authentication and authorization options used for dependency management. The jvm block in toolchainManagement only resolves after applying a toolchain resolver plugin. Viewing and debugging toolchains Gradle can display the list of all detected toolchains including their metadata. For example, to show all toolchains of a project, run: $ ./gradlew -q javaToolchains > gradle -q javaToolchains + Options | Auto-detection: Enabled | Auto-download: Enabled + AdoptOpenJDK 1.8.0_242 | Location: /Users/username/myJavaInstalls/8.0.242.hs-adpt/jre | Language Version: 8 | Vendor: AdoptOpenJDK | Architecture: x86_64 | Is JDK: false | Detected by: Gradle property 'org.gradle.java.installations.paths' + Microsoft JDK 16.0.2+7 | Location: /Users/username/.sdkman/candidates/java/16.0.2.7.1-ms | Language Version: 16 | Vendor: Microsoft | Architecture: aarch64 | Is JDK: true | Detected by: SDKMAN! + OpenJDK 15-ea | Location: /Users/user/customJdks/15.ea.21-open | Language Version: 15 | Vendor: AdoptOpenJDK | Architecture: x86_64 | Is JDK: true | Detected by: environment variable 'JDK16' + Oracle JDK 1.7.0_80 | Location: /Library/Java/JavaVirtualMachines/jdk1.7.0_80.jdk/Contents/Home/jre | Language Version: 7 | Vendor: Oracle | Architecture: x86_64 | Is JDK: false | Detected by: MacOS java_home This can help to debug which toolchains are available to the build, how they are detected and what kind of metadata Gradle knows about those toolchains. Disabling auto provisioning In order to disable auto-provisioning, you can use the org.gradle.java.installations.auto-download Gradle property: Either start Gradle using -Dorg.gradle.java.installations.auto-download=false Or put org.gradle.java.installations.auto-download=false into a gradle.properties file. After disabling the auto provisioning, ensure that the specified JRE/JDK version in the build file is already installed locally. Then, stop the Gradle daemon so that it can be reinitialized for the next build. You can use the ./gradlew --stop command to stop the daemon process. Removing an auto-provisioned toolchain When removing an auto-provisioned toolchain is necessary, remove the relevant toolchain located in the /jdks directory within the Gradle User Home . The Gradle Daemon caches information about your project, including configuration details such as toolchain paths or versions. Changes to a project’s toolchain configuration might only occur once the Gradle Daemon is restarted. It is recommended to stop the Gradle Daemon to ensure that Gradle updates the configuration for subsequent builds. Custom toolchain locations If auto-detecting local toolchains is not sufficient or disabled, there are additional ways you can let Gradle know about installed toolchains. If your setup already provides environment variables pointing to installed JVMs, you can also let Gradle know about which environment variables to take into account. Assuming the environment variables JDK8 and JRE17 point to valid java installations, the following instructs Gradle to resolve those environment variables and consider those installations when looking for a matching toolchain. org.gradle.java.installations.fromEnv=JDK8,JRE17 Additionally, you can provide a comma-separated list of paths to specific installations using the org.gradle.java.installations.paths property. For example, using the following in your gradle.properties will let Gradle know which directories to look at when detecting toolchains. Gradle will treat these directories as possible installations but will not descend into any nested directories. org.gradle.java.installations.paths=/custom/path/jdk1.8,/shared/jre11 Gradle does not prioritize custom toolchains over auto-detected toolchains. If you enable auto-detection in your build, custom toolchains extend the set of toolchain locations. Gradle picks a toolchain according to the precedence rules . Toolchain installations precedence Gradle will sort all the JDK/JRE installations matching the toolchain specification of the build and will pick the first one. Sorting is done based on the following rules: the installation currently running Gradle is preferred over any other JDK installations are preferred over JRE ones certain vendors take precedence over others; their ordering (from the highest priority to lowest): ADOPTIUM ADOPTOPENJDK AMAZON APPLE AZUL BELLSOFT GRAAL_VM HEWLETT_PACKARD IBM JETBRAINS MICROSOFT ORACLE SAP TENCENT everything else higher major versions take precedence over lower ones higher minor versions take precedence over lower ones installation paths take precedence according to their lexicographic ordering (last resort criteria for deterministically deciding between installations of the same type, from the same vendor and with the same version) All these rules are applied as multilevel sorting criteria, in the order shown . Let’s illustrate with an example. A toolchain specification requests Java version 17. Gradle detects the following matching installations: Oracle JRE v17.0.1 Oracle JDK v17.0.0 Microsoft JDK 17.0.0 Microsoft JRE 17.0.1 Microsoft JDK 17.0.1 Assume that Gradle runs on a major Java version other than 17. Otherwise, that installation would have priority. When we apply the above rules to sort this set we will end up with following ordering: Microsoft JDK 17.0.1 Microsoft JDK 17.0.0 Oracle JDK v17.0.0 Microsoft JRE v17.0.1 Oracle JRE v17.0.1 Gradle prefers JDKs over JREs, so the JREs come last. Gradle prefers the Microsoft vendor over Oracle, so the Microsoft installations come first. Gradle prefers higher version numbers, so JDK 17.0.1 comes before JDK 17.0.0. So Gradle picks the first match in this order: Microsoft JDK 17.0.1. Toolchains for plugin authors When creating a plugin or a task that uses toolchains, it is essential to provide sensible defaults and allow users to override them. For JVM projects, it is usually safe to assume that the java plugin has been applied to the project. The java plugin is automatically applied for the core Groovy and Scala plugins, as well as for the Kotlin plugin. In such a case, using the toolchain defined via the java extension as a default value for the tool property is appropriate. This way, the users will need to configure the toolchain only once on the project level. The example below showcases how to use the default toolchain as convention while allowing users to individually configure the toolchain per task. build.gradle.kts abstract class CustomTaskUsingToolchains : DefaultTask() { @get:Nested abstract val launcher: Property<JavaLauncher> (1) init { val toolchain = project.extensions.getByType<JavaPluginExtension>().toolchain (2) val defaultLauncher = javaToolchainService.launcherFor(toolchain) (3) launcher.convention(defaultLauncher) (4) } @TaskAction fun showConfiguredToolchain() { println(launcher.get().executablePath) println(launcher.get().metadata.installationPath) } @get:Inject protected abstract val javaToolchainService: JavaToolchainService } build.gradle abstract class CustomTaskUsingToolchains extends DefaultTask { @Nested abstract Property<JavaLauncher> getLauncher() (1) CustomTaskUsingToolchains() { def toolchain = project.extensions.getByType(JavaPluginExtension.class).toolchain (2) Provider<JavaLauncher> defaultLauncher = getJavaToolchainService().launcherFor(toolchain) (3) launcher.convention(defaultLauncher) (4) } @TaskAction def showConfiguredToolchain() { println launcher.get().executablePath println launcher.get().metadata.installationPath } @Inject protected abstract JavaToolchainService getJavaToolchainService() } 1 We declare a JavaLauncher property on the task. The property must be marked as a @Nested input to make sure the task is responsive to toolchain changes. 2 We obtain the toolchain spec from the java extension to use it as a default. 3 Using the JavaToolchainService we get a provider of the JavaLauncher that matches the toolchain. 4 Finally, we wire the launcher provider as a convention for our property. In a project where the java plugin was applied, we can use the task as follows: build.gradle.kts plugins { java } java { toolchain { (1) languageVersion = JavaLanguageVersion.of(8) } } tasks.register<CustomTaskUsingToolchains>("showDefaultToolchain") (2) tasks.register<CustomTaskUsingToolchains>("showCustomToolchain") { launcher = javaToolchains.launcherFor { (3) languageVersion = JavaLanguageVersion.of(17) } } build.gradle plugins { id 'java' } java { toolchain { (1) languageVersion = JavaLanguageVersion.of(8) } } tasks.register('showDefaultToolchain', CustomTaskUsingToolchains) (2) tasks.register('showCustomToolchain', CustomTaskUsingToolchains) { launcher = javaToolchains.launcherFor { (3) languageVersion = JavaLanguageVersion.of(17) } } 1 The toolchain defined on the java extension is used by default to resolve the launcher. 2 The custom task without additional configuration will use the default Java 8 toolchain. 3 The other task overrides the value of the launcher by selecting a different toolchain using javaToolchains service. When a task needs access to toolchains without the java plugin being applied the toolchain service can be used directly. If an unconfigured toolchain spec is provided to the service, it will always return a tool provider for the toolchain that is running Gradle. This can be achieved by passing an empty lambda when requesting a tool: javaToolchainService.launcherFor({}) . You can find more details on defining custom tasks in the Authoring tasks documentation. Toolchains limitations Gradle may detect toolchains incorrectly when it’s running in a JVM compiled against musl , an alternative implementation of the C standard library. JVMs compiled against musl can sometimes override the LD_LIBRARY_PATH environment variable to control dynamic library resolution. This can influence forked java processes launched by Gradle, resulting in unexpected behavior. As a consequence, using multiple java toolchains is discouraged in environments with the musl library. This is the case in most Alpine distributions — consider using another distribution, like Ubuntu, instead. If you are using a single toolchain, the JVM running Gradle, to build and run your application, you can safely ignore this limitation. Docs Release Notes Groovy DSL Kotlin DSL Javadoc News Blog Newsletter Twitter Status Products Develocity Build Scan® Build Cache Services Get Help Forums GitHub Events DPE University Stay UP-TO-DATE on new features and news: By entering your email, you agree to our Terms and Privacy Policy . Subscribe © 2025 Gradle, Inc. Gradle®, Develocity®, Build Scan®, and the Gradlephant logo are registered trademarks of Gradle, Inc. Gradle Privacy | Terms of Service
