package io.github.youngledo.jmcfx.domain.model;

public record LeakCandidate(
        String object,
        int count,
        String description,
        String address,
        double relevance) {
}
