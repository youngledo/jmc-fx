package io.github.youngledo.jmcfx.ui.aggregate;

import java.util.List;

import io.github.youngledo.jmcfx.domain.model.KeyValueEntry;
import io.github.youngledo.jmcfx.domain.model.KeyValueSection;

import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/// Reusable component for displaying grouped key-value metadata.
public class AggregateViewer extends VBox {

    public AggregateViewer() {
        getStyleClass().add("aggregate-viewer");
    }

    public void setSections(List<KeyValueSection> sections) {
        getChildren().clear();
        if (sections == null || sections.isEmpty()) {
            return;
        }
        for (int i = 0; i < sections.size(); i++) {
            if (i > 0) {
                getChildren().add(new Separator());
            }
            getChildren().add(buildSection(sections.get(i)));
        }
    }

    private VBox buildSection(KeyValueSection section) {
        VBox box = new VBox();
        box.getStyleClass().add("aggregate-section");
        Label titleLabel = new Label(section.title());
        titleLabel.getStyleClass().add("aggregate-section-title");
        box.getChildren().add(titleLabel);
        GridPane grid = new GridPane();
        grid.getStyleClass().add("aggregate-grid");
        grid.setHgap(16);
        grid.setVgap(4);
        for (int i = 0; i < section.entries().size(); i++) {
            KeyValueEntry entry = section.entries().get(i);
            Label key = new Label(entry.label());
            key.getStyleClass().add("aggregate-key");
            Label value = new Label(entry.value());
            value.getStyleClass().add("aggregate-value");
            grid.add(key, 0, i);
            grid.add(value, 1, i);
        }
        box.getChildren().add(grid);
        return box;
    }
}
