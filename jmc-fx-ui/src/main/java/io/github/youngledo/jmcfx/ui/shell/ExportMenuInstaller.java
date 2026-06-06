package io.github.youngledo.jmcfx.ui.shell;

import io.github.youngledo.jmcfx.ui.i18n.I18n;
import io.github.youngledo.jmcfx.ui.util.CsvExport;
import io.github.youngledo.jmcfx.ui.util.TableExportRegistration;
import io.github.youngledo.jmcfx.ui.util.TableExportRequest;
import io.github.youngledo.jmcfx.ui.util.TableExportRequests;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
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
        install(TableExportRequests.currentView(table, "Workspace", "Current Page", "Table", "TableView"));
    }

    void install(TableExportRegistration registration) {
        TableView<?> table = registration.table();
        MenuItem exportItem = new MenuItem(exportMenuText(registration.requestSupplier().get()));
        exportItem.setDisable(tableIsEmpty(table));
        exportItem.setOnAction(event -> {
            TableExportRequest request = registration.requestSupplier().get();
            if (tableIsEmpty(request.table())) {
                viewModel.showStatus(i18n.get("status.exportNoRows"));
                return;
            }
            FileChooser chooser = new FileChooser();
            chooser.setTitle(i18n.get("fileChooser.saveCsv.title"));
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter(i18n.get("fileChooser.csvFiles"), "*.csv"));
            java.io.File target = chooser.showSaveDialog(root.getScene().getWindow());
            if (target != null) {
                try {
                    CsvExport.export(request, target.toPath());
                    viewModel.showStatus(i18n.format("status.exportedWithScope",
                            target.getName(), exportScopeSummaryForCurrentLocale(request)));
                } catch (Exception e) {
                    viewModel.showStatus(i18n.get("status.exportFailed"));
                }
            }
        });
        ContextMenu menu = new ContextMenu(exportItem);
        menu.setOnShowing(event -> {
            TableExportRequest request = registration.requestSupplier().get();
            exportItem.setText(exportMenuText(request));
            exportItem.setDisable(tableIsEmpty(request.table()));
        });
        table.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                event.consume();
            }
        });
        table.setContextMenu(menu);
    }

    private String exportMenuText(TableExportRequest request) {
        String scope = exportScopeSummaryForCurrentLocale(request);
        if (scope.isBlank()) {
            return i18n.get("context.exportCsv");
        }
        return i18n.format("context.exportCsvWithScope", scope);
    }

    private static String exportScopeSummary(TableExportRequest request) {
        return exportScopeSummary(request, java.util.Locale.getDefault());
    }

    private static String exportScopeSummary(TableExportRequest request, java.util.Locale locale) {
        if (request == null || request.context() == null) {
            return "";
        }
        return request.context().summary(locale);
    }

    private String exportScopeSummaryForCurrentLocale(TableExportRequest request) {
        return exportScopeSummary(request, i18n.localeProperty().get());
    }

    private static boolean tableIsEmpty(TableView<?> table) {
        return table == null || table.getItems() == null || table.getItems().isEmpty();
    }
}
