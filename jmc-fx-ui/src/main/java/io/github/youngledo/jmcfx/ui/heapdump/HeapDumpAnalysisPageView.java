package io.github.youngledo.jmcfx.ui.heapdump;

import io.github.youngledo.jmcfx.domain.model.HeapDumpIssue;
import io.github.youngledo.jmcfx.domain.model.HeapDumpIssueCategory;
import io.github.youngledo.jmcfx.domain.model.HeapDumpObjectGroup;
import io.github.youngledo.jmcfx.domain.model.HeapDumpObjectSummary;
import io.github.youngledo.jmcfx.domain.model.HeapDumpReferencePath;

import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;

/// Narrow view handle for the HPROF Heap Dump Analysis split table/detail page.
public record HeapDumpAnalysisPageView(
        Label titleLabel,
        ComboBox<HeapDumpIssueCategory> categoryFilterCombo,
        Button clearCategoryFilterButton,
        TableView<HeapDumpIssue> issuesTable,
        TabPane detailsTabs,
        Tab issueDetailTab,
        Tab objectGroupsTab,
        Tab referencePathsTab,
        Tab textReportTab,
        Label issueDetailTitleLabel,
        TextArea issueDetailArea,
        TableView<HeapDumpObjectGroup> objectGroupsTable,
        Button loadReferencePathsButton,
        TableView<HeapDumpReferencePath> referencePathsTable,
        Label objectGroupDetailTitleLabel,
        Label objectGroupMetaLabel,
        TextArea objectGroupDetailArea,
        TableView<HeapDumpObjectSummary> objectGroupObjectsTable,
        TextArea textReportArea) {
}
