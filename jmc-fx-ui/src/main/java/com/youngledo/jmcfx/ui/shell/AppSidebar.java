package com.youngledo.jmcfx.ui.shell;

import java.util.Locale;
import java.util.function.Consumer;

import com.youngledo.jmcfx.ui.i18n.I18n;

import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class AppSidebar extends VBox {

    private I18n i18n;
    private AppNavTree navTree;
    private Label title;
    private Label subtitle;
    private Label version;
    private TextField searchField;
    private final ContextMenu searchResultsMenu = new ContextMenu();

    public AppSidebar() {
        this(new I18n(java.util.Locale.getDefault()));
    }

    public AppSidebar(I18n i18n) {
        this.i18n = i18n;
        this.navTree = new AppNavTree(i18n);
        setPrefWidth(270);
        setId("sidebar");
        getStyleClass().add("app-sidebar");
        VBox.setVgrow(navTree, Priority.ALWAYS);
        getChildren().setAll(createHeader(), navTree, createFooter());
        bindLocalizedText();
    }

    void bind(AppShellViewModel viewModel) {
        navTree.bind(viewModel);
    }

    void setNavigationHandler(Consumer<String> navigationHandler) {
        navTree.setNavigationHandler(navigationHandler);
    }

    void setI18n(I18n i18n) {
        this.i18n = i18n;
        navTree.setI18n(i18n);
        bindLocalizedText();
    }

    private void bindLocalizedText() {
        if (title != null) {
            title.textProperty().bind(i18n.text("sidebar.brand"));
        }
        if (subtitle != null) {
            subtitle.textProperty().bind(i18n.text("sidebar.subtitle"));
        }
        if (searchField != null) {
            searchField.promptTextProperty().bind(i18n.text("sidebar.searchWorkflows"));
            searchField.setTooltip(i18n.tooltip("sidebar.searchWorkflows"));
        }
        if (version != null) {
            version.textProperty().bind(i18n.text("sidebar.version"));
        }
    }

    private VBox createHeader() {
        VBox header = new VBox();
        header.getStyleClass().add("sidebar-header");
        header.getChildren().setAll(createBrand(), createSearchField());
        return header;
    }

    private HBox createBrand() {
        StackPane mark = new StackPane(new Label(i18n.get("sidebar.productMark")));
        mark.getStyleClass().add("sidebar-product-mark");

        title = new Label("JMC FX");
        title.getStyleClass().add("sidebar-title");
        subtitle = new Label("Flight Recorder diagnostics");
        subtitle.getStyleClass().add("sidebar-subtitle");
        VBox copy = new VBox(title, subtitle);
        copy.getStyleClass().add("sidebar-brand-copy");

        HBox brand = new HBox(mark, copy);
        brand.getStyleClass().add("sidebar-brand");
        brand.setAlignment(Pos.CENTER_LEFT);
        return brand;
    }

    private TextField createSearchField() {
        searchField = new TextField();
        searchField.getStyleClass().add("sidebar-search");
        searchField.setMaxWidth(Double.MAX_VALUE);
        searchField.setAccessibleText("Search workflows");
        searchField.textProperty().addListener((observable, oldValue, newValue) -> updateSearchResults(newValue));
        searchField.setOnAction(event -> navigateFirstSearchResult());
        searchField.focusedProperty().addListener((observable, oldValue, focused) -> {
            if (!focused) {
                searchResultsMenu.hide();
            }
        });
        return searchField;
    }

    private HBox createFooter() {
        version = new Label("v0.1.0-SNAPSHOT");
        version.getStyleClass().add("sidebar-footer-text");
        HBox footer = new HBox(version);
        footer.getStyleClass().add("sidebar-footer");
        return footer;
    }

    private void updateSearchResults(String query) {
        if (!shouldSearchQueryImmediately(i18n, query)) {
            searchResultsMenu.hide();
            return;
        }
        java.util.List<AppNavSearchResult> results = navTree.search(query);
        searchResultsMenu.getItems().setAll(results.stream()
                .limit(8)
                .map(this::searchMenuItem)
                .toList());
        if (searchResultsMenu.getItems().isEmpty()) {
            searchResultsMenu.hide();
            return;
        }
        if (searchField.isFocused()) {
            searchResultsMenu.show(searchField, javafx.geometry.Side.BOTTOM, 0, 2);
        }
    }

    private MenuItem searchMenuItem(AppNavSearchResult result) {
        MenuItem item = new MenuItem(result.title());
        item.setOnAction(event -> {
            navTree.navigateToSection(result.sectionId());
            searchField.clear();
            searchResultsMenu.hide();
        });
        return item;
    }

    private void navigateFirstSearchResult() {
        if (!shouldSearchQueryImmediately(i18n, searchField.getText())) {
            return;
        }
        navTree.search(searchField.getText()).stream().findFirst().ifPresent(result -> {
            navTree.navigateToSection(result.sectionId());
            searchField.clear();
            searchResultsMenu.hide();
        });
    }

    static boolean shouldSearchQueryImmediately(I18n i18n, String query) {
        if (query == null || query.isBlank()) {
            return false;
        }
        String trimmedQuery = query.trim();
        if (trimmedQuery.codePointCount(0, trimmedQuery.length()) < 2) {
            return false;
        }
        if (!Locale.SIMPLIFIED_CHINESE.equals(i18n.localeProperty().get())) {
            return true;
        }
        return !trimmedQuery.chars().allMatch(AppSidebar::isAsciiLowercaseLetter);
    }

    private static boolean isAsciiLowercaseLetter(int codePoint) {
        return codePoint >= 'a' && codePoint <= 'z';
    }
}
