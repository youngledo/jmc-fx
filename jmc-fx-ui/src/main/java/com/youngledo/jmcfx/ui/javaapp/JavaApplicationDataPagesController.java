package com.youngledo.jmcfx.ui.javaapp;

import java.time.ZoneId;
import java.util.List;

import com.youngledo.jmcfx.domain.model.ChartDefinition;
import com.youngledo.jmcfx.domain.model.NativeLibraryEntry;
import com.youngledo.jmcfx.domain.model.ThreadDumpEntry;
import com.youngledo.jmcfx.domain.model.ThreadHistogramRow;
import com.youngledo.jmcfx.domain.model.X509CertificateEntry;
import com.youngledo.jmcfx.ui.events.EventsPageController;
import com.youngledo.jmcfx.ui.i18n.I18n;
import com.youngledo.jmcfx.ui.util.DisplayFormats;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/// Controller for Java Application data table pages that are not standalone workflows.
public final class JavaApplicationDataPagesController {

    private final JavaApplicationDataPagesView view;
    private final I18n i18n;
    private final ChangeListener<ChartDefinition> threadHistogramChartListener;
    private JavaAppOverviewViewModel threadHistogramViewModel;
    private SecurityViewModel securityViewModel;
    private NativeLibraryViewModel nativeLibraryViewModel;
    private ThreadDumpViewModel threadDumpViewModel;

    public JavaApplicationDataPagesController(JavaApplicationDataPagesView view, I18n i18n) {
        this.view = view;
        this.i18n = i18n;
        threadHistogramChartListener = (observable, oldValue, newValue) -> view.threadHistogramChart().setData(newValue);
    }

    public void configure() {
        bindLocalizedText();
        configureThreadHistogramTable();
        configureSecurityTable();
        configureNativeLibrariesTable();
        configureThreadDumpsTable();
        bindThreadHistogram(null);
        bindSecurity(null);
        bindNativeLibraries(null);
        bindThreadDumps(null);
    }

    public List<TableView<?>> exportTables() {
        return List.of(
                view.threadHistogramTable(),
                view.securityTable(),
                view.nativeLibrariesTable(),
                view.threadDumpsTable());
    }

    public void bindThreadHistogram(JavaAppOverviewViewModel nextViewModel) {
        JavaAppOverviewViewModel currentThreadHistogramViewModel = threadHistogramViewModel;
        if (currentThreadHistogramViewModel != null) {
            currentThreadHistogramViewModel.chartProperty().removeListener(threadHistogramChartListener);
        }
        view.threadHistogramChart().setData(null);
        view.threadHistogramTable().setItems(FXCollections.emptyObservableList());
        threadHistogramViewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        view.threadHistogramTable().setItems(nextViewModel.histogramRowsProperty());
        nextViewModel.chartProperty().addListener(threadHistogramChartListener);
        view.threadHistogramChart().setData(nextViewModel.chartProperty().get());
    }

    public void bindSecurity(SecurityViewModel nextViewModel) {
        view.securityTable().setItems(FXCollections.emptyObservableList());
        securityViewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        view.securityTable().setItems(nextViewModel.certificatesProperty());
    }

    public void bindNativeLibraries(NativeLibraryViewModel nextViewModel) {
        view.nativeLibrariesTable().setItems(FXCollections.emptyObservableList());
        nativeLibraryViewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        view.nativeLibrariesTable().setItems(nextViewModel.librariesProperty());
    }

    public void bindThreadDumps(ThreadDumpViewModel nextViewModel) {
        view.threadDumpsTable().setItems(FXCollections.emptyObservableList());
        view.threadDumpTextArea().setText("");
        threadDumpViewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        view.threadDumpsTable().setItems(nextViewModel.dumpsProperty());
    }

    private void bindLocalizedText() {
        view.threadHistogramTitleLabel().textProperty().bind(i18n.text("threadHistogram.title"));
        view.securityTitleLabel().textProperty().bind(i18n.text("security.title"));
        view.nativeLibrariesTitleLabel().textProperty().bind(i18n.text("nativeLibraries.title"));
        view.threadDumpsTitleLabel().textProperty().bind(i18n.text("threadDumps.title"));
    }

