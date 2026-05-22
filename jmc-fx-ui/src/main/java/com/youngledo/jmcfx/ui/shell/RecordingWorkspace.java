package com.youngledo.jmcfx.ui.shell;

import java.util.UUID;
import java.util.Objects;

import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.ui.events.EventBrowserViewModel;
import com.youngledo.jmcfx.ui.overview.OverviewViewModel;
import com.youngledo.jmcfx.ui.rules.RuleResultsViewModel;

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

    public RecordingWorkspace(RecordingSummary recording, OverviewViewModel overviewViewModel,
            EventBrowserViewModel eventBrowserViewModel, RuleResultsViewModel ruleResultsViewModel) {
        this.recording = Objects.requireNonNull(recording, "recording");
        this.overviewViewModel = Objects.requireNonNull(overviewViewModel, "overviewViewModel");
        this.eventBrowserViewModel = Objects.requireNonNull(eventBrowserViewModel, "eventBrowserViewModel");
        this.ruleResultsViewModel = Objects.requireNonNull(ruleResultsViewModel, "ruleResultsViewModel");
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

    public void close() {
        eventBrowserViewModel.close();
    }
}
