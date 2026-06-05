package io.github.youngledo.jmcfx.ui.shell;

import java.util.Objects;
import java.nio.file.Path;

import io.github.youngledo.jmcfx.application.HeapDumpApplicationServices;
import io.github.youngledo.jmcfx.application.LiveJvmApplicationServices;
import io.github.youngledo.jmcfx.application.RecordingApplicationServices;
import io.github.youngledo.jmcfx.ui.i18n.I18n;

/// Controller for the code-first application shell view.
///
/// The controller wires shell actions and bindings while feature behavior stays
/// in view models.
public class AppShellController {

    private final AppShellView view;
    private final ShellRuntimeController runtimeController;

    AppShellController(AppShellView view, AppShellViewModel viewModel, RecordingApplicationServices recordingServices,
            LiveJvmApplicationServices liveJvmServices, HeapDumpApplicationServices heapDumpServices, I18n i18n,
            RecordingOpenExecutor recordingOpenExecutor) {
        this.view = Objects.requireNonNull(view, "view");
        this.runtimeController = new ShellRuntimeController(view, viewModel, recordingServices, liveJvmServices,
                heapDumpServices, i18n, recordingOpenExecutor);
    }

    void initialize() {
        runtimeController.initialize();
    }

    public javafx.scene.layout.BorderPane root() {
        return view.root;
    }

    public void close() {
        runtimeController.close();
    }

    void openRecordingInBackground(Path path) {
        runtimeController.openRecordingInBackground(path);
    }

}
