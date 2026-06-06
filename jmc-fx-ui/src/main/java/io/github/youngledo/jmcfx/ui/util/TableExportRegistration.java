package io.github.youngledo.jmcfx.ui.util;

import javafx.scene.control.TableView;

import java.util.function.Supplier;

public record TableExportRegistration(
        TableView<?> table,
        Supplier<TableExportRequest> requestSupplier) {
}
