package io.github.youngledo.jmcfx.ui.detail;

public record DetailSelection(String pageId, String selectionId, String title, String meta, String body) {

    public DetailSelection {
        if (pageId == null || pageId.isBlank()) {
            throw new IllegalArgumentException("pageId must not be blank");
        }
        if (selectionId == null || selectionId.isBlank()) {
            throw new IllegalArgumentException("selectionId must not be blank");
        }
        title = title == null ? "" : title;
        meta = meta == null ? "" : meta;
        body = body == null ? "" : body;
    }
}
