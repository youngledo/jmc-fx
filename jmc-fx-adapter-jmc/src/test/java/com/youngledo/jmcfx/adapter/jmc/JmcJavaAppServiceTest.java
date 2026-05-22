package com.youngledo.jmcfx.adapter.jmc;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import com.youngledo.jmcfx.domain.model.NativeLibraryEntry;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.ThreadDumpEntry;
import com.youngledo.jmcfx.domain.model.ThreadHistogramRow;
import com.youngledo.jmcfx.domain.model.X509CertificateEntry;

import jdk.jfr.Recording;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class JmcJavaAppServiceTest {

    private final JmcJavaAppService service = new JmcJavaAppService();

    @Test
    void loadThreadHistogram_returnsResults(@TempDir Path tempDir) throws Exception {
        Path jfrFile = createMinimalRecording(tempDir);
        RecordingSummary recording = new RecordingSummary("test", jfrFile, "test",
                Instant.now(), Instant.now(), 1000, 1024);
        List<ThreadHistogramRow> histogram = service.loadThreadHistogram(recording);
        assertNotNull(histogram);
    }

    @Test
    void loadOverviewChart_returnsDefinition(@TempDir Path tempDir) throws Exception {
        Path jfrFile = createMinimalRecording(tempDir);
        RecordingSummary recording = new RecordingSummary("test", jfrFile, "test",
                Instant.now(), Instant.now(), 1000, 1024);
        var chart = service.loadOverviewChart(recording);
        assertNotNull(chart);
    }

    @Test
    void loadCertificates_returnsResults(@TempDir Path tempDir) throws Exception {
        Path jfrFile = createMinimalRecording(tempDir);
        RecordingSummary recording = new RecordingSummary("test", jfrFile, "test",
                Instant.now(), Instant.now(), 1000, 1024);
        List<X509CertificateEntry> certs = service.loadCertificates(recording);
        assertNotNull(certs);
    }

    @Test
    void loadNativeLibraries_returnsResults(@TempDir Path tempDir) throws Exception {
        Path jfrFile = createMinimalRecording(tempDir);
        RecordingSummary recording = new RecordingSummary("test", jfrFile, "test",
                Instant.now(), Instant.now(), 1000, 1024);
        List<NativeLibraryEntry> libs = service.loadNativeLibraries(recording);
        assertNotNull(libs);
    }

    @Test
    void loadThreadDumps_returnsResults(@TempDir Path tempDir) throws Exception {
        Path jfrFile = createMinimalRecording(tempDir);
        RecordingSummary recording = new RecordingSummary("test", jfrFile, "test",
                Instant.now(), Instant.now(), 1000, 1024);
        List<ThreadDumpEntry> dumps = service.loadThreadDumps(recording);
        assertNotNull(dumps);
    }

    private Path createMinimalRecording(Path tempDir) throws Exception {
        try (Recording recording = new Recording()) {
            recording.start();
            Thread.sleep(50);
            recording.stop();
            Path file = tempDir.resolve("javaapp-test.jfr");
            recording.dump(file);
            return file;
        }
    }
}
