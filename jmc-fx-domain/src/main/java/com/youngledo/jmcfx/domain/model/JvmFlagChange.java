package com.youngledo.jmcfx.domain.model;

import java.time.Instant;

public record JvmFlagChange(
        Instant startTime,
        String flagName,
        String oldValue,
        String newValue,
        String origin) {
}
