package com.youngledo.jmcfx.ui.shell;

import java.util.UUID;
import java.util.Objects;

import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.ui.events.EventBrowserViewModel;
import com.youngledo.jmcfx.ui.exceptions.ExceptionViewModel;
import com.youngledo.jmcfx.ui.fileio.FileIOViewModel;
import com.youngledo.jmcfx.ui.heap.HeapViewModel;
import com.youngledo.jmcfx.ui.leaks.LeakSuspectsViewModel;
import com.youngledo.jmcfx.ui.locks.LockViewModel;
import com.youngledo.jmcfx.ui.overview.OverviewViewModel;
import com.youngledo.jmcfx.ui.profiling.ProfilingViewModel;
import com.youngledo.jmcfx.ui.rules.RuleResultsViewModel;
import com.youngledo.jmcfx.ui.socketio.SocketIOViewModel;
import com.youngledo.jmcfx.ui.tlab.TlabViewModel;
import com.youngledo.jmcfx.ui.threads.ThreadViewModel;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/// UI-layer state for one opened recording workspace.
public final class RecordingWorkspace {

    private final String id = UUID.randomUUID().toString();
    private final RecordingSummary recording;
    private final StringProperty selectedSection = new SimpleStringProperty("analysis");
    private final OverviewViewModel overviewViewModel;
    private final EventBrowserViewModel eventBrowserViewModel;
    private final RuleResultsViewModel ruleResultsViewModel;
    private final ProfilingViewModel profilingViewModel;
    private final ExceptionViewModel exceptionViewModel;
    private final ThreadViewModel threadViewModel;
    private final FileIOViewModel fileIOViewModel;
    private final SocketIOViewModel socketIOViewModel;
    private final LockViewModel lockViewModel;
    private final HeapViewModel heapViewModel;
    private final LeakSuspectsViewModel leakSuspectsViewModel;
    private final TlabViewModel tlabViewModel;

    public RecordingWorkspace(RecordingSummary recording, OverviewViewModel overviewViewModel,
            EventBrowserViewModel eventBrowserViewModel, RuleResultsViewModel ruleResultsViewModel,
            ProfilingViewModel profilingViewModel, ExceptionViewModel exceptionViewModel,
            ThreadViewModel threadViewModel, FileIOViewModel fileIOViewModel,
            SocketIOViewModel socketIOViewModel, LockViewModel lockViewModel,
            HeapViewModel heapViewModel, LeakSuspectsViewModel leakSuspectsViewModel,
            TlabViewModel tlabViewModel) {
        this.recording = Objects.requireNonNull(recording, "recording");
        this.overviewViewModel = Objects.requireNonNull(overviewViewModel, "overviewViewModel");
        this.eventBrowserViewModel = Objects.requireNonNull(eventBrowserViewModel, "eventBrowserViewModel");
        this.ruleResultsViewModel = Objects.requireNonNull(ruleResultsViewModel, "ruleResultsViewModel");
        this.profilingViewModel = profilingViewModel;
        this.exceptionViewModel = exceptionViewModel;
        this.threadViewModel = threadViewModel;
        this.fileIOViewModel = fileIOViewModel;
        this.socketIOViewModel = socketIOViewModel;
        this.lockViewModel = lockViewModel;
        this.heapViewModel = heapViewModel;
        this.leakSuspectsViewModel = leakSuspectsViewModel;
        this.tlabViewModel = tlabViewModel;
    }

    public String id() {
        return id;
    }

    public RecordingSummary recording() {
        return recording;
    }

    public StringProperty selectedSectionProperty() {
        return selectedSection;
    }

    public OverviewViewModel overviewViewModel() {
        return overviewViewModel;
    }

    public EventBrowserViewModel eventBrowserViewModel() {
        return eventBrowserViewModel;
    }

    public RuleResultsViewModel ruleResultsViewModel() {
        return ruleResultsViewModel;
    }

    public ProfilingViewModel profilingViewModel() {
        return profilingViewModel;
    }

    public ExceptionViewModel exceptionViewModel() {
        return exceptionViewModel;
    }

    public ThreadViewModel threadViewModel() {
        return threadViewModel;
    }

    public FileIOViewModel fileIOViewModel() {
        return fileIOViewModel;
    }

    public SocketIOViewModel socketIOViewModel() {
        return socketIOViewModel;
    }

    public LockViewModel lockViewModel() {
        return lockViewModel;
    }

    public HeapViewModel heapViewModel() {
        return heapViewModel;
    }

    public LeakSuspectsViewModel leakSuspectsViewModel() {
        return leakSuspectsViewModel;
    }

    public TlabViewModel tlabViewModel() {
        return tlabViewModel;
    }

    public void close() {
        eventBrowserViewModel.close();
    }
}
