package io.github.youngledo.jmcfx.ui.shell;

import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.ui.advanced.AdvancedJfrViewModel;
import io.github.youngledo.jmcfx.ui.environment.EnvironmentViewModel;
import io.github.youngledo.jmcfx.ui.exceptions.ExceptionViewModel;
import io.github.youngledo.jmcfx.ui.fileio.FileIOViewModel;
import io.github.youngledo.jmcfx.ui.gc.G1GcViewModel;
import io.github.youngledo.jmcfx.ui.heap.HeapViewModel;
import io.github.youngledo.jmcfx.ui.i18n.I18n;
import io.github.youngledo.jmcfx.ui.javaapp.JavaAppOverviewViewModel;
import io.github.youngledo.jmcfx.ui.javaapp.NativeLibraryViewModel;
import io.github.youngledo.jmcfx.ui.javaapp.SecurityViewModel;
import io.github.youngledo.jmcfx.ui.javaapp.ThreadDumpViewModel;
import io.github.youngledo.jmcfx.ui.jfx.JavaFxEventsViewModel;
import io.github.youngledo.jmcfx.ui.jvm.ClassLoadingViewModel;
import io.github.youngledo.jmcfx.ui.jvm.CodeCacheViewModel;
import io.github.youngledo.jmcfx.ui.jvm.CompilationsViewModel;
import io.github.youngledo.jmcfx.ui.jvm.GcConfigViewModel;
import io.github.youngledo.jmcfx.ui.jvm.GcDetailsViewModel;
import io.github.youngledo.jmcfx.ui.jvm.GcSummaryViewModel;
import io.github.youngledo.jmcfx.ui.jvm.JvmInfoViewModel;
import io.github.youngledo.jmcfx.ui.jvm.VmOperationsViewModel;
import io.github.youngledo.jmcfx.ui.leaks.LeakSuspectsViewModel;
import io.github.youngledo.jmcfx.ui.locks.LockViewModel;
import io.github.youngledo.jmcfx.ui.metadata.JfrMetadataViewModel;
import io.github.youngledo.jmcfx.ui.profiling.ProfilingViewModel;
import io.github.youngledo.jmcfx.ui.socketio.SocketIOViewModel;
import io.github.youngledo.jmcfx.ui.threads.ThreadViewModel;
import io.github.youngledo.jmcfx.ui.tlab.TlabViewModel;

final class RecordingSectionLoader {

    private static final Logger LOGGER = LogManager.getLogger(RecordingSectionLoader.class);

    private final RecordingOpenExecutor recordingOpenExecutor;
    private final I18n i18n;
    private final Consumer<Boolean> backgroundWorkVisible;
    private final Consumer<String> taskSummary;
    private final Consumer<Runnable> fxThread;

    RecordingSectionLoader(RecordingOpenExecutor recordingOpenExecutor, I18n i18n,
            Consumer<Boolean> backgroundWorkVisible, Consumer<String> taskSummary, Consumer<Runnable> fxThread) {
        this.recordingOpenExecutor = recordingOpenExecutor;
        this.i18n = i18n;
        this.backgroundWorkVisible = backgroundWorkVisible;
        this.taskSummary = taskSummary;
        this.fxThread = fxThread;
    }

    void load(RecordingWorkspace workspace, String sectionId) {
        String canonicalSectionId = canonicalLoadSectionId(sectionId);
        if (workspace == null) {
            return;
        }
        if (canonicalSectionId == null) {
            workspace.cancelPendingSectionLoads();
            return;
        }
        if (!workspace.markSectionLoading(canonicalSectionId)) {
            return;
        }
        backgroundWorkVisible.accept(true);
        taskSummary.accept(i18n.format("taskSummary.preparingSection", displayNameForSection(sectionId)));
        recordingOpenExecutor.execute(() -> {
            if (!workspace.shouldLoadSection(canonicalSectionId)) {
                boolean stillLoading = workspace.markSectionLoadSkipped(canonicalSectionId);
                if (!stillLoading) {
                    fxThread.accept(() -> backgroundWorkVisible.accept(false));
                }
                return;
            }
            try {
                loadWorkspaceSectionNow(workspace, canonicalSectionId);
                boolean stillLoading = workspace.markSectionLoaded(canonicalSectionId);
                fxThread.accept(() -> taskSummary.accept(i18n.get("taskSummary.recordingReady")));
                if (!stillLoading) {
                    fxThread.accept(() -> backgroundWorkVisible.accept(false));
                }
            } catch (RuntimeException exception) {
                LOGGER.atError()
                        .withThrowable(exception)
                        .log("Unable to load recording section {} for {}",
                                canonicalSectionId, workspace.recording().path());
                boolean stillLoading = workspace.markSectionLoadFailed(canonicalSectionId);
                fxThread.accept(() -> taskSummary.accept(i18n.format("taskSummary.sectionFailed",
                        displayNameForSection(sectionId))));
                if (!stillLoading) {
                    fxThread.accept(() -> backgroundWorkVisible.accept(false));
                }
            }
        });
    }

