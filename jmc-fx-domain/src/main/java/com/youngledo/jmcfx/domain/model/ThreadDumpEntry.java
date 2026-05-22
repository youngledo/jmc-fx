package com.youngledo.jmcfx.domain.model;

import java.time.Instant;

/// Immutable data carrier for one jdk.ThreadDump event.
///
/// @param startTime  the event timestamp
/// @param dumpText   the raw jstack-style thread dump text
public record ThreadDumpEntry(
        Instant startTime,
        String dumpText) {
}
