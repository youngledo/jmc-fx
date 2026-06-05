package io.github.youngledo.jmcfx.ui.tlab;

import java.time.ZoneId;
import java.util.List;

import io.github.youngledo.jmcfx.domain.model.ChartDefinition;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.model.TlabAllocation;
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
import javafx.scene.layout.Region;

/// Controller for the JFR TLAB data table and timeline page.
public final class TlabPageController {

    private final TlabPageView view;
    private final I18n i18n;
    private final ChangeListener<ChartDefinition> timelineListener;
    private final ChangeListener<Boolean> placeholderListener;
    private StringBinding recordingContextBinding;
    private TlabViewModel viewModel;
    private RecordingTimeRangeChartBinding timelineSelectionBinding;
    private RecordingTimeRangeClearButtonBinding clearTimeRangeButtonBinding;

    public TlabPageController(TlabPageView view, I18n i18n) {
        this.view = view;
        this.i18n = i18n;
        timelineListener = (observable, oldValue, newValue) -> setTimelineData(newValue);
        placeholderListener = (observable, oldValue, newValue) -> updateTablePlaceholder(viewModel);
    }

    public void configure() {
        bindLocalizedText();
        configureTable();
        bind(null);
    }

    public TableView<TlabAllocation> table() {
        return view.table();
    }

    public void bind(TlabViewModel nextViewModel) {
        bind(nextViewModel, null);
    }

    public void bind(TlabViewModel nextViewModel, ObjectProperty<RecordingTimeRange> sharedTimeRange) {
        TlabViewModel currentViewModel = viewModel;
        if (currentViewModel != null) {
            currentViewModel.timelineProperty().removeListener(timelineListener);
            currentViewModel.loadingProperty().removeListener(placeholderListener);
            currentViewModel.loadedProperty().removeListener(placeholderListener);
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
        view.table().setPlaceholder(emptyTablePlaceholder());
        view.timelineChart().setData(null);
        viewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        view.table().setItems(nextViewModel.allocationsProperty());
        view.table().getSelectionModel().selectFirst();
        updateTablePlaceholder(nextViewModel);
        nextViewModel.loadingProperty().addListener(placeholderListener);
        nextViewModel.loadedProperty().addListener(placeholderListener);
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
        view.titleLabel().textProperty().bind(i18n.text("tlab.title"));
    }

    private void configureTable() {
        view.table().setPlaceholder(emptyTablePlaceholder());

        TableColumn<TlabAllocation, String> threadCol = new TableColumn<>();
        threadCol.textProperty().bind(i18n.text("tlab.column.thread"));
        threadCol.setPrefWidth(200);
        threadCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().thread()));

        TableColumn<TlabAllocation, Number> insideCountCol = new TableColumn<>();
        insideCountCol.textProperty().bind(i18n.text("tlab.column.insideCount"));
        insideCountCol.setPrefWidth(100);
        insideCountCol.setCellValueFactory(
                cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().insideCount()));
        useFormattedIntegerCells(insideCountCol);

        TableColumn<TlabAllocation, Number> outsideCountCol = new TableColumn<>();
        outsideCountCol.textProperty().bind(i18n.text("tlab.column.outsideCount"));
        outsideCountCol.setPrefWidth(100);
        outsideCountCol.setCellValueFactory(
                cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().outsideCount()));
        useFormattedIntegerCells(outsideCountCol);

        TableColumn<TlabAllocation, String> insideAvgCol = new TableColumn<>();
        insideAvgCol.textProperty().bind(i18n.text("tlab.column.insideAvgSize"));
        insideAvgCol.setPrefWidth(120);
        insideAvgCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(Math.round(cell.getValue().insideAvgSize()))));

        TableColumn<TlabAllocation, String> outsideAvgCol = new TableColumn<>();
        outsideAvgCol.textProperty().bind(i18n.text("tlab.column.outsideAvgSize"));
        outsideAvgCol.setPrefWidth(120);
        outsideAvgCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(Math.round(cell.getValue().outsideAvgSize()))));

        TableColumn<TlabAllocation, String> insideTotalCol = new TableColumn<>();
        insideTotalCol.textProperty().bind(i18n.text("tlab.column.insideTotalSize"));
        insideTotalCol.setPrefWidth(120);
        insideTotalCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(cell.getValue().insideTotalSize())));

        TableColumn<TlabAllocation, String> outsideTotalCol = new TableColumn<>();
        outsideTotalCol.textProperty().bind(i18n.text("tlab.column.outsideTotalSize"));
        outsideTotalCol.setPrefWidth(120);
        outsideTotalCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(cell.getValue().outsideTotalSize())));

        view.table().getColumns().setAll(List.of(threadCol, insideCountCol, outsideCountCol, insideAvgCol,
                outsideAvgCol, insideTotalCol, outsideTotalCol));
    }

    private void updateTablePlaceholder(TlabViewModel viewModel) {
        if (viewModel == null || !viewModel.loadedProperty().get()) {
            view.table().setPlaceholder(viewModel != null && viewModel.loadingProperty().get()
                    ? localizedTablePlaceholder("tlab.loading")
                    : emptyTablePlaceholder());
            return;
        }
        view.table().setPlaceholder(localizedTablePlaceholder("tlab.empty"));
    }

    private Label localizedTablePlaceholder(String key) {
        Label label = new Label();
        label.textProperty().bind(i18n.text(key));
        return label;
    }

    private static Region emptyTablePlaceholder() {
        Region placeholder = new Region();
        placeholder.setManaged(false);
        return placeholder;
    }

    private String recordingContext(RecordingSummary recording) {
        if (recording == null) {
            return "";
        }
        ZoneId zone = ZoneId.systemDefault();
        String start = DisplayFormats.formatTimestamp(recording.startTime(), zone);
        String end = DisplayFormats.formatTimestamp(recording.endTime(), zone);
        String duration = DisplayFormats.formatDuration(recording.durationMillis());
        return i18n.format("tlab.recordingContext", start, end, duration);
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
