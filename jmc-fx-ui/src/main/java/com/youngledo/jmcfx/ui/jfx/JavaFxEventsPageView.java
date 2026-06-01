package com.youngledo.jmcfx.ui.jfx;

import com.youngledo.jmcfx.domain.model.JavaFxInputEvent;
import com.youngledo.jmcfx.domain.model.JavaFxPulsePhase;
import com.youngledo.jmcfx.domain.model.JavaFxPulseSummary;

import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;

/// Narrow view handle for the JFR JavaFX Events detail page.
public record JavaFxEventsPageView(
        Label titleLabel,
        Label summaryLabel,
        Label phaseLabel,
        TableView<JavaFxPulsePhase> phaseTable,
        Label detailTitleLabel,
        TextArea detailArea,
        Label pulseLabel,
        TableView<JavaFxPulseSummary> pulseTable,
        Label inputLabel,
        TableView<JavaFxInputEvent> inputTable) {
}
