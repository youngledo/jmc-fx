package io.github.youngledo.jmcfx.ui.analysis;

import io.github.youngledo.jmcfx.domain.model.DiagnosticFinding;

import javafx.scene.control.CheckBox;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

/// Narrow view handle for the recording Analysis page.
public record AnalysisPageView(
        Label titleLabel,
        Label findingsSummaryLabel,
        Label highestSeverityLabel,
        Label ruleAnalysisStatusLabel,
        Label aiStatusLabel,
        TextField searchField,
        Label minimumScoreLabel,
        Spinner<Integer> minimumScoreSpinner,
        CheckBox showOkCheckBox,
        CheckBox showIgnoredCheckBox,
        CheckBox showUnavailableCheckBox,
        TableView<DiagnosticFinding> table,
        Label detailTitleLabel,
        Label detailMetaLabel,
        Label detailExplanationCaption,
        Label detailExplanationArea,
        Label detailRecommendationCaption,
        Label detailRecommendationArea,
        Label aiTitleLabel,
        Button aiAnalyzeButton,
        AiReportView aiReportView,
        HBox aiActionBar,
        TabPane detailTabs) {
}
