package io.github.youngledo.jmcfx.ui.shell;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

final class SettingsPaneView {

    final VBox pane = new VBox();
    final Label titleLabel = new Label();
    final Label languageLabel = new Label();
    final ToggleGroup languageToggleGroup = new ToggleGroup();
    final RadioButton languageFollowSystemRadio = new RadioButton();
    final RadioButton languageEnglishRadio = new RadioButton();
    final RadioButton languageChineseRadio = new RadioButton();
    final Label themeLabel = new Label();
    final ToggleGroup themeToggleGroup = new ToggleGroup();
    final RadioButton themeFollowSystemRadio = new RadioButton();
    final RadioButton themeLightRadio = new RadioButton();
    final RadioButton themeDarkRadio = new RadioButton();
    final Label aiLabel = new Label();
    final CheckBox aiEnabledCheckBox = new CheckBox();
    final Label aiApiKeyStatusLabel = new Label();
    final Label aiBaseUrlLabel = new Label();
    final TextField aiBaseUrlField = new TextField();
    final Label aiModelLabel = new Label();
    final TextField aiModelField = new TextField();
    final Label aiTemperatureLabel = new Label();
    final Spinner<Double> aiTemperatureSpinner = new Spinner<>();
    final Label aiMaxOutputTokensLabel = new Label();
    final Spinner<Integer> aiMaxOutputTokensSpinner = new Spinner<>();
    final Button aiSaveButton = new Button();
    final Button aiResetButton = new Button();

    SettingsPaneView() {
        configure();
    }

    private void configure() {
        pane.setSpacing(36);
        styles(titleLabel, "view-title");
        styles(languageLabel, "detail-title");
        styles(themeLabel, "detail-title");
        styles(aiLabel, "detail-title");
        styles(aiApiKeyStatusLabel, "detail-panel-meta");
        languageFollowSystemRadio.setToggleGroup(languageToggleGroup);
        languageEnglishRadio.setToggleGroup(languageToggleGroup);
        languageChineseRadio.setToggleGroup(languageToggleGroup);
        themeFollowSystemRadio.setToggleGroup(themeToggleGroup);
        themeLightRadio.setToggleGroup(themeToggleGroup);
        themeDarkRadio.setToggleGroup(themeToggleGroup);
        VBox languageGroup = vbox(16, languageLabel,
                hbox(24, languageFollowSystemRadio, languageEnglishRadio, languageChineseRadio));
        VBox themeGroup = vbox(16, themeLabel,
                hbox(24, themeFollowSystemRadio, themeLightRadio, themeDarkRadio));
        VBox aiGroup = vbox(10, aiLabel, aiEnabledCheckBox, aiApiKeyStatusLabel, aiForm(),
                hbox(8, aiSaveButton, aiResetButton));
        styles(aiGroup, "settings-form-group");
        pane.getChildren().setAll(titleLabel, languageGroup, themeGroup, aiGroup);
    }

    private GridPane aiForm() {
        aiBaseUrlField.setPrefColumnCount(36);
        aiModelField.setPrefColumnCount(28);
        aiTemperatureSpinner.setEditable(true);
        aiMaxOutputTokensSpinner.setEditable(true);
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.getStyleClass().add("settings-form-grid");
        grid.addRow(0, aiBaseUrlLabel, aiBaseUrlField);
        grid.addRow(1, aiModelLabel, aiModelField);
        grid.addRow(2, aiTemperatureLabel, aiTemperatureSpinner);
        grid.addRow(3, aiMaxOutputTokensLabel, aiMaxOutputTokensSpinner);
        return grid;
    }

    private static VBox vbox(double spacing, Node... children) {
        return new VBox(spacing, children);
    }

    private static HBox hbox(double spacing, Node... children) {
        return new HBox(spacing, children);
    }

    private static void styles(Node node, String... styleClasses) {
        node.getStyleClass().addAll(styleClasses);
    }
}
