package io.github.youngledo.jmcfx.ui.heap;

import java.time.ZoneId;
import java.util.List;

import io.github.youngledo.jmcfx.domain.model.ChartDefinition;
import io.github.youngledo.jmcfx.domain.model.HeapClassHistogram;
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

/// Controller for the JFR Heap data table and timeline page.
public final class HeapPageController {

    private final HeapPageView view;
    private final I18n i18n;
    private final ChangeListener<ChartDefinition> timelineListener;
    private StringBinding recordingContextBinding;
    private HeapViewModel viewModel;
    private RecordingTimeRangeChartBinding timelineSelectionBinding;
    private RecordingTimeRangeClearButtonBinding clearTimeRangeButtonBinding;

    public HeapPageController(HeapPageView view, I18n i18n) {
        this.view = view;
        this.i18n = i18n;
        timelineListener = (observable, oldValue, newValue) -> setTimelineData(newValue);
    }

    public void configure() {
        bindLocalizedText();
        configureTable();
        bind(null);
    }

    public TableView<HeapClassHistogram> table() {
        return view.table();
    }

    public void bind(HeapViewModel nextViewModel) {
        bind(nextViewModel, null);
    }

    public void bind(HeapViewModel nextViewModel, ObjectProperty<RecordingTimeRange> sharedTimeRange) {
        HeapViewModel currentViewModel = viewModel;
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
        view.table().setItems(FXCollections.emptyObservableList());
        view.timelineChart().setData(null);
        viewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        view.table().setItems(nextViewModel.histogramProperty());
        view.table().getSelectionModel().selectFirst();
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
        view.titleLabel().textProperty().bind(i18n.text("heap.title"));
    }

    private void configureTable() {
        view.table().setPlaceholder(localizedTablePlaceholder("heap.empty"));

        TableColumn<HeapClassHistogram, String> classNameCol = new TableColumn<>();
        classNameCol.textProperty().bind(i18n.text("heap.column.className"));
        classNameCol.setPrefWidth(300);
        classNameCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().className()));

        TableColumn<HeapClassHistogram, Number> instancesCol = new TableColumn<>();
        instancesCol.textProperty().bind(i18n.text("heap.column.instances"));
        instancesCol.setPrefWidth(100);
        instancesCol.setCellValueFactory(
                cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().instances()));
        useFormattedIntegerCells(instancesCol);

        TableColumn<HeapClassHistogram, String> sizeCol = new TableColumn<>();
        sizeCol.textProperty().bind(i18n.text("heap.column.size"));
        sizeCol.setPrefWidth(100);
        sizeCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(cell.getValue().size())));

        TableColumn<HeapClassHistogram, String> pctCol = new TableColumn<>();
        pctCol.textProperty().bind(i18n.text("heap.column.allocationPct"));
        pctCol.setPrefWidth(120);
        pctCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatPercent(cell.getValue().allocationPct())));

        view.table().getColumns().setAll(List.of(classNameCol, instancesCol, sizeCol, pctCol));
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
        return i18n.format("heap.recordingContext", start, end, duration);
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
