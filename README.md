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

## Native Package

Build the current platform's native installer with the `native-package` profile:

```bash
sdk env
./mvnw -pl jmc-fx-app -am -Pnative-package package
```

The installer is written to `jmc-fx-app/target/jpackage/`. On macOS, the
default output is `JMC FX-1.0.0.dmg`.

The default profile builds a trimmed runtime image with `jlink` before invoking
`jpackage`. A full-JDK runtime fallback is available for troubleshooting:

```bash
./mvnw -pl jmc-fx-app -am -Pnative-package-full-runtime package
```

`jpackage` requires platform-specific package versions. The default package
version is `1.0.0` because macOS rejects versions whose first number is `0`.
Override it for releases with:

```bash
./mvnw -pl jmc-fx-app -am -Pnative-package -Djmcfx.package.version=1.2.3 package
```

## Run

```bash
sdk env
./mvnw -pl jmc-fx-app -am org.openjfx:javafx-maven-plugin:0.0.8:run
```

## Legal Notice

JMC FX is an independent project and is not affiliated with, endorsed by, or sponsored by Oracle or the OpenJDK project.
