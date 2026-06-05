package io.github.youngledo.jmcfx.adapter.jmc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.youngledo.jmcfx.domain.model.HeapDumpAnalysisReport;
import io.github.youngledo.jmcfx.domain.model.HeapDumpIssue;
import io.github.youngledo.jmcfx.domain.model.HeapDumpIssueCategory;
import io.github.youngledo.jmcfx.domain.service.HeapDumpAnalysisService;
import io.github.youngledo.jmcfx.domain.service.JmcFxException;

import org.openjdk.jmc.joverflow.ReportGenerator;
import org.openjdk.jmc.joverflow.heap.parser.DumpCorruptedException;
import org.openjdk.jmc.joverflow.heap.parser.HprofParsingCancelledException;

public class JmcHeapDumpAnalysisService implements HeapDumpAnalysisService {

    private static final Pattern OBJECTS = Pattern.compile("Total num of objects:\\s*([0-9,]+)");
    private static final Pattern INSTANCE_COUNTS = Pattern.compile(
            "Instances:\\s*([0-9,]+),\\s*object arrays:\\s*([0-9,]+),\\s*primitive arrays:\\s*([0-9,]+)");
    private static final Pattern TOTAL_SIZE = Pattern.compile("Total size of all objects:\\s*([0-9,.]+)\\s*([KMGT]?)(?:B)?");
    private static final Pattern SIZE_PREFIX = Pattern.compile("^\\s*([0-9,.]+)\\s*([KMGT]?)(?:B)?\\s*:");
    private static final Pattern OBJECT_COUNT = Pattern.compile("\\b([0-9,]+)\\s+(?:instances|objects|strings|arrays)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final int MAX_ISSUES = 200;

    @Override
    public HeapDumpAnalysisReport analyze(Path hprofPath) {
        try {
            long fileSize = Files.size(hprofPath);
            ReportGenerator generator = ReportGenerator.parseDump(hprofPath.toString(), null, false);
            String textReport = generator.getReport(false, 8, new String[] {"oracle.apps."});
            ParsedJOverflowText parsed = parseTextReport(hprofPath, fileSize, textReport);
            return new HeapDumpAnalysisReport(hprofPath, fileSize, parsed.totalObjectSizeBytes(),
                    parsed.objectCount(), parsed.instanceCount(), parsed.objectArrayCount(),
                    parsed.primitiveArrayCount(), parsed.issues(), textReport);
        } catch (IOException | DumpCorruptedException | HprofParsingCancelledException exception) {
            throw new JmcFxException("Unable to analyze heap dump: " + exception.getMessage(), exception);
        } catch (RuntimeException exception) {
            throw new JmcFxException("Unable to analyze heap dump: " + exception.getMessage(), exception);
        }
    }

    static ParsedJOverflowText parseTextReport(Path path, long fileSizeBytes, String textReport) {
        String safeReport = textReport == null ? "" : textReport;
        long objectCount = findFirstLong(OBJECTS, safeReport, 1);
        long instanceCount = 0;
        long objectArrayCount = 0;
        long primitiveArrayCount = 0;
        Matcher counts = INSTANCE_COUNTS.matcher(safeReport);
        if (counts.find()) {
            instanceCount = parseLong(counts.group(1));
            objectArrayCount = parseLong(counts.group(2));
            primitiveArrayCount = parseLong(counts.group(3));
        }
        long totalObjectSizeBytes = findFirstSize(TOTAL_SIZE, safeReport);
        List<HeapDumpIssue> issues = parseIssues(safeReport);
        if (issues.isEmpty() && !safeReport.isBlank()) {
            issues = List.of(new HeapDumpIssue(HeapDumpIssueCategory.RAW_REPORT_SECTION,
                    path.getFileName().toString(), 0, totalObjectSizeBytes, objectCount, 0,
                    firstNonBlankLine(safeReport), ""));
        }
        return new ParsedJOverflowText(fileSizeBytes, totalObjectSizeBytes, objectCount, instanceCount,
                objectArrayCount, primitiveArrayCount, issues);
    }

    private static List<HeapDumpIssue> parseIssues(String textReport) {
        List<HeapDumpIssue> issues = new ArrayList<>();
        HeapDumpIssueCategory currentCategory = HeapDumpIssueCategory.RAW_REPORT_SECTION;
        String previousLine = "";
        for (String rawLine : textReport.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.chars().allMatch(ch -> ch == '-')) {
                continue;
            }
            HeapDumpIssueCategory headingCategory = categoryFromHeading(line);
            if (headingCategory != null) {
                currentCategory = headingCategory;
                previousLine = "";
                continue;
            }
            Matcher size = SIZE_PREFIX.matcher(line);
            if (!size.find()) {
                if (line.endsWith("-->")) {
                    previousLine = line.substring(0, line.length() - 3).trim();
                }
                continue;
            }
            long bytes = parseSize(size.group(1), size.group(2));
            long objectCount = findFirstLong(OBJECT_COUNT, line, 1);
            String subject = subjectFrom(line, previousLine);
            double score = bytes <= 0 ? 0 : 1.0;
            issues.add(new HeapDumpIssue(currentCategory, subject, bytes, bytes, objectCount,
                    score, line, previousLine));
            if (issues.size() >= MAX_ISSUES) {
                return List.copyOf(issues);
            }
            previousLine = "";
        }
        return List.copyOf(issues);
    }

