package com.youngledo.jmcfx.adapter.jmc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator.FrameCategorization;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceFormatToolkit;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceModel;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceModel.Branch;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceModel.Fork;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceFrame;

import com.youngledo.jmcfx.domain.model.HotMethod;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.StackTreeNode;
import com.youngledo.jmcfx.domain.service.JmcFxException;
import com.youngledo.jmcfx.domain.service.ProfilingService;

/// JMC-backed profiling adapter.
///
/// Uses {@link StacktraceModel} with {@link JdkFilters#EXECUTION_SAMPLE} to
/// extract hot methods and build stack trace trees from flight recordings.
public class JmcProfilingService implements ProfilingService {

	private static final FrameSeparator FRAME_SEPARATOR =
			new FrameSeparator(FrameCategorization.METHOD, false);

	@Override
	public List<HotMethod> loadHotMethods(RecordingSummary recording) {
		IItemCollection events = loadEvents(recording);
		IItemCollection samples = events.apply(JdkFilters.EXECUTION_SAMPLE);
		if (!samples.hasItems()) {
			return List.of();
		}

		StacktraceModel model = new StacktraceModel(false, FRAME_SEPARATOR, samples);
		Fork root = model.getRootFork();
		int totalCount = root.getItemsInFork();
		if (totalCount == 0) {
			return List.of();
		}

		List<HotMethod> methods = new ArrayList<>();
		for (Branch branch : root.getBranches()) {
			StacktraceFrame frame = branch.getFirstFrame();
			String method = formatFrame(frame);
			int count = frame.getItemCount();
			double pct = (count * 100.0) / totalCount;
			String frameType = frame.getFrame().getType() != null
					? frame.getFrame().getType().getName() : "UNKNOWN";
			methods.add(new HotMethod(method, frameType, count, pct));
		}
		methods.sort(Comparator.comparingInt(HotMethod::count).reversed());
		return List.copyOf(methods);
	}

	@Override
	public StackTreeNode loadStackTraceTree(RecordingSummary recording, String method, boolean callers) {
		IItemCollection events = loadEvents(recording);
		IItemCollection samples = events.apply(JdkFilters.EXECUTION_SAMPLE);
		if (!samples.hasItems()) {
			return StackTreeNode.EMPTY;
		}

		StacktraceModel model = new StacktraceModel(callers, FRAME_SEPARATOR, samples);
		Fork root = model.getRootFork();
		int totalCount = root.getItemsInFork();
		if (totalCount == 0) {
			return StackTreeNode.EMPTY;
		}

		Fork targetFork = findFork(root, method);
		if (targetFork == null) {
			return StackTreeNode.EMPTY;
		}

		return buildTree(targetFork, targetFork.getItemsInFork());
	}

	private Fork findFork(Fork fork, String method) {
		for (Branch branch : fork.getBranches()) {
			String branchMethod = formatFrame(branch.getFirstFrame());
			if (branchMethod.equals(method)) {
				return branch.getEndFork();
			}
			Fork found = findFork(branch.getEndFork(), method);
			if (found != null) {
				return found;
			}
		}
		return null;
	}

	private StackTreeNode buildTree(Fork fork, int parentCount) {
		int count = fork.getItemsInFork();
		if (count == 0) {
			return new StackTreeNode("<empty>", 0, 0, List.of());
		}
		List<StackTreeNode> children = new ArrayList<>();
		for (Branch branch : fork.getBranches()) {
			String branchMethod = formatFrame(branch.getFirstFrame());
			int branchCount = branch.getFirstFrame().getItemCount();
			double pct = parentCount > 0 ? (branchCount * 100.0) / parentCount : 0;
			StackTreeNode subTree = buildTree(branch.getEndFork(), branchCount);
			children.add(new StackTreeNode(branchMethod, branchCount, pct,
					subTree.children()));
		}
		children.sort(Comparator.comparingInt(StackTreeNode::count).reversed());
		return new StackTreeNode("<root>", count, 100.0, List.copyOf(children));
	}

	private static String formatFrame(StacktraceFrame frame) {
		return StacktraceFormatToolkit.formatFrame(frame.getFrame(), FRAME_SEPARATOR,
				false, false, true, true, true, false);
	}

	private IItemCollection loadEvents(RecordingSummary recording) {
		try {
			return JfrLoaderToolkit.loadEvents(recording.path().toFile());
		} catch (IOException | CouldNotLoadRecordingException e) {
			throw new JmcFxException("Unable to load recording for profiling: " + recording.path(), e);
		}
	}
}
