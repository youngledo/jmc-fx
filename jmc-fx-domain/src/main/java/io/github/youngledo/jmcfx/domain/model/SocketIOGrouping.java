package io.github.youngledo.jmcfx.domain.model;

/// Grouping strategy for the socket I/O histogram.
public enum SocketIOGrouping {
    /// Group by remote host address only.
    BY_HOST,
    /// Group by remote port only.
    BY_PORT,
    /// Group by host and port combined (host:port).
    BY_HOST_AND_PORT
}
