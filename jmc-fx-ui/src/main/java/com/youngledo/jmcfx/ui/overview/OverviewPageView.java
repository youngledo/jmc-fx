package com.youngledo.jmcfx.ui.overview;

import javafx.scene.control.Label;

/// Narrow view boundary for the main recording Overview page.
public record OverviewPageView(
        Label titleLabel,
        Label recordingNameLabel,
        Label recordingDetailsLabel,
        Label analysisTitleLabel,
        Label analysisStatusLabel,
        Label jvmsTitleLabel,
        Label jvmStatusLabel) {
}
