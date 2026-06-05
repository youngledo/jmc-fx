package com.youngledo.jmcfx.packager;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class ToolExecutor {

    private final Path javaHome;
    private final CommandRunner commandRunner;

    ToolExecutor(Path javaHome, CommandRunner commandRunner) {
        this.javaHome = javaHome;
        this.commandRunner = commandRunner;
    }

    List<String> jlinkCommand(Path inputDirectory, Path runtimeDirectory, List<String> modules) {
        return List.of(javaHome.resolve("bin").resolve("jlink").toString(),
                "--no-header-files",
                "--no-man-pages",
                "--strip-debug",
                "--output", runtimeDirectory.toString(),
                "--module-path", inputDirectory.toString(),
                "--add-modules", String.join(",", modules));
    }

    List<String> appImageCommand(PackagingPaths paths, OperatingSystem operatingSystem, String name,
            String version, String vendor, String description, String mainJar, String mainClass,
            List<String> javaOptions, String trainingProperty, String macPackageIdentifier, String macPackageName,
            String linuxPackageName, String linuxAppCategory, boolean winMenu, boolean winShortcut,
            String winMenuGroup) {
        var command = baseJpackageCommand(paths.appImageDirectory(), name, "app-image", version, vendor, description);
        command.add("--input");
        command.add(paths.inputDirectory().toString());
        command.add("--main-jar");
        command.add(mainJar);
        command.add("--main-class");
        command.add(mainClass);
        command.add("--runtime-image");
        command.add(paths.runtimeDirectory().toString());
        for (var option : javaOptions) {
            command.add("--java-options");
            command.add(option);
        }
        command.add("--java-options");
        command.add("-D" + trainingProperty + "=true");
        command.add("--java-options");
        command.add("-XX:AOTCacheOutput=$APPDIR/" + paths.aotCache().getFileName());
        addPlatformOptions(command, operatingSystem, macPackageIdentifier, macPackageName, linuxPackageName, linuxAppCategory,
                winMenu, winShortcut, winMenuGroup);
        return command;
    }

    List<String> installerCommand(PackagingPaths paths, OperatingSystem operatingSystem, String name,
            String packageType, String version, String vendor, String description, String macPackageIdentifier, String macPackageName,
            String linuxPackageName, String linuxAppCategory, boolean winMenu, boolean winShortcut,
            String winMenuGroup) {
        var command = baseJpackageCommand(paths.packageDirectory(), name, packageType, version, vendor, description);
        command.add("--app-image");
        command.add(paths.appImage().toString());
        addPlatformOptions(command, operatingSystem, macPackageIdentifier, macPackageName, linuxPackageName, linuxAppCategory,
                winMenu, winShortcut, winMenuGroup);
        return command;
    }

    List<String> codesignCommand(Path appImage) {
        return List.of("/usr/bin/codesign", "-s", "-", "--force", "--deep", appImage.toString());
    }

    void run(List<String> command) throws Exception {
        commandRunner.run(command);
    }

    private List<String> baseJpackageCommand(Path destination, String name, String type, String version,
            String vendor, String description) {
        var command = new ArrayList<String>();
        command.add(javaHome.resolve("bin").resolve("jpackage").toString());
        command.add("--name");
        command.add(name);
        command.add("--dest");
        command.add(destination.toString());
        command.add("--verbose");
        command.add("--type");
        command.add(type);
        command.add("--app-version");
        command.add(version);
        command.add("--vendor");
        command.add(vendor);
        command.add("--description");
        command.add(description);
        return command;
    }

    private void addPlatformOptions(List<String> command, OperatingSystem operatingSystem,
            String macPackageIdentifier, String macPackageName, String linuxPackageName, String linuxAppCategory,
            boolean winMenu, boolean winShortcut, String winMenuGroup) {
        switch (operatingSystem) {
            case MACOS -> {
                command.add("--mac-package-identifier");
                command.add(macPackageIdentifier);
                command.add("--mac-package-name");
                command.add(macPackageName);
            }
            case LINUX -> {
                command.add("--linux-package-name");
                command.add(linuxPackageName);
                command.add("--linux-app-category");
                command.add(linuxAppCategory);
            }
            case WINDOWS -> {
                if (winMenu) {
                    command.add("--win-menu");
                }
                if (winShortcut) {
                    command.add("--win-shortcut");
                }
                command.add("--win-menu-group");
                command.add(winMenuGroup);
            }
        }
    }
}
