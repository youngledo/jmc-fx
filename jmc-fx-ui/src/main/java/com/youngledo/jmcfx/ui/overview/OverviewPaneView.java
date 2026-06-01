package com.youngledo.jmcfx.ui.overview;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/// Code-first view for the main recording Overview page.
public final class OverviewPaneView {

    private final Label titleLabel = new Label();
    private final Label recordingNameLabel = new Label();
    private final Label recordingDetailsLabel = new Label();
    private final Label analysisTitleLabel = new Label();
    private final Label analysisStatusLabel = new Label();
    private final Label jvmsTitleLabel = new Label();
    private final Label jvmStatusLabel = new Label();

    public OverviewPaneView(VBox pane) {
        configure(pane);
    }

    public OverviewPageView view() {
        return new OverviewPageView(titleLabel, recordingNameLabel, recordingDetailsLabel,
                analysisTitleLabel, analysisStatusLabel, jvmsTitleLabel, jvmStatusLabel);
    }

    private void configure(VBox pane) {
        pane.setSpacing(12);
        styles(titleLabel, "view-title");
        styles(recordingNameLabel, "detail-title");
        styles(analysisTitleLabel, "detail-title");
        styles(jvmsTitleLabel, "detail-title");
        styles(analysisStatusLabel, "unavailable-state");
        styles(jvmStatusLabel, "unavailable-state");
        wrap(recordingDetailsLabel, analysisStatusLabel, jvmStatusLabel);
        VBox recording = vbox(6, recordingNameLabel, recordingDetailsLabel);
        styles(recording, "summary-panel");
        VBox analysis = vbox(6, analysisTitleLabel, analysisStatusLabel);
        styles(analysis, "summary-panel");
        VBox jvms = vbox(6, jvmsTitleLabel, jvmStatusLabel);
        styles(jvms, "summary-panel");
        HBox row = hbox(12, analysis, jvms);
        HBox.setHgrow(analysis, Priority.ALWAYS);
        HBox.setHgrow(jvms, Priority.ALWAYS);
        pane.getChildren().setAll(titleLabel, recording, row);
    }

    private static VBox vbox(double spacing, Node... children) {
        return new VBox(spacing, children);
    }

    private static HBox hbox(double spacing, Node... children) {
        return new HBox(spacing, children);
    }

    private static void wrap(Label... labels) {
        for (Label label : labels) {
            label.setWrapText(true);
        }
    }

    private static void styles(Node node, String... styleClasses) {
        node.getStyleClass().addAll(styleClasses);
    }
}
