package com.youngledo.jmcfx.ui.javaapp;

import com.youngledo.jmcfx.domain.model.NativeLibraryEntry;
import com.youngledo.jmcfx.domain.model.ThreadDumpEntry;
import com.youngledo.jmcfx.domain.model.ThreadHistogramRow;
import com.youngledo.jmcfx.domain.model.X509CertificateEntry;
import com.youngledo.jmcfx.ui.chart.TimelineChart;

import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;

/// Narrow view handle for Java Application data pages owned by the javaapp package.
public record JavaApplicationDataPagesView(
        Label threadHistogramTitleLabel,
        TimelineChart threadHistogramChart,
        TableView<ThreadHistogramRow> threadHistogramTable,
        Label securityTitleLabel,
        TableView<X509CertificateEntry> securityTable,
        Label nativeLibrariesTitleLabel,
        TableView<NativeLibraryEntry> nativeLibrariesTable,
        Label threadDumpsTitleLabel,
        TableView<ThreadDumpEntry> threadDumpsTable,
        TextArea threadDumpTextArea) {
}
