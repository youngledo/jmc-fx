package com.youngledo.jmcfx.ui.leaks;

import com.youngledo.jmcfx.domain.model.LeakCandidate;
import com.youngledo.jmcfx.domain.model.LeakReferenceNode;

import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeView;

/// Narrow view handle for the JFR Leak Suspects split table/tree page.
public record LeakSuspectsPageView(
        Label titleLabel,
        TableView<LeakCandidate> table,
        TreeView<LeakReferenceNode> referenceTree) {
}
