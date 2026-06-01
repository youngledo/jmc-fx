package com.youngledo.jmcfx.ui.leaks;

import java.util.List;

import com.youngledo.jmcfx.domain.model.LeakCandidate;
import com.youngledo.jmcfx.domain.model.LeakReferenceNode;
import com.youngledo.jmcfx.ui.i18n.I18n;
import com.youngledo.jmcfx.ui.util.DisplayFormats;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;

/// Controller for the JFR Leak Suspects split table/tree page.
public final class LeakSuspectsPageController {

    private final LeakSuspectsPageView view;
    private final I18n i18n;
    private final ChangeListener<LeakReferenceNode> referenceTreeListener;
    private LeakSuspectsViewModel viewModel;

    public LeakSuspectsPageController(LeakSuspectsPageView view, I18n i18n) {
        this.view = view;
        this.i18n = i18n;
        referenceTreeListener = (observable, oldValue, newValue) -> updateReferenceTree(newValue);
    }

    public void configure() {
        bindLocalizedText();
        configureTable();
        configureReferenceTree();
        bind(null);
    }

    public TableView<LeakCandidate> table() {
        return view.table();
    }

    public void bind(LeakSuspectsViewModel nextViewModel) {
        LeakSuspectsViewModel currentViewModel = viewModel;
        if (currentViewModel != null) {
            currentViewModel.referenceTreeProperty().removeListener(referenceTreeListener);
        }
        view.table().setItems(FXCollections.emptyObservableList());
        view.referenceTree().setRoot(null);
        viewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        view.table().setItems(nextViewModel.candidatesProperty());
        nextViewModel.referenceTreeProperty().addListener(referenceTreeListener);
        view.table().getSelectionModel().selectFirst();
        updateReferenceTree(nextViewModel.referenceTreeProperty().get());
    }

    private void bindLocalizedText() {
        view.titleLabel().textProperty().bind(i18n.text("leaks.title"));
    }

    private void configureTable() {
        view.table().setPlaceholder(localizedTablePlaceholder("leaks.empty"));

        TableColumn<LeakCandidate, String> objectCol = new TableColumn<>();
        objectCol.textProperty().bind(i18n.text("leaks.column.object"));
        objectCol.setPrefWidth(300);
        objectCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().object()));

        TableColumn<LeakCandidate, Number> countCol = new TableColumn<>();
        countCol.textProperty().bind(i18n.text("leaks.column.count"));
        countCol.setPrefWidth(80);
        countCol.setCellValueFactory(
                cell -> new javafx.beans.property.SimpleIntegerProperty(cell.getValue().count()));
        useFormattedIntegerCells(countCol);

        TableColumn<LeakCandidate, String> descCol = new TableColumn<>();
        descCol.textProperty().bind(i18n.text("leaks.column.description"));
        descCol.setPrefWidth(200);
        descCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().description()));

        TableColumn<LeakCandidate, String> addressCol = new TableColumn<>();
        addressCol.textProperty().bind(i18n.text("leaks.column.address"));
        addressCol.setPrefWidth(100);
        addressCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().address()));

        TableColumn<LeakCandidate, String> relevanceCol = new TableColumn<>();
        relevanceCol.textProperty().bind(i18n.text("leaks.column.relevance"));
        relevanceCol.setPrefWidth(120);
        relevanceCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatPercent(cell.getValue().relevance())));

        view.table().getColumns().setAll(List.of(objectCol, countCol, descCol, addressCol, relevanceCol));
        view.table().getSelectionModel().selectedItemProperty()
                .addListener((obs, old, val) -> {
                    if (val != null && viewModel != null) {
                        int idx = view.table().getItems().indexOf(val);
                        viewModel.selectCandidate(idx);
                    }
                });
    }

    private void configureReferenceTree() {
        view.referenceTree().setShowRoot(false);
        view.referenceTree().setCellFactory(tree -> new javafx.scene.control.TreeCell<>() {
            @Override
            protected void updateItem(LeakReferenceNode item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.object());
            }
        });
    }

    private void updateReferenceTree(LeakReferenceNode node) {
        if (node == null || node == LeakReferenceNode.EMPTY) {
            view.referenceTree().setRoot(null);
            return;
        }
        TreeItem<LeakReferenceNode> root = buildReferenceTreeItem(node);
        root.setExpanded(true);
        view.referenceTree().setRoot(root);
    }

    private TreeItem<LeakReferenceNode> buildReferenceTreeItem(LeakReferenceNode node) {
        TreeItem<LeakReferenceNode> item = new TreeItem<>(node);
        for (LeakReferenceNode child : node.children()) {
            item.getChildren().add(buildReferenceTreeItem(child));
        }
        return item;
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
