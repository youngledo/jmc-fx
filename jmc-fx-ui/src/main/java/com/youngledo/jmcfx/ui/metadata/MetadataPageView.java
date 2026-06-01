package com.youngledo.jmcfx.ui.metadata;

import com.youngledo.jmcfx.domain.model.JfrMetadataEventType;

import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;

/// Narrow view handle for the JFR Metadata split table/detail page.
public record MetadataPageView(
        Label titleLabel,
        Label summaryLabel,
        TableView<JfrMetadataEventType> eventTypesTable,
        Label detailTitleLabel,
        TextArea detailArea) {
}
