package io.github.youngledo.jmcfx.ui.shell;

import io.github.youngledo.jmcfx.ui.i18n.I18n;
import io.github.youngledo.jmcfx.ui.util.CsvExport;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;

final class ExportMenuInstaller {

    private final BorderPane root;
    private final AppShellViewModel viewModel;
    private final I18n i18n;

    ExportMenuInstaller(BorderPane root, AppShellViewModel viewModel, I18n i18n) {
        this.root = root;
        this.viewModel = viewModel;
        this.i18n = i18n;
    }

    void install(TableView<?> table) {
        MenuItem exportItem = new MenuItem(i18n.get("context.exportCsv"));
        exportItem.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle(i18n.get("fileChooser.saveCsv.title"));
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter(i18n.get("fileChooser.csvFiles"), "*.csv"));
            java.io.File target = chooser.showSaveDialog(root.getScene().getWindow());
            if (target != null) {
                try {
                    CsvExport.export(table, target.toPath());
                    viewModel.showStatus(i18n.format("status.exported", target.getName()));
                } catch (Exception e) {
                    viewModel.showStatus(i18n.get("status.exportFailed"));
                }
            }
        });
        ContextMenu menu = new ContextMenu(exportItem);
        table.setContextMenu(menu);
    }
}
