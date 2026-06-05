package io.github.youngledo.jmcfx.ui.jfx;

import java.time.ZoneId;
import java.util.List;

import io.github.youngledo.jmcfx.domain.model.JavaFxInputEvent;
import io.github.youngledo.jmcfx.domain.model.JavaFxPulsePhase;
import io.github.youngledo.jmcfx.domain.model.JavaFxPulseSummary;
import io.github.youngledo.jmcfx.ui.i18n.I18n;
import io.github.youngledo.jmcfx.ui.util.DisplayFormats;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/// Controller for the JFR JavaFX Events split table/detail page.
public final class JavaFxEventsPageController {

    private final JavaFxEventsPageView view;
    private final I18n i18n;
    private final ChangeListener<JavaFxPulsePhase> selectedPulsePhaseListener;
    private JavaFxEventsViewModel viewModel;

    public JavaFxEventsPageController(JavaFxEventsPageView view, I18n i18n) {
        this.view = view;
        this.i18n = i18n;
        selectedPulsePhaseListener = (observable, oldValue, newValue) ->
                view.phaseTable().getSelectionModel().select(newValue);
    }

    public void configure() {
        bindLocalizedText();
        configureTables();
        bind(null);
    }

    public List<TableView<?>> exportTables() {
        return List.of(view.phaseTable(), view.pulseTable(), view.inputTable());
    }

    public void bind(JavaFxEventsViewModel nextViewModel) {
        JavaFxEventsViewModel currentViewModel = viewModel;
        if (currentViewModel != null) {
            currentViewModel.selectedPulsePhaseProperty().removeListener(selectedPulsePhaseListener);
        }
        view.summaryLabel().textProperty().unbind();
        view.detailArea().textProperty().unbind();
        view.pulseTable().setItems(FXCollections.emptyObservableList());
        view.phaseTable().setItems(FXCollections.emptyObservableList());
        view.inputTable().setItems(FXCollections.emptyObservableList());
        view.phaseTable().getSelectionModel().clearSelection();
        view.summaryLabel().setText(i18n.get("javaFxEvents.summary"));
        view.detailArea().setText("");
        viewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        view.summaryLabel().textProperty().bind(nextViewModel.summaryProperty());
        view.detailArea().textProperty().bind(nextViewModel.selectedDetailProperty());
        view.pulseTable().setItems(nextViewModel.pulseSummariesProperty());
        view.phaseTable().setItems(nextViewModel.pulsePhasesProperty());
        view.inputTable().setItems(nextViewModel.inputEventsProperty());
        view.phaseTable().getSelectionModel().select(nextViewModel.selectedPulsePhaseProperty().get());
        nextViewModel.selectedPulsePhaseProperty().addListener(selectedPulsePhaseListener);
    }

    private void bindLocalizedText() {
        view.titleLabel().textProperty().bind(i18n.text("javaFxEvents.title"));
        view.phaseLabel().textProperty().bind(i18n.text("javaFxEvents.phases"));
        view.pulseLabel().textProperty().bind(i18n.text("javaFxEvents.pulses"));
        view.inputLabel().textProperty().bind(i18n.text("javaFxEvents.inputs"));
        view.detailTitleLabel().textProperty().bind(i18n.text("javaFxEvents.detail.title"));
    }

