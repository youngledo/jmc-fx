package com.youngledo.jmcfx.ui.jvm;

import com.youngledo.jmcfx.domain.model.ClassloadEvent;
import com.youngledo.jmcfx.domain.model.ClassloaderStatistics;
import com.youngledo.jmcfx.domain.model.ClassloaderSummary;
import com.youngledo.jmcfx.domain.model.CodeCacheStats;
import com.youngledo.jmcfx.domain.model.CodeCacheSweep;
import com.youngledo.jmcfx.domain.model.CompilationEvent;
import com.youngledo.jmcfx.domain.model.GcEvent;
import com.youngledo.jmcfx.domain.model.GcHeapSummary;
import com.youngledo.jmcfx.domain.model.GcReferenceStat;
import com.youngledo.jmcfx.domain.model.GcSummary;
import com.youngledo.jmcfx.domain.model.JvmFlag;
import com.youngledo.jmcfx.domain.model.JvmFlagChange;
import com.youngledo.jmcfx.domain.model.VmOperationEvent;
import com.youngledo.jmcfx.domain.model.VmOperationSummary;
import com.youngledo.jmcfx.ui.chart.TimelineChart;

import javafx.scene.control.Label;
import javafx.scene.control.TableView;

/// Narrow view handle for JVM Internals data table and timeline pages.
public record JvmInternalsPagesView(
        Label jvmInfoTitleLabel,
        Label jvmFlagsLabel,
        Label jvmFlagChangesLabel,
        TableView<JvmFlag> jvmFlagsTable,
        TableView<JvmFlagChange> jvmFlagChangesTable,
        Label gcConfigTitleLabel,
        Label gcConfigDescriptionLabel,
        Label gcSummaryTitleLabel,
        TableView<GcSummary> gcSummaryTable,
        Label gcDetailsTitleLabel,
        TimelineChart gcHeapChart,
        TimelineChart gcMetaspaceChart,
        TimelineChart gcPauseChart,
        Label gcEventsLabel,
        TableView<GcEvent> gcEventsTable,
        Label gcReferenceStatsLabel,
        TableView<GcReferenceStat> gcReferenceStatsTable,
        Label gcHeapSummaryLabel,
        TableView<GcHeapSummary> gcHeapSummaryTable,
        Label compilationsTitleLabel,
        TimelineChart compilationDurationChart,
        Label compilationEventsLabel,
        TableView<CompilationEvent> compilationsTable,
        Label compilationFailuresLabel,
        TableView<CompilationEvent> compilationFailuresTable,
        Label codeCacheTitleLabel,
        TimelineChart codeCacheEntriesChart,
        TimelineChart codeCacheSweepChart,
        Label codeCacheSweepsLabel,
        TableView<CodeCacheSweep> codeCacheSweepsTable,
        Label codeCacheStatsLabel,
        TableView<CodeCacheStats> codeCacheStatsTable,
        Label classLoadingTitleLabel,
        TimelineChart classLoadingChart,
        Label classLoadingHistogramLabel,
        TableView<ClassloaderSummary> classLoadingHistogramTable,
        Label classLoadingEventsLabel,
        TableView<ClassloadEvent> classLoadingEventsTable,
        Label classLoadingStatsLabel,
        TableView<ClassloaderStatistics> classLoadingStatsTable,
        Label vmOperationsTitleLabel,
        Label vmOperationSummaryLabel,
        TableView<VmOperationSummary> vmOperationSummaryTable,
        Label vmOperationEventsLabel,
        TableView<VmOperationEvent> vmOperationEventsTable) {
}
