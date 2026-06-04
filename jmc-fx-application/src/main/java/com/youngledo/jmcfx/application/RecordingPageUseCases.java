package com.youngledo.jmcfx.application;

import java.util.Objects;

public record RecordingPageUseCases(
        OpenRecordingWorkspaceUseCase openRecordingWorkspace,
        BrowseEventsUseCase browseEvents,
        AnalyzeRulesUseCase analyzeRules,
        RecordingApplicationServices pageQueries) {

    public RecordingPageUseCases {
        Objects.requireNonNull(openRecordingWorkspace, "openRecordingWorkspace");
        Objects.requireNonNull(browseEvents, "browseEvents");
        Objects.requireNonNull(analyzeRules, "analyzeRules");
        Objects.requireNonNull(pageQueries, "pageQueries");
    }

    public static RecordingPageUseCases from(RecordingApplicationServices services) {
        Objects.requireNonNull(services, "services");
        return new RecordingPageUseCases(
                new OpenRecordingWorkspaceUseCase(services),
                new BrowseEventsUseCase(services.eventQueryService()),
                new AnalyzeRulesUseCase(services.ruleAnalysisService()),
                services);
    }
}
