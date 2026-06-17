package io.github.youngledo.jmcfx.ui.overview;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/// Narrow view boundary for the main recording Overview page.
public record OverviewPageView(
        Label titleLabel,
        Label recordingNameLabel,
        Label recordingDetailsLabel,
        VBox liveOriginPane,
        Label liveOriginTitleLabel,
        Label liveOriginDetailsLabel,
        Label analysisTitleLabel,
        Label analysisStatusLabel,
        Label jvmsTitleLabel,
        Label jvmStatusLabel) {
}
