package com.youngledo.jmcfx.ui.shell;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

final class RecordingPageCatalog {

    private static final String DEFAULT_SECTION_ID = "analysis";
    private static final List<RecordingPageDescriptor> PAGES = List.of(
            page("analysis", "recording", "analysis.title", RecordingPageTemplate.SPLIT_TABLE_DETAIL, 0.36,
                    column("score", "analysis.column.score", 96, true),
                    column("rule", "analysis.column.rule", 240, false),
                    column("summary", "analysis.column.summary", 360, false)),
            page("overview", "recording", "overview.title", RecordingPageTemplate.OVERVIEW, 0.50),
            page("events", "recording", "events.title", RecordingPageTemplate.SPLIT_TABLE_DETAIL, 0.34,
                    column("eventType", "events.column.eventType", 260, false),
                    column("startTime", "events.column.startTime", 180, false),
                    column("duration", "events.column.duration", 120, true)),
            page("metadata", "recording", "nav.metadata", RecordingPageTemplate.SPLIT_TABLE_DETAIL, 0.32,
                    column("name", "metadata.column.name", 240, false),
                    column("type", "metadata.column.type", 180, false)),
            page("advancedJfr", "recording", "nav.advancedJfr", RecordingPageTemplate.VISUAL_ANALYSIS, 0.50),
            page("javaApplication", "javaApplication", "javaApplication.title", RecordingPageTemplate.OVERVIEW, 0.50),
            page("profiling", "javaApplication", "nav.profiling", RecordingPageTemplate.SPLIT_TABLE_DETAIL, 0.38),
            page("exceptions", "javaApplication", "nav.exceptions", RecordingPageTemplate.SPLIT_TABLE_DETAIL, 0.38),
            page("threads", "javaApplication", "nav.threads", RecordingPageTemplate.SPLIT_TABLE_DETAIL, 0.40),
            page("fileio", "javaApplication", "nav.fileio", RecordingPageTemplate.SPLIT_TABLE_DETAIL, 0.38),
            page("socketio", "javaApplication", "nav.socketio", RecordingPageTemplate.SPLIT_TABLE_DETAIL, 0.38),
            page("locks", "javaApplication", "nav.locks", RecordingPageTemplate.SPLIT_TABLE_DETAIL, 0.38),
            page("threadHistogram", "javaApplication", "nav.threadHistogram", RecordingPageTemplate.DATA_TABLE, 0.50),
            page("security", "javaApplication", "nav.security", RecordingPageTemplate.SPLIT_TABLE_DETAIL, 0.38),
            page("threadDumps", "javaApplication", "nav.threadDumps", RecordingPageTemplate.SPLIT_TABLE_DETAIL, 0.38),
            page("heap", "javaApplication", "nav.heap", RecordingPageTemplate.SPLIT_TABLE_DETAIL, 0.38),
            page("leaks", "javaApplication", "nav.leaks", RecordingPageTemplate.SPLIT_TABLE_DETAIL, 0.38),
            page("jvmInternals", "jvmInternals", "jvmInternals.title", RecordingPageTemplate.OVERVIEW, 0.50),
            page("jvmInfo", "jvmInternals", "nav.jvmInfo", RecordingPageTemplate.DATA_TABLE, 0.50),
            page("gcConfig", "jvmInternals", "nav.gcConfig", RecordingPageTemplate.DATA_TABLE, 0.50),
            page("gcSummary", "jvmInternals", "nav.gcSummary", RecordingPageTemplate.SPLIT_TABLE_DETAIL, 0.38),
            page("gcDetails", "jvmInternals", "nav.gcDetails", RecordingPageTemplate.SPLIT_TABLE_DETAIL, 0.38),
            page("g1Gc", "jvmInternals", "nav.g1Gc", RecordingPageTemplate.VISUAL_ANALYSIS, 0.50),
            page("javaFxEvents", "jvmInternals", "nav.javaFxEvents", RecordingPageTemplate.SPLIT_TABLE_DETAIL, 0.38),
            page("compilations", "jvmInternals", "nav.compilations", RecordingPageTemplate.SPLIT_TABLE_DETAIL, 0.38),
            page("codeCache", "jvmInternals", "nav.codeCache", RecordingPageTemplate.SPLIT_TABLE_DETAIL, 0.38),
            page("classLoading", "jvmInternals", "nav.classLoading", RecordingPageTemplate.SPLIT_TABLE_DETAIL, 0.38),
            page("vmOperations", "jvmInternals", "nav.vmOperations", RecordingPageTemplate.SPLIT_TABLE_DETAIL, 0.38),
            page("tlab", "jvmInternals", "nav.tlab", RecordingPageTemplate.SPLIT_TABLE_DETAIL, 0.38),
            page("environment", "environment", "environment.title", RecordingPageTemplate.OVERVIEW, 0.50),
            page("processes", "environment", "nav.processes", RecordingPageTemplate.DATA_TABLE, 0.50),
            page("envVars", "environment", "nav.envVars", RecordingPageTemplate.DATA_TABLE, 0.50),
            page("sysProps", "environment", "nav.sysProps", RecordingPageTemplate.DATA_TABLE, 0.50),
            page("nativeLibraries", "environment", "nav.nativeLibraries", RecordingPageTemplate.DATA_TABLE, 0.50),
            page("recordingInfo", "environment", "nav.recordingInfo", RecordingPageTemplate.SPLIT_TABLE_DETAIL, 0.38),
            page("agents", "environment", "nav.agents", RecordingPageTemplate.SPLIT_TABLE_DETAIL, 0.38),
            page("constantPools", "environment", "nav.constantPools", RecordingPageTemplate.SPLIT_TABLE_DETAIL, 0.38));

    private static final Map<String, RecordingPageDescriptor> BY_ID = PAGES.stream()
            .collect(Collectors.toUnmodifiableMap(RecordingPageDescriptor::id, Function.identity()));

    private RecordingPageCatalog() {
    }

    static String defaultSectionId() {
        return DEFAULT_SECTION_ID;
    }

    static List<RecordingPageDescriptor> pages() {
        return PAGES;
    }

    static Optional<RecordingPageDescriptor> page(String sectionId) {
        return Optional.ofNullable(BY_ID.get(sectionId));
    }

    static boolean contains(String sectionId) {
        return BY_ID.containsKey(sectionId);
    }

    private static RecordingPageDescriptor page(String id, String groupId, String titleKey,
            RecordingPageTemplate template, double defaultSplitPosition,
            RecordingPageColumnDescriptor... defaultColumns) {
        return new RecordingPageDescriptor(id, groupId, titleKey, template, defaultSplitPosition,
                List.of(defaultColumns));
    }

    private static RecordingPageColumnDescriptor column(String id, String titleKey, double defaultWidth,
            boolean numeric) {
        return new RecordingPageColumnDescriptor(id, titleKey, defaultWidth, numeric);
    }
}
