package com.youngledo.jmcfx.domain.model;

public record RuleResult(
        String id,
        String name,
        Severity severity,
        int score,
        String topic,
        String summary,
        String explanation) {
}
