package io.github.youngledo.jmcfx.ui.shell;

import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.github.youngledo.jmcfx.application.HeapDumpApplicationServices;
import io.github.youngledo.jmcfx.application.HeapDumpWorkspacePlan;
import io.github.youngledo.jmcfx.application.OpenHeapDumpWorkspaceUseCase;
import io.github.youngledo.jmcfx.domain.model.HeapDumpAnalysisState;
import io.github.youngledo.jmcfx.ui.heapdump.HeapDumpAnalysisViewModel;
import io.github.youngledo.jmcfx.ui.heapdump.VirtualThreadHeapDumpAnalysisExecutor;
import io.github.youngledo.jmcfx.ui.i18n.I18n;

import javafx.application.Platform;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class WorkspaceOpenCoordinator {

    private static final Logger LOGGER = LogManager.getLogger(WorkspaceOpenCoordinator.class);

    private final BorderPane root;
    private final AppShellViewModel viewModel;
    private final RecordingWorkspaceFactory recordingWorkspaceFactory;
    private final HeapDumpApplicationServices heapDumpServices;
    private final OpenHeapDumpWorkspaceUseCase openHeapDumpWorkspace;
    private final I18n i18n;
    private final RecordingOpenExecutor recordingOpenExecutor;
    private final BiConsumer<PreparedRecordingWorkspace, Long> recordingWorkspaceConsumer;
    private final Consumer<Boolean> recordingOpeningConsumer;
    private final Consumer<Boolean> backgroundWorkVisibleConsumer;
    private boolean recordingOpening;

    WorkspaceOpenCoordinator(BorderPane root, AppShellViewModel viewModel,
            RecordingWorkspaceFactory recordingWorkspaceFactory, HeapDumpApplicationServices heapDumpServices, I18n i18n,
            RecordingOpenExecutor recordingOpenExecutor,
            BiConsumer<PreparedRecordingWorkspace, Long> recordingWorkspaceConsumer,
            Consumer<Boolean> recordingOpeningConsumer,
            Consumer<Boolean> backgroundWorkVisibleConsumer) {
        this.root = root;
        this.viewModel = viewModel;
        this.recordingWorkspaceFactory = recordingWorkspaceFactory;
        this.heapDumpServices = heapDumpServices;
        openHeapDumpWorkspace = new OpenHeapDumpWorkspaceUseCase(heapDumpServices);
        this.i18n = i18n;
        this.recordingOpenExecutor = recordingOpenExecutor;
        this.recordingWorkspaceConsumer = recordingWorkspaceConsumer;
        this.recordingOpeningConsumer = recordingOpeningConsumer;
        this.backgroundWorkVisibleConsumer = backgroundWorkVisibleConsumer;
    }

    void openRecording() {
        if (recordingOpening) {
            return;
        }
        Platform.runLater(this::showOpenRecordingChooser);
    }

    void showOpenHeapDumpChooser() {
        if (heapDumpServices.heapDumpAnalysisService() == null || root == null || root.getScene() == null) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle(i18n.get("heapDump.fileChooser.title"));
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(i18n.get("heapDump.fileChooser.hprof"), "*.hprof"));
        java.io.File file = chooser.showOpenDialog(root.getScene().getWindow());
        if (file != null) {
            openHeapDumpInBackground(file.toPath());
        }
    }

    void openRecordingInBackground(Path path) {
        if (selectExistingRecordingWorkspace(path)) {
            return;
        }
        long openRequestGeneration = viewModel.nextOpenGeneration();
        setRecordingOpening(true);
        backgroundWorkVisibleConsumer.accept(true);
        viewModel.showStatus(openingRecordingStatus(i18n, path));
        viewModel.showTaskSummary(i18n.get("taskSummary.openingRecording"));
        recordingOpenExecutor.execute(() -> {
            try {
                PreparedRecordingWorkspace preparedWorkspace = prepareRecordingWorkspace(path);
                onFxThread(() -> {
                    recordingWorkspaceConsumer.accept(preparedWorkspace, openRequestGeneration);
                    if (!isSelectedRecording(path)) {
                        backgroundWorkVisibleConsumer.accept(false);
                    }
                    finishRecordingOpen();
                });
            } catch (RuntimeException exception) {
                LOGGER.atError()
                        .withThrowable(exception)
                        .log("Unable to open recording {}", path);
                onFxThread(() -> showOpenRecordingFailure(exception));
            }
        });
    }

    PreparedRecordingWorkspace prepareRecordingWorkspace(Path path) {
        return recordingWorkspaceFactory.prepare(path);
    }

    private void showOpenRecordingChooser() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(openRecordingChooserTitle(i18n));
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(jfrRecordingsFilterDescription(i18n), "*.jfr"));
        java.io.File file = chooser.showOpenDialog(root.getScene().getWindow());
        if (file == null) {
            return;
        }
        openRecordingInBackground(file.toPath());
    }

    private void openHeapDumpInBackground(Path path) {
        if (selectExistingHeapDumpWorkspace(path)) {
            return;
        }
        backgroundWorkVisibleConsumer.accept(true);
        viewModel.showStatus(openingHeapDumpStatus(i18n, path));
        viewModel.showTaskSummary(i18n.get("taskSummary.openingHeapDump"));
        HeapDumpWorkspacePlan plan = openHeapDumpWorkspace.open(path);
        HeapDumpAnalysisViewModel nextViewModel = new HeapDumpAnalysisViewModel(plan.analyzeHeapDump(),
                plan.browseObjectGroups(), plan.loadObjectGroupDetail(), plan.loadReferencePaths(),
                new VirtualThreadHeapDumpAnalysisExecutor(), i18n);
        HeapDumpWorkspace workspace = new HeapDumpWorkspace(plan.path(), nextViewModel);
        viewModel.openHeapDump(workspace);
        nextViewModel.stateProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != HeapDumpAnalysisState.ANALYZING) {
                backgroundWorkVisibleConsumer.accept(false);
                viewModel.showTaskSummary(nextViewModel.statusMessageProperty().get());
            }
        });
        nextViewModel.analyze(plan.path());
    }

    private boolean selectExistingRecordingWorkspace(Path path) {
        if (viewModel.selectRecordingWorkspaceByPath(path)) {
            viewModel.showStatus(i18n.format("status.openedRecording", path.getFileName()));
            viewModel.showTaskSummary("");
            return true;
        }
        return false;
    }

    private boolean selectExistingHeapDumpWorkspace(Path path) {
        if (viewModel.selectHeapDumpWorkspaceByPath(path)) {
            viewModel.showStatus(openingHeapDumpStatus(i18n, path));
            viewModel.showTaskSummary("");
            return true;
        }
        return false;
    }

    void finishRecordingOpen() {
        setRecordingOpening(false);
    }

    private boolean isSelectedRecording(Path path) {
        RecordingWorkspace workspace = viewModel.selectedWorkspaceProperty().get();
        return workspace != null && workspace.recording().path().toAbsolutePath().normalize()
                .equals(path.toAbsolutePath().normalize());
    }

    private void showOpenRecordingFailure(RuntimeException exception) {
        String message = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
        viewModel.showStatus(i18n.format("status.openRecordingFailed", message));
        viewModel.showTaskSummary("");
        backgroundWorkVisibleConsumer.accept(false);
        setRecordingOpening(false);
    }

    private void setRecordingOpening(boolean opening) {
        recordingOpening = opening;
        recordingOpeningConsumer.accept(opening);
    }

    private void onFxThread(Runnable runnable) {
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

    static String openRecordingChooserTitle(I18n i18n) {
        return i18n.get("fileChooser.openRecording.title");
    }

    static String jfrRecordingsFilterDescription(I18n i18n) {
        return i18n.get("fileChooser.jfrRecordings");
    }

    static String openingHeapDumpStatus(I18n i18n, Path path) {
        return i18n.format("status.openingHeapDump", path.getFileName());
    }

    static boolean shouldDisableOpenRecordingButton(boolean opening) {
        return opening;
    }

    static String openingRecordingStatus(I18n i18n, Path path) {
        return i18n.format("status.openingRecording", path.getFileName());
    }
}
