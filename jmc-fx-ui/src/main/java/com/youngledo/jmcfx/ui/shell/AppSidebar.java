package com.youngledo.jmcfx.ui.shell;

import java.util.function.Consumer;

import com.youngledo.jmcfx.ui.i18n.I18n;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2MZ;

import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Styles;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class AppSidebar extends VBox {

    private I18n i18n;
    private AppNavTree navTree;
    private final Label recordingNameLabel = new Label();
    private Label title;
    private Label subtitle;
    private Label currentRecordingLabel;
    private Label version;
    private Button searchButton;
    private Label searchTitleLabel;

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
        getChildren().setAll(createHeader(), createRecordingCard(), navTree, createFooter());
        bindLocalizedText();
    }

    void bind(AppShellViewModel viewModel) {
        recordingNameLabel.textProperty().bind(viewModel.currentRecordingNameProperty());
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
        if (searchButton != null) {
            searchButton.setTooltip(i18n.tooltip("sidebar.searchWorkflows"));
        }
        if (searchTitleLabel != null) {
            searchTitleLabel.textProperty().bind(i18n.text("sidebar.searchWorkflows"));
        }
        if (currentRecordingLabel != null) {
            currentRecordingLabel.textProperty().bind(i18n.text("sidebar.currentRecording"));
        }
        if (version != null) {
            version.textProperty().bind(i18n.text("sidebar.version"));
        }
    }

    private VBox createHeader() {
        VBox header = new VBox();
        header.getStyleClass().add("sidebar-header");
        header.getChildren().setAll(createBrand(), createSearchButton());
        return header;
    }

    private HBox createBrand() {
        StackPane mark = new StackPane(new Label("J"));
        mark.getStyleClass().add("sidebar-product-mark");

        title = new Label("JMC FX");
        title.getStyleClass().add("sidebar-title");
        subtitle = new Label("Flight Recorder diagnostics");
        subtitle.getStyleClass().add("sidebar-subtitle");
        VBox copy = new VBox(title, subtitle);
        copy.getStyleClass().add("sidebar-brand-copy");

        Button themeButton = iconButton(i18n.get("sidebar.theme"), Material2MZ.WB_SUNNY);
        themeButton.setDisable(true);

        HBox brand = new HBox(mark, copy, new Spacer(), themeButton);
        brand.getStyleClass().add("sidebar-brand");
        brand.setAlignment(Pos.CENTER_LEFT);
        return brand;
    }

    private Button createSearchButton() {
        searchTitleLabel = new Label("Search workflows", new FontIcon(Material2MZ.SEARCH));
        searchTitleLabel.getStyleClass().add("search-title");
        Label hint = new Label("/");
        hint.getStyleClass().add("search-hint");
        HBox content = new HBox(searchTitleLabel, new Spacer(), hint);
        content.getStyleClass().add("search-content");
        content.setAlignment(Pos.CENTER_LEFT);

        searchButton = new Button();
        searchButton.setGraphic(content);
        searchButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        searchButton.getStyleClass().add("sidebar-search");
        searchButton.setMaxWidth(Double.MAX_VALUE);
        searchButton.setDisable(true);
        searchButton.setAccessibleText("Search workflows");
        return searchButton;
    }

    private VBox createRecordingCard() {
        currentRecordingLabel = new Label("Current Recording");
        currentRecordingLabel.getStyleClass().add("recording-card-label");
        recordingNameLabel.getStyleClass().add("recording-card-title");
        recordingNameLabel.setWrapText(true);
        VBox card = new VBox(currentRecordingLabel, recordingNameLabel);
        card.getStyleClass().add("sidebar-recording-card");
        return card;
    }

    private HBox createFooter() {
        version = new Label("v0.1.0-SNAPSHOT");
        version.getStyleClass().add("sidebar-footer-text");
        HBox footer = new HBox(version);
        footer.getStyleClass().add("sidebar-footer");
        return footer;
    }

    private static Button iconButton(String accessibleText, org.kordamp.ikonli.Ikon icon) {
        Button button = new Button();
        button.setGraphic(new FontIcon(icon));
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.getStyleClass().addAll("icon-button", Styles.BUTTON_CIRCLE, Styles.FLAT);
        button.setAccessibleRole(AccessibleRole.BUTTON);
        button.setAccessibleText(accessibleText);
        button.setTooltip(new Tooltip(accessibleText));
        return button;
    }
}
