package io.github.youngledo.jmcfx.ui.profiling;

import java.util.List;

import io.github.youngledo.jmcfx.domain.model.DependencyGraphEdge;
import io.github.youngledo.jmcfx.domain.model.HotMethod;
import io.github.youngledo.jmcfx.domain.model.StackFrameInfo;
import io.github.youngledo.jmcfx.domain.model.StackTreeNode;
import io.github.youngledo.jmcfx.flamegraph.FlameGraphMode;
import io.github.youngledo.jmcfx.flamegraph.FlameGraphModel;
import io.github.youngledo.jmcfx.ui.i18n.I18n;
import io.github.youngledo.jmcfx.ui.util.DisplayFormats;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.ZoomEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.util.StringConverter;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2MZ;

/// Controller for the JFR Profiling split table/graph page.
public final class ProfilingPageController {

    private final ProfilingPageView view;
    private final I18n i18n;
    private ProfilingViewModel profilingViewModel;
    private CallGraphView profilingCallGraphView;
    private CallGraphView profilingDependencyGraphView;
    private io.github.youngledo.jmcfx.flamegraph.FlameGraphView<StackFrameInfo> profilingCallersFlameGraphView;
    private io.github.youngledo.jmcfx.flamegraph.FlameGraphView<StackFrameInfo> profilingCalleesFlameGraphView;
    private boolean callGraphZoomGestureActive;
    private final ChangeListener<StackTreeNode> callersTreeListener;
    private final ChangeListener<StackTreeNode> calleesTreeListener;
    private final ChangeListener<CallGraphLayout> callGraphListener;
    private final ChangeListener<CallGraphLayout> dependencyGraphListener;
    private final ChangeListener<FlameGraphModel<StackFrameInfo>> callersFlameGraphListener;
    private final ChangeListener<FlameGraphModel<StackFrameInfo>> calleesFlameGraphListener;

    public ProfilingPageController(ProfilingPageView view, I18n i18n) {
        this.view = view;
        this.i18n = i18n;
        callersTreeListener = (observable, oldValue, newValue) -> rebuildStackTree(view.callersTree(), newValue);
        calleesTreeListener = (observable, oldValue, newValue) -> rebuildStackTree(view.calleesTree(), newValue);
        callGraphListener = (observable, oldValue, newValue) -> profilingCallGraphView.setLayout(newValue);
        dependencyGraphListener = (observable, oldValue, newValue) -> profilingDependencyGraphView.setLayout(newValue);
        callersFlameGraphListener =
                (observable, oldValue, newValue) -> profilingCallersFlameGraphView.setModel(newValue);
        calleesFlameGraphListener =
                (observable, oldValue, newValue) -> profilingCalleesFlameGraphView.setModel(newValue);
    }

    public void configure() {
        bindLocalizedText();
        configureProfilingTable();
        bind(null);
    }

    public javafx.scene.control.TableView<HotMethod> table() {
        return view.hotMethodsTable();
    }

