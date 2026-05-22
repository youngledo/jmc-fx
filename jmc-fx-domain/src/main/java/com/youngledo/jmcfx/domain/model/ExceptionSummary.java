package com.youngledo.jmcfx.domain.model;

/// Immutable data carrier for one row in the exception histogram.
///
/// @param key        the grouping key (class name, message, or combined)
/// @param className  the thrown exception class name
/// @param message    the exception message, may be null
/// @param count      number of occurrences
/// @param percentage percentage of total exceptions (0-100)
public record ExceptionSummary(String key, String className, String message, int count, double percentage) {
}
