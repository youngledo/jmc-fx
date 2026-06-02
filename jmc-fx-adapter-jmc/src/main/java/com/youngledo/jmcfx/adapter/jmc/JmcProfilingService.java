package com.youngledo.jmcfx.adapter.jmc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.common.IMCMethod;
import org.openjdk.jmc.common.IMCPackage;
import org.openjdk.jmc.common.IMCType;
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

import com.youngledo.jmcfx.domain.model.DependencyGraphEdge;
import com.youngledo.jmcfx.domain.model.DependencyGraphReport;
import com.youngledo.jmcfx.domain.model.HotMethod;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.StackFrameInfo;
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
		return JmcResultLimiter.limitRows(methods);
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

	@Override
	public DependencyGraphReport loadPackageDependencies(RecordingSummary recording, int packageDepth) {
		int resolvedDepth = Math.max(1, packageDepth);
		IItemCollection events = loadEvents(recording);
		IItemCollection samples = events.apply(JdkFilters.EXECUTION_SAMPLE);
		if (!samples.hasItems()) {
			return new DependencyGraphReport(List.of(), 0, resolvedDepth);
		}

		StacktraceModel model = new StacktraceModel(false, FRAME_SEPARATOR, samples);
		Map<EdgeKey, Integer> counts = new HashMap<>();
		for (Branch branch : model.getRootFork().getBranches()) {
			collectDependencyEdges(branch, null, resolvedDepth, counts);
		}
		int totalTransitions = counts.values().stream().mapToInt(Integer::intValue).sum();
		if (totalTransitions == 0) {
			return new DependencyGraphReport(List.of(), 0, resolvedDepth);
		}
		List<DependencyGraphEdge> edges = counts.entrySet().stream()
				.map(entry -> new DependencyGraphEdge(entry.getKey().source(), entry.getKey().target(),
						entry.getValue(), (entry.getValue() * 100.0) / totalTransitions))
				.sorted(Comparator.comparingInt(DependencyGraphEdge::count).reversed()
						.thenComparing(DependencyGraphEdge::source)
						.thenComparing(DependencyGraphEdge::target))
				.toList();
		return new DependencyGraphReport(JmcResultLimiter.limitRows(edges), totalTransitions, resolvedDepth);
	}

	private void collectDependencyEdges(
			Branch branch,
			String parentPackage,
			int packageDepth,
			Map<EdgeKey, Integer> counts) {
		String currentPackage = packageLabel(formatFrame(branch.getFirstFrame()), packageDepth);
		int count = branch.getFirstFrame().getItemCount();
		if (parentPackage != null && !parentPackage.equals(currentPackage) && count > 0) {
			counts.merge(new EdgeKey(parentPackage, currentPackage), count, Integer::sum);
		}
		for (Branch child : branch.getEndFork().getBranches()) {
			collectDependencyEdges(child, currentPackage, packageDepth, counts);
		}
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
			children.add(new StackTreeNode(branchMethod, branchCount, pct, frameInfo(branch.getFirstFrame()),
					subTree.children()));
		}
		children.sort(Comparator.comparingInt(StackTreeNode::count).reversed());
		return new StackTreeNode("<root>", count, 100.0, StackFrameInfo.EMPTY, List.copyOf(children));
	}

	private static String formatFrame(StacktraceFrame frame) {
		return StacktraceFormatToolkit.formatFrame(frame.getFrame(), FRAME_SEPARATOR,
				false, false, true, true, true, false);
	}

	private static StackFrameInfo frameInfo(StacktraceFrame stacktraceFrame) {
		if (stacktraceFrame == null || stacktraceFrame.getFrame() == null) {
			return StackFrameInfo.EMPTY;
		}
		return frameInfo(stacktraceFrame.getFrame(), formatFrame(stacktraceFrame));
	}

	static StackFrameInfo frameInfo(IMCFrame frame, String formattedFrame) {
		if (frame == null) {
			return StackFrameInfo.EMPTY;
		}
		IMCMethod method = frame.getMethod();
		IMCType type = method == null ? null : method.getType();
		IMCPackage pkg = type == null ? null : type.getPackage();
		String methodName = firstPresent(method == null ? null : method.getMethodName(), formattedFrame);
		String typeName = firstPresent(type == null ? null : type.getFullName(), type == null ? null : type.getTypeName());
		String packageName = pkg == null ? packageFromTypeName(typeName) : firstPresent(pkg.getName(), packageFromTypeName(typeName));
		String label = readableFrameLabel(methodName, typeName, formattedFrame);
		String frameType = frame.getType() == null ? "" : frame.getType().getName();
		return new StackFrameInfo(label, methodName, packageName, typeName, frameType, frame.getBCI(), frame.getFrameLineNumber());
	}

	private static String readableFrameLabel(String methodName, String typeName, String fallback) {
		if (methodName != null && !methodName.isBlank()) {
			String typeLabel = simpleTypeName(typeName);
			return typeLabel.isBlank() ? methodName : typeLabel + "." + methodName;
		}
		return shortMethodLabel(fallback);
	}

	private static String shortMethodLabel(String formattedFrame) {
		if (formattedFrame == null || formattedFrame.isBlank()) {
			return "";
		}
		int parenIndex = formattedFrame.indexOf('(');
		String prefix = parenIndex >= 0 ? formattedFrame.substring(0, parenIndex) : formattedFrame;
		String suffix = parenIndex >= 0 ? formattedFrame.substring(parenIndex) : "";
		int methodSeparator = prefix.lastIndexOf('.');
		if (methodSeparator <= 0) {
			return formattedFrame;
		}
		int classSeparator = prefix.lastIndexOf('.', methodSeparator - 1);
		return classSeparator < 0 ? formattedFrame.substring(methodSeparator + 1) : prefix.substring(classSeparator + 1) + suffix;
	}

	private static String simpleTypeName(String typeName) {
		if (typeName == null || typeName.isBlank()) {
			return "";
		}
		int separator = typeName.lastIndexOf('.');
		return separator < 0 ? typeName : typeName.substring(separator + 1);
	}

	private static String packageFromTypeName(String typeName) {
		if (typeName == null || typeName.isBlank()) {
			return "";
		}
		int separator = typeName.lastIndexOf('.');
		return separator <= 0 ? "" : typeName.substring(0, separator);
	}

	private static String firstPresent(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return value;
			}
		}
		return "";
	}

	static String packageLabel(String formattedFrame, int packageDepth) {
		if (formattedFrame == null || formattedFrame.isBlank()) {
			return "<unknown>";
		}
		String methodQualifiedName = formattedFrame;
		int parenIndex = methodQualifiedName.indexOf('(');
		if (parenIndex >= 0) {
			methodQualifiedName = methodQualifiedName.substring(0, parenIndex);
		}
		int methodSeparator = methodQualifiedName.lastIndexOf('.');
		if (methodSeparator <= 0) {
			return "<default>";
		}
		String classQualifiedName = methodQualifiedName.substring(0, methodSeparator);
		int classSeparator = classQualifiedName.lastIndexOf('.');
		if (classSeparator <= 0) {
			return "<default>";
		}
		String packageName = classQualifiedName.substring(0, classSeparator);
		String[] parts = packageName.split("\\.");
		int depth = Math.min(Math.max(1, packageDepth), parts.length);
		return String.join(".", Arrays.copyOf(parts, depth));
	}

	private IItemCollection loadEvents(RecordingSummary recording) {
		return JmcRecordingDataCache.SHARED.events(recording);
	}

	private record EdgeKey(String source, String target) {
	}
}
