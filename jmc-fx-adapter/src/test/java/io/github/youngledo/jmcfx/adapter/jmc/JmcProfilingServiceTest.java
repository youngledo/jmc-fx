package io.github.youngledo.jmcfx.adapter.jmc;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import io.github.youngledo.jmcfx.domain.model.DependencyGraphReport;
import io.github.youngledo.jmcfx.domain.model.HotMethod;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.model.StackFrameInfo;
import io.github.youngledo.jmcfx.domain.model.StackTreeNode;

import jdk.jfr.Recording;
import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.common.IMCMethod;
import org.openjdk.jmc.common.IMCPackage;
import org.openjdk.jmc.common.IMCType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class JmcProfilingServiceTest {

	private final JmcProfilingService service = new JmcProfilingService();

	@Test
	void loadHotMethods_returnsResults(@TempDir Path tempDir) throws Exception {
		Path jfrFile = createMinimalRecording(tempDir);
		RecordingSummary recording = new RecordingSummary("test", jfrFile, "test",
				Instant.now(), Instant.now(), 1000, 1024);
		List<HotMethod> methods = service.loadHotMethods(recording);
		assertNotNull(methods);
	}

	@Test
	void loadStackTraceTree_returnsNode(@TempDir Path tempDir) throws Exception {
		Path jfrFile = createMinimalRecording(tempDir);
		RecordingSummary recording = new RecordingSummary("test", jfrFile, "test",
				Instant.now(), Instant.now(), 1000, 1024);
		StackTreeNode tree = service.loadStackTraceTree(recording, "java.lang.Thread.run", true);
		assertNotNull(tree);
	}

	@Test
	void loadFlameGraphTreeReturnsRecordingLevelStackTree(@TempDir Path tempDir) throws Exception {
		Path jfrFile = createMinimalRecording(tempDir);
		RecordingSummary recording = new RecordingSummary("test", jfrFile, "test",
				Instant.now(), Instant.now(), 1000, 1024);

		assertNotNull(service.loadFlameGraphTree(recording, false));
		assertNotNull(service.loadFlameGraphTree(recording, true));
	}

	@Test
	void loadFlameGraphTreeUsesJmcStacktraceTreeModelDirections() throws Exception {
		RecordingSummary recording = localStartupRecording();

		StackTreeNode regular = service.loadFlameGraphTree(recording, false);
		StackTreeNode inverted = service.loadFlameGraphTree(recording, true);

		assertFalse(regular.children().isEmpty());
		assertFalse(inverted.children().isEmpty());
		assertTrue(regular.count() > 0);
		assertTrue(inverted.count() > 0);
		assertNotEquals(regular.children().getFirst().method(), inverted.children().getFirst().method());
	}

	@Test
	void loadMethodFlameGraphTreeUsesJmcMethodFilteredFullStacks() throws Exception {
		RecordingSummary recording = localStartupRecording();
		List<HotMethod> methods = service.loadHotMethods(recording);
		assumeTrue(!methods.isEmpty(), "startup.jfr must contain execution samples for method flame graph coverage");
		HotMethod selectedMethod = methods.getFirst();
		String method = selectedMethod.method();

		StackTreeNode regular = service.loadFlameGraphTree(recording, method, false);
		StackTreeNode inverted = service.loadFlameGraphTree(recording, method, true);

		assertFalse(regular.children().isEmpty());
		assertFalse(inverted.children().isEmpty());
		assertTrue(regular.count() > 0);
		assertTrue(inverted.count() > 0);
		assertEquals(selectedMethod.count(), regular.count());
		assertEquals(selectedMethod.count(), inverted.count());
		assertNotEquals(method, regular.children().getFirst().method());
	}

	@Test
	void loadPackageDependenciesReturnsReport(@TempDir Path tempDir) throws Exception {
		Path jfrFile = createMinimalRecording(tempDir);
		RecordingSummary recording = new RecordingSummary("test", jfrFile, "test",
				Instant.now(), Instant.now(), 1000, 1024);

		DependencyGraphReport report = service.loadPackageDependencies(recording, 2);

		assertNotNull(report);
		assertEquals(2, report.packageDepth());
		assertTrue(report.totalTransitions() >= 0);
	}

	@Test
	void loadPackageDependenciesClampsPackageDepth(@TempDir Path tempDir) throws Exception {
		Path jfrFile = createMinimalRecording(tempDir);
		RecordingSummary recording = new RecordingSummary("test", jfrFile, "test",
				Instant.now(), Instant.now(), 1000, 1024);

		DependencyGraphReport report = service.loadPackageDependencies(recording, 0);

		assertEquals(1, report.packageDepth());
	}

	@Test
	void packageLabelUsesRequestedPackageDepth() {
		assertEquals("java", JmcProfilingService.packageLabel("java.lang.Thread.run()", 1));
		assertEquals("java.lang", JmcProfilingService.packageLabel("java.lang.Thread.run()", 2));
		assertEquals("java.lang", JmcProfilingService.packageLabel("java.lang.Thread.run()", 8));
		assertEquals("<default>", JmcProfilingService.packageLabel("Thread.run()", 2));
		assertEquals("<unknown>", JmcProfilingService.packageLabel("", 2));
	}

	@Test
	void frameInfoExtractsJmcTooltipFields() {
		StackFrameInfo info = JmcProfilingService.frameInfo(
				new TestFrame(
						new TestMethod(new TestType("com.example.Worker", "Worker", new TestPackage("com.example")), "run"),
						IMCFrame.Type.JIT_COMPILED,
						42,
						128),
				"com.example.Worker.run()");

		assertEquals("Worker.run", info.label());
		assertEquals("run", info.methodName());
		assertEquals("com.example", info.packageName());
		assertEquals("com.example.Worker", info.typeName());
		assertEquals(IMCFrame.Type.JIT_COMPILED.getName(), info.frameType());
		assertEquals(42, info.bci());
		assertEquals(128, info.lineNumber());
	}

	private Path createMinimalRecording(Path tempDir) throws Exception {
		try (Recording recording = new Recording()) {
			recording.start();
			Thread.sleep(50);
			recording.stop();
			Path file = tempDir.resolve("profiling-test.jfr");
			recording.dump(file);
			return file;
		}
	}

	private RecordingSummary localStartupRecording() throws Exception {
		Path path = startupRecordingPath();
		assumeTrue(java.nio.file.Files.isRegularFile(path),
				"startup.jfr is only used for local profiling flame graph regression coverage");
		return new RecordingSummary("startup", path, "startup.jfr",
				Instant.EPOCH, Instant.EPOCH, 0, java.nio.file.Files.size(path));
	}

	private Path startupRecordingPath() {
		Path modulePath = Path.of("startup.jfr");
		if (java.nio.file.Files.isRegularFile(modulePath)) {
			return modulePath;
		}
		return Path.of("..", "startup.jfr");
	}

	private record TestFrame(IMCMethod method, IMCFrame.Type type, Integer bci, Integer lineNumber) implements IMCFrame {

		@Override
		public Integer getFrameLineNumber() {
			return lineNumber;
		}

		@Override
		public Integer getBCI() {
			return bci;
		}

		@Override
		public IMCMethod getMethod() {
			return method;
		}

		@Override
		public Type getType() {
			return type;
		}
	}

	private record TestMethod(IMCType type, String methodName) implements IMCMethod {

		@Override
		public IMCType getType() {
			return type;
		}

		@Override
		public String getMethodName() {
			return methodName;
		}

		@Override
		public String getFormalDescriptor() {
			return null;
		}

		@Override
		public Integer getModifier() {
			return null;
		}

		@Override
		public Boolean isNative() {
			return null;
		}

		@Override
		public Boolean isHidden() {
			return null;
		}
	}

	private record TestType(String fullName, String typeName, IMCPackage packageName) implements IMCType {

		@Override
		public String getTypeName() {
			return typeName;
		}

		@Override
		public IMCPackage getPackage() {
			return packageName;
		}

		@Override
		public String getFullName() {
			return fullName;
		}
	}

	private record TestPackage(String name) implements IMCPackage {

		@Override
		public String getName() {
			return name;
		}

		@Override
		public org.openjdk.jmc.common.IMCModule getModule() {
			return null;
		}

		@Override
		public Boolean isExported() {
			return null;
		}
	}
}
