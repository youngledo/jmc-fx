# JMC FX

JMC FX is an independent JavaFX desktop application that rebuilds the JDK Mission Control UI while reusing [JMC](https://github.com/openjdk/jmc) core/headless libraries.

## Requirements

- JDK 26
- JavaFX 26
- Maven Wrapper from this repository, which downloads Maven 4.0.0-rc-5

This repository includes `.sdkmanrc` with the required Java version. If you use
SDKMAN, activate the project JDK before running Maven:

```bash
sdk env
./mvnw -v
```

The Maven Wrapper fixes the Maven version, but it does not choose the JDK by
itself. `./mvnw -v` must report Java 26 before build or run commands are
expected to work.

## Build

```bash
sdk env
./mvnw verify
```

Architecture boundaries are documented in
`docs/hexagonal-boundary-guide.md`. Use that guide when adding workflows,
ports, adapters, UI pages, or startup wiring.

## Platform Installer

Build the current platform's Leyden-optimized installer with the
`jpackage-classpath-jlink-leyden` profile:

```bash
sdk env && ./mvnw -pl jmc-fx-launcher -am -Pjpackage-classpath-jlink-leyden package
```

There is one Leyden installer profile. It delegates the packaging workflow to
the in-repository `jmc-fx-leyden-packager-maven-plugin`, which detects the
current operating system and derives the platform-specific `jpackage` options,
installer type, app-image paths, and AOT cache location internally. The
installer is written to `jmc-fx-launcher/target/jpackage-leyden/`. On macOS, the
default output is `JMC FX-1.0.0.dmg`.

Leyden packaging is intentionally kept behind an explicit profile instead of the
default build. The installer flow reaches Maven's `package` phase, runs the
application once to train an AOT cache, rewrites the jpackage launcher
configuration from `-XX:AOTCacheOutput` to `-XX:AOTCache`, then packages the
trained app image. Keeping it explicit prevents normal `./mvnw verify` runs from
building installers.

The package uses a `jlink`-trimmed JDK/JavaFX runtime and launches the
application from the classpath. OpenJDK JMC 9.1.2 dependencies are automatic
modules, so they cannot currently be linked into a full JPMS `jlink` image.
When JMC artifacts become explicit JPMS modules, this installer path can move to
a module launch and full application-module `jlink` image.

Future GraalVM native-image packaging should use a separate profile instead of
overloading this jpackage/Leyden installer path.

`jpackage` requires platform-specific package versions. The default package
version is `1.0.0` because macOS rejects versions whose first number is `0`.
Override it for releases with:

```bash
./mvnw -pl jmc-fx-launcher -am -Pjpackage-classpath-jlink-leyden -Djmcfx.package.version=1.2.3 package
```

## Run

```bash
sdk env && ./mvnw -pl jmc-fx-launcher -am org.openjfx:javafx-maven-plugin:0.0.8:run
```

## Legal Notice

JMC FX is an independent project and is not affiliated with, endorsed by, or sponsored by Oracle or the OpenJDK project.
