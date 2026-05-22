package com.youngledo.jmcfx.domain.service;

import java.util.List;

import com.youngledo.jmcfx.domain.model.HotMethod;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.StackTreeNode;

/// Port for CPU/method profiling analysis on a flight recording.
public interface ProfilingService {

	/// Loads the hot methods ranked by sample count from the given recording.
	List<HotMethod> loadHotMethods(RecordingSummary recording);

	/// Loads a stack trace tree for the specified method.
	///
	/// @param recording the flight recording to analyze
	/// @param method    the target method signature
	/// @param callers   if true, show callers; if false, show callees
	/// @return the stack trace tree rooted at the target method, or
	///         {@link StackTreeNode#EMPTY} if not found
	StackTreeNode loadStackTraceTree(RecordingSummary recording, String method, boolean callers);
}
