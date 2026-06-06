package io.github.youngledo.jmcfx.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class HeapDumpBrowsingModelTest {

    @Test
    void browseWindowCopiesRowsAndRequiresPositiveLimit() {
        List<HeapDumpObjectGroup> rows = new ArrayList<>();
        rows.add(sampleGroup("java.lang.String", 42, 1024, 4096));

        HeapDumpBrowseWindow<HeapDumpObjectGroup> window =
                new HeapDumpBrowseWindow<>(rows, 0, 50, 1, false);
        rows.clear();

        assertEquals(1, window.rows().size());
        assertEquals("java.lang.String", window.rows().getFirst().label());
        assertThrows(IllegalArgumentException.class,
                () -> new HeapDumpBrowseWindow<>(List.of(), 0, 0, 0, false));
    }

    @Test
    void browseRequestBoundsPagingAndSearchText() {
        HeapDumpBrowseRequest request = new HeapDumpBrowseRequest(Path.of("demo.hprof"),
                HeapDumpObjectGroupKind.CLASS, HeapDumpBrowseSort.RETAINED_SIZE_BYTES, false,
                25, 100, " String ");

        assertEquals(Path.of("demo.hprof"), request.path());
        assertEquals(25, request.offset());
        assertEquals(100, request.limit());
        assertEquals("String", request.searchText());
        assertThrows(IllegalArgumentException.class,
                () -> new HeapDumpBrowseRequest(Path.of("demo.hprof"), HeapDumpObjectGroupKind.CLASS,
                        HeapDumpBrowseSort.RETAINED_SIZE_BYTES, false, -1, 100, ""));
    }

    @Test
    void referencePathCarriesTruncationState() {
        HeapDumpReferenceEdge edge = new HeapDumpReferenceEdge("root", "object-1", "field:value", "strong");
        HeapDumpReferencePath path = new HeapDumpReferencePath("object-1", List.of(edge), 4096, true);

        assertEquals("object-1", path.selectedObjectId());
        assertEquals(1, path.edges().size());
        assertTrue(path.truncated());
    }

    private static HeapDumpObjectGroup sampleGroup(String label, long count, long shallow, long retained) {
        return new HeapDumpObjectGroup("group-1", label, HeapDumpObjectGroupKind.CLASS,
                count, shallow, retained, 128, true);
    }
}
