package com.youngledo.jmcfx.ui.shell;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.ui.events.EventBrowserViewModel;
import com.youngledo.jmcfx.ui.i18n.LanguageMode;
import com.youngledo.jmcfx.ui.overview.OverviewViewModel;
import com.youngledo.jmcfx.domain.service.EventQueryService;
import com.youngledo.jmcfx.ui.rules.RuleResultsViewModel;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/// View model for the primary application shell.
///
/// The shell owns navigation, the current recording context, and cross-view
/// operational status. Diagnostic behavior stays in feature-specific view
/// models.
public class AppShellViewModel {

    private static final String HOME_SECTION = "home";
    private static final String SETTINGS_SECTION = "settings";
    private static final String JVMS_SECTION = "jvms";
    private static final String DEFAULT_RECORDING_SECTION = "analysis";
    private static final Set<String> RECORDING_SECTIONS = Set.of("analysis", "overview", "events");

    private final ObservableList<RecordingWorkspace> recordingWorkspaces = FXCollections.observableArrayList();
    private final ObservableList<RecordingWorkspace> readOnlyRecordingWorkspaces =
            FXCollections.unmodifiableObservableList(recordingWorkspaces);
    private final ObjectProperty<RecordingWorkspace> selectedWorkspace = new SimpleObjectProperty<>();
    private final StringProperty selectedSection = new SimpleStringProperty("home");
    private final StringProperty statusMessage = new SimpleStringProperty("");
    private final StringProperty taskSummary = new SimpleStringProperty("");
    private final StringProperty currentRecordingName = new SimpleStringProperty("");
    private final BooleanProperty recordingOpen = new SimpleBooleanProperty(false);
    private final ObjectProperty<LanguageMode> languageMode = new SimpleObjectProperty<>(LanguageMode.SYSTEM);

    public StringProperty selectedSectionProperty() {
        return selectedSection;
    }

    public StringProperty statusMessageProperty() {
        return statusMessage;
    }

    public StringProperty taskSummaryProperty() {
        return taskSummary;
    }

    public StringProperty currentRecordingNameProperty() {
        return currentRecordingName;
    }

    public BooleanProperty recordingOpenProperty() {
        return recordingOpen;
    }

    public ObjectProperty<LanguageMode> languageModeProperty() {
        return languageMode;
    }

    public void setLanguageMode(LanguageMode mode) {
        languageMode.set(mode == null ? LanguageMode.ENGLISH : mode);
    }

    public ObservableList<RecordingWorkspace> recordingWorkspacesProperty() {
        return readOnlyRecordingWorkspaces;
    }

    public ReadOnlyObjectProperty<RecordingWorkspace> selectedWorkspaceProperty() {
        return selectedWorkspace;
    }

    public void showSection(String sectionId) {
        if (!knownSection(sectionId)) {
            return;
        }
        RecordingWorkspace workspace = selectedWorkspace.get();
        if (workspace != null && isRecordingSection(sectionId)) {
            workspace.selectedSectionProperty().set(sectionId);
        }
        selectedSection.set(sectionId);
    }

    public RecordingWorkspace openRecording(RecordingSummary recording) {
        return openRecording(recording, new OverviewViewModel(),
                new EventBrowserViewModel(new UnavailableEventQueryService()),
                new RuleResultsViewModel(rec -> List.of()));
    }

    public RecordingWorkspace openRecording(RecordingSummary recording, OverviewViewModel overview,
            EventBrowserViewModel events, RuleResultsViewModel ruleResults) {
        Objects.requireNonNull(recording, "recording");
        Objects.requireNonNull(overview, "overview");
        Objects.requireNonNull(events, "events");
        Objects.requireNonNull(ruleResults, "ruleResults");
        RecordingWorkspace workspace = new RecordingWorkspace(recording, overview, events, ruleResults);
        workspace.selectedSectionProperty().set(DEFAULT_RECORDING_SECTION);
        recordingWorkspaces.add(workspace);
        selectWorkspace(workspace);
        selectedSection.set(DEFAULT_RECORDING_SECTION);
        statusMessage.set("");
        taskSummary.set("");
        return workspace;
    }

    public void selectWorkspace(RecordingWorkspace workspace) {
        if (workspace == null || !recordingWorkspaces.contains(workspace)) {
            return;
        }
        selectedWorkspace.set(workspace);
        recordingOpen.set(true);
        currentRecordingName.set(workspace.recording().name());
        if (isRecordingSection(selectedSection.get())) {
            selectedSection.set(workspace.selectedSectionProperty().get());
        }
    }

    public void closeWorkspace(RecordingWorkspace workspace) {
        if (workspace == null || !recordingWorkspaces.contains(workspace)) {
            return;
        }
        boolean active = workspace == selectedWorkspace.get();
        int closedIndex = recordingWorkspaces.indexOf(workspace);
        recordingWorkspaces.remove(workspace);
        workspace.close();
        if (!active) {
            return;
        }
        if (recordingWorkspaces.isEmpty()) {
            selectedWorkspace.set(null);
            recordingOpen.set(false);
            currentRecordingName.set("");
            selectedSection.set(HOME_SECTION);
            return;
        }
        int nextIndex = Math.min(closedIndex, recordingWorkspaces.size() - 1);
        selectWorkspace(recordingWorkspaces.get(nextIndex));
        if (isRecordingSection(selectedSection.get())) {
            selectedSection.set(selectedWorkspace.get().selectedSectionProperty().get());
        }
    }

    public void showStatus(String message) {
        statusMessage.set(Objects.requireNonNullElse(message, ""));
    }

    public void showTaskSummary(String message) {
        taskSummary.set(Objects.requireNonNullElse(message, ""));
    }

    private static boolean isRecordingSection(String sectionId) {
        return sectionId != null && RECORDING_SECTIONS.contains(sectionId);
    }

    private static boolean knownSection(String sectionId) {
        return HOME_SECTION.equals(sectionId)
                || SETTINGS_SECTION.equals(sectionId)
                || JVMS_SECTION.equals(sectionId)
                || isRecordingSection(sectionId);
    }

    private static final class UnavailableEventQueryService implements EventQueryService {
        @Override
        public com.youngledo.jmcfx.domain.service.EventQuerySession openSession(RecordingSummary recording) {
            throw new UnsupportedOperationException("Event browser service is not connected for this workspace.");
        }
    }
}
