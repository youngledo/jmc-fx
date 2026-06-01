package com.youngledo.jmcfx.ui.profiling;

import com.youngledo.jmcfx.domain.model.DependencyGraphEdge;
import com.youngledo.jmcfx.domain.model.HotMethod;
import com.youngledo.jmcfx.domain.model.StackTreeNode;

import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tab;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/// Narrow view handle for the JFR Profiling split table/graph page.
public record ProfilingPageView(
        Label titleLabel,
        TableView<HotMethod> hotMethodsTable,
        Tab callGraphTab,
        HBox callGraphToolbar,
        ComboBox<CallGraphDirection> callGraphDirectionCombo,
        Label callGraphDepthLabel,
        Spinner<Integer> callGraphDepthSpinner,
        Button callGraphZoomOutButton,
        Button callGraphResetZoomButton,
        Button callGraphZoomInButton,
        Button callGraphFitButton,
        ScrollPane callGraphScrollPane,
        VBox callGraphContainer,
        Tab dependencyGraphTab,
        HBox dependencyToolbar,
        Label dependencyDepthLabel,
        Spinner<Integer> dependencyDepthSpinner,
        Button dependencyZoomOutButton,
        Button dependencyResetZoomButton,
        Button dependencyZoomInButton,
        Button dependencyFitButton,
        TableView<DependencyGraphEdge> dependencyTable,
        ScrollPane dependencyGraphScrollPane,
        VBox dependencyGraphContainer,
        Tab callersFlameTab,
        HBox callersFlameToolbar,
        Button callersFlameOrientationButton,
        Button callersFlameZoomOutButton,
        Button callersFlameResetZoomButton,
        Button callersFlameZoomInButton,
        Button callersFlameFitButton,
        VBox callersFlameContainer,
        Tab calleesFlameTab,
        HBox calleesFlameToolbar,
        Button calleesFlameOrientationButton,
        Button calleesFlameZoomOutButton,
        Button calleesFlameResetZoomButton,
        Button calleesFlameZoomInButton,
        Button calleesFlameFitButton,
        VBox calleesFlameContainer,
        Tab callersTab,
        TreeView<StackTreeNode> callersTree,
        Tab calleesTab,
        TreeView<StackTreeNode> calleesTree) {
}
