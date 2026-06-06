package io.github.youngledo.jmcfx.domain.model;

import java.util.Objects;

public record HeapDumpObjectGroupDetail(
        HeapDumpObjectGroup group,
        HeapDumpBrowseWindow<HeapDumpObjectSummary> objects,
        String note) {

    public HeapDumpObjectGroupDetail {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(objects, "objects");
        note = note == null ? "" : note;
    }
}
