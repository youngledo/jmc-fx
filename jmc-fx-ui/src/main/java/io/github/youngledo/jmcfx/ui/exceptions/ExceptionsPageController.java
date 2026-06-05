package io.github.youngledo.jmcfx.ui.exceptions;

import java.time.ZoneId;
import java.util.List;

import io.github.youngledo.jmcfx.domain.model.ChartDefinition;
import io.github.youngledo.jmcfx.domain.model.ExceptionGrouping;
import io.github.youngledo.jmcfx.domain.model.ExceptionSummary;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.ui.i18n.I18n;
import io.github.youngledo.jmcfx.ui.recording.RecordingTimeRange;
import io.github.youngledo.jmcfx.ui.recording.RecordingTimeRangeChartBinding;
import io.github.youngledo.jmcfx.ui.recording.RecordingTimeRangeClearButtonBinding;
import io.github.youngledo.jmcfx.ui.util.DisplayFormats;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/// Controller for the JFR Exceptions data table and timeline page.
public final class ExceptionsPageController {

    private final ExceptionsPageView view;
    private final I18n i18n;
    private final ChangeListener<ChartDefinition> timelineListener;
    private StringBinding recordingContextBinding;
    private ExceptionViewModel viewModel;
    private RecordingTimeRangeChartBinding timelineSelectionBinding;
    private RecordingTimeRangeClearButtonBinding clearTimeRangeButtonBinding;

    public ExceptionsPageController(ExceptionsPageView view, I18n i18n) {
        this.view = view;
        this.i18n = i18n;
        timelineListener = (observable, oldValue, newValue) -> setTimelineData(newValue);
    }

    public void configure() {
        bindLocalizedText();
        configureTable();
        bind(null);
    }

    public TableView<ExceptionSummary> table() {
        return view.table();
    }

    public void bind(ExceptionViewModel nextViewModel) {
        bind(nextViewModel, null);
    }

    public void bind(ExceptionViewModel nextViewModel, ObjectProperty<RecordingTimeRange> sharedTimeRange) {
        ExceptionViewModel currentViewModel = viewModel;
        if (currentViewModel != null) {
            currentViewModel.timelineProperty().removeListener(timelineListener);
        }
        if (timelineSelectionBinding != null) {
            timelineSelectionBinding.close();
            timelineSelectionBinding = null;
        }
        if (clearTimeRangeButtonBinding != null) {
            clearTimeRangeButtonBinding.close();
            clearTimeRangeButtonBinding = null;
        }
        if (recordingContextBinding != null) {
            view.recordingContextLabel().textProperty().unbind();
            recordingContextBinding.dispose();
            recordingContextBinding = null;
        }
        view.recordingContextLabel().setText("");
        view.timelineChart().setData(null);
        view.table().setItems(FXCollections.emptyObservableList());
        viewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        view.table().setItems(nextViewModel.histogramProperty());
        nextViewModel.timelineProperty().addListener(timelineListener);
        timelineSelectionBinding = new RecordingTimeRangeChartBinding(view.timelineChart(), sharedTimeRange);
        clearTimeRangeButtonBinding = new RecordingTimeRangeClearButtonBinding(
                view.clearTimeRangeButton(), i18n, sharedTimeRange);
        setTimelineData(nextViewModel.timelineProperty().get());
        recordingContextBinding = Bindings.createStringBinding(
                () -> recordingContext(nextViewModel.currentRecordingProperty().get()),
                nextViewModel.currentRecordingProperty(),
                i18n.localeProperty());
        view.recordingContextLabel().textProperty().bind(recordingContextBinding);
    }

    private void setTimelineData(ChartDefinition definition) {
        if (timelineSelectionBinding != null) {
            timelineSelectionBinding.setData(definition);
            return;
        }
        view.timelineChart().setData(definition);
    }

    private void bindLocalizedText() {
        view.titleLabel().textProperty().bind(i18n.text("exceptions.title"));
        view.groupByClassButton().textProperty().bind(i18n.text("exceptions.grouping.byClass"));
        view.groupByMessageButton().textProperty().bind(i18n.text("exceptions.grouping.byMessage"));
        view.groupByClassAndMessageButton().textProperty().bind(i18n.text("exceptions.grouping.byClassAndMessage"));
    }

    private void configureTable() {
        view.table().setPlaceholder(localizedTablePlaceholder("exceptions.empty"));

        TableColumn<ExceptionSummary, String> keyCol = new TableColumn<>();
        keyCol.textProperty().bind(i18n.text("exceptions.column.key"));
        keyCol.setPrefWidth(620);
        keyCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().key()));

        TableColumn<ExceptionSummary, Number> countCol = new TableColumn<>();
        countCol.textProperty().bind(i18n.text("exceptions.column.count"));
        countCol.setPrefWidth(80);
        countCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleIntegerProperty(cell.getValue().count()));
        useFormattedIntegerCells(countCol);

        TableColumn<ExceptionSummary, String> pctCol = new TableColumn<>();
        pctCol.textProperty().bind(i18n.text("exceptions.column.percentage"));
        pctCol.setPrefWidth(80);
        pctCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatPercent(cell.getValue().percentage())));

        view.table().getColumns().setAll(List.of(keyCol, countCol, pctCol));

        view.groupByClassButton().setOnAction(event -> setExceptionGrouping(ExceptionGrouping.BY_CLASS));
        view.groupByMessageButton().setOnAction(event -> setExceptionGrouping(ExceptionGrouping.BY_MESSAGE));
        view.groupByClassAndMessageButton().setOnAction(
                event -> setExceptionGrouping(ExceptionGrouping.BY_CLASS_AND_MESSAGE));
    }

    private void setExceptionGrouping(ExceptionGrouping grouping) {
        if (viewModel == null) {
            return;
        }
        viewModel.setGrouping(grouping);
    }

    private Label localizedTablePlaceholder(String key) {
        Label label = new Label();
        label.textProperty().bind(i18n.text(key));
        return label;
    }

    private String recordingContext(RecordingSummary recording) {
        if (recording == null) {
            return "";
        }
        ZoneId zone = ZoneId.systemDefault();
        String start = DisplayFormats.formatTimestamp(recording.startTime(), zone);
        String end = DisplayFormats.formatTimestamp(recording.endTime(), zone);
        String duration = DisplayFormats.formatDuration(recording.durationMillis());
        return i18n.format("exceptions.recordingContext", start, end, duration);
    }

    private static <T> void useFormattedIntegerCells(TableColumn<T, Number> column) {
        column.setCellFactory(ignored -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : DisplayFormats.formatInteger(item.longValue()));
            }
        });
    }
}
