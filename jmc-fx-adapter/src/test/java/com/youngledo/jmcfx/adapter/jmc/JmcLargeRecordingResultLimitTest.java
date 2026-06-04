package com.youngledo.jmcfx.adapter.jmc;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.youngledo.jmcfx.domain.model.ChartDefinition;
import com.youngledo.jmcfx.domain.model.ChartSeries;
import com.youngledo.jmcfx.domain.model.ExceptionGrouping;
import com.youngledo.jmcfx.domain.model.LockGrouping;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.SocketIOGrouping;
import com.youngledo.jmcfx.domain.model.ThreadSummary;

class JmcLargeRecordingResultLimitTest {

    @Test
    void largeStartupRecordingKeepsSidebarResultsBounded() throws Exception {
        RecordingSummary recording = startupRecording();

        assertTimeoutPreemptively(Duration.ofSeconds(60), () -> {
            JmcFileIOService fileIO = new JmcFileIOService();
            assertRowsBounded("file histogram", fileIO.loadFileIOHistogram(recording));
            assertRowsBounded("file events", fileIO.loadFileIOEvents(recording));
            assertChartBounded("file timeline", fileIO.loadTimeline(recording));

            JmcSocketIOService socketIO = new JmcSocketIOService();
            assertRowsBounded("socket histogram", socketIO.loadSocketIOHistogram(recording, SocketIOGrouping.BY_HOST_AND_PORT));
            assertRowsBounded("socket events", socketIO.loadSocketIOEvents(recording));
            assertChartBounded("socket timeline", socketIO.loadTimeline(recording));

            JmcProfilingService profiling = new JmcProfilingService();
            assertRowsBounded("profiling hot methods", profiling.loadHotMethods(recording));

            JmcExceptionService exceptions = new JmcExceptionService();
            assertRowsBounded("exception histogram", exceptions.loadHistogram(recording, ExceptionGrouping.BY_CLASS_AND_MESSAGE));
            assertChartBounded("exception timeline", exceptions.loadTimeline(recording));

            JmcThreadService threads = new JmcThreadService();
            List<ThreadSummary> threadSummaries = threads.loadThreadSummaries(recording);
            assertRowsBounded("thread summaries", threadSummaries);
            assertTrue(threadSummaries.stream().allMatch(summary -> summary.activities().size() <= 2_000),
                    "thread activities must be capped before JavaFX receives them");

            JmcLockService locks = new JmcLockService();
            assertRowsBounded("locks by class", locks.loadLockHistogram(recording, LockGrouping.BY_CLASS));
            assertRowsBounded("locks by address", locks.loadLockHistogram(recording, LockGrouping.BY_ADDRESS));
            assertRowsBounded("locks by thread", locks.loadLockHistogram(recording, LockGrouping.BY_THREAD));

            JmcHeapService heap = new JmcHeapService();
            assertRowsBounded("heap histogram", heap.loadHeapClassHistogram(recording));
            assertChartBounded("heap timeline", heap.loadHeapUsageTimeline(recording));

            JmcTlabService tlab = new JmcTlabService();
            assertRowsBounded("tlab allocations", tlab.loadTlabAllocations(recording));
            assertChartBounded("tlab timeline", tlab.loadTlabAllocationTimeline(recording));

            JmcJavaAppService javaApp = new JmcJavaAppService();
            assertRowsBounded("thread histogram", javaApp.loadThreadHistogram(recording));
            assertChartBounded("java app overview", javaApp.loadOverviewChart(recording));
            assertRowsBounded("certificates", javaApp.loadCertificates(recording));
            assertRowsBounded("native libraries", javaApp.loadNativeLibraries(recording));
            assertRowsBounded("thread dumps", javaApp.loadThreadDumps(recording));

            JmcJvmInternalsService jvm = new JmcJvmInternalsService();
            assertChartBounded("gc heap chart", jvm.loadGcHeapChart(recording));
            assertChartBounded("gc metaspace chart", jvm.loadGcMetaspaceChart(recording));
            assertChartBounded("gc pause chart", jvm.loadGcPauseChart(recording));
            assertRowsBounded("gc events", jvm.loadGcEvents(recording));
            assertRowsBounded("gc reference stats", jvm.loadGcReferenceStats(recording));
            assertRowsBounded("gc heap summaries", jvm.loadGcHeapSummaries(recording));
            assertRowsBounded("compilation events", jvm.loadCompilationEvents(recording));
            assertRowsBounded("compilation failures", jvm.loadCompilationFailures(recording));
            assertChartBounded("compilation chart", jvm.loadCompilationDurationChart(recording));
            assertRowsBounded("code cache sweeps", jvm.loadCodeCacheSweeps(recording));
            assertRowsBounded("code cache statistics", jvm.loadCodeCacheStatistics(recording));
            assertChartBounded("code cache entries chart", jvm.loadCodeCacheEntriesChart(recording));
            assertChartBounded("code cache sweep chart", jvm.loadCodeCacheSweepChart(recording));
            assertRowsBounded("classload events", jvm.loadClassloadEvents(recording));
            assertRowsBounded("classloader statistics", jvm.loadClassloaderStatistics(recording));
            assertChartBounded("class loading chart", jvm.loadClassLoadingChart(recording));
            assertRowsBounded("vm operation events", jvm.loadVmOperationEvents(recording));
        });
    }

    private static void assertRowsBounded(String label, List<?> rows) {
        assertTrue(rows.size() <= JmcResultLimiter.MAX_EVENT_ROWS,
                () -> label + " returned " + rows.size() + " rows");
    }

    private static void assertChartBounded(String label, ChartDefinition chart) {
        for (ChartSeries series : chart.series()) {
            assertTrue(series.points().size() <= JmcResultLimiter.MAX_CHART_POINTS_PER_SERIES,
                    () -> label + "/" + series.name() + " returned " + series.points().size() + " points");
        }
    }

    private static RecordingSummary startupRecording() throws Exception {
        Path path = startupRecordingPath();
        assumeTrue(Files.isRegularFile(path), "startup.jfr is only used for local large-recording coverage");
        return new RecordingSummary("startup", path, "startup.jfr", Instant.EPOCH, Instant.EPOCH, 0, Files.size(path));
    }

    private static Path startupRecordingPath() {
        String configuredPath = System.getProperty("jmcfx.realJfr", "");
        if (!configuredPath.isBlank()) {
            return Path.of(configuredPath);
        }
        return firstExisting(List.of(
                Path.of("startup.jfr"),
                Path.of("..", "startup.jfr"),
                Path.of("..", "..", "startup.jfr")));
    }

    private static Path firstExisting(List<Path> candidates) {
        return candidates.stream()
                .filter(Files::isRegularFile)
                .findFirst()
                .orElse(candidates.getFirst());
    }
}
