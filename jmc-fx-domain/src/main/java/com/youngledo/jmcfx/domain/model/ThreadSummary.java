package com.youngledo.jmcfx.domain.model;

import java.util.List;

public record ThreadSummary(
        String threadName,
        long threadId,
        String threadGroup,
        boolean virtual,
        int sampleCount,
        long blockedDurationMillis,
        List<ThreadActivity> activities) {
}
