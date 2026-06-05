# Third-Party Notices

This file tracks third-party components used by JMC FX.

## OpenJDK JMC Core/Headless Libraries

JMC FX may consume OpenJDK JMC core/headless artifacts for JFR parsing, JFR rule
analysis, and related data models. OpenJDK JMC is distributed under its own
license notices, including UPL 1.0 and BSD-style terms in the upstream project.

When JMC core/headless artifacts are redistributed, include the corresponding
OpenJDK JMC license and third-party notice files.

## JavaFX

JavaFX dependencies must be reviewed when runtime packaging is added.

## AtlantaFX

AtlantaFX dependencies must be reviewed when runtime packaging is added.

## Ikonli

Ikonli dependencies must be reviewed when runtime packaging is added.

## JPackage Maven Plugin

The `io.github.youngledo:jpackage-maven-plugin` Maven plugin is used at build
time to invoke `jlink`, `jpackage`, and the Leyden AOT cache workflow for
application installers. Review its license and notices before distributing
generated packages.

## SLF4J

SLF4J is used as the logging API. Review its license terms before redistributing
runtime artifacts.

## Apache Log4j 2

Apache Log4j 2 is used as the runtime logging implementation. Review its Apache
License 2.0 terms and bundled notices before redistributing runtime artifacts.
