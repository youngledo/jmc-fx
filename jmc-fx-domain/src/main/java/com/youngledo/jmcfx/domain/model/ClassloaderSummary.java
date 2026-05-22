package com.youngledo.jmcfx.domain.model;

public record ClassloaderSummary(
        String classloader,
        long loadedCount,
        long unloadedCount) {
}