    public void bind(ProfilingViewModel nextViewModel) {
        ProfilingViewModel currentProfilingViewModel = profilingViewModel;
        if (currentProfilingViewModel != null) {
            currentProfilingViewModel.callGraphProperty().removeListener(callGraphListener);
            currentProfilingViewModel.dependencyGraphProperty().removeListener(dependencyGraphListener);
            currentProfilingViewModel.callersTreeProperty().removeListener(callersTreeListener);
            currentProfilingViewModel.calleesTreeProperty().removeListener(calleesTreeListener);
            currentProfilingViewModel.callersFlameGraphProperty().removeListener(callersFlameGraphListener);
            currentProfilingViewModel.calleesFlameGraphProperty().removeListener(calleesFlameGraphListener);
        }
        view.callersFlameSummaryLabel().textProperty().unbind();
        view.calleesFlameSummaryLabel().textProperty().unbind();
        view.callersFlameSummaryLabel().setText("");
        view.calleesFlameSummaryLabel().setText("");
        view.hotMethodsTable().setItems(FXCollections.emptyObservableList());
        view.dependencyTable().setItems(FXCollections.emptyObservableList());
        view.callersTree().setRoot(new TreeItem<>());
        view.calleesTree().setRoot(new TreeItem<>());
        profilingCallGraphView.setLayout(null);
        profilingDependencyGraphView.setLayout(null);
        profilingCallersFlameGraphView.setModel(null);
        profilingCalleesFlameGraphView.setModel(null);
        profilingViewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        view.hotMethodsTable().setItems(nextViewModel.hotMethodsProperty());
        view.dependencyTable().setItems(nextViewModel.dependencyEdgesProperty());
        nextViewModel.callGraphProperty().addListener(callGraphListener);
        nextViewModel.dependencyGraphProperty().addListener(dependencyGraphListener);
        nextViewModel.callersTreeProperty().addListener(callersTreeListener);
        nextViewModel.calleesTreeProperty().addListener(calleesTreeListener);
        nextViewModel.callersFlameGraphProperty().addListener(callersFlameGraphListener);
        nextViewModel.calleesFlameGraphProperty().addListener(calleesFlameGraphListener);
        rebuildStackTree(view.callersTree(), nextViewModel.callersTreeProperty().get());
        rebuildStackTree(view.calleesTree(), nextViewModel.calleesTreeProperty().get());
        profilingCallGraphView.setLayout(nextViewModel.callGraphProperty().get());
        profilingDependencyGraphView.setLayout(nextViewModel.dependencyGraphProperty().get());
        profilingCallersFlameGraphView.setModel(nextViewModel.callersFlameGraphProperty().get());
        profilingCalleesFlameGraphView.setModel(nextViewModel.calleesFlameGraphProperty().get());
        bindFlameGraphSummaryLabels(nextViewModel);
        view.callGraphDirectionCombo().getSelectionModel().select(nextViewModel.callGraphDirectionProperty().get());
        view.callGraphDepthSpinner().getValueFactory().setValue(nextViewModel.callGraphMaxDepthProperty().get());
        view.dependencyDepthSpinner().getValueFactory().setValue(nextViewModel.dependencyPackageDepthProperty().get());
    }

    private void bindLocalizedText() {
        view.titleLabel().textProperty().bind(i18n.text("profiling.title"));
        view.callGraphTab().textProperty().bind(i18n.text("profiling.tab.callGraph"));
        view.callGraphDirectionCombo().promptTextProperty().bind(i18n.text("profiling.callGraph.direction"));
        view.callGraphDepthLabel().textProperty().bind(i18n.text("profiling.callGraph.depth"));
        view.dependencyGraphTab().textProperty().bind(i18n.text("profiling.tab.dependencyGraph"));
        view.dependencyDepthLabel().textProperty().bind(i18n.text("profiling.dependency.depth"));
        view.callersFlameTab().textProperty().bind(i18n.text("profiling.tab.callersFlame"));
        view.calleesFlameTab().textProperty().bind(i18n.text("profiling.tab.calleesFlame"));
        view.callersFlameSearchField().promptTextProperty().bind(i18n.text("profiling.flame.search.prompt"));
        view.calleesFlameSearchField().promptTextProperty().bind(i18n.text("profiling.flame.search.prompt"));
        view.callersTab().textProperty().bind(i18n.text("profiling.tab.callers"));
        view.calleesTab().textProperty().bind(i18n.text("profiling.tab.callees"));
    }

