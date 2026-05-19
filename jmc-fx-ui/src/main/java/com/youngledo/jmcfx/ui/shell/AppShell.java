package com.youngledo.jmcfx.ui.shell;

import javafx.beans.binding.StringBinding;
import javafx.scene.layout.BorderPane;

/// Loaded application shell plus lifecycle cleanup.
public record AppShell(BorderPane root, StringBinding titleBinding, AutoCloseable closeHandle) implements AutoCloseable {

    @Override
    public void close() throws Exception {
        closeHandle.close();
    }
}
