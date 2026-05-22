package com.youngledo.jmcfx.domain.model;

public record ExceptionSummary(
        String key,
        String className,
        String message,
        int count,
        double percentage) {
}
