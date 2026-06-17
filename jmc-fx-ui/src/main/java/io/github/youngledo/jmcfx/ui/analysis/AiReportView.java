package io.github.youngledo.jmcfx.ui.analysis;

import java.util.List;
import java.util.function.Consumer;

import io.github.youngledo.jmcfx.domain.model.ai.AiEvidence;
import io.github.youngledo.jmcfx.domain.model.ai.AiFinding;
import io.github.youngledo.jmcfx.domain.model.ai.AiRecordingReport;
import io.github.youngledo.jmcfx.domain.model.ai.AiSeverity;
import io.github.youngledo.jmcfx.ui.i18n.I18n;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.commonmark.node.Code;
import org.commonmark.node.Emphasis;
import org.commonmark.node.HardLineBreak;
import org.commonmark.node.Heading;
import org.commonmark.node.ListItem;
import org.commonmark.node.Paragraph;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.StrongEmphasis;
import org.commonmark.node.ThematicBreak;
import org.commonmark.parser.Parser;

/// Structured JavaFX report view for AI recording reports.
public final class AiReportView {

    private static final Parser MARKDOWN_PARSER = Parser.builder().build();

    private final ScrollPane scrollPane = new ScrollPane();
    private final VBox content = new VBox(12);
    private String loadingMessage = "";

    public AiReportView() {
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setMaxHeight(Double.MAX_VALUE);
        scrollPane.setContent(content);
        scrollPane.getStyleClass().add("ai-report-scroll");
        content.getStyleClass().add("ai-report");
        content.setPadding(new Insets(8, 0, 8, 0));
    }

    public Node node() {
        return scrollPane;
    }

    public void clear() {
        content.getChildren().clear();
        loadingMessage = "";
    }

    public void showLoading(I18n i18n) {
        showLoadingMessage(i18n.get("analysis.ai.report.generating"));
    }

    public void showStreamingResponse(I18n i18n) {
        showLoadingMessage(i18n.get("analysis.ai.report.receiving"));
    }

    public void showUnavailable(I18n i18n) {
        loadingMessage = "";
        content.getChildren().setAll(status(i18n.get("analysis.ai.report.empty")));
    }

    public void showError(String message) {
        loadingMessage = "";
        Label label = status(message);
        label.getStyleClass().add("ai-report-error");
        content.getChildren().setAll(label);
    }

    public void showReport(AiRecordingReport report, I18n i18n) {
        showReport(report, "", i18n, null);
    }

    public void showReport(AiRecordingReport report, String processingTime, I18n i18n) {
        showReport(report, processingTime, i18n, null);
    }

    public void showReport(AiRecordingReport report, String processingTime, I18n i18n,
            Consumer<String> relatedPageNavigator) {
        loadingMessage = "";
        content.getChildren().clear();
        if (report == null) {
            showUnavailable(i18n);
            return;
        }
        if (processingTime != null && !processingTime.isBlank()) {
            Label processed = new Label(i18n.format("analysis.ai.report.processedIn", processingTime));
            processed.getStyleClass().add("ai-report-meta");
            content.getChildren().add(processed);
            content.getChildren().add(markdown("---"));
        }
        appendSection(i18n.get("analysis.ai.report.summary"), markdown(report.summaryMarkdown()));
        if (!report.findings().isEmpty()) {
            Label title = sectionTitle(i18n.get("analysis.ai.report.findings"));
            VBox findings = new VBox(10);
            findings.getChildren().add(title);
            for (AiFinding finding : report.findings()) {
                findings.getChildren().add(findingNode(finding, i18n, relatedPageNavigator));
            }
            content.getChildren().add(findings);
        }
        appendListSection(i18n.get("analysis.ai.report.limitations"), report.contextLimitations());
        appendListSection(i18n.get("analysis.ai.report.followUpQuestions"), report.followUpQuestions());
    }

