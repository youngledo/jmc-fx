package io.github.youngledo.jmcfx.ui.shell;

import java.util.List;

record RecordingPageDescriptor(String id, String groupId, String titleKey, RecordingPageTemplate template,
        double defaultSplitPosition, List<RecordingPageColumnDescriptor> defaultColumns) {

    RecordingPageDescriptor {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (groupId == null || groupId.isBlank()) {
            throw new IllegalArgumentException("groupId must not be blank");
        }
        if (titleKey == null || titleKey.isBlank()) {
            throw new IllegalArgumentException("titleKey must not be blank");
        }
        if (template == null) {
            throw new IllegalArgumentException("template must not be null");
        }
        if (defaultSplitPosition <= 0 || defaultSplitPosition >= 1) {
            throw new IllegalArgumentException("defaultSplitPosition must be between 0 and 1");
        }
        defaultColumns = List.copyOf(defaultColumns == null ? List.of() : defaultColumns);
    }
}
