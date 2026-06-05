package io.github.youngledo.jmcfx.ui.jvm;

import java.time.ZoneId;
import java.util.List;

import io.github.youngledo.jmcfx.domain.model.ChartDefinition;
import io.github.youngledo.jmcfx.domain.model.ClassloadEvent;
import io.github.youngledo.jmcfx.domain.model.ClassloaderStatistics;
import io.github.youngledo.jmcfx.domain.model.ClassloaderSummary;
import io.github.youngledo.jmcfx.domain.model.CodeCacheStats;
import io.github.youngledo.jmcfx.domain.model.CodeCacheSweep;
import io.github.youngledo.jmcfx.domain.model.CompilationEvent;
import io.github.youngledo.jmcfx.domain.model.GcEvent;
import io.github.youngledo.jmcfx.domain.model.GcHeapSummary;
import io.github.youngledo.jmcfx.domain.model.GcReferenceStat;
import io.github.youngledo.jmcfx.domain.model.GcSummary;
import io.github.youngledo.jmcfx.domain.model.JvmFlag;
import io.github.youngledo.jmcfx.domain.model.JvmFlagChange;
import io.github.youngledo.jmcfx.domain.model.VmOperationEvent;
import io.github.youngledo.jmcfx.domain.model.VmOperationSummary;
import io.github.youngledo.jmcfx.ui.i18n.I18n;
import io.github.youngledo.jmcfx.ui.util.DisplayFormats;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/// Controller for JVM Internals table and timeline pages.
public final class JvmInternalsPagesController {

    private final JvmInternalsPagesView view;
    private final I18n i18n;
    private final ChangeListener<ChartDefinition> heapChartListener;
    private final ChangeListener<ChartDefinition> metaspaceChartListener;
    private final ChangeListener<ChartDefinition> pauseChartListener;
    private final ChangeListener<ChartDefinition> compilationDurationChartListener;
    private final ChangeListener<ChartDefinition> codeCacheEntriesChartListener;
    private final ChangeListener<ChartDefinition> codeCacheSweepChartListener;
    private final ChangeListener<ChartDefinition> classLoadingChartListener;
    private GcDetailsViewModel gcDetailsViewModel;
    private CompilationsViewModel compilationsViewModel;
    private CodeCacheViewModel codeCacheViewModel;
    private ClassLoadingViewModel classLoadingViewModel;

    public JvmInternalsPagesController(JvmInternalsPagesView view, I18n i18n) {
        this.view = view;
        this.i18n = i18n;
        heapChartListener = (observable, oldValue, newValue) -> view.gcHeapChart().setData(newValue);
        metaspaceChartListener = (observable, oldValue, newValue) -> view.gcMetaspaceChart().setData(newValue);
        pauseChartListener = (observable, oldValue, newValue) -> view.gcPauseChart().setData(newValue);
        compilationDurationChartListener = (observable, oldValue, newValue) ->
                view.compilationDurationChart().setData(newValue);
        codeCacheEntriesChartListener = (observable, oldValue, newValue) ->
                view.codeCacheEntriesChart().setData(newValue);
        codeCacheSweepChartListener = (observable, oldValue, newValue) -> view.codeCacheSweepChart().setData(newValue);
        classLoadingChartListener = (observable, oldValue, newValue) -> view.classLoadingChart().setData(newValue);
    }

    public void configure() {
        bindLocalizedText();
        configureJvmFlagsTable();
        configureJvmFlagChangesTable();
        configureGcSummaryTable();
        configureGcEventsTable();
        configureGcReferenceStatsTable();
        configureGcHeapSummaryTable();
        configureCompilationsTable();
        configureCompilationFailuresTable();
        configureCodeCacheSweepsTable();
        configureCodeCacheStatsTable();
        configureClassLoadingHistogramTable();
        configureClassLoadingEventsTable();
        configureClassLoadingStatsTable();
        configureVmOperationSummaryTable();
        configureVmOperationEventsTable();
        bindJvmInfo(null);
        bindGcConfig(null);
        bindGcSummary(null);
        bindGcDetails(null);
        bindCompilations(null);
        bindCodeCache(null);
        bindClassLoading(null);
        bindVmOperations(null);
    }

