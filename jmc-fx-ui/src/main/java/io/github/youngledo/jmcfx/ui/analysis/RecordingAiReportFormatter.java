package io.github.youngledo.jmcfx.ui.analysis;

import java.util.List;
import java.util.Objects;

import io.github.youngledo.jmcfx.domain.model.ai.AiAssistantAnswer;
import io.github.youngledo.jmcfx.domain.model.ai.AiEvidence;
import io.github.youngledo.jmcfx.domain.model.ai.AiFinding;
import io.github.youngledo.jmcfx.domain.model.ai.AiRecordingReport;
import io.github.youngledo.jmcfx.domain.model.ai.AiSeverity;
import io.github.youngledo.jmcfx.ui.i18n.I18n;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.text.TextContentRenderer;

/// Formats structured AI report data for the current plain-text report surface.
public final class RecordingAiReportFormatter {

    private static final Parser MARKDOWN_PARSER = Parser.builder().build();
    private static final TextContentRenderer PLAIN_TEXT_RENDERER = TextContentRenderer.builder().build();
    private static final String BULLET = "\u2022 ";

    private final I18n i18n;

    public RecordingAiReportFormatter(I18n i18n) {
        this.i18n = Objects.requireNonNull(i18n, "i18n");
    }

    public String reportText(AiRecordingReport report) {
        StringBuilder text = new StringBuilder();
        appendSection(text, i18n.get("analysis.ai.report.summary"), report.summaryMarkdown());
        if (!report.findings().isEmpty()) {
            text.append("\n").append(i18n.get("analysis.ai.report.findings")).append(":\n");
            int index = 1;
            for (AiFinding finding : report.findings()) {
                text.append(i18n.format("analysis.ai.report.findingHeader",
                        index++,
                        clean(finding.title()),
                        severity(finding.severity()),
                        i18n.get("analysis.ai.report.confidence"),
                        Math.round(finding.confidence() * 100)))
                        .append('\n');
                appendIndented(text, finding.recommendedNextStepMarkdown());
                if (!finding.relatedPageId().isBlank()) {
                    text.append("   ")
                            .append(i18n.get("analysis.ai.report.relatedPage"))
                            .append(": ")
                            .append(clean(finding.relatedPageId()))
                            .append('\n');
                }
                appendEvidence(text, finding.evidence(), "   ");
            }
        }
        appendList(text, i18n.get("analysis.ai.report.limitations"), report.contextLimitations());
        appendList(text, i18n.get("analysis.ai.report.followUpQuestions"), report.followUpQuestions());
        return text.toString().strip();
    }

    public String summaryText(AiRecordingReport report) {
        return clean(report.summaryMarkdown());
    }

    public String findingDetail(AiFinding finding) {
        if (finding == null) {
            return "";
        }
        StringBuilder detail = new StringBuilder();
        detail.append(i18n.get("analysis.ai.report.severity"))
                .append(": ")
                .append(severity(finding.severity()))
                .append('\n');
        detail.append(i18n.get("analysis.ai.report.confidence"))
                .append(": ")
                .append(Math.round(finding.confidence() * 100))
                .append("%\n");
        if (!finding.relatedPageId().isBlank()) {
            detail.append(i18n.get("analysis.ai.report.relatedPage"))
                    .append(": ")
                    .append(clean(finding.relatedPageId()))
                    .append('\n');
        }
        appendSection(detail, i18n.get("analysis.ai.report.recommendedNextStep"),
                finding.recommendedNextStepMarkdown());
        appendSection(detail, i18n.get("analysis.ai.report.limitations"), finding.limitationsMarkdown());
        appendEvidence(detail, finding.evidence(), "");
        return detail.toString().strip();
    }

    public String answerText(AiAssistantAnswer answer) {
        if (answer == null) {
            return "";
        }
        StringBuilder text = new StringBuilder(clean(answer.answerMarkdown()));
        appendList(text, i18n.get("analysis.ai.report.followUpQuestions"), answer.followUpQuestions());
        return text.toString().strip();
    }

    private void appendEvidence(StringBuilder text, List<AiEvidence> evidence, String indent) {
        if (evidence == null || evidence.isEmpty()) {
            return;
        }
        StringBuilder evidenceText = new StringBuilder();
        for (AiEvidence item : evidence) {
            String line = evidenceLine(item);
            if (!line.isBlank()) {
                evidenceText.append(indent).append(BULLET).append(line).append('\n');
            }
        }
        if (!evidenceText.isEmpty()) {
            text.append(indent).append(i18n.get("analysis.ai.report.evidence")).append(":\n");
            text.append(evidenceText);
        }
    }

    private String evidenceLine(AiEvidence evidence) {
        String label = clean(evidence.label());
        String value = clean(evidence.value());
        String source = clean(evidence.source());
        if (label.isBlank() && value.isBlank()) {
            return "";
        }
        String line;
        if (label.isBlank()) {
            line = value;
        } else if (value.isBlank()) {
            line = label;
        } else {
            line = label + ": " + value;
        }
        if (!source.isBlank()) {
            line = line + " (" + source + ")";
        }
        return line;
    }

    private void appendIndented(StringBuilder text, String value) {
        String cleaned = clean(value);
        if (cleaned.isBlank()) {
            return;
        }
        for (String line : cleaned.split("\\R")) {
            if (!line.isBlank()) {
                text.append("   ").append(line).append('\n');
            }
        }
    }

    private void appendSection(StringBuilder text, String title, String value) {
        String cleaned = clean(value);
        if (cleaned.isBlank()) {
            return;
        }
        text.append("\n").append(title).append(":\n").append(cleaned).append('\n');
    }

    private void appendList(StringBuilder text, String title, List<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        StringBuilder listText = new StringBuilder();
        for (String value : values) {
            String cleaned = clean(value);
            if (!cleaned.isBlank()) {
                listText.append(BULLET).append(cleaned).append('\n');
            }
        }
        if (!listText.isEmpty()) {
            text.append("\n").append(title).append(":\n").append(listText);
        }
    }

    private String severity(AiSeverity severity) {
        AiSeverity value = severity == null ? AiSeverity.UNKNOWN : severity;
        return i18n.get("analysis.ai.report.severity." + value.name().toLowerCase());
    }

    private static String clean(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        Node document = MARKDOWN_PARSER.parse(value.replace("\r\n", "\n").replace('\r', '\n'));
        return PLAIN_TEXT_RENDERER.render(document).strip();
    }
}
