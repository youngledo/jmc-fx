package com.youngledo.jmcfx.ui.rules;

import java.util.List;
import java.util.Set;

import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.RuleResult;
import com.youngledo.jmcfx.domain.model.Severity;
import com.youngledo.jmcfx.domain.service.RuleAnalysisService;
import com.youngledo.jmcfx.ui.util.FxDispatch;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

/// View model for JFR rule analysis results with severity filtering.
public class RuleResultsViewModel {

    private final RuleAnalysisService ruleAnalysisService;
    private final ObservableList<RuleResult> allResults = FXCollections.observableArrayList();
    private final FilteredList<RuleResult> results = new FilteredList<>(allResults);
    private final ObjectProperty<RuleResult> selectedResult = new SimpleObjectProperty<>();
    private final ObjectProperty<Set<Severity>> visibleSeverities =
            new SimpleObjectProperty<>(Set.of(Severity.WARNING, Severity.CRITICAL, Severity.INFO));

    public RuleResultsViewModel(RuleAnalysisService ruleAnalysisService) {
        this.ruleAnalysisService = ruleAnalysisService;
        visibleSeverities.addListener((obs, old, val) -> updateFilter());
        updateFilter();
    }

    public ObservableList<RuleResult> resultsProperty() {
        return results;
    }

    public ObjectProperty<RuleResult> selectedResultProperty() {
        return selectedResult;
    }

    public ObjectProperty<Set<Severity>> visibleSeveritiesProperty() {
        return visibleSeverities;
    }

    public void analyze(RecordingSummary recording) {
        List<RuleResult> analyzed = ruleAnalysisService.analyze(recording);
        FxDispatch.run(() -> {
            allResults.setAll(analyzed);
            updateFilter();
            selectedResult.set(results.isEmpty() ? null : results.getFirst());
        });
    }

    private void updateFilter() {
        Set<Severity> visible = visibleSeverities.get();
        results.setPredicate(visible == null || visible.isEmpty() ? null : r -> visible.contains(r.severity()));
    }
}
