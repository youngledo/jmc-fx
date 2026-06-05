package io.github.youngledo.jmcfx.ui.profiling;

import io.github.youngledo.jmcfx.domain.model.DependencyGraphEdge;
import io.github.youngledo.jmcfx.domain.model.HotMethod;
import io.github.youngledo.jmcfx.domain.model.StackTreeNode;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/// Code-first view for the JFR Profiling split table/graph page.
public final class ProfilingPaneView {

    private final Label titleLabel = new Label();
    private final TableView<HotMethod> hotMethodsTable = denseTable();
    private final TabPane detailTabs = new TabPane();
    private final Tab callGraphTab = tab();
    private final HBox callGraphToolbar = new HBox();
    private final ComboBox<CallGraphDirection> callGraphDirectionCombo = new ComboBox<>();
    private final Label callGraphDepthLabel = new Label();
    private final Spinner<Integer> callGraphDepthSpinner = new Spinner<>();
    private final Button callGraphZoomOutButton = new Button();
    private final Button callGraphResetZoomButton = new Button();
    private final Button callGraphZoomInButton = new Button();
    private final Button callGraphFitButton = new Button();
    private final ScrollPane callGraphScrollPane = new ScrollPane();
    private final VBox callGraphContainer = new VBox();
    private final Tab dependencyGraphTab = tab();
    private final HBox dependencyToolbar = new HBox();
    private final Label dependencyDepthLabel = new Label();
    private final Spinner<Integer> dependencyDepthSpinner = new Spinner<>();
    private final Button dependencyZoomOutButton = new Button();
    private final Button dependencyResetZoomButton = new Button();
    private final Button dependencyZoomInButton = new Button();
    private final Button dependencyFitButton = new Button();
    private final TableView<DependencyGraphEdge> dependencyTable = denseTable();
    private final ScrollPane dependencyGraphScrollPane = new ScrollPane();
    private final VBox dependencyGraphContainer = new VBox();
    private final Tab callersFlameTab = tab();
    private final HBox callersFlameToolbar = new HBox();
    private final Button callersFlameOrientationButton = new Button();
    private final TextField callersFlameSearchField = searchField();
    private final Label callersFlameSearchStatusLabel = searchStatusLabel();
    private final Button callersFlamePreviousMatchButton = new Button();
    private final Button callersFlameNextMatchButton = new Button();
    private final Button callersFlameClearSearchButton = new Button();
    private final Button callersFlameZoomOutButton = new Button();
    private final Button callersFlameResetZoomButton = new Button();
    private final Button callersFlameZoomInButton = new Button();
    private final Button callersFlameFitButton = new Button();
    private final VBox callersFlameContainer = new VBox();
    private final Label callersFlameSummaryLabel = new Label();
    private final Tab calleesFlameTab = tab();
    private final HBox calleesFlameToolbar = new HBox();
    private final Button calleesFlameOrientationButton = new Button();
    private final TextField calleesFlameSearchField = searchField();
    private final Label calleesFlameSearchStatusLabel = searchStatusLabel();
    private final Button calleesFlamePreviousMatchButton = new Button();
    private final Button calleesFlameNextMatchButton = new Button();
    private final Button calleesFlameClearSearchButton = new Button();
    private final Button calleesFlameZoomOutButton = new Button();
    private final Button calleesFlameResetZoomButton = new Button();
    private final Button calleesFlameZoomInButton = new Button();
    private final Button calleesFlameFitButton = new Button();
    private final VBox calleesFlameContainer = new VBox();
    private final Label calleesFlameSummaryLabel = new Label();
    private final Tab callersTab = tab();
    private final TreeView<StackTreeNode> callersTree = new TreeView<>();
    private final Tab calleesTab = tab();
    private final TreeView<StackTreeNode> calleesTree = new TreeView<>();

    public ProfilingPaneView(VBox pane) {
        configure(pane);
    }

    public ProfilingPageView view() {
        return new ProfilingPageView(titleLabel, hotMethodsTable, callGraphTab,
                callGraphToolbar, callGraphDirectionCombo, callGraphDepthLabel,
                callGraphDepthSpinner, callGraphZoomOutButton, callGraphResetZoomButton,
                callGraphZoomInButton, callGraphFitButton, callGraphScrollPane,
                callGraphContainer, dependencyGraphTab, dependencyToolbar,
                dependencyDepthLabel, dependencyDepthSpinner, dependencyZoomOutButton,
                dependencyResetZoomButton, dependencyZoomInButton, dependencyFitButton,
                dependencyTable, dependencyGraphScrollPane, dependencyGraphContainer,
                callersFlameTab, callersFlameToolbar, callersFlameOrientationButton,
                callersFlameSearchField, callersFlameSearchStatusLabel, callersFlamePreviousMatchButton,
                callersFlameNextMatchButton, callersFlameClearSearchButton,
                callersFlameZoomOutButton, callersFlameResetZoomButton,
                callersFlameZoomInButton, callersFlameFitButton, callersFlameContainer,
                callersFlameSummaryLabel,
                calleesFlameTab, calleesFlameToolbar, calleesFlameOrientationButton,
                calleesFlameSearchField, calleesFlameSearchStatusLabel, calleesFlamePreviousMatchButton,
                calleesFlameNextMatchButton, calleesFlameClearSearchButton,
                calleesFlameZoomOutButton, calleesFlameResetZoomButton,
                calleesFlameZoomInButton, calleesFlameFitButton, calleesFlameContainer,
                calleesFlameSummaryLabel,
                callersTab, callersTree, calleesTab, calleesTree);
    }

