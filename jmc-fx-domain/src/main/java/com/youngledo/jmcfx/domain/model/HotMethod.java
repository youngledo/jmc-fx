package com.youngledo.jmcfx.domain.model;

public record HotMethod(
        String method,
        String frameType,
        int count,
        double percentage) {
}