    private Node findingNode(AiFinding finding, I18n i18n, Consumer<String> relatedPageNavigator) {
        VBox box = new VBox(6);
        box.getStyleClass().add("ai-finding");

        HBox header = new HBox(8);
        header.getStyleClass().add("ai-finding-header");
        Label severity = new Label(severityText(finding.severity(), i18n));
        severity.getStyleClass().addAll("ai-severity", "ai-severity-" + finding.severity().name().toLowerCase());
        Label title = new Label(finding.title());
        title.getStyleClass().add("ai-finding-title");
        HBox.setHgrow(title, Priority.ALWAYS);
        Label confidence = new Label(i18n.format("analysis.ai.report.confidenceValue",
                Math.round(finding.confidence() * 100)));
        confidence.getStyleClass().add("ai-confidence");
        header.getChildren().setAll(severity, title, confidence);
        box.getChildren().add(header);

        Node recommendation = markdown(finding.recommendedNextStepMarkdown());
        if (recommendation != null) {
            box.getChildren().add(recommendation);
        }
        Node relatedPage = finding.relatedPageId().isBlank()
                ? null : relatedPageNode(finding.relatedPageId(), i18n, relatedPageNavigator);
        appendEvidence(box, finding.evidence(), i18n);
        Node limitations = markdown(finding.limitationsMarkdown());
        if (limitations != null) {
            VBox limitationBox = new VBox(4, smallTitle(i18n.get("analysis.ai.report.limitations")), limitations);
            box.getChildren().add(limitationBox);
        }
        if (relatedPage != null) {
            box.getChildren().add(relatedPage);
        }
        return box;
    }

    private Node relatedPageNode(String relatedPageId, I18n i18n, Consumer<String> relatedPageNavigator) {
        String text = i18n.format("analysis.ai.report.relatedPageValue", relatedPageId);
        if (relatedPageNavigator == null) {
            Label label = new Label(text);
            label.getStyleClass().add("ai-related-page");
            return label;
        }
        Hyperlink link = new Hyperlink(text);
        link.getStyleClass().add("ai-related-page");
        link.setOnAction(event -> {
            relatedPageNavigator.accept(relatedPageId);
            link.setVisited(false);
        });
        return link;
    }

    private void appendEvidence(VBox parent, List<AiEvidence> evidence, I18n i18n) {
        if (evidence == null || evidence.isEmpty()) {
            return;
        }
        VBox evidenceBox = new VBox(4);
        evidenceBox.getChildren().add(smallTitle(i18n.get("analysis.ai.report.evidence")));
        for (AiEvidence item : evidence) {
            String line = evidenceLine(item);
            if (!line.isBlank()) {
                Label label = new Label("• " + line);
                label.setWrapText(true);
                label.getStyleClass().add("ai-evidence-item");
                evidenceBox.getChildren().add(label);
            }
        }
        if (evidenceBox.getChildren().size() > 1) {
            parent.getChildren().add(evidenceBox);
        }
    }

    private void appendSection(String title, Node body) {
        if (body != null) {
            content.getChildren().add(new VBox(6, sectionTitle(title), body));
        }
    }

    private void appendListSection(String title, List<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        VBox box = new VBox(6);
        box.getChildren().add(sectionTitle(title));
        for (String value : values) {
            Node markdown = markdown(value);
            if (markdown != null) {
                HBox row = new HBox(6, new Label("•"), markdown);
                row.getStyleClass().add("ai-report-list-item");
                box.getChildren().add(row);
            }
        }
        if (box.getChildren().size() > 1) {
            content.getChildren().add(box);
        }
    }

