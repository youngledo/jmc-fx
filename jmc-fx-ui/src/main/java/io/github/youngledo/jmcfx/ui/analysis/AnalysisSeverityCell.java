package io.github.youngledo.jmcfx.ui.analysis;

import io.github.youngledo.jmcfx.domain.model.Severity;
import javafx.scene.control.TableCell;
import javafx.scene.text.Text;

/// Table cell that displays severity with a colored indicator.
public class AnalysisSeverityCell<S> extends TableCell<S, Severity> {

    @Override
    protected void updateItem(Severity item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
            setText(null);
            return;
        }
        Text text = new Text(severitySymbol(item));
        text.getStyleClass().setAll("severity-icon", severityStyle(item));
        setGraphic(text);
        setText(null);
    }

    static String severitySymbol(Severity severity) {
        return switch (severity) {
            case IGNORED -> "–";
            case UNAVAILABLE -> "N/A";
            case OK -> "✓";
            case INFO -> "ℹ";
            case WARNING -> "⚠";
            case CRITICAL -> "✖";
            case UNKNOWN -> "?";
        };
    }

    static String severityStyle(Severity severity) {
        return switch (severity) {
            case IGNORED -> "severity-ignored";
            case UNAVAILABLE -> "severity-unavailable";
            case OK -> "severity-ok";
            case INFO -> "severity-info";
            case WARNING -> "severity-warning";
            case CRITICAL -> "severity-critical";
            case UNKNOWN -> "severity-unknown";
        };
    }
}
