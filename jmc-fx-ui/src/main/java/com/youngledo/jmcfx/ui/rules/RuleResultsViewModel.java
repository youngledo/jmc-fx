package com.youngledo.jmcfx.ui.rules;

import java.util.List;

import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.RuleResult;
import com.youngledo.jmcfx.domain.service.RuleAnalysisService;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/// View model for JFR rule analysis results.
///
/// Rule execution is delegated to `RuleAnalysisService` so the UI layer can
/// display analysis results without knowing which adapter produced them.
public class RuleResultsViewModel {

    private final RuleAnalysisService ruleAnalysisService;
    private final ObservableList<RuleResult> results = FXCollections.observableArrayList();
    private final ObjectProperty<RuleResult> selectedResult = new SimpleObjectProperty<>();

    public RuleResultsViewModel(RuleAnalysisService ruleAnalysisService) {
        this.ruleAnalysisService = ruleAnalysisService;
    }

    public ObservableList<RuleResult> resultsProperty() {
        return results;
    }

    public ObjectProperty<RuleResult> selectedResultProperty() {
        return selectedResult;
    }

    public void analyze(RecordingSummary recording) {
        List<RuleResult> analyzed = ruleAnalysisService.analyze(recording);
        results.setAll(analyzed);
        selectedResult.set(analyzed.isEmpty() ? null : analyzed.getFirst());
    }
}
