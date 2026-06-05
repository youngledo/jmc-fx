package io.github.youngledo.jmcfx.domain.model;

public record ProcessInfo(
        String pid,
        String commandLine,
        String startTime,
        String lastSample) {
}
