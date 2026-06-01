package com.youngledo.jmcfx.ui.shell;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

final class HomePaneView {

    final VBox pane = new VBox();
    final Button openRecordingButton = new Button();
    final Button openHeapDumpButton = new Button();
    final Button connectJvmButton = new Button();
    final Label kickerLabel = new Label();
    final Label titleLabel = new Label();
    final Label subtitleLabel = new Label();
    final Label openWorkflowTitleLabel = new Label();
    final Label openWorkflowDescriptionLabel = new Label();
    final Label heapDumpWorkflowTitleLabel = new Label();
    final Label heapDumpWorkflowDescriptionLabel = new Label();
    final Label jvmWorkflowTitleLabel = new Label();
    final Label jvmWorkflowDescriptionLabel = new Label();
    final Label disclaimerLabel = new Label();
    final VBox jfrTile = new VBox();
    final VBox heapDumpTile = new VBox();
    final VBox jvmTile = new VBox();

    HomePaneView() {
        configure();
    }

    private void configure() {
        pane.setSpacing(18);
        styles(pane, "welcome-pane");
        styles(kickerLabel, "home-kicker");
        styles(titleLabel, "welcome-title");
        styles(subtitleLabel, "welcome-subtitle");
        wrap(subtitleLabel, openWorkflowDescriptionLabel, heapDumpWorkflowDescriptionLabel,
                jvmWorkflowDescriptionLabel, disclaimerLabel);
        styles(openWorkflowTitleLabel, "workflow-tile-title");
        styles(heapDumpWorkflowTitleLabel, "workflow-tile-title");
        styles(jvmWorkflowTitleLabel, "workflow-tile-title");
        styles(openWorkflowDescriptionLabel, "workflow-tile-copy");
        styles(heapDumpWorkflowDescriptionLabel, "workflow-tile-copy");
        styles(jvmWorkflowDescriptionLabel, "workflow-tile-copy");
        styles(disclaimerLabel, "legal-disclaimer");
        styles(jfrTile, "workflow-tile");
        styles(heapDumpTile, "workflow-tile");
        styles(jvmTile, "workflow-tile");
        jfrTile.setSpacing(6);
        heapDumpTile.setSpacing(6);
        jvmTile.setSpacing(6);
        HBox actions = hbox(8, openRecordingButton, openHeapDumpButton, connectJvmButton);
        styles(actions, "home-actions");
        VBox hero = vbox(10, kickerLabel, titleLabel, subtitleLabel, actions);
        styles(hero, "home-hero");
        jfrTile.getChildren().setAll(openWorkflowTitleLabel, openWorkflowDescriptionLabel);
        heapDumpTile.getChildren().setAll(heapDumpWorkflowTitleLabel, heapDumpWorkflowDescriptionLabel);
        jvmTile.getChildren().setAll(jvmWorkflowTitleLabel, jvmWorkflowDescriptionLabel);
        HBox tiles = hbox(12, jfrTile, heapDumpTile, jvmTile);
        styles(tiles, "workflow-tiles");
        HBox.setHgrow(jfrTile, Priority.ALWAYS);
        HBox.setHgrow(heapDumpTile, Priority.ALWAYS);
        HBox.setHgrow(jvmTile, Priority.ALWAYS);
        pane.getChildren().setAll(hero, tiles, disclaimerLabel);
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