    public List<TableView<?>> exportTables() {
        return List.of(view.jvmFlagsTable(), view.jvmFlagChangesTable(), view.gcEventsTable(),
                view.gcReferenceStatsTable(), view.gcHeapSummaryTable(), view.compilationsTable(),
                view.compilationFailuresTable(), view.codeCacheSweepsTable(), view.codeCacheStatsTable(),
                view.classLoadingHistogramTable(), view.classLoadingEventsTable(), view.classLoadingStatsTable(),
                view.vmOperationSummaryTable(), view.vmOperationEventsTable());
    }

    public void bindJvmInfo(JvmInfoViewModel nextViewModel) {
        view.jvmFlagsTable().setItems(FXCollections.emptyObservableList());
        view.jvmFlagChangesTable().setItems(FXCollections.emptyObservableList());
        if (nextViewModel == null) {
            return;
        }
        view.jvmFlagsTable().setItems(nextViewModel.flags());
        view.jvmFlagChangesTable().setItems(nextViewModel.flagChanges());
    }

    public void bindGcConfig(GcConfigViewModel nextViewModel) {
        // GC config is currently a localized empty-state page.
    }

    public void bindGcSummary(GcSummaryViewModel nextViewModel) {
        view.gcSummaryTable().setItems(FXCollections.emptyObservableList());
        if (nextViewModel == null) {
            return;
        }
        view.gcSummaryTable().setItems(nextViewModel.summaries());
    }

    public void bindGcDetails(GcDetailsViewModel nextViewModel) {
        GcDetailsViewModel currentViewModel = gcDetailsViewModel;
        if (currentViewModel != null) {
            currentViewModel.heapChartProperty().removeListener(heapChartListener);
            currentViewModel.metaspaceChartProperty().removeListener(metaspaceChartListener);
            currentViewModel.pauseChartProperty().removeListener(pauseChartListener);
        }
        view.gcHeapChart().setData(null);
        view.gcMetaspaceChart().setData(null);
        view.gcPauseChart().setData(null);
        view.gcEventsTable().setItems(FXCollections.emptyObservableList());
        view.gcReferenceStatsTable().setItems(FXCollections.emptyObservableList());
        view.gcHeapSummaryTable().setItems(FXCollections.emptyObservableList());
        gcDetailsViewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        view.gcEventsTable().setItems(nextViewModel.gcEvents());
        view.gcReferenceStatsTable().setItems(nextViewModel.referenceStats());
        view.gcHeapSummaryTable().setItems(nextViewModel.heapSummaries());
        nextViewModel.heapChartProperty().addListener(heapChartListener);
        nextViewModel.metaspaceChartProperty().addListener(metaspaceChartListener);
        nextViewModel.pauseChartProperty().addListener(pauseChartListener);
        view.gcHeapChart().setData(nextViewModel.heapChartProperty().get());
        view.gcMetaspaceChart().setData(nextViewModel.metaspaceChartProperty().get());
        view.gcPauseChart().setData(nextViewModel.pauseChartProperty().get());
    }

    public void bindCompilations(CompilationsViewModel nextViewModel) {
        CompilationsViewModel currentViewModel = compilationsViewModel;
        if (currentViewModel != null) {
            currentViewModel.durationChartProperty().removeListener(compilationDurationChartListener);
        }
        view.compilationDurationChart().setData(null);
        view.compilationsTable().setItems(FXCollections.emptyObservableList());
        view.compilationFailuresTable().setItems(FXCollections.emptyObservableList());
        compilationsViewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        view.compilationsTable().setItems(nextViewModel.compilations());
        view.compilationFailuresTable().setItems(nextViewModel.failures());
        nextViewModel.durationChartProperty().addListener(compilationDurationChartListener);
        view.compilationDurationChart().setData(nextViewModel.durationChartProperty().get());
    }

