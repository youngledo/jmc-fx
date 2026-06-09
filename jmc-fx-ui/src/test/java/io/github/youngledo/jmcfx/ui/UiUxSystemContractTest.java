package io.github.youngledo.jmcfx.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class UiUxSystemContractTest {

    private static final Path RESOURCES = Path.of("src/main/resources");
    private static final List<String> LEGACY_DETAIL_CLASSES = List.of(
            "analysis-detail",
            "analysis-detail-title",
            "analysis-detail-explanation",
            "analysis-detail-scroll",
            "advanced-jfr-memory-detail",
            "heap-dump-detail");

    @Test
    void uiResourcesDoNotUseLegacyDetailStyleAliases() throws IOException {
        for (Path file : uiResourceFiles()) {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            for (String legacyClass : LEGACY_DETAIL_CLASSES) {
                assertFalse(content.contains(legacyClass),
                        () -> file + " must use the shared detail-panel contract instead of " + legacyClass);
            }
        }
    }

    @Test
    void fxmlDoesNotPackMultipleStyleClassesIntoOneAttribute() throws IOException {
        for (Path file : filesWithExtension(".fxml")) {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            assertFalse(content.matches("(?s).*styleClass=\"[^\"]*\\s+[^\"]*\".*"),
                    () -> file + " must use JavaFX list syntax for multiple style classes");
        }
    }

    @Test
    void uiUxSystemDocumentsSharedDetailPanelContract() throws IOException {
        String system = Files.readString(Path.of("../docs/ui-ux-system.md"), StandardCharsets.UTF_8);

        assertTrue(system.contains("`detail-panel`"));
        assertTrue(system.contains("`detail-panel-title`"));
        assertTrue(system.contains("`detail-panel-body`"));
        assertTrue(system.contains("Do not keep or introduce parallel detail semantics"));
    }

    @Test
    void uiUxSystemDocumentsTypographicHierarchy() throws IOException {
        String system = Files.readString(Path.of("../docs/ui-ux-system.md"), StandardCharsets.UTF_8);

        assertTrue(system.contains("Headings must be visually distinguishable from body text"));
        assertTrue(system.contains("18 px, bold"));
        assertTrue(system.contains("14 px, bold"));
        assertTrue(system.contains("13-14 px, bold"));
        assertTrue(system.contains("`view-title`"));
        assertTrue(system.contains("`detail-panel-title`"));
        assertTrue(system.contains("`detail-panel-meta`"));
        assertTrue(system.contains("rely on spacing alone"));
    }

    @Test
    void sharedApplicationCssDefinesTypographicHierarchy() throws IOException {
        String css = Files.readString(Path.of("src/main/resources/css/app.css"), StandardCharsets.UTF_8);

        String viewTitle = cssBlock(css, ".view-title");
        String detailPanelTitle = cssBlock(css, ".detail-panel-title");
        String detailPanelMeta = cssBlock(css, ".detail-panel-meta");
        String detailSectionLabel = cssBlock(css, ".detail-section-label");
        String aiReportSectionTitle = cssBlock(css, ".ai-report-section-title");

        assertTrue(viewTitle.contains("-fx-font-size: 18px"));
        assertTrue(viewTitle.contains("-fx-font-weight: bold"));
        assertTrue(detailPanelTitle.contains("-fx-font-size: 14px"));
        assertTrue(detailPanelTitle.contains("-fx-font-weight: bold"));
        assertTrue(detailPanelMeta.contains("-fx-text-fill: -color-fg-muted"));
        assertFalse(detailPanelMeta.contains("-fx-font-weight: bold"));
        assertTrue(detailSectionLabel.contains("-fx-font-weight: bold"));
        assertTrue(aiReportSectionTitle.contains("-fx-font-size: 14px"));
        assertTrue(aiReportSectionTitle.contains("-fx-font-weight: bold"));
    }

    @Test
    void uiUxSystemRequiresExplicitExportScope() throws IOException {
        String system = Files.readString(Path.of("../docs/ui-ux-system.md"), StandardCharsets.UTF_8);

        assertTrue(system.contains("tables and charts to agree on selected time range")
                        || system.contains("Tables and charts should agree on selected time range"),
                "The UI system must document selected time range consistency for exportable views.");
        assertTrue(system.contains("exports should export the current view or clearly state their scope")
                        || system.contains("Export actions should export the current view or clearly state their scope"),
                "The UI system must require export actions to state their scope.");
    }

    @Test
    void representativeTablesUseSharedWorkbenchTableSupport() throws IOException {
        String eventsController = Files.readString(Path.of(
                "src/main/java/io/github/youngledo/jmcfx/ui/events/EventsPageController.java"), StandardCharsets.UTF_8);
        String analysisController = Files.readString(Path.of(
                "src/main/java/io/github/youngledo/jmcfx/ui/analysis/AnalysisPageController.java"), StandardCharsets.UTF_8);
        String liveJvmController = Files.readString(Path.of(
                "src/main/java/io/github/youngledo/jmcfx/ui/shell/LiveJvmPaneController.java"), StandardCharsets.UTF_8);

        assertTrue(eventsController.contains("WorkbenchTableSupport.localizedPlaceholder"));
        assertTrue(analysisController.contains("WorkbenchTableSupport.localizedPlaceholder"));
        assertTrue(liveJvmController.contains("WorkbenchTableSupport.localizedPlaceholder"));
        assertFalse(eventsController.contains("setPlaceholder(new Label(i18n.get("));
        assertFalse(analysisController.contains("setPlaceholder(new Label(i18n.get("));
    }

    @Test
    void compactWorkbenchControlsExposeAccessibleText() throws IOException {
        String messages = Files.readString(Path.of(
                "src/main/resources/io/github/youngledo/jmcfx/ui/i18n/messages.properties"), StandardCharsets.UTF_8);
        String messagesZh = Files.readString(Path.of(
                "src/main/resources/io/github/youngledo/jmcfx/ui/i18n/messages_zh_CN.properties"), StandardCharsets.UTF_8);
        String liveJvmController = Files.readString(Path.of(
                "src/main/java/io/github/youngledo/jmcfx/ui/shell/LiveJvmPaneController.java"), StandardCharsets.UTF_8);
        String profilingController = Files.readString(Path.of(
                "src/main/java/io/github/youngledo/jmcfx/ui/profiling/ProfilingPageController.java"), StandardCharsets.UTF_8);

        assertTrue(liveJvmController.contains("accessibleTextProperty().bind"));
        assertTrue(liveJvmController.contains("setTooltip"));
        assertTrue(profilingController.contains("accessibleTextProperty().bind"));
        assertTrue(profilingController.contains("setTooltip"));
        assertTrue(messages.contains("workbench.focus.navigation"));
        assertTrue(messages.contains("workbench.focus.tabs"));
        assertTrue(messages.contains("workbench.focus.primary"));
        assertTrue(messages.contains("workbench.focus.filter"));
        assertTrue(messagesZh.contains("workbench.focus.navigation"));
        assertTrue(messagesZh.contains("workbench.focus.tabs"));
        assertTrue(messagesZh.contains("workbench.focus.primary"));
        assertTrue(messagesZh.contains("workbench.focus.filter"));
    }

    @Test
    void applicationCssDoesNotOverrideStandardControlStates() throws IOException {
        String css = Files.readString(Path.of("src/main/resources/css/app.css"), StandardCharsets.UTF_8);

        assertFalse(css.contains(".button:focused"));
        assertFalse(css.contains(".button:selected"));
        assertFalse(css.contains(".table-view:focused"));
        assertFalse(css.contains(".tab-pane:focused"));
        assertFalse(css.contains(".text-field:focused"));
        assertFalse(css.contains(".combo-box:focused"));
    }

    private static List<Path> uiResourceFiles() throws IOException {
        try (Stream<Path> paths = Files.walk(RESOURCES)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".fxml") || path.toString().endsWith(".css"))
                    .toList();
        }
    }

    private static List<Path> filesWithExtension(String extension) throws IOException {
        try (Stream<Path> paths = Files.walk(RESOURCES)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(extension))
                    .toList();
        }
    }

    private static String cssBlock(String css, String selector) {
        int selectorStart = css.indexOf("\n" + selector + " {");
        if (selectorStart < 0 && css.startsWith(selector + " {")) {
            selectorStart = 0;
        }
        assertTrue(selectorStart >= 0, () -> "Missing CSS selector " + selector);
        int blockStart = css.indexOf('{', selectorStart);
        int blockEnd = css.indexOf('}', blockStart);
        assertTrue(blockStart >= 0 && blockEnd > blockStart, () -> "Missing CSS block for " + selector);
        return css.substring(blockStart + 1, blockEnd);
    }
}
