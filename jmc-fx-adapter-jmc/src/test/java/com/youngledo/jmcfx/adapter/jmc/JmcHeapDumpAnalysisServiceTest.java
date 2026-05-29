package com.youngledo.jmcfx.adapter.jmc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import com.youngledo.jmcfx.domain.model.HeapDumpIssueCategory;
import com.youngledo.jmcfx.domain.service.JmcFxException;

import org.junit.jupiter.api.Test;

class JmcHeapDumpAnalysisServiceTest {

    @Test
    void parseOverallStatsExtractsFundamentals() {
        String text = """
                1. OVERALL STATS:

                Total num of objects: 1,234
                Instances: 1,000, object arrays: 100, primitive arrays: 134
                Total size of all objects: 2,048K

                3. NUMBER, SIZE AND NEAREST FIELDS FOR HIGH MEMORY CONSUMERS:

                  com.example.Root.cache -->
                1,024K: 15 instances of java.util.HashMap
                """;

        JmcHeapDumpAnalysisService.ParsedJOverflowText parsed =
                JmcHeapDumpAnalysisService.parseTextReport(Path.of("demo.hprof"), 4096, text);

        assertEquals(1_234, parsed.objectCount());
        assertEquals(1_000, parsed.instanceCount());
        assertEquals(100, parsed.objectArrayCount());
        assertEquals(134, parsed.primitiveArrayCount());
        assertEquals(2_048L * 1024L, parsed.totalObjectSizeBytes());
        assertFalse(parsed.issues().isEmpty());
    }

    @Test
    void parseTextReportClassifiesDuplicateStrings() {
        String text = """
                6. DUPLICATED STRINGS:

                512K: 20 duplicate strings reachable from com.example.Cache.values
                """;

        JmcHeapDumpAnalysisService.ParsedJOverflowText parsed =
                JmcHeapDumpAnalysisService.parseTextReport(Path.of("demo.hprof"), 4096, text);

        assertEquals(HeapDumpIssueCategory.DUPLICATE_STRING, parsed.issues().getFirst().category());
        assertEquals(512L * 1024L, parsed.issues().getFirst().wastedBytes());
        assertTrue(parsed.issues().getFirst().evidence().contains("duplicate strings"));
    }

    @Test
    void analyzeWrapsInvalidPathFailure() {
        JmcHeapDumpAnalysisService service = new JmcHeapDumpAnalysisService();

        JmcFxException exception = assertThrows(JmcFxException.class,
                () -> service.analyze(Path.of("missing-file.hprof")));

        assertTrue(exception.getMessage().contains("Unable to analyze heap dump"));
    }
}
