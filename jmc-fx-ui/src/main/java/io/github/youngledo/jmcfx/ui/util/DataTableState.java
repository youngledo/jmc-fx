package io.github.youngledo.jmcfx.ui.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

record DataTableState(List<DataTableColumnState> columns, List<String> sortColumnIds) {

    DataTableState {
        columns = List.copyOf(columns == null ? List.of() : columns);
        sortColumnIds = List.copyOf(sortColumnIds == null ? List.of() : sortColumnIds);
    }

    static DataTableState capture(TableView<?> table) {
        List<DataTableColumnState> columns = table.getColumns().stream()
                .filter(column -> !columnId(column).isBlank())
                .map(column -> new DataTableColumnState(columnId(column), column.isVisible(), column.getPrefWidth()))
                .toList();
        List<String> sortColumnIds = table.getSortOrder().stream()
                .map(DataTableState::columnId)
                .filter(id -> !id.isBlank())
                .toList();
        return new DataTableState(columns, sortColumnIds);
    }

    Optional<DataTableColumnState> column(String id) {
        return columns.stream()
                .filter(column -> column.id().equals(id))
                .findFirst();
    }

    void applyTo(TableView<?> table) {
        Map<String, TableColumn<?, ?>> byId = new LinkedHashMap<>();
        for (TableColumn<?, ?> column : table.getColumns()) {
            String id = columnId(column);
            if (!id.isBlank()) {
                byId.put(id, column);
            }
        }
        for (DataTableColumnState state : columns) {
            TableColumn<?, ?> column = byId.get(state.id());
            if (column != null) {
                column.setVisible(state.visible());
                column.setPrefWidth(state.width());
            }
        }
        table.getSortOrder().clear();
        for (String id : sortColumnIds) {
            TableColumn<?, ?> column = byId.get(id);
            if (column != null) {
                addSortColumn(table, column);
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void addSortColumn(TableView table, TableColumn column) {
        table.getSortOrder().add(column);
    }

    private static String columnId(TableColumn<?, ?> column) {
        String id = column.getId();
        return id == null ? "" : id;
    }
}
