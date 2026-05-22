package com.youngledo.jmcfx.domain.model;

public record GcHeapConfiguration(
        long minSize,
        long maxSize,
        long initialSize,
        long objectAlignment,
        long addressSize,
        boolean useCompressedOops,
        String compressedOopsMode) {
}
