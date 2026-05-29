package com.youngledo.jmcfx.ui;

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
