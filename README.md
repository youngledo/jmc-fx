# JMC FX

JMC FX is an independent JavaFX desktop application that rebuilds the JDK Mission Control UI while reusing JMC core/headless libraries.

## Requirements

- JDK 25
- JavaFX 25
- Maven Wrapper from this repository, which downloads Maven 4.0.0-rc-5

## Build

```bash
./mvnw verify
```

## Run

```bash
./mvnw -pl jmc-fx-app -am org.openjfx:javafx-maven-plugin:0.0.8:run
```

## Legal Notice

JMC FX is an independent project and is not affiliated with, endorsed by, or sponsored by Oracle or the OpenJDK project.
