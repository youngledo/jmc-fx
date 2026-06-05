package io.github.youngledo.jmcfx.ui.locks;

import io.github.youngledo.jmcfx.domain.model.LockHistogram;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TableView;

/// Narrow view handle for the JFR Locks tabbed page.
public record LocksPageView(
        Label titleLabel,
        Button groupByClassButton,
        Button groupByAddressButton,
        Button groupByThreadButton,
        Tab byClassTab,
        TableView<LockHistogram> byClassTable,
        Tab byAddressTab,
        TableView<LockHistogram> byAddressTable,
        Tab byThreadTab,
        TableView<LockHistogram> byThreadTable) {
}
