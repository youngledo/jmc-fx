# Hexagonal Boundary Guide

This guide keeps JMC FX module boundaries stable after the application-layer
refactor. Use it when adding workflows, ports, adapters, UI pages, launch
wiring, or architecture tests.

## Current Shape

```text
                  primary adapter
                 +----------------+
                 |  jmc-fx-ui     |
                 +-------+--------+
                         |
                         v
                 +----------------+
                 | jmc-fx-        |
                 | application    |
                 +-------+--------+
                         |
                         v
                 +----------------+
                 | jmc-fx-domain  |
                 +-------+--------+
                         ^
                         |
                 +-------+--------+
                 | jmc-fx-adapter |
                 | -jmc           |
                 +----------------+

      jmc-fx-launcher wires UI, application, and adapters at startup.
```

Dependencies point toward the core. The launcher is the composition root, not
the application layer.

## Module Responsibilities

`jmc-fx-domain`

- Defines UI-neutral records, enums, exceptions, and service ports.
- Has no dependency on JavaFX, OpenJDK JMC, adapters, UI, application, or
  launcher.
- Use this module when a type names a business or technical concept that is
  stable across UI and adapter implementations.

`jmc-fx-application`

- Coordinates use cases and workflow-level service groups.
- Depends on `jmc-fx-domain` and JDK types.
- Does not depend on JavaFX, AtlantaFX, Ikonli, OpenJDK JMC, Jolokia, adapter,
  UI, or launcher packages.
- Use this module when UI actions need orchestration across multiple ports,
  workspace opening, capability decisions, or workflow result shaping.

`jmc-fx-adapter-jmc`

- Implements domain ports using OpenJDK JMC, JFR, JMX, Jolokia, and JOverflow
  APIs.
- Does not depend on UI, application, launcher, JavaFX, AtlantaFX, or Ikonli.
- Use this module for all integration details that would change if the backing
  JMC implementation changed.

`jmc-fx-ui`

- Owns JavaFX views, controllers, view models, navigation, theme, CSS, i18n,
  preferences, presentation state, JavaFX properties, `ObservableList`, and
  user-facing task state.
- May call application use cases and use domain records.
- Must not import `com.youngledo.jmcfx.domain.service` ports in production
  sources.
- Must not import concrete adapter classes or OpenJDK JMC APIs.
- Keep controllers thin; put testable presentation behavior in view models.
- `JavaAppPreferences` remains a UI-local Java Preferences-backed presentation
  preference store. Live JVM persistence adapters belong in
  `jmc-fx-adapter-preferences`; do not add new domain-port persistence
  implementations to UI.

`jmc-fx-launcher`

- Owns JavaFX startup, dependency assembly, stage lifecycle, theme bootstrap,
  Maven run configuration, jlink, and jpackage.
- Wires concrete adapters to UI and application services.
- Keep detailed adapter construction delegated to focused assembly factories so
  the JavaFX `Application` subclass does not grow into a service registry.

`jmc-fx-test-support`

- Owns fakes, fixtures, builders, and deterministic helpers shared by tests.
- Keep production modules independent of test-support code.

## Placement Rules

Put a new type in `jmc-fx-domain` when it is a UI-neutral data concept, domain
exception, enum, or outbound port needed by application code.

Put it in `jmc-fx-application` when it coordinates a user workflow, combines
multiple domain ports, opens a recording or heap dump repository, decides which
sections or capabilities are available, or returns a workflow result to the UI.

Put it in `jmc-fx-adapter-jmc` when it imports `org.openjdk.jmc`, `org.jolokia`,
or implements a domain port with JMC-specific details.

Put it in `jmc-fx-ui` when it is JavaFX-specific, presentation-specific, or
depends on control state, bindings, selections, formatting, localization, or
view lifecycle.

Put it in `jmc-fx-launcher` when it starts the app, chooses concrete
implementations, creates top-level dependencies, or changes packaging/runtime
configuration.

## Common Boundary Smells

- `jmc-fx-ui` imports `com.youngledo.jmcfx.adapter...` or `org.openjdk.jmc...`.
- `jmc-fx-ui` imports `com.youngledo.jmcfx.domain.service...`.
- `jmc-fx-application` imports JavaFX, adapter, UI, launcher, or external JMC
  packages.
- `jmc-fx-adapter-jmc` imports UI, application, launcher, JavaFX, AtlantaFX, or
  Ikonli packages.
- A controller directly performs workflow orchestration across several ports.
- The JavaFX `Application` subclass contains a long list of concrete
  `new Jmc...` adapter constructions instead of delegating assembly.
- A domain record contains JavaFX properties, controls, observable collections,
  or JMC framework objects.

## Guardrails

ArchUnit tests protect the most important boundaries:

- `jmc-fx-launcher/src/test/java/com/youngledo/jmcfx/launcher/JmcFxArchitectureTest.java`
- `jmc-fx-launcher/src/test/java/com/youngledo/jmcfx/launcher/JmcFxModuleBoundaryTest.java`
- `jmc-fx-application/src/test/java/com/youngledo/jmcfx/application/JmcFxApplicationArchitectureTest.java`

Run these tests after changing module dependencies or moving workflow logic:

```bash
sdk env
./mvnw -pl jmc-fx-launcher,jmc-fx-application -am test
```

Before completing any change, run the repository verification baseline:

```bash
sdk env
./mvnw -v
./mvnw verify
rg -n "<modules>|<module>" pom.xml **/pom.xml
rg -n "org.openjdk.jmc|JfrLoaderToolkit" jmc-fx-ui jmc-fx-launcher jmc-fx-application jmc-fx-domain
rg -n "import com\\.youngledo\\.jmcfx\\.domain\\.service" jmc-fx-ui/src/main/java
```

Expected results:

- Maven is 4.x and Java is 26.
- Full verification passes.
- No Maven 3 `<modules>` syntax appears.
- No OpenJDK JMC API usage appears outside `jmc-fx-adapter-jmc`.
- No production UI source imports domain service ports.

## Startup Module Name

The startup and packaging module is `jmc-fx-launcher`. Do not use the old
`jmc-fx-app` name in Maven commands.

Run the desktop app with:

```bash
sdk env
./mvnw -pl jmc-fx-launcher -am org.openjfx:javafx-maven-plugin:0.0.8:run
```

Using `-pl jmc-fx-app` fails because that reactor project no longer exists.