    public void bindCodeCache(CodeCacheViewModel nextViewModel) {
        CodeCacheViewModel currentViewModel = codeCacheViewModel;
        if (currentViewModel != null) {
            currentViewModel.entriesChartProperty().removeListener(codeCacheEntriesChartListener);
            currentViewModel.sweepChartProperty().removeListener(codeCacheSweepChartListener);
        }
        view.codeCacheEntriesChart().setData(null);
        view.codeCacheSweepChart().setData(null);
        view.codeCacheSweepsTable().setItems(FXCollections.emptyObservableList());
        view.codeCacheStatsTable().setItems(FXCollections.emptyObservableList());
        codeCacheViewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        view.codeCacheSweepsTable().setItems(nextViewModel.sweeps());
        view.codeCacheStatsTable().setItems(nextViewModel.statistics());
        nextViewModel.entriesChartProperty().addListener(codeCacheEntriesChartListener);
        nextViewModel.sweepChartProperty().addListener(codeCacheSweepChartListener);
        view.codeCacheEntriesChart().setData(nextViewModel.entriesChartProperty().get());
        view.codeCacheSweepChart().setData(nextViewModel.sweepChartProperty().get());
    }

    public void bindClassLoading(ClassLoadingViewModel nextViewModel) {
        ClassLoadingViewModel currentViewModel = classLoadingViewModel;
        if (currentViewModel != null) {
            currentViewModel.chartProperty().removeListener(classLoadingChartListener);
        }
        view.classLoadingChart().setData(null);
        view.classLoadingHistogramTable().setItems(FXCollections.emptyObservableList());
        view.classLoadingEventsTable().setItems(FXCollections.emptyObservableList());
        view.classLoadingStatsTable().setItems(FXCollections.emptyObservableList());
        classLoadingViewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        view.classLoadingHistogramTable().setItems(nextViewModel.histogram());
        view.classLoadingEventsTable().setItems(nextViewModel.events());
        view.classLoadingStatsTable().setItems(nextViewModel.statistics());
        nextViewModel.chartProperty().addListener(classLoadingChartListener);
        view.classLoadingChart().setData(nextViewModel.chartProperty().get());
    }

    public void bindVmOperations(VmOperationsViewModel nextViewModel) {
        view.vmOperationSummaryTable().setItems(FXCollections.emptyObservableList());
        view.vmOperationEventsTable().setItems(FXCollections.emptyObservableList());
        if (nextViewModel == null) {
            return;
        }
        view.vmOperationSummaryTable().setItems(nextViewModel.summary());
        view.vmOperationEventsTable().setItems(nextViewModel.events());
    }

