package com.youngledo.jmcfx.domain.model;

/// Immutable data carrier for a hot method identified by profiling analysis.
///
/// @param method     fully qualified method signature
/// @param frameType  the frame categorization (e.g. JIT_COMPILED, INTERPRETED)
/// @param count      number of samples in this method
/// @param percentage percentage of total samples (0-100)
public record HotMethod(String method, String frameType, int count, double percentage) {
}
