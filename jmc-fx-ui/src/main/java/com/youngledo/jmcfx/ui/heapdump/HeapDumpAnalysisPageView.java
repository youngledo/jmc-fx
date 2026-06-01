package com.youngledo.jmcfx.ui.heapdump;

import com.youngledo.jmcfx.domain.model.HeapDumpIssue;

import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;

/// Narrow view handle for the HPROF Heap Dump Analysis split table/detail page.
public record HeapDumpAnalysisPageView(
        Label titleLabel,
        TableView<HeapDumpIssue> issuesTable,
        TabPane detailsTabs,
        Tab issueDetailTab,
        Tab textReportTab,
        Label issueDetailTitleLabel,
        TextArea issueDetailArea,
        TextArea textReportArea) {
}
