package com.youngledo.jmcfx.ui.threads;

import com.youngledo.jmcfx.domain.model.ThreadSummary;

import javafx.scene.control.Label;
import javafx.scene.control.TableView;

/// Narrow view handle for the JFR Thread Activity data table page.
public record ThreadsPageView(Label titleLabel, TableView<ThreadSummary> table) {
}