    private void bindLocalizedText() {
        view.jvmInfoTitleLabel().textProperty().bind(i18n.text("jvmInfo.title"));
        view.jvmFlagsLabel().textProperty().bind(i18n.text("jvmInfo.flags"));
        view.jvmFlagChangesLabel().textProperty().bind(i18n.text("jvmInfo.flagChanges"));
        view.gcConfigTitleLabel().textProperty().bind(i18n.text("gcConfig.title"));
        view.gcConfigDescriptionLabel().textProperty().bind(i18n.text("gcConfig.empty"));
        view.gcSummaryTitleLabel().textProperty().bind(i18n.text("gcSummary.title"));
        view.gcDetailsTitleLabel().textProperty().bind(i18n.text("gcDetails.title"));
        view.gcEventsLabel().textProperty().bind(i18n.text("gcDetails.events"));
        view.gcReferenceStatsLabel().textProperty().bind(i18n.text("gcDetails.referenceStats"));
        view.gcHeapSummaryLabel().textProperty().bind(i18n.text("gcDetails.heapSummaries"));
        view.compilationsTitleLabel().textProperty().bind(i18n.text("compilations.title"));
        view.compilationEventsLabel().textProperty().bind(i18n.text("compilations.events"));
        view.compilationFailuresLabel().textProperty().bind(i18n.text("compilations.failures"));
        view.codeCacheTitleLabel().textProperty().bind(i18n.text("codeCache.title"));
        view.codeCacheSweepsLabel().textProperty().bind(i18n.text("codeCache.sweeps"));
        view.codeCacheStatsLabel().textProperty().bind(i18n.text("codeCache.statistics"));
        view.classLoadingTitleLabel().textProperty().bind(i18n.text("classLoading.title"));
        view.classLoadingHistogramLabel().textProperty().bind(i18n.text("classLoading.histogram"));
        view.classLoadingEventsLabel().textProperty().bind(i18n.text("classLoading.events"));
        view.classLoadingStatsLabel().textProperty().bind(i18n.text("classLoading.statistics"));
        view.vmOperationsTitleLabel().textProperty().bind(i18n.text("vmOperations.title"));
        view.vmOperationSummaryLabel().textProperty().bind(i18n.text("vmOperations.summary"));
        view.vmOperationEventsLabel().textProperty().bind(i18n.text("vmOperations.events"));
    }