    private static HeapDumpIssueCategory categoryFromHeading(String line) {
        String upper = line.toUpperCase(Locale.ROOT);
        if (upper.contains("DUPLICATED STRING")) {
            return HeapDumpIssueCategory.DUPLICATE_STRING;
        }
        if (upper.contains("DUPLICATED ARRAY")) {
            return HeapDumpIssueCategory.DUPLICATE_ARRAY;
        }
        if (upper.contains("WEAKHASHMAP")) {
            return HeapDumpIssueCategory.WEAK_HASH_MAP_BACK_REFERENCE;
        }
        if (upper.contains("COLLECTION") || upper.contains("OVERHEAD")) {
            return HeapDumpIssueCategory.COLLECTION_OVERHEAD;
        }
        if (upper.contains("HIGH MEMORY") || upper.contains("TOP MEMORY")) {
            return HeapDumpIssueCategory.HIGH_MEMORY_CONSUMER;
        }
        if (upper.contains("OBJECT HISTOGRAM")) {
            return HeapDumpIssueCategory.OBJECT_HISTOGRAM;
        }
        return null;
    }

    private static String subjectFrom(String line, String previousLine) {
        if (!previousLine.isBlank()) {
            return previousLine;
        }
        int colon = line.indexOf(':');
        String subject = colon >= 0 ? line.substring(colon + 1).trim() : line.trim();
        return subject.length() > 120 ? subject.substring(0, 117) + "..." : subject;
    }

    private static long findFirstLong(Pattern pattern, String text, int group) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? parseLong(matcher.group(group)) : 0;
    }

    private static long findFirstSize(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? parseSize(matcher.group(1), matcher.group(2)) : 0;
    }

    private static long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Long.parseLong(value.replace(",", ""));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private static long parseSize(String value, String unit) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            double number = Double.parseDouble(value.replace(",", ""));
            long multiplier = switch (unit == null ? "" : unit.toUpperCase(Locale.ROOT)) {
                case "T" -> 1024L * 1024L * 1024L * 1024L;
                case "G" -> 1024L * 1024L * 1024L;
                case "M" -> 1024L * 1024L;
                case "K" -> 1024L;
                default -> 1L;
            };
            return Math.max(0, (long) (number * multiplier));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private static String firstNonBlankLine(String text) {
        for (String line : text.split("\\R")) {
            if (!line.isBlank()) {
                return line.trim();
            }
        }
        return "";
    }

    record ParsedJOverflowText(
            long fileSizeBytes,
            long totalObjectSizeBytes,
            long objectCount,
            long instanceCount,
            long objectArrayCount,
            long primitiveArrayCount,
            List<HeapDumpIssue> issues) {

        ParsedJOverflowText {
            issues = List.copyOf(issues);
        }
    }
}
