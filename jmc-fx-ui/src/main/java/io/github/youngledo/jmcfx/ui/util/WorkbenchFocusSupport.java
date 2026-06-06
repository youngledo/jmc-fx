package io.github.youngledo.jmcfx.ui.util;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public final class WorkbenchFocusSupport {

    private WorkbenchFocusSupport() {
    }

    public static void requestFocusWhenReady(Node node) {
        if (node == null || !node.isVisible() || !node.isManaged()) {
            return;
        }
        node.setFocusTraversable(true);
        if (Platform.isFxApplicationThread()) {
            node.requestFocus();
        } else {
            Platform.runLater(node::requestFocus);
        }
    }

    public static boolean isCommandShortcut(KeyEvent event, KeyCode code) {
        return event != null && event.isShortcutDown() && event.getCode() == code;
    }

    public static boolean shouldHandleNavigationShortcut(Node focusedNode, KeyEvent event) {
        return event != null
                && event.isShortcutDown()
                && !(focusedNode instanceof TextInputControl);
    }
}
