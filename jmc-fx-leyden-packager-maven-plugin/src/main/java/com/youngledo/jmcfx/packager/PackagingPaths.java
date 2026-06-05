package com.youngledo.jmcfx.packager;

import java.nio.file.Path;

record PackagingPaths(
        Path inputDirectory,
        Path runtimeDirectory,
        Path appImageDirectory,
        Path packageDirectory,
        Path appImage,
        Path appExecutable,
        Path appConfig,
        Path aotCache,
        Path installer) {

    static PackagingPaths derive(Path buildDirectory, String appName, String version, String aotCacheName,
            OperatingSystem operatingSystem) {
        var input = buildDirectory.resolve("jpackage-leyden-input");
        var runtime = buildDirectory.resolve("jpackage-leyden-runtime");
        var appImageDirectory = buildDirectory.resolve("jpackage-leyden-app-image");
        var packageDirectory = buildDirectory.resolve("jpackage-leyden");
        var installer = packageDirectory.resolve(appName + "-" + version + "." + operatingSystem.installerExtension());

        return switch (operatingSystem) {
            case MACOS -> {
                var appImage = appImageDirectory.resolve(appName + ".app");
                var appDir = appImage.resolve("Contents").resolve("app");
                yield new PackagingPaths(input, runtime, appImageDirectory, packageDirectory, appImage,
                        appImage.resolve("Contents").resolve("MacOS").resolve(appName),
                        appDir.resolve(appName + ".cfg"),
                        appDir.resolve(aotCacheName),
                        installer);
            }
            case LINUX -> {
                var appImage = appImageDirectory.resolve(appName);
                var appDir = appImage.resolve("lib").resolve("app");
                yield new PackagingPaths(input, runtime, appImageDirectory, packageDirectory, appImage,
                        appImage.resolve("bin").resolve(appName),
                        appDir.resolve(appName + ".cfg"),
                        appDir.resolve(aotCacheName),
                        installer);
            }
            case WINDOWS -> {
                var appImage = appImageDirectory.resolve(appName);
                var appDir = appImage.resolve("app");
                yield new PackagingPaths(input, runtime, appImageDirectory, packageDirectory, appImage,
                        appImage.resolve(appName + ".exe"),
                        appDir.resolve(appName + ".cfg"),
                        appDir.resolve(aotCacheName),
                        installer);
            }
        };
    }
}