    private String canonicalLoadSectionId(String sectionId) {
        return switch (sectionId) {
            case null -> null;
            case "home", "overview", "javaApplication", "jvmInternals", "environment", "jvms", "settings" -> null;
            case "envVars", "sysProps", "recordingInfo", "agents", "constantPools" -> "processes";
            default -> sectionId;
        };
    }

    private void loadWorkspaceSectionNow(RecordingWorkspace workspace, String sectionId) {
        RecordingSummary recording = workspace.recording();
        switch (sectionId) {
            case "analysis" -> workspace.ruleResultsViewModel().analyze(recording);
            case "events" -> workspace.eventBrowserViewModel().loadRecording(recording);
            case "metadata" -> loadIfPresent(workspace.jfrMetadataViewModel(), recording);
            case "advancedJfr" -> loadIfPresent(workspace.advancedJfrViewModel(), recording);
            case "profiling" -> loadIfPresent(workspace.profilingViewModel(), recording);
            case "exceptions" -> loadIfPresent(workspace.exceptionViewModel(), recording);
            case "threads" -> loadIfPresent(workspace.threadViewModel(), recording);
            case "fileio" -> loadIfPresent(workspace.fileIOViewModel(), recording);
            case "socketio" -> loadIfPresent(workspace.socketIOViewModel(), recording);
            case "locks" -> loadIfPresent(workspace.lockViewModel(), recording);
            case "threadHistogram" -> loadIfPresent(workspace.javaAppOverviewViewModel(), recording);
            case "security" -> loadIfPresent(workspace.securityViewModel(), recording);
            case "nativeLibraries" -> loadIfPresent(workspace.nativeLibraryViewModel(), recording);
            case "threadDumps" -> loadIfPresent(workspace.threadDumpViewModel(), recording);
            case "heap" -> loadIfPresent(workspace.heapViewModel(), recording);
            case "leaks" -> loadIfPresent(workspace.leakSuspectsViewModel(), recording);
            case "tlab" -> loadIfPresent(workspace.tlabViewModel(), recording);
            case "jvmInfo" -> loadIfPresent(workspace.jvmInfoViewModel(), recording);
            case "gcConfig" -> loadIfPresent(workspace.gcConfigViewModel(), recording);
            case "gcSummary" -> loadIfPresent(workspace.gcSummaryViewModel(), recording);
            case "gcDetails" -> loadIfPresent(workspace.gcDetailsViewModel(), recording);
            case "g1Gc" -> loadIfPresent(workspace.g1GcViewModel(), recording);
            case "javaFxEvents" -> loadIfPresent(workspace.javaFxEventsViewModel(), recording);
            case "compilations" -> loadIfPresent(workspace.compilationsViewModel(), recording);
            case "codeCache" -> loadIfPresent(workspace.codeCacheViewModel(), recording);
            case "classLoading" -> loadIfPresent(workspace.classLoadingViewModel(), recording);
            case "vmOperations" -> loadIfPresent(workspace.vmOperationsViewModel(), recording);
            case "processes", "envVars", "sysProps", "recordingInfo", "agents", "constantPools" ->
                    loadIfPresent(workspace.environmentViewModel(), recording);
            default -> {
            }
        }
    }

    private String displayNameForSection(String sectionId) {
        String key = "nav." + sectionId;
        String value = i18n.get(key);
        return value.equals(key) ? sectionId : value;
    }

    private void loadIfPresent(ProfilingViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }

    private void loadIfPresent(ExceptionViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }

    private void loadIfPresent(ThreadViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }

    private void loadIfPresent(FileIOViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }

    private void loadIfPresent(SocketIOViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }

    private void loadIfPresent(LockViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }

    private void loadIfPresent(JavaAppOverviewViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }

    private void loadIfPresent(SecurityViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }

    private void loadIfPresent(NativeLibraryViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }

    private void loadIfPresent(ThreadDumpViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }

    private void loadIfPresent(HeapViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }

    private void loadIfPresent(LeakSuspectsViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }

    private void loadIfPresent(TlabViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }

    private void loadIfPresent(JvmInfoViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }

    private void loadIfPresent(GcConfigViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }

    private void loadIfPresent(GcSummaryViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }

    private void loadIfPresent(GcDetailsViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }

    private void loadIfPresent(CompilationsViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }

    private void loadIfPresent(CodeCacheViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }

    private void loadIfPresent(ClassLoadingViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }

    private void loadIfPresent(VmOperationsViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }

    private void loadIfPresent(EnvironmentViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }

    private void loadIfPresent(JfrMetadataViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }

    private void loadIfPresent(G1GcViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }

    private void loadIfPresent(JavaFxEventsViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }

    private void loadIfPresent(AdvancedJfrViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }
}
