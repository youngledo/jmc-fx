package io.github.youngledo.jmcfx.ui.gc;

import io.github.youngledo.jmcfx.domain.model.G1GcRegionState;
import io.github.youngledo.jmcfx.domain.model.G1GcRegionSummary;
import io.github.youngledo.jmcfx.domain.model.GcEvent;

import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;

/// Narrow view handle for the JFR G1 GC detail page.
public record G1GcPageView(
        Label titleLabel,
        Label summaryLabel,
        Label regionStatesLabel,
        TableView<G1GcRegionState> regionStatesTable,
        Label detailTitleLabel,
        TextArea detailArea,
        Label regionSummaryLabel,
        TableView<G1GcRegionSummary> regionSummaryTable,
        Label pausesLabel,
        TableView<GcEvent> pauseTable) {
}
