package io.github.youngledo.jmcfx.ui.javaapp;

import io.github.youngledo.jmcfx.domain.model.ExceptionSummary;
import io.github.youngledo.jmcfx.domain.model.NativeLibraryEntry;
import io.github.youngledo.jmcfx.domain.model.ThreadDumpEntry;
import io.github.youngledo.jmcfx.domain.model.ThreadHistogramRow;
import io.github.youngledo.jmcfx.domain.model.ThreadSummary;
import io.github.youngledo.jmcfx.domain.model.X509CertificateEntry;
import io.github.youngledo.jmcfx.ui.chart.TimelineChart;
import io.github.youngledo.jmcfx.ui.exceptions.ExceptionsPageView;
import io.github.youngledo.jmcfx.ui.threads.ThreadsPageView;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/// Code-first view for Java Application recording data pages.
public final class JavaApplicationDataPaneView {

    private final Label exceptionsTitleLabel = new Label();
    private final Button exceptionsGroupByClass = new Button();
    private final Button exceptionsGroupByMessage = new Button();
    private final Button exceptionsGroupByClassAndMessage = new Button();
    private final TableView<ExceptionSummary> exceptionsTable = denseTable();
    private final VBox exceptionsTimelineContainer = new VBox();
    private final TimelineChart exceptionsTimelineChart = new TimelineChart();
    private final Label threadsTitleLabel = new Label();
    private final TableView<ThreadSummary> threadsTable = denseTable();
    private final Label threadHistogramTitleLabel = new Label();
    private final VBox threadHistogramChartContainer = new VBox();
    private final TimelineChart threadHistogramChart = new TimelineChart();
    private final TableView<ThreadHistogramRow> threadHistogramTable = denseTable();
    private final Label securityTitleLabel = new Label();
    private final TableView<X509CertificateEntry> securityTable = denseTable();
    private final Label nativeLibrariesTitleLabel = new Label();
    private final TableView<NativeLibraryEntry> nativeLibrariesTable = denseTable();
    private final Label threadDumpsTitleLabel = new Label();
    private final TableView<ThreadDumpEntry> threadDumpsTable = denseTable();
    private final TextArea threadDumpTextArea = textArea();

    public JavaApplicationDataPaneView(VBox exceptionsPane, VBox threadsPane,
            VBox threadHistogramPane, VBox securityPane, VBox nativeLibrariesPane,
            VBox threadDumpsPane) {
        configure(exceptionsPane, threadsPane, threadHistogramPane, securityPane,
                nativeLibrariesPane, threadDumpsPane);
    }

    public ExceptionsPageView exceptionsPage() {
        return new ExceptionsPageView(exceptionsTitleLabel, exceptionsGroupByClass, exceptionsGroupByMessage,
                exceptionsGroupByClassAndMessage, exceptionsTable, exceptionsTimelineChart);
    }

    public ThreadsPageView threadsPage() {
        return new ThreadsPageView(threadsTitleLabel, threadsTable);
    }

    public JavaApplicationDataPagesView javaApplicationDataPages() {
        return new JavaApplicationDataPagesView(threadHistogramTitleLabel,
                threadHistogramChart, threadHistogramTable,
                securityTitleLabel, securityTable, nativeLibrariesTitleLabel, nativeLibrariesTable,
                threadDumpsTitleLabel, threadDumpsTable, threadDumpTextArea);
    }

    private void configure(VBox exceptionsPane, VBox threadsPane, VBox threadHistogramPane,
            VBox securityPane, VBox nativeLibrariesPane, VBox threadDumpsPane) {
        exceptionsTimelineContainer.getChildren().setAll(exceptionsTimelineChart);
        configureTablePage(exceptionsPane, exceptionsTitleLabel,
                hbox(8, exceptionsGroupByClass, exceptionsGroupByMessage, exceptionsGroupByClassAndMessage),
                new SplitPane(exceptionsTable, exceptionsTimelineContainer));
        configureTablePage(threadsPane, threadsTitleLabel, threadsTable);
        threadHistogramChartContainer.getChildren().setAll(threadHistogramChart);
        configureTablePage(threadHistogramPane, threadHistogramTitleLabel, threadHistogramChartContainer,
                threadHistogramTable);
        configureTablePage(securityPane, securityTitleLabel, securityTable);
        configureTablePage(nativeLibrariesPane, nativeLibrariesTitleLabel, nativeLibrariesTable);
        configureTablePage(threadDumpsPane, threadDumpsTitleLabel, new SplitPane(threadDumpsTable, threadDumpTextArea));
        readonly(threadDumpTextArea);
        styles(threadDumpTextArea, "dump-text-area");
    }

    private void configureTablePage(VBox pane, Label title, Node... content) {
        pane.setSpacing(8);
        styles(title, "view-title");
        pane.getChildren().setAll(title);
        pane.getChildren().addAll(content);
        for (Node node : content) {
            if (node instanceof TableView<?> || node instanceof TabPane || node instanceof SplitPane) {
                VBox.setVgrow(node, Priority.ALWAYS);
            }
        }
    }

    private static HBox hbox(double spacing, Node... children) {
        return new HBox(spacing, children);
    }

    private static TextArea textArea() {
        TextArea area = new TextArea();
        area.setWrapText(true);
        return area;
    }

    private static <T> TableView<T> denseTable() {
        TableView<T> table = new TableView<>();
        styles(table, "dense-table");
        return table;
    }

    private static void readonly(TextArea... areas) {
        for (TextArea area : areas) {
            area.setEditable(false);
        }
    }

    private static void styles(Node node, String... styleClasses) {
        node.getStyleClass().addAll(styleClasses);
    }
}
