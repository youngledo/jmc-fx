package com.youngledo.jmcfx.adapter.jmc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.openjdk.jmc.flightrecorder.rules.IResult;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.ResultToolkit;
import org.openjdk.jmc.flightrecorder.rules.RuleRegistry;
import org.openjdk.jmc.flightrecorder.rules.Severity;
import org.openjdk.jmc.flightrecorder.rules.TypedResult;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;

import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.RuleResult;
import com.youngledo.jmcfx.domain.service.JmcFxException;
import com.youngledo.jmcfx.domain.service.RuleAnalysisService;

/// JMC-backed rule analysis adapter.
///
/// Discovers rules via {@link RuleRegistry}, evaluates them against the
/// recording's {@link IItemCollection}, and maps results to domain records.
/// Message templates are resolved via {@link ResultToolkit#populateMessage}
/// for named placeholders and simple string replacement for positional ones.
/// Solution text ({@link IResult#getSolution()}) is appended to explanation.
public class JmcRuleAnalysisService implements RuleAnalysisService {

	private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");
	private static final Pattern CONSECUTIVE_BLANK_LINES = Pattern.compile("\n{3,}");

	@Override
	public List<RuleResult> analyze(RecordingSummary recording) {
		IItemCollection events = loadEvents(recording);
		Collection<IRule> rules = RuleRegistry.getRules();
		Map<IRule, Future<IResult>> futures = RulesToolkit.evaluateParallel(
				rules, events, DefaultPreferenceProvider.INSTANCE,
				Runtime.getRuntime().availableProcessors());
		List<RuleResult> results = new ArrayList<>();
		for (Map.Entry<IRule, Future<IResult>> entry : futures.entrySet()) {
			try {
				IResult result = entry.getValue().get();
				if (result == null) {
					continue;
				}
				Severity jmcSeverity = result.getSeverity();
				if (jmcSeverity == Severity.NA || jmcSeverity == Severity.IGNORE) {
					continue;
				}
				IRule rule = entry.getKey();
				results.add(new RuleResult(
						rule.getId(),
						rule.getName(),
						mapSeverity(jmcSeverity),
						severityToScore(jmcSeverity),
						rule.getTopic(),
						formatPlainText(rule, result, result.getSummary()),
						buildExplanation(rule, result)));
			} catch (Exception exception) {
				// Skip rules that fail to evaluate
			}
		}
		results.sort((a, b) -> Integer.compare(b.score(), a.score()));
		return List.copyOf(results);
	}

	static com.youngledo.jmcfx.domain.model.Severity mapSeverity(Severity jmcSeverity) {
		if (jmcSeverity == null) {
			return com.youngledo.jmcfx.domain.model.Severity.UNKNOWN;
		}
		return switch (jmcSeverity) {
			case OK -> com.youngledo.jmcfx.domain.model.Severity.OK;
			case INFO -> com.youngledo.jmcfx.domain.model.Severity.INFO;
			case WARNING -> com.youngledo.jmcfx.domain.model.Severity.WARNING;
			case NA, IGNORE -> com.youngledo.jmcfx.domain.model.Severity.UNKNOWN;
		};
	}

	/// Builds the full detail text by combining summary, explanation, and
	/// solution, matching how JMC's ResultOverview displays rule results.
	static String buildExplanation(IRule rule, IResult result) {
		String summary = formatRichText(rule, result, result.getSummary());
		String explanation = formatRichText(rule, result, result.getExplanation());
		String solution = formatRichText(rule, result, result.getSolution());
		StringBuilder sb = new StringBuilder();
		if (!summary.isEmpty()) {
			sb.append(summary);
		}
		if (!explanation.isEmpty()) {
			if (!sb.isEmpty()) {
				sb.append("<p>");
			}
			sb.append(explanation);
		}
		if (!solution.isEmpty()) {
			if (!sb.isEmpty()) {
				sb.append("<p>");
			}
			sb.append(solution);
		}
		return sb.toString();
	}

	/// Resolves placeholders without HTML, then strips any remaining tags
	/// for plain-text display (e.g. table cells).
	static String formatPlainText(IRule rule, IResult result, String raw) {
		String resolved = resolvePlaceholders(rule, result, raw, false);
		resolved = HTML_TAG.matcher(resolved).replaceAll("");
		resolved = CONSECUTIVE_BLANK_LINES.matcher(resolved).replaceAll("\n\n");
		return resolved.trim();
	}

	/// Resolves placeholders with HTML enabled so collection results
	/// produce {@code <ul>/<li>} lists, keeping HTML intact for rich-text
	/// rendering in the UI.
	static String formatRichText(IRule rule, IResult result, String raw) {
		return resolvePlaceholders(rule, result, raw, true).trim();
	}

	private static String resolvePlaceholders(IRule rule, IResult result, String raw, boolean withHtml) {
		if (raw == null || raw.isBlank()) {
			return "";
		}
		String populated = ResultToolkit.populateMessage(result, raw, withHtml);
		return resolvePositionalPlaceholders(rule, result, populated);
	}

	private static String resolvePositionalPlaceholders(IRule rule, IResult result, String text) {
		if (!text.contains("{0}")) {
			return text;
		}
		Collection<TypedResult<?>> typedResults = rule.getResults();
		if (typedResults == null || typedResults.isEmpty()) {
			return text;
		}
		List<Object> values = new ArrayList<>();
		for (TypedResult<?> tr : typedResults) {
			try {
				Object value = result.getResult(tr);
				values.add(value != null ? value : "");
			} catch (Exception e) {
				values.add("");
			}
		}
		String resolved = text;
		for (int i = values.size() - 1; i >= 0; i--) {
			resolved = resolved.replace("{" + i + "}", String.valueOf(values.get(i)));
		}
		return resolved;
	}

	private static int severityToScore(Severity severity) {
		if (severity == null) {
			return 0;
		}
		return (int) Math.round(severity.getLimit());
	}

	private IItemCollection loadEvents(RecordingSummary recording) {
		try {
			return JfrLoaderToolkit.loadEvents(recording.path().toFile());
		} catch (IOException | CouldNotLoadRecordingException exception) {
			throw new JmcFxException(
					"Unable to load recording for analysis: " + recording.path(), exception);
		}
	}

	private static final class DefaultPreferenceProvider implements IPreferenceValueProvider {
		static final DefaultPreferenceProvider INSTANCE = new DefaultPreferenceProvider();

		@Override
		public <T> T getPreferenceValue(TypedPreference<T> attribute) {
			return attribute.getDefaultValue();
		}
	}
}
