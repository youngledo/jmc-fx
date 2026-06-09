package io.github.youngledo.jmcfx.application.ai;

import java.util.List;

public record RecordingAiContextSection(
        String id,
        String title,
        List<String> rows,
        boolean capped,
        int totalCount,
        int limit) {

    public RecordingAiContextSection {
        id = id == null ? "" : id;
        title = title == null ? "" : title;
        rows = List.copyOf(rows == null ? List.of() : rows);
        totalCount = Math.max(totalCount, rows.size());
        limit = Math.max(0, limit);
    }

    public boolean available() {
        return !rows.isEmpty();
    }
}
