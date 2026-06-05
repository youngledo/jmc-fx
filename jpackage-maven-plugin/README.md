# JPackage Maven Plugin

This Maven plugin packages desktop Java applications with the JDK `jlink` and
`jpackage` tools, with an optional Project Leyden AOT cache training workflow.
It is developed inside the JMC FX repository first, but the plugin is designed
as a generic desktop Java packaging plugin, not a JMC FX or JavaFX-specific
facade.

The plugin is a new configuration model. It does not try to be compatible with
`org.panteleyev:jpackage-maven-plugin` configuration.

## Goals

- `jlink`: invokes the JDK `jlink` tool directly.
- `jpackage`: invokes the JDK `jpackage` tool directly.
- `leyden`: stages project artifacts, creates a `jlink` runtime, creates a
  `jpackage` app image, trains a Leyden AOT cache, rewrites the launcher config,
  and creates the final installer.

## Scope

Supported desktop application shapes:

- Classpath desktop applications using `mainJar` and `mainClass`.
- JPMS module desktop applications using `module`.
- JavaFX, Swing, AWT, SWT, or other desktop stacks, as long as the consuming
  project supplies the required modules and runtime options.
- Multiple launchers through `addLaunchers` properties files.

Out of scope for this in-repository plugin work:

- Maven Central publishing, release automation, and repository split.
- Cross-platform installer verification beyond the host OS.
- GraalVM native-image packaging.
- Apple notarization. Notarization is a post-`jpackage` release step, not a JDK
  `jpackage` option. The plugin supports the `jpackage` macOS signing options;
  notarization can be handled later by a separate release workflow or goal.

## `jlink`

The `jlink` goal supports:

- `modulePath`
- `addModules`
- `output`
- `noHeaderFiles`
- `noManPages`
- `stripDebug`
- `compress`
- `bindServices`
- `extraOptions`
- `javaHome`

Example:

```xml
<plugin>
    <groupId>com.youngledo.jmcfx</groupId>
    <artifactId>jpackage-maven-plugin</artifactId>
    <version>${project.version}</version>
    <executions>
        <execution>
            <goals>
                <goal>jlink</goal>
            </goals>
            <configuration>
                <modulePath>
                    <path>${project.build.directory}/mods</path>
                </modulePath>
                <addModules>
                    <module>com.acme.app</module>
                </addModules>
                <output>${project.build.directory}/runtime</output>
                <noHeaderFiles>true</noHeaderFiles>
                <noManPages>true</noManPages>
                <stripDebug>true</stripDebug>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## `jpackage`

The `jpackage` goal maps the current JDK `jpackage` option groups. Parameters
are optional unless the underlying `jpackage` mode requires them.

General options:

- `optionFiles`: emits `@file` arguments.
- `name`
- `packageType`
- `packageVersion`
- `copyright`
- `description`
- `destination`
- `icon`
- `temp`
- `vendor`
- `verbose`
- `help`
- `toolVersion`
- `extraOptions`: escape hatch for future JDK options not yet modeled.

Runtime image options:

- `addModules`
- `jlinkOptions`
- `modulePath`
- `runtimeImage`

Application image options:

- `appContent`
- `input`

Launcher options:

- `addLaunchers`
- `arguments`
- `javaOptions`
- `mainClass`
- `mainJar`
- `module`

Package options:

- `aboutUrl`
- `appImage`
- `fileAssociations`
- `installDir`
- `launcherAsService`
- `licenseFile`
- `resourceDir`

macOS package options:

- `macAppCategory`
- `macAppImageSignIdentity`
- `macAppStore`
- `macDmgContent`
- `macEntitlements`
- `macInstallerSignIdentity`
- `macPackageIdentifier`
- `macPackageName`
- `macPackageSigningPrefix`
- `macSign`
- `macSigningKeyUserName`
- `macSigningKeychain`

Linux package options:

- `linuxAppCategory`
- `linuxAppRelease`
- `linuxDebMaintainer`
- `linuxMenuGroup`
- `linuxPackageDeps`
- `linuxPackageName`
- `linuxRpmLicenseType`
- `linuxShortcut`

Windows package options:

- `winConsole`
- `winDirChooser`
- `winHelpUrl`
- `winMenu`
- `winMenuGroup`
- `winPerUserInstall`
- `winShortcut`
- `winShortcutPrompt`
- `winUpdateUrl`
- `winUpgradeUuid`

Classpath example:

```xml
<plugin>
    <groupId>com.youngledo.jmcfx</groupId>
    <artifactId>jpackage-maven-plugin</artifactId>
    <version>${project.version}</version>
    <configuration>
        <name>Sample App</name>
        <packageType>app-image</packageType>
        <destination>${project.build.directory}/jpackage</destination>
        <input>${project.build.directory}/jpackage-input</input>
        <mainJar>${project.build.finalName}.jar</mainJar>
        <mainClass>com.acme.Main</mainClass>
        <runtimeImage>${project.build.directory}/runtime</runtimeImage>
    </configuration>
