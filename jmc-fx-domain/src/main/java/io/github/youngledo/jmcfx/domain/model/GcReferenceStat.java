package io.github.youngledo.jmcfx.domain.model;

public record GcReferenceStat(
        long gcId,
        String referenceType,
        long count) {
}
