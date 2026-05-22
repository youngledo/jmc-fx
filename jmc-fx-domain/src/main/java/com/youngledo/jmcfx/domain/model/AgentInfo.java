package com.youngledo.jmcfx.domain.model;

public record AgentInfo(
        String name,
        String options,
        String initTime,
        boolean dynamic,
        String kind) {
}
