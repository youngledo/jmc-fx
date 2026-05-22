package com.youngledo.jmcfx.domain.model;

public record ThreadActivity(
        ThreadLaneType laneType,
        long startEpochMillis,
        long endEpochMillis,
        String detail) {
}