</plugin>
```

JPMS module example:

```xml
<plugin>
    <groupId>com.youngledo.jmcfx</groupId>
    <artifactId>jpackage-maven-plugin</artifactId>
    <version>${project.version}</version>
    <configuration>
        <name>Sample App</name>
        <packageType>app-image</packageType>
        <destination>${project.build.directory}/jpackage</destination>
        <modulePath>
            <path>${project.build.directory}/modules</path>
        </modulePath>
        <module>com.acme.app/com.acme.Main</module>
        <runtimeImage>${project.build.directory}/runtime</runtimeImage>
    </configuration>
</plugin>
```

## `leyden`

The `leyden` goal runs this workflow:

1. Stage the project main artifact and runtime dependencies into `inputDirectory`.
2. Build a runtime image with `jlink`.
3. Build a `jpackage --type app-image` app image with Leyden
   `-XX:AOTCacheOutput` enabled.
4. Run the generated app executable once in training mode.
5. Verify that the AOT cache was generated.
6. Rewrite the launcher config from `AOTCacheOutput` to runtime `AOTCache`.
7. Re-sign the mutated app image on macOS with an ad-hoc signature.
8. Build the final current-platform installer with `jpackage`.

Leyden-specific parameters:

- `trainingProperty`: defaults to `leyden.training`.
- `aotCacheName`: defaults to `startup.aot`.
- `inputDirectory`: defaults to `${project.build.directory}/jpackage-input`.
- `runtimeDirectory`: defaults to `${project.build.directory}/jlink-runtime`.
- `appImageDirectory`: defaults to `${project.build.directory}/jpackage-app-image`.
- `packageDirectory`: defaults to `${project.build.directory}/jpackage`.

Internal `jlink` parameters:

- `runtimeModules`
- `modulePath`
- `includeInputDirectoryOnModulePath`
- `noHeaderFiles`
- `noManPages`
- `stripDebug`
- `compress`
- `bindServices`
- `jlinkOptions`

The staged `inputDirectory` is included on the `jlink` module path by default so
automatic modules from runtime dependencies can be resolved. Additional module
or JMOD locations can be supplied with `modulePath`.

The `leyden` goal also accepts the same app, launcher, package, and platform
configuration used by the `jpackage` goal, including `module` for JPMS launch.
JPMS module support is implemented but not yet verified by a dedicated module
sample in this repository.

Classpath Leyden example:

```xml
<plugin>
    <groupId>com.youngledo.jmcfx</groupId>
    <artifactId>jpackage-maven-plugin</artifactId>
    <version>${project.version}</version>
    <executions>
        <execution>
            <phase>package</phase>
            <goals>
                <goal>leyden</goal>
            </goals>
            <configuration>
                <name>Sample App</name>
                <mainJar>${project.build.finalName}.jar</mainJar>
                <mainClass>com.acme.Main</mainClass>
                <runtimeModules>
                    <runtimeModule>java.desktop</runtimeModule>
                    <runtimeModule>java.logging</runtimeModule>
                </runtimeModules>
                <trainingProperty>sample.leyden.training</trainingProperty>
            </configuration>
        </execution>
    </executions>
</plugin>
```

JPMS Leyden example:

```xml
<plugin>
    <groupId>com.youngledo.jmcfx</groupId>
    <artifactId>jpackage-maven-plugin</artifactId>
    <version>${project.version}</version>
    <executions>
        <execution>
            <phase>package</phase>
            <goals>
                <goal>leyden</goal>
            </goals>
            <configuration>
                <name>Sample App</name>
                <module>com.acme.app/com.acme.Main</module>
                <runtimeModules>
                    <runtimeModule>com.acme.app</runtimeModule>
                    <runtimeModule>java.desktop</runtimeModule>
                </runtimeModules>
                <modulePath>
                    <path>${project.build.directory}/modules</path>
                </modulePath>
                <trainingProperty>sample.leyden.training</trainingProperty>
            </configuration>
        </execution>
    </executions>
</plugin>
```

The application must exit on its own when `-D<trainingProperty>=true` is present.
The plugin starts the generated app image directly during the Maven build and
waits for that process to finish.

## JMC FX Example

Inside this repository, JMC FX uses the Leyden workflow from the launcher
profile. The profile supplies JMC FX-specific modules, Java options, package
metadata, and output directories as normal consumer configuration:

```bash
sdk env && ./mvnw -pl jmc-fx-launcher -am -Pjpackage-classpath-jlink-leyden package
```

The installer is written to:

```text
jmc-fx-launcher/target/jpackage-leyden/
```

On macOS, the default output is:

```text
jmc-fx-launcher/target/jpackage-leyden/JMC FX-1.0.0.dmg
```
