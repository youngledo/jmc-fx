# Contributing to JMC FX

## Project Scope

JMC FX is an independent JavaFX desktop application for modern JDK Mission Control workflows. It reuses OpenJDK JMC core/headless libraries through adapter boundaries and replaces the old UI approach with JavaFX.

JMC FX does not modify or copy the OpenJDK JMC Eclipse RCP/SWT application UI.

## Required Toolchain

- JDK 26
- JavaFX 26
- Maven Wrapper from this repository, fixed to Maven 4.0.0-rc-5

The repository includes `.sdkmanrc` for SDKMAN users. Run `sdk env` after
entering the project root so `./mvnw` starts on the required JDK:

```bash
sdk env
./mvnw -v
```

The wrapper controls Maven, not Java. If `./mvnw -v` reports Java 25 or any
other JDK, fix the active shell JDK before building.

Maven build files must use:

- POM model version `4.1.0`
- `root="true"` on the root POM
- `<subprojects>` for reactor aggregation

Do not use Maven 3 `<modules>` in this repository.

## Engineering Principles

- Keep domain models UI-neutral.
- Keep all OpenJDK JMC API calls inside `jmc-fx-adapter`, under the
  `com.youngledo.jmcfx.adapter.jmc` package.
- Drive user workflows through `jmc-fx-application` use cases instead of
  letting UI controllers call secondary adapters directly.
- Keep JavaFX controllers thin and move behavior into view models.
- Keep long-running file, parsing, analysis, JMX, and recording-control work off the FX Application Thread.
- Prefer Java 26 language and library features when they make code clearer.
- Do not enable preview or experimental JDK features in the default build.
- Use AtlantaFX as the base theme and layer small app-specific CSS after it.

## Architecture Boundaries

JMC FX follows Hexagonal Architecture. The durable boundary guide is
`docs/hexagonal-boundary-guide.md`; use it before adding new features, moving
workflow logic, introducing ports, or wiring adapters.

In short:

- `jmc-fx-domain` owns UI-neutral records, enums, exceptions, and secondary
  ports.
- `jmc-fx-application` owns use-case orchestration and workflow service
  groupings.
- `jmc-fx-adapter` owns secondary adapters. Keep package boundaries clear:
  `com.youngledo.jmcfx.adapter.jmc` owns OpenJDK JMC, JFR, JMX, Jolokia, and
  JOverflow integration, while `com.youngledo.jmcfx.adapter.preferences` owns
  Java Preferences-backed persistence adapters.
- `jmc-fx-ui` owns JavaFX views, controllers, view models, themes,
  preferences, i18n, and presentation state. Production UI code may use domain
  models, but must not import domain service ports.
- `jmc-fx-launcher` owns startup, dependency assembly, stage lifecycle, and
  packaging.

UI-driven workflows enter through `jmc-fx-application` use cases. Secondary
persistence adapters belong outside `jmc-fx-ui`.

## UI Design Workflow

Use the project UI guidelines in `docs/ui-guidelines.md`, the project UI
information architecture in `docs/superpowers/specs/`, and the hard UI rules in
`AGENTS.md` as the default source of truth for normal JavaFX UI work. These
rules cover the v1 enterprise desktop shell, AtlantaFX theme usage, navigation
structure, detail panels, status regions, dense technical tables,
controller/view model boundaries, and long-running task behavior.

Do not invoke `ui-ux-pro-max` mechanically for every UI change. Invoke it when a
change introduces a new or complex UI surface that is not covered by the
project UI IA, such as:

- major layout or navigation changes,
- charting or advanced data visualization,
- theme, palette, typography, or accessibility review,
- complex forms, empty states, error states, or workflow review,
- explicit UI/UX design critique requested by a maintainer.

## Comments and Documentation

Comments are part of the code contract. Add comments when they preserve design
intent that is not obvious from the code itself, especially around architecture
boundaries, JMC adapter assumptions, JavaFX threading decisions, paging choices
for large JFR data, licensing constraints, and non-trivial failure handling.