    private void configureThreadHistogramTable() {
        view.threadHistogramTable().setPlaceholder(localizedTablePlaceholder("threadHistogram.empty"));

        TableColumn<ThreadHistogramRow, String> threadCol = new TableColumn<>();
        threadCol.textProperty().bind(i18n.text("threadHistogram.column.threadName"));
        threadCol.setPrefWidth(360);
        threadCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().threadName()));

        TableColumn<ThreadHistogramRow, Number> profilingCol = new TableColumn<>();
        profilingCol.textProperty().bind(i18n.text("threadHistogram.column.profilingCount"));
        profilingCol.setPrefWidth(100);
        profilingCol.setCellValueFactory(
                cell -> new javafx.beans.property.SimpleIntegerProperty(cell.getValue().profilingCount()));
        useFormattedIntegerCells(profilingCol);

        TableColumn<ThreadHistogramRow, String> ioCol = new TableColumn<>();
        ioCol.textProperty().bind(i18n.text("threadHistogram.column.ioDurationMs"));
        ioCol.setPrefWidth(100);
        ioCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatDuration(cell.getValue().ioDurationMillis())));

        TableColumn<ThreadHistogramRow, String> blockedCol = new TableColumn<>();
        blockedCol.textProperty().bind(i18n.text("threadHistogram.column.blockedDurationMs"));
        blockedCol.setPrefWidth(100);
        blockedCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatDuration(cell.getValue().blockedDurationMillis())));

        TableColumn<ThreadHistogramRow, String> allocCol = new TableColumn<>();
        allocCol.textProperty().bind(i18n.text("threadHistogram.column.allocatedBytes"));
        allocCol.setPrefWidth(120);
        allocCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(cell.getValue().allocatedBytes())));

        TableColumn<ThreadHistogramRow, Number> excCol = new TableColumn<>();
        excCol.textProperty().bind(i18n.text("threadHistogram.column.exceptionCount"));
        excCol.setPrefWidth(80);
        excCol.setCellValueFactory(
                cell -> new javafx.beans.property.SimpleIntegerProperty(cell.getValue().exceptionCount()));
        useFormattedIntegerCells(excCol);

        view.threadHistogramTable().getColumns().setAll(List.of(
                threadCol, profilingCol, ioCol, blockedCol, allocCol, excCol));
    }

    private void configureSecurityTable() {
        view.securityTable().setPlaceholder(localizedTablePlaceholder("security.empty"));

        TableColumn<X509CertificateEntry, String> algoCol = new TableColumn<>();
        algoCol.textProperty().bind(i18n.text("security.column.algorithm"));
        algoCol.setPrefWidth(100);
        algoCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().algorithm()));

        TableColumn<X509CertificateEntry, String> subjectCol = new TableColumn<>();
        subjectCol.textProperty().bind(i18n.text("security.column.subject"));
        subjectCol.setPrefWidth(420);
        subjectCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().subject()));

        TableColumn<X509CertificateEntry, String> issuerCol = new TableColumn<>();
        issuerCol.textProperty().bind(i18n.text("security.column.issuer"));
        issuerCol.setPrefWidth(420);
        issuerCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().issuer()));

        TableColumn<X509CertificateEntry, String> serialCol = new TableColumn<>();
        serialCol.textProperty().bind(i18n.text("security.column.serialNumber"));
        serialCol.setPrefWidth(150);
        serialCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().serialNumber()));

        TableColumn<X509CertificateEntry, String> validFromCol = new TableColumn<>();
        validFromCol.textProperty().bind(i18n.text("security.column.validFrom"));
        validFromCol.setPrefWidth(160);
        validFromCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                formatEventTimeForDisplay(cell.getValue().validFrom(), ZoneId.systemDefault())));

        TableColumn<X509CertificateEntry, String> validToCol = new TableColumn<>();
        validToCol.textProperty().bind(i18n.text("security.column.validTo"));
        validToCol.setPrefWidth(160);
        validToCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                formatEventTimeForDisplay(cell.getValue().validTo(), ZoneId.systemDefault())));

        TableColumn<X509CertificateEntry, Number> keyLenCol = new TableColumn<>();
        keyLenCol.textProperty().bind(i18n.text("security.column.keyLength"));
        keyLenCol.setPrefWidth(80);
        keyLenCol.setCellValueFactory(
                cell -> new javafx.beans.property.SimpleIntegerProperty(cell.getValue().keyLength()));
        useFormattedIntegerCells(keyLenCol);

        view.securityTable().getColumns().setAll(List.of(
                algoCol, subjectCol, issuerCol, serialCol, validFromCol, validToCol, keyLenCol));
    }

    private void configureNativeLibrariesTable() {
        view.nativeLibrariesTable().setPlaceholder(localizedTablePlaceholder("nativeLibraries.empty"));

        TableColumn<NativeLibraryEntry, String> nameCol = new TableColumn<>();
        nameCol.textProperty().bind(i18n.text("nativeLibraries.column.name"));
        nameCol.setPrefWidth(250);
        nameCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().name()));

        TableColumn<NativeLibraryEntry, String> baseCol = new TableColumn<>();
        baseCol.textProperty().bind(i18n.text("nativeLibraries.column.basePath"));
        baseCol.setPrefWidth(420);
        baseCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().basePath()));

        TableColumn<NativeLibraryEntry, String> absCol = new TableColumn<>();
        absCol.textProperty().bind(i18n.text("nativeLibraries.column.absolutePath"));
        absCol.setPrefWidth(560);
        absCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().absolutePath()));

        view.nativeLibrariesTable().getColumns().setAll(List.of(nameCol, baseCol, absCol));
    }

    private void configureThreadDumpsTable() {
        view.threadDumpsTable().setPlaceholder(localizedTablePlaceholder("threadDumps.empty"));

        TableColumn<ThreadDumpEntry, String> timeCol = new TableColumn<>();
        timeCol.textProperty().bind(i18n.text("threadDumps.column.time"));
        timeCol.setPrefWidth(200);
        timeCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                formatEventTimeForDisplay(cell.getValue().startTime(), ZoneId.systemDefault())));

        view.threadDumpsTable().getColumns().setAll(List.of(timeCol));
        view.threadDumpsTable().getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> view.threadDumpTextArea().setText(newVal != null ? newVal.dumpText() : ""));
    }

    private Label localizedTablePlaceholder(String key) {
        Label label = new Label();
        label.textProperty().bind(i18n.text(key));
        return label;
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

    private static String formatEventTimeForDisplay(java.time.Instant instant, ZoneId zoneId) {
        return EventsPageController.formatEventTimeForDisplay(instant, zoneId);
    }
}
