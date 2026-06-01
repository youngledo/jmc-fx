package com.youngledo.jmcfx.ui.overview;

import java.time.Instant;
import java.time.ZoneId;

import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.ui.i18n.I18n;
import com.youngledo.jmcfx.ui.util.DisplayFormats;

/// Controller for the main recording Overview page.
public final class OverviewPageController {

    private final OverviewPageView view;
    private final I18n i18n;
    private OverviewViewModel viewModel;

    public OverviewPageController(OverviewPageView view, I18n i18n) {
        this.view = view;
        this.i18n = i18n;
    }

    public void configure() {
        view.titleLabel().textProperty().bind(i18n.text("overview.title"));
        view.analysisTitleLabel().textProperty().bind(i18n.text("overview.card.analysis"));
        view.jvmsTitleLabel().textProperty().bind(i18n.text("overview.card.jvms"));
        bind(null);
    }

    public void bind(OverviewViewModel nextViewModel) {
        view.recordingNameLabel().textProperty().unbind();
        view.recordingDetailsLabel().textProperty().unbind();
        view.analysisStatusLabel().textProperty().unbind();
        view.jvmStatusLabel().textProperty().unbind();
        viewModel = nextViewModel;
        if (nextViewModel == null) {
            showEmptyState();
            return;
        }
        view.recordingNameLabel().textProperty().bind(nextViewModel.recordingNameProperty());
        view.recordingDetailsLabel().textProperty().bind(nextViewModel.recordingDetailsProperty());
        view.analysisStatusLabel().setText(i18n.get("overview.analysisUnavailable"));
        view.jvmStatusLabel().setText(i18n.get("overview.jvmUnavailable"));
    }

    public void refreshLocale() {
        OverviewViewModel currentViewModel = viewModel;
        if (currentViewModel == null) {
            showEmptyState();
        } else {
            RecordingSummary recording = currentViewModel.recordingProperty().get();
            if (recording != null) {
                currentViewModel.recordingDetailsProperty().set(formatRecordingDetails(recording));
            }
            view.analysisStatusLabel().setText(i18n.get("overview.analysisUnavailable"));
            view.jvmStatusLabel().setText(i18n.get("overview.jvmUnavailable"));
        }
    }

    public String formatRecordingDetails(RecordingSummary recording) {
        return i18n.format("overview.details.format",
                recording.path(),
                formatEventTime(recording.startTime()),
                formatEventTime(recording.endTime()),
                DisplayFormats.formatDuration(recording.durationMillis()),
                DisplayFormats.formatFileSize(recording.sizeBytes()));
    }

    private void showEmptyState() {
        view.recordingNameLabel().setText(i18n.get("overview.noRecording"));
        view.recordingDetailsLabel().setText(i18n.get("overview.openPrompt"));
        view.analysisStatusLabel().setText(i18n.get("overview.analysisUnavailable"));
        view.jvmStatusLabel().setText(i18n.get("overview.jvmUnavailable"));
    }

    private static String formatEventTime(Instant instant) {
        return DisplayFormats.formatTimestamp(instant, ZoneId.systemDefault());
    }
}
