package io.github.youngledo.jmcfx.application.ai;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.youngledo.jmcfx.domain.model.ai.AiAssistantAnswer;
import io.github.youngledo.jmcfx.domain.model.ai.AiEvidence;
import io.github.youngledo.jmcfx.domain.model.ai.AiFinding;
import io.github.youngledo.jmcfx.domain.model.ai.AiRecordingReport;
import io.github.youngledo.jmcfx.domain.model.ai.AiSeverity;

final class AiReportJsonParser {

    private static final int MAX_FINDINGS = 8;
    private static final int MAX_EVIDENCE = 5;
    private static final int MAX_FOLLOW_UP_QUESTIONS = 6;

    private final ObjectMapper objectMapper = new ObjectMapper();

    AiRecordingReport parse(String responseText) {
        JsonNode root = readObject(reportJson(responseText), "AI report JSON must be an object");
        return new AiRecordingReport(
                text(root, "summaryMarkdown"),
                findings(root.path("findings")),
                strings(root.path("followUpQuestions"), MAX_FOLLOW_UP_QUESTIONS),
                strings(root.path("contextLimitations"), MAX_FOLLOW_UP_QUESTIONS));
    }

    AiAssistantAnswer parseAnswer(String json) {
        JsonNode root = readObject(json, "AI answer JSON must be an object");
        return new AiAssistantAnswer(
                text(root, "answerMarkdown"),
                strings(root.path("followUpQuestions"), MAX_FOLLOW_UP_QUESTIONS));
    }

    private JsonNode readObject(String json, String message) {
        try {
            JsonNode root = objectMapper.readTree(json == null ? "" : json);
            if (root == null || !root.isObject()) {
                throw new IllegalArgumentException(message);
            }
            return root;
        } catch (IOException e) {
            throw new IllegalArgumentException(message, e);
        }
    }

    private static String reportJson(String responseText) {
        String text = responseText == null ? "" : responseText.strip();
        if (text.startsWith("{")) {
            return text;
        }
        String fence = "```jmcfx-report-json";
        int fenceStart = text.lastIndexOf(fence);
        if (fenceStart < 0) {
            return text;
        }
        int jsonStart = text.indexOf('\n', fenceStart + fence.length());
        if (jsonStart < 0) {
            return text;
        }
        int jsonEnd = text.indexOf("```", jsonStart + 1);
        if (jsonEnd < 0) {
            return text;
        }
        return text.substring(jsonStart + 1, jsonEnd).strip();
    }

    private static List<AiFinding> findings(JsonNode node) {
        List<AiFinding> findings = new ArrayList<>();
        if (!node.isArray()) {
            return findings;
        }
        for (JsonNode finding : node) {
            if (findings.size() == MAX_FINDINGS) {
                break;
            }
            if (finding.isObject()) {
                findings.add(new AiFinding(
                        text(finding, "title"),
                        AiSeverity.fromText(text(finding, "severity")),
                        number(finding, "confidence"),
                        text(finding, "relatedPageId"),
                        text(finding, "recommendedNextStepMarkdown"),
                        text(finding, "limitationsMarkdown"),
                        evidence(finding.path("evidence"))));
            }
        }
        return findings;
    }

    private static List<AiEvidence> evidence(JsonNode node) {
        List<AiEvidence> evidence = new ArrayList<>();
        if (!node.isArray()) {
            return evidence;
        }
        for (JsonNode item : node) {
            if (evidence.size() == MAX_EVIDENCE) {
                break;
            }
            if (item.isObject()) {
                evidence.add(new AiEvidence(
                        text(item, "label"),
                        text(item, "value"),
                        text(item, "source"),
                        text(item, "relatedPageId"),
                        text(item, "relatedEntityId")));
            }
        }
        return evidence;
    }

    private static List<String> strings(JsonNode node, int max) {
        List<String> strings = new ArrayList<>();
        if (!node.isArray()) {
            return strings;
        }
        for (JsonNode value : node) {
            if (strings.size() == max) {
                break;
            }
            if (value.isTextual()) {
                strings.add(value.asText());
            }
        }
        return strings;
    }

    private static String text(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isTextual() ? value.asText() : "";
    }

    private static double number(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isNumber() ? value.asDouble() : 0.0;
    }
}
