package io.github.youngledo.jmcfx.domain.model;

/// Grouping strategy for the exception histogram.
public enum ExceptionGrouping {
	/// Group by exception class name only.
	BY_CLASS,
	/// Group by exception message only.
	BY_MESSAGE,
	/// Group by class name and message combined.
	BY_CLASS_AND_MESSAGE
}