Do not add comments that merely repeat the code. Prefer clearer names, smaller
methods, records, sealed types, or focused helper methods before adding a
comment.

Java documentation comments must use Java 26 Markdown documentation comments
with `///`, not traditional block Javadoc with `/** ... */`. Their prose and
examples must use Markdown formatting:

- Use backticks for identifiers, file names, commands, and literal values.
- Use Markdown lists for multi-item behavior notes.
- Use fenced code blocks for examples or command snippets.
- Keep the first sentence a concise summary.
- Document public APIs, service ports, adapter boundary types, view models with
  user-visible behavior, and any method whose threading or lifecycle contract is
  important.

## Build and Test

Use the repository Maven Wrapper so every contributor builds with the same
Maven 4 runtime. Activate the `.sdkmanrc` JDK first:

```bash
sdk env
./mvnw verify
```

To run the desktop app:

```bash
sdk env
./mvnw -pl jmc-fx-launcher -am org.openjfx:javafx-maven-plugin:0.0.8:run
```

Before submitting changes, verify:

```bash
sdk env
./mvnw -v
./mvnw verify
rg -n "<modules>|<module>" pom.xml **/pom.xml
rg -n "org.openjdk.jmc|JfrLoaderToolkit" jmc-fx-ui jmc-fx-launcher jmc-fx-application jmc-fx-domain
```

Expected:

- Maven 4.0.0-rc-5 through the repository wrapper
- Java 26
- No Maven 3 `<modules>` syntax
- No JMC API usage outside `jmc-fx-adapter`

## Git Commit Messages

Commit messages must use Conventional Commits:

```text
<type>(optional-scope): <subject>
```

Allowed types:

- `feat`
- `fix`
- `docs`
- `style`
- `refactor`
- `test`
- `build`
- `ci`
- `chore`
- `perf`
- `revert`

Examples:

```text
feat: add JVM browser
fix(adapter-jmc): handle missing recording id
docs!: rewrite contributor guide
```

Rules:

- Use a lowercase allowed type.
- Use lowercase scopes with letters, digits, `.`, `_`, or `-`.
- Keep the subject required and concise.
- Do not end the subject with a period.

Enable the repository Git hooks once after cloning:

```bash
git config core.hooksPath .githooks
```

The local `commit-msg` hook and GitHub Actions both run
`scripts/validate-commit-message.sh`, so the same rules apply locally and in CI.

## Licensing

JMC FX may reuse OpenJDK JMC core/headless libraries under their open source licenses, but contributors must keep attribution and redistribution obligations intact.

Rules:

- Do not copy Oracle JDK Mission Control commercial binary assets.
- Do not copy Eclipse RCP/SWT UI code, icons, branding, workbench layouts, or product assets.
- Preserve OpenJDK JMC copyright notices, license notices, and third-party notices when redistributing JMC-derived artifacts.
- Update `THIRD_PARTY_NOTICES.md` when dependencies change.
- Keep project-level `LICENSE`, `NOTICE`, and `THIRD_PARTY_NOTICES.md` ready before any public binary distribution.

## Trademark and Affiliation

JMC FX is an independent project and is not affiliated with, endorsed by, or sponsored by Oracle or the OpenJDK project.

Before public release, review the project name, package metadata, app bundle name, icons, screenshots, and README wording for Oracle, Java, OpenJDK, and JDK Mission Control trademark risk.

## Pull Request Checklist

- The change preserves the Java 26, JavaFX 26, and Maven 4 baseline.
- The change does not introduce Maven 3 reactor syntax.
- UI code does not directly depend on OpenJDK JMC APIs.
- Long-running operations do not run on the FX Application Thread.
- New user-facing workflows have view model tests or TestFX coverage.
- Dependency changes update third-party notices or explain why no notice update is needed.
