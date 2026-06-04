# JMC FX Agent Instructions

## Project Identity

JMC FX is an independent JavaFX desktop application that rebuilds JDK Mission Control user workflows with a modern JavaFX UI while reusing OpenJDK JMC core/headless libraries through adapter boundaries.

This project is not a fork of the OpenJDK JMC Eclipse RCP/SWT application. Do not modify the OpenJDK JMC source tree while working in this repository.

## Hard Platform Baseline

These rules apply to every agent task, every implementation plan, every code review, and every future feature.

- Java 26 is the minimum and primary runtime.
- JavaFX 26 is the minimum and primary UI toolkit.
- Maven 4.x is mandatory.
- Maven build POMs must use model version `4.1.0`.
- The root POM must use `root="true"`.
- Maven reactor aggregation must use `<subprojects>` and `<subproject>...</subproject>`, not Maven 3 `<modules>`.
- Do not add Java 17 or Java 21 compatibility constraints.
- Do not add obsolete JavaFX startup flags or workarounds for unsupported old JDKs.
- Do not silently enable preview, incubator, or experimental JDK features in the default build. If needed, use an explicit Maven profile and document the reason.

## Modern Java Expectations

Use Java 26 style deliberately where it improves clarity:

- Use records for immutable data carriers.
- Use sealed types for bounded domain hierarchies.
- Use modern switch and pattern matching when they simplify control flow.
- Use virtual threads for non-FX blocking service work where appropriate.
- Keep JavaFX UI state on the FX Application Thread.
- Use JavaFX `Task` or `Service` for long UI-triggered operations.
- Avoid legacy Swing, SWT, or Eclipse RCP UI patterns.

## Architecture Boundaries

JMC FX follows the **Hexagonal (Ports & Adapters)** architecture:

- **Domain core** (`jmc-fx-domain`) defines ports (interfaces) and data records, depending on nothing.
- **Application** (`jmc-fx-application`) coordinates use cases and workflow service groups using domain ports.
- **Adapter** (`jmc-fx-adapter`) implements those ports by calling external frameworks and persistence APIs.
- **UI** (`jmc-fx-ui`) drives application use cases and uses domain models, never domain service ports or adapter internals.
- **Launcher** (`jmc-fx-launcher`) assembles concrete adapters and injects them at startup.

The intended subprojects are:

- `jmc-fx-domain`: UI-neutral records, enums, ports, and exceptions.
- `jmc-fx-application`: use-case orchestration and workflow-level service groups.
- `jmc-fx-adapter`: secondary adapters, separated by package:
  `com.youngledo.jmcfx.adapter.jmc` for OpenJDK JMC core/headless integration,
  and `com.youngledo.jmcfx.adapter.preferences` for Java Preferences-backed
  persistence adapters.
- `jmc-fx-ui`: JavaFX/FXML/CSS, controllers, view models, navigation, task state.
- `jmc-fx-launcher`: application startup, dependency assembly, stage lifecycle, packaging.
- `jmc-fx-test-support`: fakes, fixtures, and deterministic test helpers.

Rules:

- UI code must not directly call OpenJDK JMC APIs.
- UI code must not directly instantiate or call concrete adapter implementations.
- Production UI code must not import `com.youngledo.jmcfx.domain.service`.
- UI-driven workflows enter through `jmc-fx-application` use cases.
- Secondary persistence adapters belong outside `jmc-fx-ui`.
- Workflow orchestration that spans ports or decides feature capabilities belongs in `jmc-fx-application`.
- JMC API usage belongs in `jmc-fx-adapter` under
  `com.youngledo.jmcfx.adapter.jmc`.
- `jmc-fx-domain` must stay free of JavaFX types.
- `jmc-fx-application` must stay free of JavaFX, adapter, and launcher types.
- Controllers stay thin. Put testable state and behavior in view models.
- Long-running work must not run on the FX Application Thread.
- Large JFR event data must be paged, sliced, or summarized; do not eagerly load huge recordings into JavaFX observable lists.

## UI and Theme Rules

- Use AtlantaFX as the base theme.
- Use the system theme preference by default. Resolve it to `PrimerLight` for light mode and `PrimerDark` for dark mode, with `PrimerLight` as the fallback when the platform color scheme is unavailable.
- Add application CSS after AtlantaFX.
- Follow `docs/ui-guidelines.md` and `docs/ui-ux-system.md` for every UI/UX change.
- Before adding or modifying UI, identify which `docs/ui-ux-system.md` page template and workflow pattern applies.
- If a UI change intentionally deviates from `docs/ui-ux-system.md`, document the reason in the change summary or feature spec.
- Prefer AtlantaFX style classes before custom CSS.
- Do not fork or edit AtlantaFX source CSS.
- Do not override AtlantaFX standard control states unless a maintainer approves a project-wide rule.
- Use dense `TableView` layouts for large technical datasets.
- Prefer context menus and detail panels over many inline row buttons.

## Licensing and Trademark Rules

- Reuse OpenJDK JMC source-built or Maven-style core/headless artifacts only.
- Do not copy Oracle JDK Mission Control commercial binary assets.
- Do not copy Eclipse RCP/SWT UI code, icons, branding, workbench layouts, or product assets.
- Preserve OpenJDK JMC copyright notices, license notices, and third-party notices when redistributing JMC-derived artifacts.
- Maintain project-level `LICENSE`, `NOTICE`, and `THIRD_PARTY_NOTICES.md` before public binary distribution.
- Include third-party notices for JMC core/headless dependencies, JavaFX, AtlantaFX, Ikonli, TestFX, Maven plugins, and future runtime dependencies.
- Do not imply Oracle or OpenJDK endorsement.
- Keep this disclaimer in public-facing docs:

```text
JMC FX is an independent project and is not affiliated with, endorsed by, or sponsored by Oracle or the OpenJDK project.
```

## Verification Required Before Completion

Before claiming work is complete, run or explicitly document why you could not run:

```bash
sdk env
./mvnw -v
./mvnw verify
```

Also verify:

```bash
rg -n "<modules>|<module>" pom.xml **/pom.xml
rg -n "org.openjdk.jmc|JfrLoaderToolkit" jmc-fx-ui jmc-fx-launcher jmc-fx-application jmc-fx-domain
```

Expected:

- `./mvnw -v` reports Maven 4.x and Java 26.
- `./mvnw verify` passes.
- No Maven 3 `<modules>` syntax exists.
- No JMC API usage appears outside `jmc-fx-adapter`.

If verification cannot run because the project has not been scaffolded yet, state that clearly in the final response.
