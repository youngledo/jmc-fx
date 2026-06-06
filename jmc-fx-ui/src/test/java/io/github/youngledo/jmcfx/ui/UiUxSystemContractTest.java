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
}
