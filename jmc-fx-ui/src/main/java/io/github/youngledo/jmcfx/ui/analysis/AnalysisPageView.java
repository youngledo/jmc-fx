package io.github.youngledo.jmcfx.ui.analysis;

import io.github.youngledo.jmcfx.domain.model.RuleResult;

import javafx.scene.control.CheckBox;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

/// Narrow view handle for the recording Analysis page.
public record AnalysisPageView(
        Label titleLabel,
        TextField searchField,
        Label minimumScoreLabel,
        Spinner<Integer> minimumScoreSpinner,
        CheckBox showOkCheckBox,
        CheckBox showIgnoredCheckBox,
        CheckBox showUnavailableCheckBox,
        TableView<RuleResult> table,
        Label detailExplanationCaption,
        TextArea detailExplanationArea,
        Label detailEvidenceCaption,
        TextArea detailEvidenceArea,
        Label detailRecommendationCaption,
        TextArea detailRecommendationArea,
        Label aiTitleLabel,
        Button aiAnalyzeButton,
        AiReportView aiReportView,
        HBox aiActionBar,
        TabPane detailTabs) {
}
