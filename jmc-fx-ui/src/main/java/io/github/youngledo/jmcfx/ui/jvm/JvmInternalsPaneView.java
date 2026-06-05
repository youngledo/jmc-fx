package io.github.youngledo.jmcfx.ui.jvm;

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
import io.github.youngledo.jmcfx.ui.chart.TimelineChart;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/// Code-first view for JVM Internals recording pages.
public final class JvmInternalsPaneView {

    private final TableView<JvmFlag> jvmFlagsTable = denseTable();
    private final TableView<JvmFlagChange> jvmFlagChangesTable = denseTable();
    private final TableView<GcSummary> gcSummaryTable = denseTable();
    private final TableView<GcEvent> gcEventsTable = denseTable();
    private final TableView<GcReferenceStat> gcReferenceStatsTable = denseTable();
    private final TableView<GcHeapSummary> gcHeapSummaryTable = denseTable();
    private final VBox gcHeapChartContainer = new VBox();
    private final TimelineChart gcHeapChart = new TimelineChart();
    private final VBox gcMetaspaceChartContainer = new VBox();
    private final TimelineChart gcMetaspaceChart = new TimelineChart();
    private final VBox gcPauseChartContainer = new VBox();
    private final TimelineChart gcPauseChart = new TimelineChart();
    private final TableView<CompilationEvent> compilationsTable = denseTable();
    private final VBox compilationDurationChartContainer = new VBox();
    private final TimelineChart compilationDurationChart = new TimelineChart();
    private final TableView<CompilationEvent> compilationFailuresTable = denseTable();
    private final TableView<CodeCacheSweep> codeCacheSweepsTable = denseTable();
    private final VBox codeCacheEntriesChartContainer = new VBox();
    private final TimelineChart codeCacheEntriesChart = new TimelineChart();
    private final VBox codeCacheSweepChartContainer = new VBox();
    private final TimelineChart codeCacheSweepChart = new TimelineChart();
    private final TableView<CodeCacheStats> codeCacheStatsTable = denseTable();
    private final TableView<ClassloaderSummary> classLoadingHistogramTable = denseTable();
    private final VBox classLoadingChartContainer = new VBox();
    private final TimelineChart classLoadingChart = new TimelineChart();
    private final TableView<ClassloadEvent> classLoadingEventsTable = denseTable();
    private final TableView<ClassloaderStatistics> classLoadingStatsTable = denseTable();
    private final TableView<VmOperationSummary> vmOperationSummaryTable = denseTable();
    private final TableView<VmOperationEvent> vmOperationEventsTable = denseTable();
    private final Label jvmInfoTitleLabel = new Label();
    private final Label jvmFlagsLabel = new Label();
    private final Label jvmFlagChangesLabel = new Label();
    private final Label gcConfigTitleLabel = new Label();
    private final Label gcConfigDescriptionLabel = new Label();
    private final Label gcSummaryTitleLabel = new Label();
    private final Label gcDetailsTitleLabel = new Label();
    private final Label gcDetailsRecordingContextLabel = new Label();
    private final Button gcDetailsClearTimeRangeButton = new Button();
    private final HBox gcDetailsRecordingContextBar = new HBox();
    private final Label gcEventsLabel = new Label();
    private final Label gcReferenceStatsLabel = new Label();
    private final Label gcHeapSummaryLabel = new Label();
    private final Label compilationsTitleLabel = new Label();
    private final Label compilationsRecordingContextLabel = new Label();
    private final Button compilationsClearTimeRangeButton = new Button();
    private final HBox compilationsRecordingContextBar = new HBox();
    private final Label compilationEventsLabel = new Label();
    private final Label compilationFailuresLabel = new Label();
    private final Label codeCacheTitleLabel = new Label();
    private final Label codeCacheRecordingContextLabel = new Label();
    private final Button codeCacheClearTimeRangeButton = new Button();
    private final HBox codeCacheRecordingContextBar = new HBox();
    private final Label codeCacheSweepsLabel = new Label();
    private final Label codeCacheStatsLabel = new Label();
    private final Label classLoadingTitleLabel = new Label();
    private final Label classLoadingRecordingContextLabel = new Label();
    private final Button classLoadingClearTimeRangeButton = new Button();
    private final HBox classLoadingRecordingContextBar = new HBox();
    private final Label classLoadingHistogramLabel = new Label();
    private final Label classLoadingEventsLabel = new Label();
    private final Label classLoadingStatsLabel = new Label();
    private final Label vmOperationsTitleLabel = new Label();
    private final Label vmOperationSummaryLabel = new Label();
    private final Label vmOperationEventsLabel = new Label();