    private void configure(VBox pane) {
        pane.setSpacing(8);
        styles(titleLabel, "view-title");
        configureProfilingTab(callGraphTab, callGraphToolbar,
                new Node[] { callGraphDirectionCombo, callGraphDepthLabel,
                        callGraphDepthSpinner, callGraphZoomOutButton,
                        callGraphResetZoomButton, callGraphZoomInButton, callGraphFitButton },
                callGraphScrollPane, callGraphContainer, false);
        configureProfilingTab(dependencyGraphTab, dependencyToolbar,
                new Node[] { dependencyDepthLabel, dependencyDepthSpinner,
                        dependencyZoomOutButton, dependencyResetZoomButton,
                        dependencyZoomInButton, dependencyFitButton },
                dependencyGraphScrollPane, dependencyGraphContainer, true);
        configureFlameTab(callersFlameTab, callersFlameToolbar, callersFlameContainer, callersFlameSummaryLabel,
                callersFlameOrientationButton, callersFlameSearchField,
                callersFlameSearchStatusLabel, callersFlamePreviousMatchButton, callersFlameNextMatchButton,
                callersFlameClearSearchButton, callersFlameZoomOutButton,
                callersFlameResetZoomButton, callersFlameZoomInButton, callersFlameFitButton);
        configureFlameTab(calleesFlameTab, calleesFlameToolbar, calleesFlameContainer, calleesFlameSummaryLabel,
                calleesFlameOrientationButton, calleesFlameSearchField,
                calleesFlameSearchStatusLabel, calleesFlamePreviousMatchButton, calleesFlameNextMatchButton,
                calleesFlameClearSearchButton, calleesFlameZoomOutButton,
                calleesFlameResetZoomButton, calleesFlameZoomInButton, calleesFlameFitButton);
        tab(callersTab, callersTree);
        tab(calleesTab, calleesTree);
        detailTabs.getTabs().setAll(callersFlameTab, calleesFlameTab,
                callGraphTab, dependencyGraphTab, callersTab, calleesTab);
        SplitPane profilingSplit = new SplitPane(hotMethodsTable, detailTabs);
        profilingSplit.setOrientation(javafx.geometry.Orientation.VERTICAL);
        profilingSplit.setDividerPositions(0.45);
        VBox.setVgrow(profilingSplit, Priority.ALWAYS);
        pane.getChildren().setAll(titleLabel, profilingSplit);
    }

    private void configureProfilingTab(Tab tab, HBox toolbar, Node[] controls, ScrollPane scrollPane,
            VBox container, boolean includeTable) {
        toolbar.setSpacing(8);
        styles(toolbar, "page-toolbar", "profiling-graph-toolbar");
        toolbar.getChildren().setAll(controls);
        styles(container, "profiling-call-graph-container");
        scrollPane.setContent(container);
        scrollPane.setPannable(true);
        if (includeTable) {
            SplitPane split = new SplitPane(dependencyTable, scrollPane);
            split.setDividerPositions(0.35);
            tab(tab, vbox(8, toolbar, split));
        } else {
            tab(tab, vbox(8, toolbar, scrollPane));
        }
    }

    private void configureFlameTab(Tab tab, HBox toolbar, VBox container, Label summaryLabel, Node... buttons) {
        toolbar.setSpacing(8);
        styles(toolbar, "page-toolbar", "profiling-graph-toolbar");
        toolbar.getChildren().setAll(buttons);
        styles(container, "profiling-flame-container");
        container.setFillWidth(true);
        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        container.prefWidthProperty().bind(scrollPane.viewportBoundsProperty().map(bounds -> bounds.getWidth()));
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        styles(summaryLabel, "profiling-flame-summary");
        tab(tab, vbox(8, toolbar, scrollPane, summaryLabel));
    }

    private static VBox vbox(double spacing, Node... children) {
        return new VBox(spacing, children);
    }

    private static Tab tab() {
        Tab tab = new Tab();
        tab.setClosable(false);
        return tab;
    }

    private static TextField searchField() {
        TextField field = new TextField();
        field.setPrefColumnCount(18);
        field.setMinWidth(140);
        field.setPrefWidth(180);
        field.setMaxWidth(220);
        return field;
    }

    private static Label searchStatusLabel() {
        Label label = new Label();
        label.setMinWidth(56);
        return label;
    }

    private static void tab(Tab tab, Node content) {
        tab.setContent(content);
        tab.setClosable(false);
    }

    private static <T> TableView<T> denseTable() {
        TableView<T> table = new TableView<>();
        styles(table, "dense-table");
        return table;
    }

    private static void styles(Node node, String... styleClasses) {
        node.getStyleClass().addAll(styleClasses);
    }
}