    private void configureJvmFlagsTable() {
        TableColumn<JvmFlag, String> nameCol = localizedColumn("jvmInfo.column.flag");
        nameCol.setPrefWidth(260);
        nameCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().name()));
        TableColumn<JvmFlag, String> valueCol = localizedColumn("jvmInfo.column.value");
        valueCol.setPrefWidth(320);
        valueCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().value()));
        TableColumn<JvmFlag, String> originCol = localizedColumn("jvmInfo.column.origin");
        originCol.setPrefWidth(160);
        originCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().origin()));
        view.jvmFlagsTable().getColumns().setAll(List.of(nameCol, valueCol, originCol));
    }

    private void configureJvmFlagChangesTable() {
        TableColumn<JvmFlagChange, String> timeCol = localizedColumn("jvmInfo.column.time");
        timeCol.setPrefWidth(220);
        timeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatTimestamp(data.getValue().startTime(), ZoneId.systemDefault())));
        TableColumn<JvmFlagChange, String> flagCol = localizedColumn("jvmInfo.column.flag");
        flagCol.setPrefWidth(240);
        flagCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().flagName()));
        TableColumn<JvmFlagChange, String> oldCol = localizedColumn("jvmInfo.column.oldValue");
        oldCol.setPrefWidth(240);
        oldCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().oldValue()));
        TableColumn<JvmFlagChange, String> newCol = localizedColumn("jvmInfo.column.newValue");
        newCol.setPrefWidth(240);
        newCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().newValue()));
        TableColumn<JvmFlagChange, String> originCol = localizedColumn("jvmInfo.column.origin");
        originCol.setPrefWidth(160);
        originCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().origin()));
        view.jvmFlagChangesTable().getColumns().setAll(List.of(timeCol, flagCol, oldCol, newCol, originCol));
    }

    private void configureGcSummaryTable() {
        TableColumn<GcSummary, String> genCol = localizedColumn("gcSummary.column.generation");
        genCol.setPrefWidth(180);
        genCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().generation()));
        TableColumn<GcSummary, String> countCol = localizedColumn("common.column.count");
        countCol.setPrefWidth(90);
        countCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatInteger(data.getValue().collectionCount())));
        TableColumn<GcSummary, String> totalCol = localizedColumn("gcSummary.column.total");
        totalCol.setPrefWidth(120);
        totalCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatDuration(data.getValue().totalDurationMillis())));
        TableColumn<GcSummary, String> avgCol = localizedColumn("gcSummary.column.average");
        avgCol.setPrefWidth(120);
        avgCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatDurationMillis(data.getValue().avgDurationMillis())));
        TableColumn<GcSummary, String> maxCol = localizedColumn("gcSummary.column.max");
        maxCol.setPrefWidth(120);
        maxCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatDuration(data.getValue().maxDurationMillis())));
        view.gcSummaryTable().getColumns().setAll(List.of(genCol, countCol, totalCol, avgCol, maxCol));
    }

    private void configureGcEventsTable() {
        TableColumn<GcEvent, String> idCol = localizedColumn("gc.column.id");
        idCol.setPrefWidth(80);
        idCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatInteger(data.getValue().gcId())));
        TableColumn<GcEvent, String> nameCol = localizedColumn("common.column.name");
        nameCol.setPrefWidth(220);
        nameCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().name()));
        TableColumn<GcEvent, String> causeCol = localizedColumn("gcDetails.column.cause");
        causeCol.setPrefWidth(260);
        causeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().cause()));
        TableColumn<GcEvent, String> pauseCol = localizedColumn("gcDetails.column.longestPause");
        pauseCol.setPrefWidth(130);
        pauseCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatMicros(data.getValue().longestPauseMicros())));
        TableColumn<GcEvent, String> totalPauseCol = localizedColumn("gcDetails.column.totalPause");
        totalPauseCol.setPrefWidth(120);
        totalPauseCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatMicros(data.getValue().totalPauseMicros())));
        TableColumn<GcEvent, String> timeCol = localizedColumn("common.column.time");
        timeCol.setPrefWidth(220);
        timeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatTimestamp(data.getValue().startTime(), ZoneId.systemDefault())));
        view.gcEventsTable().getColumns().setAll(List.of(idCol, nameCol, causeCol, pauseCol, totalPauseCol, timeCol));
    }

    private void configureGcReferenceStatsTable() {
        TableColumn<GcReferenceStat, String> idCol = localizedColumn("gc.column.id");
        idCol.setPrefWidth(80);
        idCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatInteger(data.getValue().gcId())));
        TableColumn<GcReferenceStat, String> typeCol = localizedColumn("gcDetails.column.referenceType");
        typeCol.setPrefWidth(220);
        typeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().referenceType()));
        TableColumn<GcReferenceStat, String> countCol = localizedColumn("common.column.count");
        countCol.setPrefWidth(100);
        countCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatInteger(data.getValue().count())));
        view.gcReferenceStatsTable().getColumns().setAll(List.of(idCol, typeCol, countCol));
    }

    private void configureGcHeapSummaryTable() {
        TableColumn<GcHeapSummary, String> idCol = localizedColumn("gc.column.id");
        idCol.setPrefWidth(80);
        idCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatInteger(data.getValue().gcId())));
        TableColumn<GcHeapSummary, String> whenCol = localizedColumn("gcDetails.column.when");
        whenCol.setPrefWidth(120);
        whenCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().when()));
        TableColumn<GcHeapSummary, String> usedCol = localizedColumn("gcDetails.column.heapUsed");
        usedCol.setPrefWidth(130);
        usedCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(data.getValue().heapUsed())));
        TableColumn<GcHeapSummary, String> committedCol = localizedColumn("gcDetails.column.heapCommitted");
        committedCol.setPrefWidth(150);
        committedCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(data.getValue().heapCommitted())));
        TableColumn<GcHeapSummary, String> metaUsedCol = localizedColumn("gcDetails.column.metaspaceUsed");
        metaUsedCol.setPrefWidth(150);
        metaUsedCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(data.getValue().metaspaceUsed())));
        TableColumn<GcHeapSummary, String> metaCommittedCol = localizedColumn("gcDetails.column.metaspaceCommitted");
        metaCommittedCol.setPrefWidth(180);
        metaCommittedCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(data.getValue().metaspaceCommitted())));
        view.gcHeapSummaryTable().getColumns().setAll(List.of(idCol, whenCol, usedCol, committedCol,
                metaUsedCol, metaCommittedCol));
    }

    private void configureCompilationsTable() {
        TableColumn<CompilationEvent, String> idCol = localizedColumn("common.column.id");
        idCol.setPrefWidth(80);
        idCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatInteger(data.getValue().compilationId())));
        TableColumn<CompilationEvent, String> methodCol = localizedColumn("compilations.column.method");
        methodCol.setPrefWidth(620);
        methodCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().method()));
        TableColumn<CompilationEvent, String> okCol = localizedColumn("compilations.column.succeeded");
        okCol.setPrefWidth(100);
        okCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatBoolean(data.getValue().succeeded())));
        TableColumn<CompilationEvent, String> durCol = localizedColumn("common.column.duration");
        durCol.setPrefWidth(120);
        durCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatMicros(data.getValue().durationMicros())));
        TableColumn<CompilationEvent, String> sizeCol = localizedColumn("compilations.column.codeSize");
        sizeCol.setPrefWidth(110);
        sizeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(data.getValue().codeSize())));
        TableColumn<CompilationEvent, String> inlineCol = localizedColumn("compilations.column.inlined");
        inlineCol.setPrefWidth(100);
        inlineCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(data.getValue().inlinedBytes())));
        TableColumn<CompilationEvent, String> timeCol = localizedColumn("common.column.time");
        timeCol.setPrefWidth(220);
        timeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatTimestamp(data.getValue().startTime(), ZoneId.systemDefault())));
        view.compilationsTable().getColumns().setAll(List.of(idCol, methodCol, okCol, durCol, sizeCol, inlineCol,
                timeCol));
    }

    private void configureCompilationFailuresTable() {
        TableColumn<CompilationEvent, String> idCol = localizedColumn("common.column.id");
        idCol.setPrefWidth(80);
        idCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatInteger(data.getValue().compilationId())));
        TableColumn<CompilationEvent, String> methodCol = localizedColumn("compilations.column.method");
        methodCol.setPrefWidth(620);
        methodCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().method()));
        TableColumn<CompilationEvent, String> durCol = localizedColumn("common.column.duration");
        durCol.setPrefWidth(120);
        durCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatMicros(data.getValue().durationMicros())));
        TableColumn<CompilationEvent, String> timeCol = localizedColumn("common.column.time");
        timeCol.setPrefWidth(220);
        timeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatTimestamp(data.getValue().startTime(), ZoneId.systemDefault())));
        view.compilationFailuresTable().getColumns().setAll(List.of(idCol, methodCol, durCol, timeCol));
    }

    private void configureCodeCacheSweepsTable() {
        TableColumn<CodeCacheSweep, String> timeCol = localizedColumn("common.column.time");
        timeCol.setPrefWidth(220);
        timeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatTimestamp(data.getValue().startTime(), ZoneId.systemDefault())));
        TableColumn<CodeCacheSweep, String> idxCol = localizedColumn("codeCache.column.index");
        idxCol.setPrefWidth(80);
        idxCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatInteger(data.getValue().sweepIndex())));
        TableColumn<CodeCacheSweep, String> durCol = localizedColumn("common.column.duration");
        durCol.setPrefWidth(120);
        durCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatMicros(data.getValue().durationMicros())));
        TableColumn<CodeCacheSweep, String> flushedCol = localizedColumn("codeCache.column.flushed");
        flushedCol.setPrefWidth(100);
        flushedCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatInteger(data.getValue().flushed())));
        TableColumn<CodeCacheSweep, String> sweptCol = localizedColumn("codeCache.column.swept");
        sweptCol.setPrefWidth(100);
        sweptCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatInteger(data.getValue().swept())));
        TableColumn<CodeCacheSweep, String> countCol = localizedColumn("codeCache.column.sweptCount");
        countCol.setPrefWidth(120);
        countCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatInteger(data.getValue().sweptCount())));
        view.codeCacheSweepsTable().getColumns().setAll(List.of(timeCol, idxCol, durCol, flushedCol, sweptCol,
                countCol));
    }

    private void configureCodeCacheStatsTable() {
        TableColumn<CodeCacheStats, String> timeCol = localizedColumn("common.column.time");
        timeCol.setPrefWidth(220);
        timeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatTimestamp(data.getValue().startTime(), ZoneId.systemDefault())));
        TableColumn<CodeCacheStats, String> heapCol = localizedColumn("codeCache.column.codeHeap");
        heapCol.setPrefWidth(220);
        heapCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().codeHeap()));
        TableColumn<CodeCacheStats, String> entriesCol = localizedColumn("codeCache.column.entries");
        entriesCol.setPrefWidth(100);
        entriesCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatInteger(data.getValue().entries())));
        TableColumn<CodeCacheStats, String> methodsCol = localizedColumn("codeCache.column.methods");
        methodsCol.setPrefWidth(100);
        methodsCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatInteger(data.getValue().methods())));
        TableColumn<CodeCacheStats, String> adaptersCol = localizedColumn("codeCache.column.adapters");
        adaptersCol.setPrefWidth(100);
        adaptersCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatInteger(data.getValue().adapters())));
        TableColumn<CodeCacheStats, String> unallocCol = localizedColumn("codeCache.column.unallocated");
        unallocCol.setPrefWidth(130);
        unallocCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(data.getValue().unallocated())));
        view.codeCacheStatsTable().getColumns().setAll(List.of(timeCol, heapCol, entriesCol, methodsCol, adaptersCol,
                unallocCol));
    }

    private void configureClassLoadingHistogramTable() {
        TableColumn<ClassloaderSummary, String> loaderCol = localizedColumn("classLoading.column.classloader");
        loaderCol.setPrefWidth(520);
        loaderCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().classloader()));
        TableColumn<ClassloaderSummary, String> loadedCol = localizedColumn("classLoading.column.loaded");
        loadedCol.setPrefWidth(100);
        loadedCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatInteger(data.getValue().loadedCount())));
        TableColumn<ClassloaderSummary, String> unloadedCol = localizedColumn("classLoading.column.unloaded");
        unloadedCol.setPrefWidth(100);
        unloadedCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatInteger(data.getValue().unloadedCount())));
        view.classLoadingHistogramTable().getColumns().setAll(List.of(loaderCol, loadedCol, unloadedCol));
    }

    private void configureClassLoadingEventsTable() {
        TableColumn<ClassloadEvent, String> typeCol = localizedColumn("common.column.event");
        typeCol.setPrefWidth(160);
        typeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().eventType()));
        TableColumn<ClassloadEvent, String> timeCol = localizedColumn("common.column.time");
        timeCol.setPrefWidth(220);
        timeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatTimestamp(data.getValue().startTime(), ZoneId.systemDefault())));
        TableColumn<ClassloadEvent, String> classCol = localizedColumn("classLoading.column.class");
        classCol.setPrefWidth(360);
        classCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().loadedClass()));
        TableColumn<ClassloadEvent, String> defLoaderCol = localizedColumn("classLoading.column.definingLoader");
        defLoaderCol.setPrefWidth(320);
        defLoaderCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().definingClassloader()));
        TableColumn<ClassloadEvent, String> initLoaderCol = localizedColumn("classLoading.column.initiatingLoader");
        initLoaderCol.setPrefWidth(320);
        initLoaderCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().initiatingClassloader()));
        TableColumn<ClassloadEvent, String> durCol = localizedColumn("common.column.duration");
        durCol.setPrefWidth(120);
        durCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatMicros(data.getValue().durationMicros())));
        view.classLoadingEventsTable().getColumns().setAll(List.of(typeCol, timeCol, classCol, defLoaderCol,
                initLoaderCol, durCol));
    }

    private void configureClassLoadingStatsTable() {
        TableColumn<ClassloaderStatistics, String> loaderCol = localizedColumn("classLoading.column.classloader");
        loaderCol.setPrefWidth(420);
        loaderCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().classloader()));
        TableColumn<ClassloaderStatistics, String> parentCol = localizedColumn("classLoading.column.parent");
        parentCol.setPrefWidth(420);
        parentCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().parentClassloader()));
        TableColumn<ClassloaderStatistics, String> countCol = localizedColumn("classLoading.column.loadedClasses");
        countCol.setPrefWidth(130);
        countCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatInteger(data.getValue().loadedClassCount())));
        TableColumn<ClassloaderStatistics, String> chunkCol = localizedColumn("classLoading.column.chunkSize");
        chunkCol.setPrefWidth(120);
        chunkCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(data.getValue().anonymousBlockChunkSize())));
        TableColumn<ClassloaderStatistics, String> blockCol = localizedColumn("classLoading.column.blockSize");
        blockCol.setPrefWidth(120);
        blockCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(data.getValue().anonymousBlockSize())));
        TableColumn<ClassloaderStatistics, String> anonCol = localizedColumn("classLoading.column.anonymousClasses");
        anonCol.setPrefWidth(150);
        anonCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatInteger(data.getValue().anonymousClassCount())));
        view.classLoadingStatsTable().getColumns().setAll(List.of(loaderCol, parentCol, countCol, chunkCol, blockCol,
                anonCol));
    }

    private void configureVmOperationSummaryTable() {
        TableColumn<VmOperationSummary, String> opCol = localizedColumn("vmOperations.column.operation");
        opCol.setPrefWidth(360);
        opCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().operation()));
        TableColumn<VmOperationSummary, String> countCol = localizedColumn("common.column.count");
        countCol.setPrefWidth(100);
        countCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatInteger(data.getValue().count())));
        TableColumn<VmOperationSummary, String> totalCol = localizedColumn("common.column.totalDuration");
        totalCol.setPrefWidth(140);
        totalCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatMicros(data.getValue().totalDurationMicros())));
        TableColumn<VmOperationSummary, String> maxCol = localizedColumn("common.column.maxDuration");
        maxCol.setPrefWidth(140);
        maxCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatMicros(data.getValue().maxDurationMicros())));
        view.vmOperationSummaryTable().getColumns().setAll(List.of(opCol, countCol, totalCol, maxCol));
    }

    private void configureVmOperationEventsTable() {
        TableColumn<VmOperationEvent, String> timeCol = localizedColumn("common.column.time");
        timeCol.setPrefWidth(220);
        timeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatTimestamp(data.getValue().startTime(), ZoneId.systemDefault())));
        TableColumn<VmOperationEvent, String> opCol = localizedColumn("vmOperations.column.operation");
        opCol.setPrefWidth(320);
        opCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().operation()));
        TableColumn<VmOperationEvent, String> blockCol = localizedColumn("vmOperations.column.blocking");
        blockCol.setPrefWidth(100);
        blockCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatBoolean(data.getValue().blocking())));
        TableColumn<VmOperationEvent, String> safeCol = localizedColumn("vmOperations.column.safepoint");
        safeCol.setPrefWidth(100);
        safeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatBoolean(data.getValue().safepoint())));
        TableColumn<VmOperationEvent, String> durCol = localizedColumn("common.column.duration");
        durCol.setPrefWidth(120);
        durCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatMicros(data.getValue().durationMicros())));
        TableColumn<VmOperationEvent, String> threadCol = localizedColumn("common.column.thread");
        threadCol.setPrefWidth(260);
        threadCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().threadName()));
        view.vmOperationEventsTable().getColumns().setAll(List.of(timeCol, opCol, blockCol, safeCol, durCol, threadCol));
    }

    private <T> TableColumn<T, String> localizedColumn(String key) {
        TableColumn<T, String> column = new TableColumn<>();
        column.textProperty().bind(i18n.text(key));
        return column;
    }
}
