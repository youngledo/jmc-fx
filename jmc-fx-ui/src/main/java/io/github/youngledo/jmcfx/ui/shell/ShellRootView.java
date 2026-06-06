package io.github.youngledo.jmcfx.ui.shell;

import javafx.scene.Node;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

final class ShellRootView {

    final BorderPane root = new BorderPane();
    final AppSidebar sidebar = new AppSidebar();
    final TabPane recordingTabs = new TabPane();
    final ProgressBar progressBar = new ProgressBar(0);

    ShellRootView(StackPane workspaceStack) {
        configure(workspaceStack);
    }

    void configure(StackPane workspaceStack) {
        styles(root, "enterprise-shell", "app-shell");
        root.setLeft(sidebar);
        VBox workspaceShell = new VBox();
        styles(workspaceShell, "workspace-shell");
        styles(recordingTabs, "recording-tabs");
        styles(workspaceStack, "work-area");
        recordingTabs.setMinHeight(Region.USE_PREF_SIZE);
        VBox.setVgrow(workspaceStack, Priority.ALWAYS);
        workspaceShell.getChildren().setAll(recordingTabs, workspaceStack);
        root.setCenter(workspaceShell);

        HBox statusBar = new HBox(8, progressBar);
        styles(statusBar, "status-bar");
        progressBar.setPrefWidth(160);
        root.setBottom(statusBar);
    }

    private static void styles(Node node, String... styleClasses) {
        node.getStyleClass().addAll(styleClasses);
    }
}
