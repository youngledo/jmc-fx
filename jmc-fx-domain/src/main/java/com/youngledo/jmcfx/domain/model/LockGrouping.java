package com.youngledo.jmcfx.domain.model;

/// Grouping strategy for the lock instance histogram.
public enum LockGrouping {
    /// Group by monitor class name.
    BY_CLASS,
    /// Group by monitor address.
    BY_ADDRESS,
    /// Group by the thread that entered the monitor.
    BY_THREAD
}
