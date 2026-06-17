package io.github.youngledo.jmcfx.ui.overview;

import java.time.Instant;
import java.time.ZoneId;

import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.ui.i18n.I18n;
import io.github.youngledo.jmcfx.ui.jvms.LiveFlightRecordingOrigin;
import io.github.youngledo.jmcfx.ui.util.DisplayFormats;

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
        view.liveOriginTitleLabel().textProperty().bind(i18n.text("overview.card.liveSource"));
        view.analysisTitleLabel().textProperty().bind(i18n.text("overview.card.analysis"));
        view.jvmsTitleLabel().textProperty().bind(i18n.text("overview.card.jvms"));
        bind(null);
    }

    public void bind(OverviewViewModel nextViewModel) {
        view.recordingNameLabel().textProperty().unbind();
        view.recordingDetailsLabel().textProperty().unbind();
        view.liveOriginDetailsLabel().textProperty().unbind();
        view.analysisStatusLabel().textProperty().unbind();
        view.jvmStatusLabel().textProperty().unbind();
        viewModel = nextViewModel;
        if (nextViewModel == null) {
            showEmptyState();
            return;
        }
        view.recordingNameLabel().textProperty().bind(nextViewModel.recordingNameProperty());
        view.recordingDetailsLabel().textProperty().bind(nextViewModel.recordingDetailsProperty());
        view.liveOriginDetailsLabel().textProperty().bind(nextViewModel.liveOriginDetailsProperty());
        view.liveOriginPane().visibleProperty().bind(nextViewModel.liveOriginVisibleProperty());
        view.liveOriginPane().managedProperty().bind(nextViewModel.liveOriginVisibleProperty());
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
            LiveFlightRecordingOrigin origin = currentViewModel.liveOriginProperty().get();
            currentViewModel.liveOriginDetailsProperty().set(origin == null ? "" : formatLiveOriginDetails(origin));
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

    public String formatLiveOriginDetails(LiveFlightRecordingOrigin origin) {
        return i18n.format("overview.liveSource.format",
                origin.displayName(),
                i18n.get("jvms.source." + origin.source().name().toLowerCase(java.util.Locale.ROOT)),
                blankFallback(origin.connectionUrl()),
                blankFallback(origin.pid()),
                blankFallback(origin.javaVersion()),
                origin.recordingName(),
                origin.recordingId());
    }

    private void showEmptyState() {
        view.liveOriginPane().visibleProperty().unbind();
        view.liveOriginPane().managedProperty().unbind();
        view.recordingNameLabel().setText(i18n.get("overview.noRecording"));
        view.recordingDetailsLabel().setText(i18n.get("overview.openPrompt"));
        view.liveOriginPane().setVisible(false);
        view.liveOriginPane().setManaged(false);
        view.liveOriginDetailsLabel().setText("");
        view.analysisStatusLabel().setText(i18n.get("overview.analysisUnavailable"));
        view.jvmStatusLabel().setText(i18n.get("overview.jvmUnavailable"));
    }

    private String blankFallback(String value) {
        return value == null || value.isBlank() ? i18n.get("overview.liveSource.unknown") : value;
    }

    private static String formatEventTime(Instant instant) {
        return DisplayFormats.formatTimestamp(instant, ZoneId.systemDefault());
    }
}
