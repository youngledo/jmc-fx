package com.youngledo.jmcfx.domain.model;

import java.time.Instant;

/// Immutable data carrier for one jdk.NativeLibrary event.
///
/// @param startTime   the event timestamp
/// @param name        the library name
/// @param basePath    the base path or directory of the library, or empty string
/// @param absolutePath the absolute path to the library file, or empty string
public record NativeLibraryEntry(
        Instant startTime,
        String name,
        String basePath,
        String absolutePath) {
}
