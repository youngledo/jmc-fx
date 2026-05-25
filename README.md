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

## Run

```bash
sdk env
./mvnw -pl jmc-fx-app -am org.openjfx:javafx-maven-plugin:0.0.8:run
```

## Legal Notice

JMC FX is an independent project and is not affiliated with, endorsed by, or sponsored by Oracle or the OpenJDK project.
