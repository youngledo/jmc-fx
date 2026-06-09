package io.github.youngledo.jmcfx.ui.analysis;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.github.youngledo.jmcfx.ui.i18n.I18n;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.Test;

class AiReportViewTest {

    @org.junit.jupiter.api.BeforeAll
    static void initToolkit() throws InterruptedException {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            assertTrue(latch.await(30, TimeUnit.SECONDS), "JavaFX toolkit did not start in time.");
        } catch (IllegalStateException ignored) {
            // Toolkit already initialized by another test class.
        }
    }

    @Test
    void rendersFinalReportMarkdownAsBlockContent() {
        AiReportView view = new AiReportView();

        view.showReport(new io.github.youngledo.jmcfx.domain.model.ai.AiRecordingReport("""
                ## Summary

                First paragraph.

                - Item one
                - Item two
                """, java.util.List.of(), java.util.List.of(), java.util.List.of()), new I18n(Locale.ENGLISH));

        assertTrue(hasStyleClass(view.node(), "ai-md-heading"));
        assertTrue(hasStyleClass(view.node(), "ai-md-list-item"));
        assertTrue(blockCount(view.node()) >= 4);
    }

    @Test
    void rendersProcessingTimeDividerAsMarkdownThematicBreak() {
        AiReportView view = new AiReportView();

        view.showReport(new io.github.youngledo.jmcfx.domain.model.ai.AiRecordingReport(
                "Summary", java.util.List.of(), java.util.List.of(), java.util.List.of()), "10s",
                new I18n(Locale.ENGLISH));

        assertTrue(hasStyleClass(view.node(), "ai-report-meta"));
        assertTrue(hasStyleClass(view.node(), "ai-md-thematic-break"));
    }

    @Test
    void rendersMarkdownThematicBreaks() {
        AiReportView view = new AiReportView();

        view.showReport(new io.github.youngledo.jmcfx.domain.model.ai.AiRecordingReport("""
                First

                ---

                Second
                """, java.util.List.of(), java.util.List.of(), java.util.List.of()), new I18n(Locale.ENGLISH));

        assertTrue(hasStyleClass(view.node(), "ai-md-thematic-break"));
    }


    @Test
    void keepsStreamingLoadingNodeStableAcrossDeltas() {
        AiReportView view = new AiReportView();
        I18n i18n = new I18n(Locale.ENGLISH);

        view.showStreamingResponse(i18n);
        Node firstLoadingNode = firstContentChild(view.node());

        view.showStreamingResponse(i18n);

        assertTrue(firstLoadingNode == firstContentChild(view.node()));
    }

    @Test
    void loadingStateUsesStableSpinner() {
        AiReportView view = new AiReportView();
        I18n i18n = new I18n(Locale.ENGLISH);

        view.showStreamingResponse(i18n);
        Node firstSpinner = firstNodeOfType(view.node(), ProgressIndicator.class);

        view.showStreamingResponse(i18n);

        assertTrue(hasStyleClass(view.node(), "ai-report-loading"));
        assertTrue(firstSpinner != null);
        assertTrue(firstSpinner == firstNodeOfType(view.node(), ProgressIndicator.class));
    }

    private static int blockCount(Node node) {
        if (node instanceof ScrollPane scrollPane && scrollPane.getContent() != null) {
            return blockCount(scrollPane.getContent());
        }
        if (node instanceof VBox box && box.getStyleClass().contains("ai-markdown-blocks")) {
            return box.getChildren().size();
        }
        if (node instanceof Parent parent) {
            int count = 0;
            for (Node child : parent.getChildrenUnmodifiable()) {
                count += blockCount(child);
            }
            return count;
        }
        return 0;
    }

    private static Node firstContentChild(Node node) {
        if (node instanceof ScrollPane scrollPane && scrollPane.getContent() != null) {
            return firstContentChild(scrollPane.getContent());
        }
        if (node instanceof VBox box && !box.getChildren().isEmpty()) {
            return box.getChildren().getFirst();
        }
        return null;
    }

    private static boolean hasStyleClass(Node node, String styleClass) {
        if (node.getStyleClass().contains(styleClass)) {
            return true;
        }
        if (node instanceof ScrollPane scrollPane && scrollPane.getContent() != null) {
            return hasStyleClass(scrollPane.getContent(), styleClass);
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                if (hasStyleClass(child, styleClass)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Node firstNodeOfType(Node node, Class<? extends Node> type) {
        if (type.isInstance(node)) {
            return node;
        }
        if (node instanceof ScrollPane scrollPane && scrollPane.getContent() != null) {
            return firstNodeOfType(scrollPane.getContent(), type);
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                Node match = firstNodeOfType(child, type);
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }
}