    private void configureTables() {
        view.phaseTable().setPlaceholder(localizedTablePlaceholder("javaFxEvents.empty"));
        TableColumn<JavaFxPulsePhase, String> pulseIdCol = localizedColumn("javaFxEvents.column.pulse");
        pulseIdCol.setPrefWidth(90);
        pulseIdCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatInteger(data.getValue().pulseId())));
        TableColumn<JavaFxPulsePhase, String> phaseNameCol = localizedColumn("javaFxEvents.column.phase");
        phaseNameCol.setPrefWidth(200);
        phaseNameCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().phaseName()));
        TableColumn<JavaFxPulsePhase, String> phaseDurationCol = localizedColumn("common.column.duration");
        phaseDurationCol.setPrefWidth(130);
        phaseDurationCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatMicros(data.getValue().durationMicros())));
        TableColumn<JavaFxPulsePhase, String> phaseThreadCol = localizedColumn("common.column.thread");
        phaseThreadCol.setPrefWidth(220);
        phaseThreadCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().threadName()));
        TableColumn<JavaFxPulsePhase, String> phaseTimeCol = localizedColumn("common.column.time");
        phaseTimeCol.setPrefWidth(220);
        phaseTimeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatTimestamp(data.getValue().startTime(), ZoneId.systemDefault())));
        view.phaseTable().getColumns().setAll(List.of(pulseIdCol, phaseNameCol, phaseDurationCol,
                phaseThreadCol, phaseTimeCol));
        view.phaseTable().getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    if (viewModel != null && newValue != viewModel.selectedPulsePhaseProperty().get()) {
                        viewModel.selectedPulsePhaseProperty().set(newValue);
                    }
                });

        view.pulseTable().setPlaceholder(localizedTablePlaceholder("javaFxEvents.empty"));
        TableColumn<JavaFxPulseSummary, String> summaryPulseIdCol = localizedColumn("javaFxEvents.column.pulse");
        summaryPulseIdCol.setPrefWidth(90);
        summaryPulseIdCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatInteger(data.getValue().pulseId())));
        TableColumn<JavaFxPulseSummary, String> phaseCountCol = localizedColumn("javaFxEvents.column.phases");
        phaseCountCol.setPrefWidth(110);
        phaseCountCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatInteger(data.getValue().phaseCount())));
        TableColumn<JavaFxPulseSummary, String> totalDurationCol = localizedColumn("javaFxEvents.column.totalDuration");
        totalDurationCol.setPrefWidth(140);
        totalDurationCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatMicros(data.getValue().totalDurationMicros())));
        TableColumn<JavaFxPulseSummary, String> maxDurationCol = localizedColumn("javaFxEvents.column.maxDuration");
        maxDurationCol.setPrefWidth(140);
        maxDurationCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatMicros(data.getValue().maxPhaseDurationMicros())));
        TableColumn<JavaFxPulseSummary, String> pulseTimeCol = localizedColumn("common.column.time");
        pulseTimeCol.setPrefWidth(220);
        pulseTimeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatTimestamp(data.getValue().startTime(), ZoneId.systemDefault())));
        view.pulseTable().getColumns().setAll(List.of(summaryPulseIdCol, phaseCountCol,
                totalDurationCol, maxDurationCol, pulseTimeCol));

        view.inputTable().setPlaceholder(localizedTablePlaceholder("javaFxEvents.inputs.empty"));
        TableColumn<JavaFxInputEvent, String> inputTypeCol = localizedColumn("javaFxEvents.column.input");
        inputTypeCol.setPrefWidth(180);
        inputTypeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().inputType()));
        TableColumn<JavaFxInputEvent, String> inputDurationCol = localizedColumn("common.column.duration");
        inputDurationCol.setPrefWidth(130);
        inputDurationCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatMicros(data.getValue().durationMicros())));
        TableColumn<JavaFxInputEvent, String> inputThreadCol = localizedColumn("common.column.thread");
        inputThreadCol.setPrefWidth(220);
        inputThreadCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().threadName()));
        TableColumn<JavaFxInputEvent, String> inputTimeCol = localizedColumn("common.column.time");
        inputTimeCol.setPrefWidth(220);
        inputTimeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatTimestamp(data.getValue().startTime(), ZoneId.systemDefault())));
        view.inputTable().getColumns().setAll(List.of(inputTypeCol, inputDurationCol, inputThreadCol,
                inputTimeCol));
    }

    private Label localizedTablePlaceholder(String key) {
        Label label = new Label();
        label.textProperty().bind(i18n.text(key));
        return label;
    }

    private <T> TableColumn<T, String> localizedColumn(String key) {
        TableColumn<T, String> column = new TableColumn<>();
        column.textProperty().bind(i18n.text(key));
        return column;
    }
}
