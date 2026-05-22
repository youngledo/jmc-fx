package com.youngledo.jmcfx.ui.histogram;

import javafx.scene.control.TableCell;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;

/// Table cell that renders a percentage bar behind the text value.
public class PercentageBarTableCell<S> extends TableCell<S, String> {

    private final StackPane pane;
    private final ProgressBar bar;
    private final Text text;

    public PercentageBarTableCell() {
        bar = new ProgressBar();
        bar.getStyleClass().add("percentage-bar");
        bar.setMaxWidth(Double.MAX_VALUE);
        text = new Text();
        pane = new StackPane(bar, text);
    }

    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
            setText(null);
            return;
        }
        text.setText(item);
        bar.setProgress(PercentageParser.parsePercentage(item));
        setGraphic(pane);
        setText(null);
    }

}
