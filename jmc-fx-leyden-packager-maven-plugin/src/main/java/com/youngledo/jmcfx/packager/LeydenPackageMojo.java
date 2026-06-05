package com.youngledo.jmcfx.packager;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import org.apache.maven.api.PathScope;
import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.plugin.Log;
import org.apache.maven.api.plugin.MojoException;
import org.apache.maven.api.plugin.annotations.Mojo;
import org.apache.maven.api.plugin.annotations.Parameter;

@Mojo(name = "leyden-package", defaultPhase = "package")
public class LeydenPackageMojo implements org.apache.maven.api.plugin.Mojo {

    @Inject
    private Project project;

    @Inject
    private Session session;

    @Inject
    private Log log;

    @Parameter(required = true)
    private String name;

    @Parameter(required = true)
    private String mainClass;

    @Parameter(defaultValue = "${project.build.finalName}.jar", required = true)
    private String mainJar;

    @Parameter(property = "jmcfx.package.version", defaultValue = "1.0.0")
    private String packageVersion;

    @Parameter(defaultValue = "${project.description}")
    private String description;

    @Parameter(defaultValue = "Youngledo")
    private String vendor;

    @Parameter
    private List<String> runtimeModules = List.of(
            "java.desktop",
            "java.management",
            "java.naming",
            "java.rmi",
            "java.sql",
            "jdk.attach",
            "jdk.jfr",
            "jdk.management.agent",
            "jdk.unsupported",
            "javafx.controls");

    @Parameter
    private List<String> javaOptions = List.of("--enable-native-access=javafx.graphics");

    @Parameter(defaultValue = "jmcfx.leyden.training")
    private String trainingProperty;

    @Parameter(defaultValue = "jmcfx-startup.aot")
    private String aotCacheName;

    @Parameter(defaultValue = "com.youngledo.jmcfx")
    private String macPackageIdentifier;

    @Parameter(defaultValue = "JMC FX")
    private String macPackageName;

    @Parameter(defaultValue = "jmc-fx")
    private String linuxPackageName;

    @Parameter(defaultValue = "Development")
    private String linuxAppCategory;

    @Parameter(defaultValue = "true")
    private boolean winMenu;

    @Parameter(defaultValue = "true")
    private boolean winShortcut;

    @Parameter(defaultValue = "JMC FX")
    private String winMenuGroup;

    @Parameter(defaultValue = "${java.home}")
    private Path javaHome;

    static String defaultAotCacheName() {
        return "jmcfx-startup.aot";
    }

    static Path defaultJavaHome() {
        return Path.of(System.getProperty("java.home"));
    }

    @Override
    public void execute() throws Exception {
        var os = OperatingSystem.current();
        var buildDirectory = Path.of(project.getBuild().getDirectory());
        var paths = PackagingPaths.derive(buildDirectory, name, packageVersion, aotCacheName, os);
        var executor = new ToolExecutor(javaHome, new ProcessCommandRunner());

        try {
            deleteIfExists(paths.inputDirectory());
            deleteIfExists(paths.runtimeDirectory());
            deleteIfExists(paths.appImageDirectory());
            deleteIfExists(paths.packageDirectory());

            var mainArtifact = project.getMainArtifact()
                    .flatMap(session::getArtifactPath)
                    .orElseThrow(() -> new MojoException("Missing main project artifact path"));
            var runtimeArtifacts = session.resolveDependencies(project, PathScope.MAIN_RUNTIME).stream()
                    .filter(path -> !path.equals(mainArtifact))
                    .toList();

            new ProjectStager().stage(mainArtifact, runtimeArtifacts, paths.inputDirectory());
            executor.run(executor.jlinkCommand(paths.inputDirectory(), paths.runtimeDirectory(), runtimeModules));
            executor.run(executor.appImageCommand(paths, os, name, packageVersion, vendor, description, mainJar, mainClass,
                    javaOptions, trainingProperty, macPackageIdentifier, macPackageName, linuxPackageName,
                    linuxAppCategory, winMenu, winShortcut, winMenuGroup));
            executor.run(List.of(paths.appExecutable().toString()));
            if (!Files.exists(paths.aotCache())) {
                throw new MojoException("Leyden AOT cache was not generated: " + paths.aotCache());
            }
            new LauncherConfigEditor().rewriteForRuntime(paths.appConfig(), trainingProperty, aotCacheName);
            if (os == OperatingSystem.MACOS) {
                executor.run(executor.codesignCommand(paths.appImage()));
            }
            executor.run(executor.installerCommand(paths, os, name, os.jpackageType(), packageVersion, vendor, description,
                    macPackageIdentifier, macPackageName, linuxPackageName, linuxAppCategory, winMenu, winShortcut,
                    winMenuGroup));
        } catch (MojoException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoException("Leyden packaging failed", e);
        }
    }

    private void deleteIfExists(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) {
                    throw exc;
                }
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private final class ProcessCommandRunner implements CommandRunner {
        @Override
        public void run(List<String> command) throws Exception {
            log.info(String.join(" ", command));
            try (var process = new ProcessBuilder(command).inheritIO().start()) {
                var exitCode = process.waitFor();
                if (exitCode != 0) {
                    throw new MojoException("Command failed with exit code " + exitCode + ": "
                            + String.join(" ", command));
                }
            }
        }
    }
}
