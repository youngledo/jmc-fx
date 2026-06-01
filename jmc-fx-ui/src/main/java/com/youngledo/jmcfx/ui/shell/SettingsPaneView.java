package com.youngledo.jmcfx.ui.shell;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
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

    SettingsPaneView() {
        configure();
    }

    private void configure() {
        pane.setSpacing(36);
        styles(titleLabel, "view-title");
        styles(languageLabel, "detail-title");
        styles(themeLabel, "detail-title");
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
        pane.getChildren().setAll(titleLabel, languageGroup, themeGroup);
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
