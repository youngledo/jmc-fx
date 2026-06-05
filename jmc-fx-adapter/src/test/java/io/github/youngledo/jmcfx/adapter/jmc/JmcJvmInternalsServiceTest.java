package io.github.youngledo.jmcfx.adapter.jmc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import io.github.youngledo.jmcfx.domain.model.ChartDefinition;
import io.github.youngledo.jmcfx.domain.model.ChartXAxisType;
import io.github.youngledo.jmcfx.domain.model.ClassloaderSummary;
import io.github.youngledo.jmcfx.domain.model.CodeCacheSweep;
import io.github.youngledo.jmcfx.domain.model.CompilationEvent;
import io.github.youngledo.jmcfx.domain.model.GcConfiguration;
import io.github.youngledo.jmcfx.domain.model.GcEvent;
import io.github.youngledo.jmcfx.domain.model.GcHeapConfiguration;
import io.github.youngledo.jmcfx.domain.model.GcSummary;
import io.github.youngledo.jmcfx.domain.model.JvmFlag;
import io.github.youngledo.jmcfx.domain.model.JvmInfo;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.model.VmOperationEvent;
import io.github.youngledo.jmcfx.domain.model.VmOperationSummary;
import io.github.youngledo.jmcfx.domain.service.JvmInternalsService;

