package com.youngledo.jmcfx.ui.advanced;

import com.youngledo.jmcfx.domain.model.MemoryIssue;

import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

/// Narrow view handle for the Advanced JFR tabbed analysis page.
public record AdvancedJfrPageView(
        Label titleLabel,
        Label summaryLabel,
        Tab heatmapTab,
        Tab memoryTab,
        VBox heatmapContainer,
        Label selectionTitleLabel,
        Label selectedEventTypeCaptionLabel,
        Label selectedEventTypeLabel,
        Label selectedCountCaptionLabel,
        Label selectedCountLabel,
        Label memorySummaryLabel,
        TableView<MemoryIssue> memoryTable,
        Label memoryDetailTitleLabel,
        TextArea memoryDetailArea) {
}
