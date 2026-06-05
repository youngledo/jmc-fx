package io.github.youngledo.jmcfx.ui.shell;

import java.util.Objects;

import javafx.application.Platform;
import javafx.scene.control.ProgressBar;

final class ShellBackgroundWorkController {

    private final ProgressBar progressBar;

    ShellBackgroundWorkController(ProgressBar progressBar) {
        this.progressBar = Objects.requireNonNull(progressBar, "progressBar");
    }

    void configure() {
        setVisible(false);
    }

    void setVisible(boolean visible) {
        progressBar.setProgress(visible ? ProgressBar.INDETERMINATE_PROGRESS : 0);
        progressBar.setVisible(visible);
        progressBar.setManaged(visible);
    }

    void onFxThread(Runnable runnable) {
        try {
            if (Platform.isFxApplicationThread()) {
                runnable.run();
            } else {
                Platform.runLater(runnable);
            }
        } catch (IllegalStateException exception) {
            runnable.run();
        }
    }
}