import jdk.jfr.Recording;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class JmcJvmInternalsServiceTest {

    private final JvmInternalsService service = new JmcJvmInternalsService();

    @Test
    void loadJvmInfo_returnsResult(@TempDir Path tempDir) throws Exception {
        RecordingSummary recording = createMinimalRecording(tempDir);
        JvmInfo info = service.loadJvmInfo(recording);
        assertNotNull(info);
        // JVM name and version are populated from jdk.JVMInformation events
        // which are present in recordings with sufficient duration
    }

    @Test
    void loadJvmFlags_returnsList(@TempDir Path tempDir) throws Exception {
        RecordingSummary recording = createMinimalRecording(tempDir);
        List<JvmFlag> flags = service.loadJvmFlags(recording);
        assertNotNull(flags);
    }

    @Test
    void loadGcConfiguration_returnsResult(@TempDir Path tempDir) throws Exception {
        RecordingSummary recording = createMinimalRecording(tempDir);
        GcConfiguration config = service.loadGcConfiguration(recording);
        assertNotNull(config);
    }

    @Test
    void loadGcHeapConfiguration_returnsResult(@TempDir Path tempDir) throws Exception {
        RecordingSummary recording = createMinimalRecording(tempDir);
        GcHeapConfiguration config = service.loadGcHeapConfiguration(recording);
        assertNotNull(config);
    }

    @Test
    void byteSizedJvmInternalsFieldsAreReadAsBytes() throws Exception {
        String source = Files.readString(
                Path.of("src/main/java/io/github/youngledo/jmcfx/adapter/jmc/JmcJvmInternalsService.java"),
                StandardCharsets.UTF_8);

        assertTrue(source.contains("readLongBytes(JdkAttributes.HEAP_MIN_SIZE, first)"));
        assertTrue(source.contains("readLongBytes(JdkAttributes.HEAP_MAX_SIZE, first)"));
        assertTrue(source.contains("readLongBytes(JdkAttributes.HEAP_INITIAL_SIZE, first)"));
        assertTrue(source.contains("readLongBytes(JdkAttributes.HEAP_OBJECT_ALIGNMENT, first)"));
        assertTrue(source.contains("readLong(JdkAttributes.HEAP_ADDRESS_SIZE, first)"));
        assertFalse(source.contains("readLongBytes(JdkAttributes.HEAP_ADDRESS_SIZE, first)"));
        assertTrue(source.contains("readLongBytes(JdkAttributes.COMPILER_CODE_SIZE, item)"));
        assertTrue(source.contains("readLongBytes(JdkAttributes.COMPILER_INLINED_SIZE, item)"));
        assertTrue(source.contains("readLongBytes(JdkAttributes.ANONYMOUS_CHUNK_SIZE, item)"));
        assertTrue(source.contains("readLongBytes(JdkAttributes.ANONYMOUS_BLOCK_SIZE, item)"));
    }

    @Test
    void loadGcSummaries_returnsList(@TempDir Path tempDir) throws Exception {
        RecordingSummary recording = createMinimalRecording(tempDir);
        List<GcSummary> summaries = service.loadGcSummaries(recording);
        assertNotNull(summaries);
    }

    @Test
    void loadGcEvents_returnsList(@TempDir Path tempDir) throws Exception {
        RecordingSummary recording = createMinimalRecording(tempDir);
        List<GcEvent> events = service.loadGcEvents(recording);
        assertNotNull(events);
    }

    @Test
    void loadCompilationEvents_returnsList(@TempDir Path tempDir) throws Exception {
        RecordingSummary recording = createMinimalRecording(tempDir);
        List<CompilationEvent> events = service.loadCompilationEvents(recording);
        assertNotNull(events);
    }

    @Test
    void loadCodeCacheSweeps_returnsList(@TempDir Path tempDir) throws Exception {
        RecordingSummary recording = createMinimalRecording(tempDir);
        List<CodeCacheSweep> sweeps = service.loadCodeCacheSweeps(recording);
        assertNotNull(sweeps);
    }

    @Test
    void loadClassloaderHistogram_returnsList(@TempDir Path tempDir) throws Exception {
        RecordingSummary recording = createMinimalRecording(tempDir);
        List<ClassloaderSummary> histogram = service.loadClassloaderHistogram(recording);
        assertNotNull(histogram);
    }

    @Test
    void loadVmOperationSummary_returnsList(@TempDir Path tempDir) throws Exception {
        RecordingSummary recording = createMinimalRecording(tempDir);
        List<VmOperationSummary> summary = service.loadVmOperationSummary(recording);
        assertNotNull(summary);
    }

    @Test
    void loadVmOperationEvents_returnsList(@TempDir Path tempDir) throws Exception {
        RecordingSummary recording = createMinimalRecording(tempDir);
        List<VmOperationEvent> events = service.loadVmOperationEvents(recording);
        assertNotNull(events);
    }

    @Test
    void loadGcHeapChart_returnsDefinition(@TempDir Path tempDir) throws Exception {
        RecordingSummary recording = createMinimalRecording(tempDir);
        ChartDefinition chart = service.loadGcHeapChart(recording);
        assertNotNull(chart);
    }

    @Test
    void loadCompilationDurationChart_returnsDefinition(@TempDir Path tempDir) throws Exception {
        RecordingSummary recording = createMinimalRecording(tempDir);
        ChartDefinition chart = service.loadCompilationDurationChart(recording);
        assertNotNull(chart);
    }

    @Test
    void loadClassLoadingChart_returnsDefinition(@TempDir Path tempDir) throws Exception {
        RecordingSummary recording = createMinimalRecording(tempDir);
        ChartDefinition chart = service.loadClassLoadingChart(recording);
        assertNotNull(chart);
    }

    @Test
    void loadCodeCacheEntriesChart_marksXAxisAsEpochSeconds(@TempDir Path tempDir) throws Exception {
        RecordingSummary recording = createMinimalRecording(tempDir);

        ChartDefinition chart = service.loadCodeCacheEntriesChart(recording);

        assertEquals(ChartXAxisType.EPOCH_SECONDS, chart.xAxisType());
    }

    @Test
    void loadCodeCacheSweepChart_marksXAxisAsEpochSeconds(@TempDir Path tempDir) throws Exception {
        RecordingSummary recording = createMinimalRecording(tempDir);

        ChartDefinition chart = service.loadCodeCacheSweepChart(recording);

        assertEquals(ChartXAxisType.EPOCH_SECONDS, chart.xAxisType());
    }

    private RecordingSummary createMinimalRecording(Path tempDir) throws Exception {
        try (Recording recording = new Recording()) {
            recording.start();
            Thread.sleep(100);
            recording.stop();
            Path file = tempDir.resolve("jvm-internals-test.jfr");
            recording.dump(file);
            return new RecordingSummary("test", file, "test",
                    Instant.now(), Instant.now(), 100, 2048);
        }
    }
}
