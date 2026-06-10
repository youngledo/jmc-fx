# JMC FX

English ｜[中文](README_ZH.md)

---

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
the standalone `io.github.youngledo:jpackage-maven-plugin` published on Maven
Central. Its
`leyden` goal detects the current operating system and derives the
platform-specific `jpackage` options, installer type, app-image paths, and AOT
cache location internally. The installer is written to
`jmc-fx-launcher/target/jpackage-leyden/`. On macOS, the default output is
`JMC FX-1.0.0.dmg`.

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

## AI Assistant

JMC FX includes an AI assistant for opened `.jfr` recordings. Open a recording,
go to the Analysis page, switch to the AI tab, and choose `Analyze with AI` to
generate a structured diagnostic report.

The assistant uses an OpenAI-compatible chat completions endpoint. Configure it
from Settings -> AI:

- Enable the AI assistant.
- Set the provider base URL, model, temperature, and maximum output tokens.
- Provide `OPENAI_API_KEY` in the process environment before launching JMC FX.

JMC FX does not display or save the API key. The AI request is manually
triggered, recording-scoped, and built from deterministic analysis summaries
such as rule results, metadata, GC, exceptions, profiling, threads, heap,
allocation, locks, and I/O summaries. The raw `.jfr` file is not uploaded.

The generated report includes a summary, findings, evidence, limitations, and
follow-up questions. Findings can include related-page links; choosing one
navigates to the relevant recording analysis page so the evidence can be
inspected in the normal JMC FX workflow. If the provider fails, times out, or
returns an invalid response, the error is contained inside the AI panel and does
not affect the rest of the recording pages.

## Legal Notice

JMC FX is an independent project and is not affiliated with, endorsed by, or sponsored by Oracle or the OpenJDK project.
