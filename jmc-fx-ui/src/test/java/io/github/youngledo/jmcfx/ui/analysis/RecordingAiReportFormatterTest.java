package io.github.youngledo.jmcfx.ui.analysis;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;

import io.github.youngledo.jmcfx.domain.model.ai.AiEvidence;
import io.github.youngledo.jmcfx.domain.model.ai.AiFinding;
import io.github.youngledo.jmcfx.domain.model.ai.AiRecordingReport;
import io.github.youngledo.jmcfx.domain.model.ai.AiSeverity;
import io.github.youngledo.jmcfx.ui.i18n.I18n;
import io.github.youngledo.jmcfx.ui.i18n.LanguageMode;
import org.junit.jupiter.api.Test;

class RecordingAiReportFormatterTest {

    @Test
    void formatsReportWithLocalizedChineseLabels() {
        RecordingAiReportFormatter formatter = formatter(Locale.SIMPLIFIED_CHINESE);
        AiRecordingReport report = new AiRecordingReport("GC pressure is elevated.",
                List.of(new AiFinding("Long GC pause", AiSeverity.WARNING, 0.8, "gcDetails",
                        "Inspect GC pauses.", "", List.of())),
                List.of("Which GC page should I inspect?"),
                List.of("Rule results only."));

        String text = formatter.reportText(report);

        assertTrue(text.contains("摘要"));
        assertTrue(text.contains("发现"));
        assertTrue(text.contains("警告"));
        assertTrue(text.contains("置信度 80%"));
        assertFalse(text.contains("Summary"));
        assertFalse(text.contains("Findings"));
        assertFalse(text.contains("confidence"));
    }

    @Test
    void skipsBlankEvidenceInsteadOfRenderingEmptyLabelAndValue() {
        RecordingAiReportFormatter formatter = formatter(Locale.ENGLISH);
        AiRecordingReport report = new AiRecordingReport("Summary",
                List.of(new AiFinding("Finding", AiSeverity.WARNING, 0.8, "",
                        "", "", List.of(
                                new AiEvidence("", "", "", "", ""),
                                new AiEvidence("Max pause", "", "", "", ""),
                                new AiEvidence("", "250 ms", "rules", "", "")))),
                List.of(),
                List.of());

        String text = formatter.reportText(report);

        assertFalse(text.contains("- :"));
        assertFalse(text.contains("- Max pause"));
        assertTrue(text.contains("• Max pause"));
        assertTrue(text.contains("• 250 ms (rules)"));
    }

    @Test
    void rendersMarkdownFieldsAsPlainTextForTextArea() {
        RecordingAiReportFormatter formatter = formatter(Locale.ENGLISH);
        AiRecordingReport report = new AiRecordingReport("**GC pressure** uses `G1`.",
                List.of(new AiFinding("Finding", AiSeverity.INFO, 0.6, "",
                        "Inspect **GC pauses** and `Pause Young` events.", "",
                        List.of(new AiEvidence("Metric", "**250 ms**", "", "", "")))),
                List.of("Open **GC Details**."),
                List.of());

        String text = formatter.reportText(report);

        assertTrue(text.contains("GC pressure uses \"G1\"."));
        assertTrue(text.contains("Inspect GC pauses and \"Pause Young\" events."));
        assertTrue(text.contains("Metric: 250 ms"));
        assertTrue(text.contains("Open GC Details."));
        assertFalse(text.contains("**"));
        assertFalse(text.contains("`"));
    }

    private static RecordingAiReportFormatter formatter(Locale locale) {
        I18n i18n = new I18n(locale);
        i18n.setLanguageMode(Locale.SIMPLIFIED_CHINESE.equals(locale)
                ? LanguageMode.CHINESE_SIMPLIFIED : LanguageMode.ENGLISH);
        return new RecordingAiReportFormatter(i18n);
    }
}
