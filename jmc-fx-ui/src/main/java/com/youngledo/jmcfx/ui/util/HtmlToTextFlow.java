package com.youngledo.jmcfx.ui.util;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Set;

import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/// Converts simple HTML fragments from JMC rule results to JavaFX
/// {@link TextFlow} nodes or plain text.
///
/// Handles: {@code <p>}, {@code <br>}, {@code <b>}/{@code <strong>},
/// {@code <i>}/{@code <em>}, {@code <ul>}/{@code <li>}, and bare text.
/// Unknown tags and non-tag {@code <} characters are kept as literal text.
public final class HtmlToTextFlow {

	private static final Set<String> KNOWN_TAGS = Set.of(
			"p", "br", "b", "strong", "i", "em", "ul", "ol", "li", "a", "div", "span"
	);

	private static final Map<String, String> ENTITIES = Map.of(
			"&amp;", "&",
			"&lt;", "<",
			"&gt;", ">",
			"&quot;", "\"",
			"&apos;", "'"
	);

	private HtmlToTextFlow() {}

	public static void render(TextFlow flow, String html) {
		flow.getChildren().clear();
		if (html == null || html.isBlank()) {
			return;
		}
		Deque<String> styleStack = new ArrayDeque<>();
		StringBuilder buffer = new StringBuilder();
		int i = 0;
		while (i < html.length()) {
			if (html.charAt(i) == '<' && isPossibleTag(html, i + 1)) {
				int end = html.indexOf('>', i);
				if (end == -1) {
					buffer.append(html.charAt(i));
					i++;
					continue;
				}
				String tagContent = html.substring(i + 1, end).trim();
				boolean closing = tagContent.startsWith("/");
				if (closing) {
					tagContent = tagContent.substring(1).trim();
				}
				int space = tagContent.indexOf(' ');
				String tagName = (space > 0 ? tagContent.substring(0, space) : tagContent).toLowerCase();
				if (KNOWN_TAGS.contains(tagName)) {
					flushBuffer(flow, buffer, styleStack);
					if (closing) {
						handleClosingTag(tagName, styleStack, buffer);
					} else {
						handleOpeningTag(tagName, styleStack, buffer);
					}
					i = end + 1;
				} else {
					buffer.append(html.charAt(i));
					i++;
				}
			} else {
				buffer.append(html.charAt(i));
				i++;
			}
		}
		flushBuffer(flow, buffer, styleStack);
	}

	/// Converts HTML to formatted plain text, preserving structure
	/// (paragraphs, bullets, line breaks) without JavaFX node overhead.
	public static String toPlainText(String html) {
		if (html == null || html.isBlank()) {
			return "";
		}
		StringBuilder buffer = new StringBuilder();
		int i = 0;
		while (i < html.length()) {
			if (html.charAt(i) == '<' && isPossibleTag(html, i + 1)) {
				int end = html.indexOf('>', i);
				if (end == -1) {
					buffer.append(html.charAt(i));
					i++;
					continue;
				}
				String tagContent = html.substring(i + 1, end).trim();
				boolean closing = tagContent.startsWith("/");
				if (closing) {
					tagContent = tagContent.substring(1).trim();
				}
				int space = tagContent.indexOf(' ');
				String tagName = (space > 0 ? tagContent.substring(0, space) : tagContent).toLowerCase();
				if (KNOWN_TAGS.contains(tagName)) {
					if (closing) {
						handleClosingTagPlain(tagName, buffer);
					} else {
						handleOpeningTagPlain(tagName, buffer);
					}
					i = end + 1;
				} else {
					buffer.append(html.charAt(i));
					i++;
				}
			} else {
				buffer.append(html.charAt(i));
				i++;
			}
		}
		return decodeEntities(buffer.toString()).trim();
	}

	private static boolean isPossibleTag(String html, int pos) {
		if (pos >= html.length()) {
			return false;
		}
		char next = html.charAt(pos);
		return Character.isLetter(next) || next == '/';
	}

	private static void flushBuffer(TextFlow flow, StringBuilder buffer, Deque<String> styleStack) {
		if (buffer.isEmpty()) {
			return;
		}
		String text = decodeEntities(buffer.toString());
		Text node = new Text(text);
		for (String style : styleStack) {
			node.getStyleClass().add(style);
		}
		flow.getChildren().add(node);
		buffer.setLength(0);
	}

	private static void handleOpeningTag(String tag, Deque<String> styleStack, StringBuilder buffer) {
		switch (tag) {
			case "b", "strong" -> styleStack.push("html-bold");
			case "i", "em" -> styleStack.push("html-italic");
			case "br" -> buffer.append('\n');
			case "p" -> {
				if (!buffer.isEmpty() || !styleStack.isEmpty()) {
					buffer.append("\n\n");
				}
			}
			case "li" -> buffer.append("\n• ");
			default -> { /* a, div, span — consume tag, no visual effect */ }
		}
	}

	private static void handleClosingTag(String tag, Deque<String> styleStack, StringBuilder buffer) {
		switch (tag) {
			case "b", "strong", "i", "em" -> {
				if (!styleStack.isEmpty()) {
					styleStack.pop();
				}
			}
			case "p" -> buffer.append("\n");
			case "ul", "ol" -> buffer.append('\n');
			default -> { /* a, div, span — consume tag, no visual effect */ }
		}
	}

	private static void handleOpeningTagPlain(String tag, StringBuilder buffer) {
		switch (tag) {
			case "br" -> buffer.append('\n');
			case "p" -> {
				if (!buffer.isEmpty()) {
					buffer.append("\n\n");
				}
			}
			case "li" -> buffer.append("\n• ");
			default -> { /* b, i, a, div, span — no visual effect in plain text */ }
		}
	}

	private static void handleClosingTagPlain(String tag, StringBuilder buffer) {
		switch (tag) {
			case "p" -> buffer.append("\n");
			case "ul", "ol" -> buffer.append('\n');
			default -> { /* b, i, a, div, span — no visual effect in plain text */ }
		}
	}

	private static String decodeEntities(String text) {
		String result = text;
		for (Map.Entry<String, String> entry : ENTITIES.entrySet()) {
			result = result.replace(entry.getKey(), entry.getValue());
		}
		return result;
	}
}
