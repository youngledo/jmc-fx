package io.github.youngledo.jmcfx.adapter.jmc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import io.github.youngledo.jmcfx.domain.model.HeapDumpBrowseRequest;
import io.github.youngledo.jmcfx.domain.model.HeapDumpBrowseSort;
import io.github.youngledo.jmcfx.domain.model.HeapDumpObjectGroupKind;
import io.github.youngledo.jmcfx.domain.model.HeapDumpReferenceDirection;
import io.github.youngledo.jmcfx.domain.model.HeapDumpReferencePathRequest;
import io.github.youngledo.jmcfx.domain.service.JmcFxException;

class JmcHeapDumpBrowsingServiceTest {

    private static final String ENABLE_INTEGRATION_PROPERTY = "jmcfx.hprof.integration";

    @Test
    void emptyFallbackKeepsRequestedWindowBoundsWhenBrowsingIsUnavailable() {
        JmcHeapDumpBrowsingService service = new JmcHeapDumpBrowsingService();
        var request = new HeapDumpBrowseRequest(Path.of("missing.hprof"), HeapDumpObjectGroupKind.CLASS_LOADER,
                HeapDumpBrowseSort.RETAINED_SIZE_BYTES, false, 100, 25, "");

        var groups = service.browseObjectGroups(request);

        assertEquals(100, groups.offset());
        assertEquals(25, groups.limit());
        assertEquals(0, groups.rows().size());
        assertEquals(0, groups.totalCount());
        assertTrue(groups.truncated());
    }

    @Test
    void classBrowsingWrapsInvalidHeapDumpFailures() {
        JmcHeapDumpBrowsingService service = new JmcHeapDumpBrowsingService();
        var request = new HeapDumpBrowseRequest(Path.of("missing.hprof"), HeapDumpObjectGroupKind.CLASS,
                HeapDumpBrowseSort.RETAINED_SIZE_BYTES, false, 0, 25, "");

        JmcFxException exception = assertThrows(JmcFxException.class, () -> service.browseObjectGroups(request));

        assertTrue(exception.getMessage().contains("Unable to browse heap dump"));
    }

    @Test
    void groupDetailFallbackReportsUnavailableBrowsingData() {
        JmcHeapDumpBrowsingService service = new JmcHeapDumpBrowsingService();
        var request = new HeapDumpBrowseRequest(Path.of("missing.hprof"), HeapDumpObjectGroupKind.CLASS_LOADER,
                HeapDumpBrowseSort.RETAINED_SIZE_BYTES, false, 0, 50, "");

        var detail = service.loadObjectGroupDetail(request, "loader-1");

        assertEquals("loader-1", detail.group().id());
        assertEquals(HeapDumpObjectGroupKind.CLASS_LOADER, detail.group().kind());
        assertTrue(detail.objects().truncated());
        assertTrue(detail.note().contains("not available"));
    }

    @Test
    void referencePathFallbackIsBoundedAndTruncated() {
        JmcHeapDumpBrowsingService service = new JmcHeapDumpBrowsingService();
        var request = new HeapDumpReferencePathRequest(Path.of("missing.hprof"), "object-1",
                HeapDumpReferenceDirection.INBOUND, 8, 10, 0, 10);

        var paths = service.loadReferencePaths(request);

        assertEquals(0, paths.offset());
        assertEquals(10, paths.limit());
        assertEquals(0, paths.rows().size());
        assertTrue(paths.truncated());
    }

    @Test
    void rejectsNullRequests() {
        JmcHeapDumpBrowsingService service = new JmcHeapDumpBrowsingService();

        assertThrows(NullPointerException.class, () -> service.browseObjectGroups(null));
        assertThrows(NullPointerException.class, () -> service.loadObjectGroupDetail(null, "group"));
        assertThrows(NullPointerException.class, () -> service.loadObjectGroupDetail(
                new HeapDumpBrowseRequest(Path.of("missing.hprof"), HeapDumpObjectGroupKind.CLASS,
                        HeapDumpBrowseSort.RETAINED_SIZE_BYTES, false, 0, 50, ""),
                null));
        assertThrows(NullPointerException.class, () -> service.loadReferencePaths(null));
    }

    @Test
    void generatedHeapDumpProducesClassObjectGroupsAndBoundedDetails() throws Exception {
        assumeTrue(Boolean.getBoolean(ENABLE_INTEGRATION_PROPERTY),
                "set -D" + ENABLE_INTEGRATION_PROPERTY + "=true to dump and browse a real HPROF");
        Path dump = Files.createTempFile("jmcfx-heap-browsing-", ".hprof");
        Files.deleteIfExists(dump);
        try {
            dumpHeap(dump);

            JmcHeapDumpBrowsingService service = new JmcHeapDumpBrowsingService();
            var request = new HeapDumpBrowseRequest(dump, HeapDumpObjectGroupKind.CLASS,
                    HeapDumpBrowseSort.LABEL, true, 0, 200, "");

            var groups = service.browseObjectGroups(request);

            assertTrue(groups.totalCount() > 0);
            assertTrue(groups.rows().size() <= request.limit());
            assertTrue(groups.totalCount() >= groups.rows().size());
            var objectGroup = groups.rows().stream()
                    .filter(group -> group.objectCount() > 0)
                    .findFirst()
                    .orElseThrow();
            assertTrue(objectGroup.shallowSizeBytes() > 0);

            var detail = service.loadObjectGroupDetail(request, objectGroup.id());

            assertEquals(objectGroup.id(), detail.group().id());
            assertTrue(detail.objects().rows().size() <= request.limit());
            assertTrue(detail.objects().rows().stream().anyMatch(object -> object.shallowSizeBytes() > 0));
        } finally {
            Files.deleteIfExists(dump);
        }
    }

    private static void dumpHeap(Path target) throws Exception {
        Class<?> diagnosticType = Class.forName("com.sun.management.HotSpotDiagnosticMXBean");
        Object diagnostic = ManagementFactory.getPlatformMXBean(diagnosticType.asSubclass(java.lang.management.PlatformManagedObject.class));
        Method dumpHeap = diagnosticType.getMethod("dumpHeap", String.class, boolean.class);
        dumpHeap.invoke(diagnostic, target.toString(), false);
    }
}
