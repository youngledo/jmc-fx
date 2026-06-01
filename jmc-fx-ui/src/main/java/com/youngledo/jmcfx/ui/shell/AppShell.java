package com.youngledo.jmcfx.ui.shell;

import javafx.beans.binding.StringBinding;
import javafx.scene.layout.BorderPane;

/// Loaded application shell plus lifecycle cleanup.
public record AppShell(BorderPane root, StringBinding titleBinding, AutoCloseable closeHandle,
        AppShellController controller) implements AutoCloseable {

    public AppShell(BorderPane root, StringBinding titleBinding, AutoCloseable closeHandle) {
        this(root, titleBinding, closeHandle, null);
    }

    @Override
    public void close() throws Exception {
        closeHandle.close();
    }
}
