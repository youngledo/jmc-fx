package com.youngledo.jmcfx.domain.model;

public record GcReferenceStat(
        long gcId,
        String referenceType,
        long count) {
}
