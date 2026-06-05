package io.github.youngledo.jmcfx.ui.util;

record DataTableColumnState(String id, boolean visible, double width) {

    DataTableColumnState {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (width <= 0) {
            throw new IllegalArgumentException("width must be positive");
        }
    }
}
