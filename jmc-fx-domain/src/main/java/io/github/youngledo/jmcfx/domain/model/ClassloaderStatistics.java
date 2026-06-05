package io.github.youngledo.jmcfx.domain.model;

public record ClassloaderStatistics(
        String classloader,
        String parentClassloader,
        long loadedClassCount,
        long anonymousBlockChunkSize,
        long anonymousBlockSize,
        long anonymousClassCount) {
}
