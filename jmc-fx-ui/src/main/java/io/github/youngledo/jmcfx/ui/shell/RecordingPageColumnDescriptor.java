package io.github.youngledo.jmcfx.ui.shell;

record RecordingPageColumnDescriptor(String id, String titleKey, double defaultWidth, boolean numeric) {

    RecordingPageColumnDescriptor {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (titleKey == null || titleKey.isBlank()) {
            throw new IllegalArgumentException("titleKey must not be blank");
        }
        if (defaultWidth <= 0) {
            throw new IllegalArgumentException("defaultWidth must be positive");
        }
    }
}
