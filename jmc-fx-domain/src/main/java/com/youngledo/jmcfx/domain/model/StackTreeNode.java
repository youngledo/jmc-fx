package com.youngledo.jmcfx.domain.model;

import java.util.List;

/// Immutable tree node representing a stack trace hierarchy.
///
/// @param method     method signature at this node
/// @param count      number of samples at this node
/// @param percentage percentage relative to parent (0-100)
/// @param children   child nodes, sorted by count descending
public record StackTreeNode(String method, int count, double percentage, List<StackTreeNode> children) {

	/// Sentinel for empty or unavailable stack trace data.
	public static final StackTreeNode EMPTY = new StackTreeNode("<empty>", 0, 0, List.of());
}
