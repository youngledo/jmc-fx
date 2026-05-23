package com.youngledo.jmcfx.ui.threads;

import java.util.List;

import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.ThreadSummary;
import com.youngledo.jmcfx.domain.service.ThreadService;
import com.youngledo.jmcfx.ui.util.FxDispatch;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/// View model for the v1 Thread Activity page.
///
/// Manages thread summary list and selected thread state for a recording.
public class ThreadViewModel {

    private final ThreadService threadService;
    private final ObservableList<ThreadSummary> threadSummaries = FXCollections.observableArrayList();
    private final ObjectProperty<ThreadSummary> selectedThread = new SimpleObjectProperty<>();

    public ThreadViewModel(ThreadService threadService) {
        this.threadService = threadService;
    }

    public ObservableList<ThreadSummary> threadSummariesProperty() {
        return threadSummaries;
    }

    public ObjectProperty<ThreadSummary> selectedThreadProperty() {
        return selectedThread;
    }

    /// Loads thread summaries for the given recording and clears the selection.
    ///
    /// @param recording the flight recording to analyze
    public void load(RecordingSummary recording) {
        List<ThreadSummary> summaries = threadService.loadThreadSummaries(recording);
        FxDispatch.run(() -> {
            threadSummaries.setAll(summaries);
            selectedThread.set(null);
        });
    }
}