    private void configureProfilingTable() {
        view.hotMethodsTable().setPlaceholder(localizedTablePlaceholder("profiling.empty"));
        profilingCallGraphView = new CallGraphView();
        profilingDependencyGraphView = new CallGraphView();
        profilingCallersFlameGraphView = new io.github.youngledo.jmcfx.flamegraph.FlameGraphView<>();
        profilingCalleesFlameGraphView = new io.github.youngledo.jmcfx.flamegraph.FlameGraphView<>();
        configureProfilingFlameGraphView(profilingCallersFlameGraphView);
        configureProfilingFlameGraphView(profilingCalleesFlameGraphView);
        profilingCallGraphView.emptyTextProperty().bind(i18n.text("profiling.callGraph.empty"));
        profilingDependencyGraphView.emptyTextProperty().bind(i18n.text("profiling.dependency.empty"));
        profilingCallersFlameGraphView.emptyTextProperty().bind(i18n.text("profiling.flame.empty"));
        profilingCalleesFlameGraphView.emptyTextProperty().bind(i18n.text("profiling.flame.empty"));
        view.callGraphContainer().getChildren().setAll(profilingCallGraphView);
        view.dependencyGraphContainer().getChildren().setAll(profilingDependencyGraphView);
        view.callersFlameContainer().getChildren().setAll(profilingCallersFlameGraphView);
        view.calleesFlameContainer().getChildren().setAll(profilingCalleesFlameGraphView);
        bindFlameGraphSummaryLabelVisibility(view.callersFlameSummaryLabel());
        bindFlameGraphSummaryLabelVisibility(view.calleesFlameSummaryLabel());
        view.callGraphDirectionCombo().setItems(FXCollections.observableArrayList(CallGraphDirection.values()));
        view.callGraphDirectionCombo().setButtonCell(callGraphDirectionCell());
        view.callGraphDirectionCombo().setCellFactory(combo -> callGraphDirectionCell());
        view.callGraphDirectionCombo().setConverter(new StringConverter<>() {
            @Override
            public String toString(CallGraphDirection direction) {
                return formatCallGraphDirection(direction);
            }

            @Override
            public CallGraphDirection fromString(String value) {
                return null;
            }
        });
        i18n.localeProperty().addListener((obs, old, val) -> refreshProfilingCallGraphDirectionLabel());
        view.callGraphDirectionCombo().getSelectionModel().select(CallGraphDirection.CALLEES);
        view.callGraphDirectionCombo().getSelectionModel().selectedItemProperty()
                .addListener((obs, old, val) -> {
                    if (profilingViewModel != null && val != null) {
                        profilingViewModel.setCallGraphDirection(val);
                    }
                });
        view.callGraphDepthSpinner().setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        1, 6, CallGraphLayoutBuilder.DEFAULT_MAX_DEPTH));
        view.callGraphDepthSpinner().valueProperty()
                .addListener((obs, old, val) -> {
                    if (profilingViewModel != null && val != null) {
                        profilingViewModel.setCallGraphMaxDepth(val);
                    }
                });
        configureGraphZoomButtons(profilingCallGraphView,
                view.callGraphZoomOutButton(),
                view.callGraphResetZoomButton(),
                view.callGraphZoomInButton(),
                view.callGraphFitButton());
        configureCallGraphGestures(profilingCallGraphView, view.callGraphScrollPane());
        view.dependencyDepthSpinner().setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 6, 2));
        view.dependencyDepthSpinner().valueProperty()
                .addListener((obs, old, val) -> {
                    if (profilingViewModel != null && val != null) {
                        profilingViewModel.setDependencyPackageDepth(val);
                    }
                });
        configureGraphZoomButtons(profilingDependencyGraphView,
                view.dependencyZoomOutButton(),
                view.dependencyResetZoomButton(),
                view.dependencyZoomInButton(),
                view.dependencyFitButton());
        configureCallGraphGestures(profilingDependencyGraphView, view.dependencyGraphScrollPane());
        configureFlameGraphButtons(profilingCallersFlameGraphView,
                view.callersFlameOrientationButton(),
                view.callersFlameZoomOutButton(),
                view.callersFlameResetZoomButton(),
                view.callersFlameZoomInButton(),
                view.callersFlameFitButton());
        configureFlameGraphSearch(profilingCallersFlameGraphView,
                view.callersFlameSearchField(),
                view.callersFlameSearchStatusLabel(),
                view.callersFlamePreviousMatchButton(),
                view.callersFlameNextMatchButton(),
                view.callersFlameClearSearchButton());
        configureFlameGraphGestures(profilingCallersFlameGraphView);
        bindFlameGraphToolbarVisibility(view.callersFlameToolbar(), profilingCallersFlameGraphView);
        configureFlameGraphButtons(profilingCalleesFlameGraphView,
                view.calleesFlameOrientationButton(),
                view.calleesFlameZoomOutButton(),
                view.calleesFlameResetZoomButton(),
                view.calleesFlameZoomInButton(),
                view.calleesFlameFitButton());
        configureFlameGraphSearch(profilingCalleesFlameGraphView,
                view.calleesFlameSearchField(),
                view.calleesFlameSearchStatusLabel(),
                view.calleesFlamePreviousMatchButton(),
                view.calleesFlameNextMatchButton(),
                view.calleesFlameClearSearchButton());
        configureFlameGraphGestures(profilingCalleesFlameGraphView);
        bindFlameGraphToolbarVisibility(view.calleesFlameToolbar(), profilingCalleesFlameGraphView);

        TableColumn<HotMethod, String> methodCol = new TableColumn<>();
        methodCol.textProperty().bind(i18n.text("profiling.column.method"));
        methodCol.setPrefWidth(620);
        methodCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().method()));

        TableColumn<HotMethod, String> frameTypeCol = new TableColumn<>();
        frameTypeCol.textProperty().bind(i18n.text("profiling.column.frameType"));
        frameTypeCol.setPrefWidth(100);
        frameTypeCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().frameType()));

        TableColumn<HotMethod, Number> countCol = new TableColumn<>();
        countCol.textProperty().bind(i18n.text("profiling.column.count"));
        countCol.setPrefWidth(80);
        countCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleIntegerProperty(cell.getValue().count()));
        useFormattedIntegerCells(countCol);

        TableColumn<HotMethod, String> pctCol = new TableColumn<>();
        pctCol.textProperty().bind(i18n.text("profiling.column.percentage"));
        pctCol.setPrefWidth(80);
        pctCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatPercent(cell.getValue().percentage())));

        view.hotMethodsTable().getColumns().setAll(List.of(methodCol, frameTypeCol, countCol, pctCol));
        view.hotMethodsTable().getSelectionModel().selectedItemProperty()
                .addListener((obs, old, val) -> selectProfilingMethod(val));

        TableColumn<DependencyGraphEdge, String> sourceCol = new TableColumn<>();
        sourceCol.textProperty().bind(i18n.text("profiling.dependency.column.source"));
        sourceCol.setPrefWidth(150);
        sourceCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().source()));

        TableColumn<DependencyGraphEdge, String> targetCol = new TableColumn<>();
        targetCol.textProperty().bind(i18n.text("profiling.dependency.column.target"));
        targetCol.setPrefWidth(150);
        targetCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().target()));

        TableColumn<DependencyGraphEdge, Number> dependencyCountCol = new TableColumn<>();
        dependencyCountCol.textProperty().bind(i18n.text("profiling.dependency.column.count"));
        dependencyCountCol.setPrefWidth(80);
        dependencyCountCol.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleIntegerProperty(cell.getValue().count()));
        useFormattedIntegerCells(dependencyCountCol);

        TableColumn<DependencyGraphEdge, String> dependencyPctCol = new TableColumn<>();
        dependencyPctCol.textProperty().bind(i18n.text("profiling.dependency.column.percentage"));
        dependencyPctCol.setPrefWidth(90);
        dependencyPctCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatPercent(cell.getValue().percentage())));

        view.dependencyTable().setPlaceholder(localizedTablePlaceholder("profiling.dependency.empty"));
        view.dependencyTable().getColumns().setAll(List.of(sourceCol, targetCol, dependencyCountCol, dependencyPctCol));
        view.dependencyTable().getSelectionModel().selectedItemProperty()
                .addListener((obs, old, val) -> {
                    if (profilingViewModel != null) {
                        profilingViewModel.selectedDependencyEdgeProperty().set(val);
                    }
                });

        view.callersTree().setShowRoot(false);
        view.callersTree().setCellFactory(tree -> new javafx.scene.control.TreeCell<>() {
            @Override
            protected void updateItem(StackTreeNode item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.method() + " (" + item.count() + ")");
            }
        });
        view.calleesTree().setShowRoot(false);
        view.calleesTree().setCellFactory(tree -> new javafx.scene.control.TreeCell<>() {
            @Override
            protected void updateItem(StackTreeNode item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.method() + " (" + item.count() + ")");
            }
        });
    }

    private void selectProfilingMethod(HotMethod method) {
        if (profilingViewModel == null) {
            return;
        }
        profilingViewModel.selectMethod(method);
    }

    private void rebuildStackTree(TreeView<StackTreeNode> tree, StackTreeNode root) {
        if (root == null || root == StackTreeNode.EMPTY) {
            tree.setRoot(new TreeItem<>());
            return;
        }
        TreeItem<StackTreeNode> rootItem = toStackTreeNodeItem(root);
        rootItem.setExpanded(true);
        tree.setRoot(rootItem);
    }

    private TreeItem<StackTreeNode> toStackTreeNodeItem(StackTreeNode node) {
        TreeItem<StackTreeNode> item = new TreeItem<>(node);
        if (node.children() != null) {
            node.children().stream()
                    .map(this::toStackTreeNodeItem)
                    .forEach(item.getChildren()::add);
        }
        return item;
    }

    private String formatCallGraphDirection(CallGraphDirection direction) {
        if (direction == null) {
            return "";
        }
        return switch (direction) {
            case CALLERS -> i18n.text("profiling.callGraph.direction.callers").get();
            case CALLEES -> i18n.text("profiling.callGraph.direction.callees").get();
        };
    }

    private void refreshProfilingCallGraphDirectionLabel() {
        CallGraphDirection selectedDirection = view.callGraphDirectionCombo().getSelectionModel().getSelectedItem();
        view.callGraphDirectionCombo().setButtonCell(callGraphDirectionCell());
        view.callGraphDirectionCombo().setCellFactory(combo -> callGraphDirectionCell());
        view.callGraphDirectionCombo().getSelectionModel().select(selectedDirection);
    }

    private ListCell<CallGraphDirection> callGraphDirectionCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(CallGraphDirection direction, boolean empty) {
                super.updateItem(direction, empty);
                setText(empty ? null : formatCallGraphDirection(direction));
            }
        };
    }

    private void configureGraphZoomButtons(CallGraphView graphView, Button zoomOutButton,
            Button resetZoomButton, Button zoomInButton, Button fitButton) {
        configureIconButton(zoomOutButton, Material2MZ.ZOOM_OUT, "profiling.graph.zoomOut");
        configureIconButton(resetZoomButton, Material2MZ.REFRESH, "profiling.graph.resetZoom");
        configureIconButton(zoomInButton, Material2MZ.ZOOM_IN, "profiling.graph.zoomIn");
        configureIconButton(fitButton, Material2MZ.ZOOM_OUT_MAP, "profiling.graph.fit");
        zoomOutButton.setOnAction(event -> graphView.zoomOut());
        resetZoomButton.setOnAction(event -> graphView.resetZoom());
        zoomInButton.setOnAction(event -> graphView.zoomIn());
        fitButton.setOnAction(event -> graphView.fitToWidth(graphViewportWidth(graphView)));
    }

    private void configureCallGraphGestures(CallGraphView graphView, ScrollPane scrollPane) {
        scrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (callGraphZoomGestureActive) {
                event.consume();
                return;
            }
            if (event.isShortcutDown()) {
                zoomCallGraphAt(graphView, scrollPane, event.getX(), event.getY(),
                        event.getDeltaY() > 0 ? 1.1 : 1 / 1.1);
                event.consume();
                return;
            }
            panCallGraphViewport(scrollPane, event.getDeltaX(), event.getDeltaY());
            event.consume();
        });
        scrollPane.addEventFilter(ZoomEvent.ZOOM_STARTED, event -> {
            callGraphZoomGestureActive = true;
            event.consume();
        });
        scrollPane.addEventFilter(ZoomEvent.ZOOM, event -> {
            zoomCallGraphAt(graphView, scrollPane, event.getX(), event.getY(), event.getZoomFactor());
            event.consume();
        });
        scrollPane.addEventFilter(ZoomEvent.ZOOM_FINISHED, event -> {
            callGraphZoomGestureActive = false;
            event.consume();
        });
        scrollPane.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                zoomCallGraphAt(graphView, scrollPane, event.getX(), event.getY(), 1.1);
                event.consume();
            }
        });
    }

    private void zoomCallGraphAt(CallGraphView graphView, ScrollPane scrollPane,
            double viewportX, double viewportY, double factor) {
        double oldContentWidth = scrollContentWidth(scrollPane);
        double oldContentHeight = scrollContentHeight(scrollPane);
        double viewportWidth = scrollPane.getViewportBounds().getWidth();
        double viewportHeight = scrollPane.getViewportBounds().getHeight();

        graphView.zoomBy(factor);

        double newContentWidth = scrollContentWidth(scrollPane);
        double newContentHeight = scrollContentHeight(scrollPane);
        scrollPane.setHvalue(scrollValueAfterZoom(scrollPane.getHvalue(),
                oldContentWidth, newContentWidth, viewportWidth, viewportX));
        scrollPane.setVvalue(scrollValueAfterZoom(scrollPane.getVvalue(),
                oldContentHeight, newContentHeight, viewportHeight, viewportY));
    }

    private double scrollContentWidth(ScrollPane scrollPane) {
        return Math.max(scrollPane.getContent().getBoundsInLocal().getWidth(),
                scrollPane.getContent().prefWidth(-1));
    }

    private double scrollContentHeight(ScrollPane scrollPane) {
        return Math.max(scrollPane.getContent().getBoundsInLocal().getHeight(),
                scrollPane.getContent().prefHeight(-1));
    }

    public static double scrollValueAfterZoom(double currentValue, double oldContentSize, double newContentSize,
            double viewportSize, double viewportCoordinate) {
        double oldScrollableSize = Math.max(0, oldContentSize - viewportSize);
        double newScrollableSize = Math.max(0, newContentSize - viewportSize);
        if (oldContentSize <= 0 || viewportSize <= 0 || newScrollableSize <= 0) {
            return 0;
        }
        double anchorInViewport = Math.clamp(viewportCoordinate, 0, viewportSize);
        double anchorInContent = (Math.clamp(currentValue, 0, 1) * oldScrollableSize) + anchorInViewport;
        double scaledAnchorInContent = anchorInContent * (newContentSize / oldContentSize);
        return Math.clamp((scaledAnchorInContent - anchorInViewport) / newScrollableSize, 0, 1);
    }

    private void panCallGraphViewport(ScrollPane scrollPane, double deltaX, double deltaY) {
        double horizontalRange = Math.max(0, scrollPane.getContent().getBoundsInLocal().getWidth()
                - scrollPane.getViewportBounds().getWidth());
        double verticalRange = Math.max(0, scrollPane.getContent().getBoundsInLocal().getHeight()
                - scrollPane.getViewportBounds().getHeight());
        if (horizontalRange > 0) {
            scrollPane.setHvalue(Math.clamp(scrollPane.getHvalue() - deltaX / horizontalRange,
                    scrollPane.getHmin(), scrollPane.getHmax()));
        }
        if (verticalRange > 0) {
            scrollPane.setVvalue(Math.clamp(scrollPane.getVvalue() - deltaY / verticalRange,
                    scrollPane.getVmin(), scrollPane.getVmax()));
        }
    }

    private void configureFlameGraphButtons(
            io.github.youngledo.jmcfx.flamegraph.FlameGraphView<StackFrameInfo> graphView,
            Button orientationButton,
            Button zoomOutButton, Button resetZoomButton, Button zoomInButton, Button fitButton) {
        configureIconButton(orientationButton, Material2MZ.SWAP_VERT, "profiling.flame.orientation");
        configureIconButton(zoomOutButton, Material2MZ.ZOOM_OUT, "profiling.graph.zoomOut");
        configureIconButton(resetZoomButton, Material2MZ.REFRESH, "profiling.graph.resetZoom");
        configureIconButton(zoomInButton, Material2MZ.ZOOM_IN, "profiling.graph.zoomIn");
        configureIconButton(fitButton, Material2MZ.ZOOM_OUT_MAP, "profiling.graph.fit");
        orientationButton.setOnAction(event -> toggleFlameGraphOrientation(graphView));
        zoomOutButton.setOnAction(event -> graphView.zoomOut());
        resetZoomButton.setOnAction(event -> graphView.resetZoom());
        zoomInButton.setOnAction(event -> graphView.zoomIn());
        fitButton.setOnAction(event -> graphView.fitToWidth(graphViewportWidth(graphView)));
    }

    private void bindFlameGraphToolbarVisibility(
            HBox toolbar,
            io.github.youngledo.jmcfx.flamegraph.FlameGraphView<StackFrameInfo> graphView) {
        toolbar.visibleProperty().bind(graphView.hasFramesProperty());
        toolbar.managedProperty().bind(toolbar.visibleProperty());
    }

    private void bindFlameGraphSummaryLabels(ProfilingViewModel viewModel) {
        var summaryBinding = Bindings.createStringBinding(
                () -> flameGraphSummaryText(viewModel.flameGraphEventCountProperty().get()),
                viewModel.flameGraphEventCountProperty(),
                i18n.localeProperty());
        view.callersFlameSummaryLabel().textProperty().bind(summaryBinding);
        view.calleesFlameSummaryLabel().textProperty().bind(summaryBinding);
    }

    private void bindFlameGraphSummaryLabelVisibility(Label label) {
        label.visibleProperty().bind(label.textProperty().isNotEmpty());
        label.managedProperty().bind(label.visibleProperty());
    }

    private String flameGraphSummaryText(Integer eventCount) {
        if (eventCount == null || eventCount <= 0) {
            return "";
        }
        String count = Integer.toString(eventCount);
        return i18n.format("profiling.flame.summary.methodProfilingSample", count);
    }

    private void configureFlameGraphSearch(
            io.github.youngledo.jmcfx.flamegraph.FlameGraphView<StackFrameInfo> graphView,
            TextField searchField,
            Label statusLabel,
            Button previousButton,
            Button nextButton,
            Button clearButton) {
        configureIconButton(previousButton, Material2MZ.NAVIGATE_BEFORE, "profiling.flame.search.previous");
        configureIconButton(nextButton, Material2MZ.NAVIGATE_NEXT, "profiling.flame.search.next");
        configureIconButton(clearButton, Material2AL.CLEAR, "profiling.flame.search.clear");
        graphView.setFocusTraversable(true);
        graphView.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isShortcutDown() && event.getCode() == KeyCode.F) {
                searchField.requestFocus();
                searchField.selectAll();
                event.consume();
            }
        });
        searchField.textProperty().addListener((obs, old, query) -> {
            if (query == null || query.isBlank()) {
                graphView.clearSearch();
                return;
            }
            graphView.search(query);
        });
        previousButton.setOnAction(event -> graphView.previousMatch());
        nextButton.setOnAction(event -> graphView.nextMatch());
        clearButton.setOnAction(event -> searchField.clear());
        previousButton.disableProperty().bind(graphView.matchCountProperty().isEqualTo(0));
        nextButton.disableProperty().bind(graphView.matchCountProperty().isEqualTo(0));
        clearButton.disableProperty().bind(searchField.textProperty().isEmpty());
        statusLabel.textProperty().bind(Bindings.createStringBinding(
                () -> flameGraphSearchStatus(searchField, graphView),
                searchField.textProperty(),
                graphView.matchCountProperty(),
                graphView.currentMatchIndexProperty(),
                i18n.localeProperty()));
    }

    private String flameGraphSearchStatus(
            TextField searchField,
            io.github.youngledo.jmcfx.flamegraph.FlameGraphView<StackFrameInfo> graphView) {
        if (searchField.getText() == null || searchField.getText().isBlank()) {
            return "";
        }
        int count = graphView.matchCountProperty().get();
        if (count == 0) {
            return i18n.get("profiling.flame.search.noMatches");
        }
        return i18n.format("profiling.flame.search.matchStatus",
                graphView.currentMatchIndexProperty().get() + 1, count);
    }

    private void configureFlameGraphGestures(
            io.github.youngledo.jmcfx.flamegraph.FlameGraphView<StackFrameInfo> graphView) {
        graphView.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.isShortcutDown()) {
                graphView.zoomBy(event.getDeltaY() > 0 ? 1.1 : 1 / 1.1,
                        event.getX() / Math.max(1, graphView.getWidth()));
                event.consume();
                return;
            }
            if (shouldPanFlameGraphHorizontally(event)) {
                double delta = event.isShiftDown() && Math.abs(event.getDeltaX()) < Math.abs(event.getDeltaY())
                        ? event.getDeltaY()
                        : event.getDeltaX();
                graphView.setViewportOffsetX(graphView.viewportOffsetXProperty().get()
                        - delta / Math.max(1, graphView.getWidth()));
                event.consume();
            }
        });
    }

    private boolean shouldPanFlameGraphHorizontally(ScrollEvent event) {
        if (event.isShiftDown()) {
            return Math.abs(event.getDeltaY()) > 0 || Math.abs(event.getDeltaX()) > 0;
        }
        return Math.abs(event.getDeltaX()) > Math.abs(event.getDeltaY());
    }

    private void toggleFlameGraphOrientation(
            io.github.youngledo.jmcfx.flamegraph.FlameGraphView<StackFrameInfo> graphView) {
        graphView.setMode(graphView.getMode() == FlameGraphMode.ICICLE
                ? FlameGraphMode.FLAME
                : FlameGraphMode.ICICLE);
    }

    private void configureProfilingFlameGraphView(
            io.github.youngledo.jmcfx.flamegraph.FlameGraphView<StackFrameInfo> graphView) {
        graphView.setTextProvider(ProfilingFlameGraphAdapter.textProvider());
        graphView.setTooltipProvider(ProfilingFlameGraphAdapter.tooltipProvider());
        graphView.setColorProvider(ProfilingFlameGraphAdapter.colorProvider());
    }

    private double graphViewportWidth(Region graphView) {
        for (Node parent = graphView.getParent(); parent != null; parent = parent.getParent()) {
            if (parent instanceof ScrollPane scrollPane) {
                return scrollPane.getViewportBounds().getWidth();
            }
        }
        return graphView.getWidth();
    }

    private void configureIconButton(Button button, Ikon icon, String tooltipKey) {
        button.setGraphic(new FontIcon(icon));
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setTooltip(i18n.tooltip(tooltipKey));
        button.accessibleTextProperty().bind(i18n.text(tooltipKey));
    }

    private Label localizedTablePlaceholder(String key) {
        Label label = new Label();
        label.textProperty().bind(i18n.text(key));
        return label;
    }

    private static <T> void useFormattedIntegerCells(TableColumn<T, Number> column) {
        column.setCellFactory(ignored -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : DisplayFormats.formatInteger(item.longValue()));
            }
        });
    }
}
