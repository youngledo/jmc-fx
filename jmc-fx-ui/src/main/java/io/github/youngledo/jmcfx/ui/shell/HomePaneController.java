package io.github.youngledo.jmcfx.ui.shell;

import io.github.youngledo.jmcfx.ui.i18n.I18n;

import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2MZ;

/// Controller for Home page text, actions, and workflow tile behavior.
final class HomePaneController {

    private final HomePaneView view;
    private final I18n i18n;
    private final Runnable openRecording;
    private final Runnable openHeapDump;
    private final Runnable openLiveJvmWorkspace;

    HomePaneController(HomePaneView view, I18n i18n, Runnable openRecording, Runnable openHeapDump,
            Runnable openLiveJvmWorkspace) {
        this.view = view;
        this.i18n = i18n;
        this.openRecording = openRecording;
        this.openHeapDump = openHeapDump;
        this.openLiveJvmWorkspace = openLiveJvmWorkspace;
    }

    void configure() {
        bindLocalizedText();
        configureActionIcons();
        configureActions();
    }

    void setOpening(boolean opening) {
        view.openRecordingButton.setDisable(WorkspaceOpenCoordinator.shouldDisableOpenRecordingButton(opening));
        view.openHeapDumpButton.setDisable(opening);
    }

    private void bindLocalizedText() {
        view.kickerLabel.textProperty().bind(i18n.text("home.kicker"));
        view.titleLabel.textProperty().bind(i18n.text("home.title"));
        view.subtitleLabel.textProperty().bind(i18n.text("home.subtitle"));
        view.openRecordingButton.textProperty().bind(i18n.text("home.openRecording"));
        view.openHeapDumpButton.textProperty().bind(i18n.text("home.openHeapDump"));
        view.connectJvmButton.textProperty().bind(i18n.text("home.connectJvm"));
        view.openWorkflowTitleLabel.textProperty().bind(i18n.text("home.workflow.openTitle"));
        view.openWorkflowDescriptionLabel.textProperty().bind(i18n.text("home.workflow.openDescription"));
        view.heapDumpWorkflowTitleLabel.textProperty().bind(i18n.text("home.workflow.heapDumpTitle"));
        view.heapDumpWorkflowDescriptionLabel.textProperty().bind(i18n.text("home.workflow.heapDumpDescription"));
        view.jvmWorkflowTitleLabel.textProperty().bind(i18n.text("home.workflow.jvmTitle"));
        view.jvmWorkflowDescriptionLabel.textProperty().bind(i18n.text("home.workflow.jvmDescription"));
        view.disclaimerLabel.textProperty().bind(i18n.text("home.disclaimer"));
    }

    private void configureActionIcons() {
        view.openRecordingButton.getStyleClass().add("toolbar-primary");
        view.openHeapDumpButton.getStyleClass().add("toolbar-secondary");
        view.connectJvmButton.getStyleClass().add("toolbar-secondary");
        configureActionButton(view.openRecordingButton, Material2AL.FOLDER_OPEN, i18n.get("home.openRecording"));
        configureActionButton(view.openHeapDumpButton, Material2MZ.STORAGE, i18n.get("home.openHeapDump"));
        configureActionButton(view.connectJvmButton, Material2MZ.MEMORY, i18n.get("home.connectJvm"));
    }

    private void configureActions() {
        view.openRecordingButton.setOnAction(event -> openRecording.run());
        view.openHeapDumpButton.setOnAction(event -> openHeapDump.run());
        view.connectJvmButton.setOnAction(event -> openLiveJvmWorkspace.run());
        view.jfrTile.setOnMouseClicked(event -> openRecording.run());
        view.heapDumpTile.setOnMouseClicked(event -> openHeapDump.run());
        view.jvmTile.setOnMouseClicked(event -> openLiveJvmWorkspace.run());
    }

    private static void configureActionButton(Button button, Ikon icon, String accessibleText) {
        button.setGraphic(new FontIcon(icon));
        button.setContentDisplay(ContentDisplay.LEFT);
        button.setAccessibleText(accessibleText);
        button.setTooltip(new javafx.scene.control.Tooltip(accessibleText));
    }
}
