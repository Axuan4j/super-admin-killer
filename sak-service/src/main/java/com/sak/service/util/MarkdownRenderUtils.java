package com.sak.service.util;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MarkdownRenderUtils {

    private static final Pattern IMAGE_PATTERN = Pattern.compile("!\\[(.*?)]\\((.+?)\\)");
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[(.*?)]\\((.+?)\\)");
    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern ITALIC_PATTERN = Pattern.compile("(?<!\\*)\\*(?!\\s)(.+?)(?<!\\s)\\*(?!\\*)");
    private static final Pattern INLINE_CODE_PATTERN = Pattern.compile("`([^`]+)`");

    private MarkdownRenderUtils() {
    }

    public static String render(String markdown) {
        if (!StringUtils.hasText(markdown)) {
            return "";
        }
        String normalized = markdown.replace("\r\n", "\n");
        String[] lines = normalized.split("\n");
        StringBuilder html = new StringBuilder();
        List<String> paragraphLines = new ArrayList<>();
        List<String> listItems = new ArrayList<>();
        boolean orderedList = false;
        boolean inCodeBlock = false;
        StringBuilder codeBlock = new StringBuilder();

        for (String rawLine : lines) {
            String line = rawLine == null ? "" : rawLine;
            String trimmed = line.trim();
            if (trimmed.startsWith("```")) {
                flushParagraph(html, paragraphLines);
                flushList(html, listItems, orderedList);
                if (inCodeBlock) {
                    html.append("<pre><code>")
                            .append(escapeHtml(codeBlock.toString()))
                            .append("</code></pre>");
                    inCodeBlock = false;
                    codeBlock.setLength(0);
                } else {
                    inCodeBlock = true;
                }
                continue;
            }
            if (inCodeBlock) {
                if (codeBlock.length() > 0) {
                    codeBlock.append('\n');
                }
                codeBlock.append(line);
                continue;
            }
            if (!StringUtils.hasText(trimmed)) {
                flushParagraph(html, paragraphLines);
                flushList(html, listItems, orderedList);
                continue;
            }

            if (trimmed.startsWith("#")) {
                flushParagraph(html, paragraphLines);
                flushList(html, listItems, orderedList);
                int level = 0;
                while (level < trimmed.length() && trimmed.charAt(level) == '#') {
                    level++;
                }
                level = Math.max(1, Math.min(level, 6));
                String content = trimmed.substring(level).trim();
                html.append("<h").append(level).append(">")
                        .append(applyInline(content))
                        .append("</h").append(level).append(">");
                continue;
            }

            if (trimmed.startsWith(">")) {
                flushParagraph(html, paragraphLines);
                flushList(html, listItems, orderedList);
                html.append("<blockquote>")
                        .append(applyInline(trimmed.substring(1).trim()))
                        .append("</blockquote>");
                continue;
            }

            if (trimmed.matches("[-*+]\\s+.+")) {
                if (!listItems.isEmpty() && orderedList) {
                    flushList(html, listItems, true);
                }
                orderedList = false;
                listItems.add(trimmed.substring(2).trim());
                continue;
            }

            if (trimmed.matches("\\d+\\.\\s+.+")) {
                if (!listItems.isEmpty() && !orderedList) {
                    flushList(html, listItems, false);
                }
                orderedList = true;
                listItems.add(trimmed.replaceFirst("\\d+\\.\\s+", ""));
                continue;
            }

            paragraphLines.add(trimmed);
        }

        if (inCodeBlock) {
            html.append("<pre><code>")
                    .append(escapeHtml(codeBlock.toString()))
                    .append("</code></pre>");
        }
        flushParagraph(html, paragraphLines);
        flushList(html, listItems, orderedList);
        return html.toString();
    }

    private static void flushParagraph(StringBuilder html, List<String> paragraphLines) {
        if (paragraphLines.isEmpty()) {
            return;
        }
        html.append("<p>")
                .append(applyInline(String.join("<br/>", paragraphLines)))
                .append("</p>");
        paragraphLines.clear();
    }

    private static void flushList(StringBuilder html, List<String> listItems, boolean orderedList) {
        if (listItems.isEmpty()) {
            return;
        }
        html.append(orderedList ? "<ol>" : "<ul>");
        for (String item : listItems) {
            html.append("<li>").append(applyInline(item)).append("</li>");
        }
        html.append(orderedList ? "</ol>" : "</ul>");
        listItems.clear();
    }

    private static String applyInline(String value) {
        String escaped = escapeHtml(value);
        escaped = replacePattern(escaped, IMAGE_PATTERN, "<img alt=\"$1\" src=\"$2\" />");
        escaped = replacePattern(escaped, LINK_PATTERN, "<a href=\"$2\" target=\"_blank\" rel=\"noopener noreferrer\">$1</a>");
        escaped = replacePattern(escaped, BOLD_PATTERN, "<strong>$1</strong>");
        escaped = replacePattern(escaped, ITALIC_PATTERN, "<em>$1</em>");
        return replacePattern(escaped, INLINE_CODE_PATTERN, "<code>$1</code>");
    }

    private static String replacePattern(String value, Pattern pattern, String replacement) {
        Matcher matcher = pattern.matcher(value);
        return matcher.replaceAll(replacement);
    }

    private static String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
