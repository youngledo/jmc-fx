package io.github.youngledo.jmcfx.ui.util;

import io.github.youngledo.jmcfx.ui.i18n.I18n;

import javafx.beans.binding.StringBinding;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public final class WorkbenchTableSupport {

    private WorkbenchTableSupport() {
    }

    public static <T> void configureDenseTable(TableView<T> table, String accessibleText) {
        if (table == null) {
            return;
        }
        if (!table.getStyleClass().contains("dense-table")) {
            table.getStyleClass().add("dense-table");
        }
        table.setAccessibleText(accessibleText);
    }

    public static Label localizedPlaceholder(I18n i18n, String key) {
        Label label = new Label();
        StringBinding text = i18n.text(key);
        label.textProperty().bind(text);
        label.accessibleTextProperty().bind(text);
        return label;
    }

    public static <T> void rightAlignNumericColumn(TableColumn<T, Number> column) {
        column.setCellFactory(ignored -> {
            TableCell<T, Number> cell = new TableCell<>() {
                @Override
                protected void updateItem(Number item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.toString());
                }
            };
            cell.setAlignment(Pos.CENTER_RIGHT);
            return cell;
        });
    }
}