    public JvmInternalsPaneView(VBox jvmInfoPane, VBox gcConfigPane, VBox gcSummaryPane,
            VBox gcDetailsPane, VBox compilationsPane, VBox codeCachePane,
            VBox classLoadingPane, VBox vmOperationsPane) {
        configure(jvmInfoPane, gcConfigPane, gcSummaryPane, gcDetailsPane,
                compilationsPane, codeCachePane, classLoadingPane, vmOperationsPane);
    }

    public JvmInternalsPagesView view() {
        return new JvmInternalsPagesView(jvmInfoTitleLabel, jvmFlagsLabel, jvmFlagChangesLabel,
                jvmFlagsTable, jvmFlagChangesTable, gcConfigTitleLabel, gcConfigDescriptionLabel,
                gcSummaryTitleLabel, gcSummaryTable, gcDetailsTitleLabel, gcDetailsRecordingContextLabel,
                gcDetailsClearTimeRangeButton,
                gcHeapChart, gcMetaspaceChart, gcPauseChart, gcEventsLabel, gcEventsTable, gcReferenceStatsLabel,
                gcReferenceStatsTable, gcHeapSummaryLabel, gcHeapSummaryTable, compilationsTitleLabel,
                compilationsRecordingContextLabel, compilationsClearTimeRangeButton,
                compilationDurationChart, compilationEventsLabel,
                compilationsTable, compilationFailuresLabel, compilationFailuresTable, codeCacheTitleLabel,
                codeCacheRecordingContextLabel, codeCacheClearTimeRangeButton,
                codeCacheEntriesChart, codeCacheSweepChart, codeCacheSweepsLabel,
                codeCacheSweepsTable, codeCacheStatsLabel, codeCacheStatsTable, classLoadingTitleLabel,
                classLoadingRecordingContextLabel, classLoadingClearTimeRangeButton,
                classLoadingChart, classLoadingHistogramLabel,
                classLoadingHistogramTable, classLoadingEventsLabel, classLoadingEventsTable,
                classLoadingStatsLabel, classLoadingStatsTable, vmOperationsTitleLabel, vmOperationSummaryLabel,
                vmOperationSummaryTable, vmOperationEventsLabel, vmOperationEventsTable);
    }