    private Node markdown(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return null;
        }
        VBox blocks = new VBox(6);
        blocks.getStyleClass().addAll("ai-markdown", "ai-markdown-blocks");
        appendMarkdownBlocks(blocks, MARKDOWN_PARSER.parse(markdown));
        if (!blocks.getChildren().isEmpty()) {
            return blocks;
        }
        TextFlow flow = new TextFlow();
        flow.getStyleClass().add("ai-markdown");
        appendMarkdown(flow, MARKDOWN_PARSER.parse(markdown), false, false);
        return flow;
    }

    private void appendMarkdownBlocks(VBox blocks, org.commonmark.node.Node node) {
        for (org.commonmark.node.Node child = node.getFirstChild(); child != null; child = child.getNext()) {
            switch (child) {
                case Heading heading -> blocks.getChildren().add(markdownFlow(heading, "ai-md-heading"));
                case Paragraph paragraph -> blocks.getChildren().add(markdownFlow(paragraph));
                case org.commonmark.node.BulletList list -> appendList(blocks, list);
                case org.commonmark.node.OrderedList list -> appendList(blocks, list);
                case ThematicBreak ignored -> blocks.getChildren().add(thematicBreak());
                default -> appendMarkdownBlocks(blocks, child);
            }
        }
    }

    private Region thematicBreak() {
        Region region = new Region();
        region.getStyleClass().add("ai-md-thematic-break");
        return region;
    }

    private void appendList(VBox blocks, org.commonmark.node.Node list) {
        for (org.commonmark.node.Node item = list.getFirstChild(); item != null; item = item.getNext()) {
            if (item instanceof ListItem listItem) {
                TextFlow flow = markdownFlow(listItem, "ai-md-list-item");
                if (!flow.getChildren().isEmpty()) {
                    Text bullet = new Text("• ");
                    bullet.getStyleClass().add("ai-md-bullet");
                    flow.getChildren().addFirst(bullet);
                    blocks.getChildren().add(flow);
                }
            }
        }
    }

    private TextFlow markdownFlow(org.commonmark.node.Node node, String... styleClasses) {
        TextFlow flow = new TextFlow();
        flow.getStyleClass().add("ai-markdown-line");
        flow.getStyleClass().addAll(styleClasses);
        appendMarkdown(flow, node, false, false);
        return flow;
    }

    private void appendMarkdown(TextFlow flow, org.commonmark.node.Node node, boolean bold, boolean italic) {
        for (org.commonmark.node.Node child = node.getFirstChild(); child != null; child = child.getNext()) {
            switch (child) {
                case org.commonmark.node.Text text -> appendText(flow, text.getLiteral(), bold, italic, false);
                case Code code -> appendText(flow, code.getLiteral(), false, false, true);
                case StrongEmphasis strong -> appendMarkdown(flow, strong, true, italic);
                case Emphasis emphasis -> appendMarkdown(flow, emphasis, bold, true);
                case SoftLineBreak ignored -> appendText(flow, "\n", false, false, false);
                case HardLineBreak ignored -> appendText(flow, "\n", false, false, false);
                default -> appendMarkdown(flow, child, bold, italic);
            }
        }
    }

    private void appendText(TextFlow flow, String value, boolean bold, boolean italic, boolean code) {
        if (value == null || value.isEmpty()) {
            return;
        }
        Text text = new Text(value);
        if (bold) {
            text.getStyleClass().add("ai-md-bold");
        }
        if (italic) {
            text.getStyleClass().add("ai-md-italic");
        }
        if (code) {
            text.getStyleClass().add("ai-md-code");
        }
        flow.getChildren().add(text);
    }

    private Label sectionTitle(String value) {
        Label label = new Label(value);
        label.getStyleClass().add("ai-report-section-title");
        return label;
    }

    private Label smallTitle(String value) {
        Label label = new Label(value);
        label.getStyleClass().add("ai-report-small-title");
        return label;
    }

    private Label status(String value) {
        Label label = new Label(value);
        label.setWrapText(true);
        label.getStyleClass().add("empty-state");
        return label;
    }

    private Node loadingStatus(String value) {
        ProgressIndicator indicator = new ProgressIndicator();
        indicator.setPrefSize(20, 20);
        indicator.setMinSize(20, 20);
        indicator.setMaxSize(20, 20);
        Label label = status(value);
        HBox row = new HBox(8, indicator, label);
        row.getStyleClass().add("ai-report-loading");
        return row;
    }

    private String severityText(AiSeverity severity, I18n i18n) {
        AiSeverity value = severity == null ? AiSeverity.UNKNOWN : severity;
        return i18n.get("analysis.ai.report.severity." + value.name().toLowerCase());
    }

    private static String evidenceLine(AiEvidence evidence) {
        String label = evidence.label().strip();
        String value = evidence.value().strip();
        String source = evidence.source().strip();
        if (label.isBlank() && value.isBlank()) {
            return "";
        }
        String line = label.isBlank() ? value : value.isBlank() ? label : label + ": " + value;
        return source.isBlank() ? line : line + " (" + source + ")";
    }

    private void showLoadingMessage(String message) {
        if (message.equals(loadingMessage)
                && content.getChildren().size() == 1
                && hasStyleClass(content.getChildren().getFirst(), "ai-report-loading")) {
            return;
        }
        loadingMessage = message;
        content.getChildren().setAll(loadingStatus(message));
    }

    private static boolean hasStyleClass(Node node, String styleClass) {
        if (node.getStyleClass().contains(styleClass)) {
            return true;
        }
        if (node instanceof javafx.scene.Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                if (hasStyleClass(child, styleClass)) {
                    return true;
                }
            }
        }
        return false;
    }
}
