package com.youngledo.jmcfx.ui.shell;

import com.youngledo.jmcfx.ui.i18n.I18n;

import javafx.beans.property.BooleanProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TreeCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2MZ;

final class AppNavTreeCell extends TreeCell<AppNavItem> {

    private static final PseudoClass GROUP = PseudoClass.getPseudoClass("group");
    private static final PseudoClass UNAVAILABLE = PseudoClass.getPseudoClass("unavailable");
    private static final String NAV_ICON = "nav-icon";

    private final BooleanProperty recordingOpen;
    private final I18n i18n;
    private final HBox root = new HBox();
    private final Label titleLabel = new Label();
    private final Label iconLabel = new Label();
    private final Label arrowLabel = new Label();
    private final Label tagLabel = new Label();

    AppNavTreeCell(BooleanProperty recordingOpen, I18n i18n) {
        this.recordingOpen = recordingOpen;
        this.i18n = i18n;
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        iconLabel.getStyleClass().add(NAV_ICON);
        titleLabel.getStyleClass().add("nav-title");
        arrowLabel.getStyleClass().add("nav-arrow");
        tagLabel.getStyleClass().add("nav-tag");
        root.getStyleClass().add("nav-cell-container");
        root.setAlignment(Pos.CENTER_LEFT);
        root.getChildren().setAll(iconLabel, titleLabel, spacer, arrowLabel, tagLabel);
        getStyleClass().add("app-nav-tree-cell");
    }

    @Override
    protected void updateItem(AppNavItem item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
            setText(null);
            titleLabel.textProperty().unbind();
            titleLabel.setText(null);
            iconLabel.getStyleClass().setAll(NAV_ICON);
            tagLabel.textProperty().unbind();
            tagLabel.setText(null);
            pseudoClassStateChanged(GROUP, false);
            pseudoClassStateChanged(UNAVAILABLE, false);
            return;
        }

        titleLabel.textProperty().unbind();
        titleLabel.textProperty().bind(i18n.text(item.titleKey()));
        iconLabel.getStyleClass().setAll(NAV_ICON, item.iconTone().styleClass());
        iconLabel.setGraphic(iconFor(item));
        boolean expanded = getTreeItem() != null && getTreeItem().isExpanded();
        arrowLabel.setGraphic(new FontIcon(expanded ? Material2MZ.REMOVE : Material2AL.ADD));

        boolean unavailable = item.unavailable(recordingOpen.get());
        tagLabel.textProperty().unbind();
        tagLabel.textProperty().bind(i18n.text("sidebar.soon"));
        tagLabel.setVisible(unavailable && item.page());
        tagLabel.setManaged(unavailable && item.page());
        arrowLabel.setVisible(item.group());
        arrowLabel.setManaged(item.group());
        pseudoClassStateChanged(GROUP, item.group());
        pseudoClassStateChanged(UNAVAILABLE, unavailable);
        setDisable(unavailable);
        setGraphic(root);
    }

    private static FontIcon iconFor(AppNavItem item) {
        FontIcon icon = new FontIcon(item.icon());
        icon.getStyleClass().add(item.iconTone().styleClass());
        return icon;
    }
}