    private void configure(VBox jvmInfoPane, VBox gcConfigPane, VBox gcSummaryPane,
            VBox gcDetailsPane, VBox compilationsPane, VBox codeCachePane,
            VBox classLoadingPane, VBox vmOperationsPane) {
        configureTablePage(jvmInfoPane, jvmInfoTitleLabel, jvmFlagsLabel, jvmFlagsTable,
                jvmFlagChangesLabel, jvmFlagChangesTable);
        configureTablePage(gcConfigPane, gcConfigTitleLabel, gcConfigDescriptionLabel);
        wrap(gcConfigDescriptionLabel);
        configureTablePage(gcSummaryPane, gcSummaryTitleLabel, gcSummaryTable);
        gcHeapChartContainer.getChildren().setAll(gcHeapChart);
        gcMetaspaceChartContainer.getChildren().setAll(gcMetaspaceChart);
        gcPauseChartContainer.getChildren().setAll(gcPauseChart);
        gcDetailsRecordingContextBar.setSpacing(8);
        gcDetailsRecordingContextBar.getChildren().setAll(gcDetailsRecordingContextLabel,
                gcDetailsClearTimeRangeButton);
        styles(gcDetailsRecordingContextLabel, "detail-panel-meta");
        styles(gcDetailsRecordingContextBar, "page-toolbar");
        configureTablePage(gcDetailsPane, gcDetailsTitleLabel, gcDetailsRecordingContextBar,
                gcHeapChartContainer, gcMetaspaceChartContainer, gcPauseChartContainer, gcEventsLabel, gcEventsTable,
                gcReferenceStatsLabel, gcReferenceStatsTable, gcHeapSummaryLabel, gcHeapSummaryTable);
        compilationDurationChartContainer.getChildren().setAll(compilationDurationChart);
        compilationsRecordingContextBar.setSpacing(8);
        compilationsRecordingContextBar.getChildren().setAll(compilationsRecordingContextLabel,
                compilationsClearTimeRangeButton);
        styles(compilationsRecordingContextLabel, "detail-panel-meta");
        styles(compilationsRecordingContextBar, "page-toolbar");
        configureTablePage(compilationsPane, compilationsTitleLabel, compilationsRecordingContextBar,
                compilationDurationChartContainer, compilationEventsLabel, compilationsTable, compilationFailuresLabel,
                compilationFailuresTable);
        codeCacheEntriesChartContainer.getChildren().setAll(codeCacheEntriesChart);
        codeCacheSweepChartContainer.getChildren().setAll(codeCacheSweepChart);
        codeCacheRecordingContextBar.setSpacing(8);
        codeCacheRecordingContextBar.getChildren().setAll(codeCacheRecordingContextLabel,
                codeCacheClearTimeRangeButton);
        styles(codeCacheRecordingContextLabel, "detail-panel-meta");
        styles(codeCacheRecordingContextBar, "page-toolbar");
        configureTablePage(codeCachePane, codeCacheTitleLabel, codeCacheRecordingContextBar,
                codeCacheEntriesChartContainer, codeCacheSweepChartContainer, codeCacheSweepsLabel, codeCacheSweepsTable,
                codeCacheStatsLabel, codeCacheStatsTable);
        classLoadingChartContainer.getChildren().setAll(classLoadingChart);
        classLoadingRecordingContextBar.setSpacing(8);
        classLoadingRecordingContextBar.getChildren().setAll(classLoadingRecordingContextLabel,
                classLoadingClearTimeRangeButton);
        styles(classLoadingRecordingContextLabel, "detail-panel-meta");
        styles(classLoadingRecordingContextBar, "page-toolbar");
        configureTablePage(classLoadingPane, classLoadingTitleLabel, classLoadingRecordingContextBar,
                classLoadingChartContainer, classLoadingHistogramLabel, classLoadingHistogramTable,
                classLoadingEventsLabel, classLoadingEventsTable, classLoadingStatsLabel, classLoadingStatsTable);
        configureTablePage(vmOperationsPane, vmOperationsTitleLabel, vmOperationSummaryLabel,
                vmOperationSummaryTable, vmOperationEventsLabel, vmOperationEventsTable);
    }

    private void configureTablePage(VBox pane, Label title, Node... content) {
        pane.setSpacing(8);
        styles(title, "view-title");
        pane.getChildren().setAll(title);
        pane.getChildren().addAll(content);
        for (Node node : content) {
            if (node instanceof TableView<?> || node instanceof TabPane || node instanceof SplitPane) {
                VBox.setVgrow(node, Priority.ALWAYS);
            }
        }
    }

    private static <T> TableView<T> denseTable() {
        TableView<T> table = new TableView<>();
        styles(table, "dense-table");
        return table;
    }

    private static void wrap(Label... labels) {
        for (Label label : labels) {
            label.setWrapText(true);
        }
    }

    private static void styles(Node node, String... styleClasses) {
        node.getStyleClass().addAll(styleClasses);
    }
}
