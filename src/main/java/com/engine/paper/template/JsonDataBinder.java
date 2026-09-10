package com.engine.paper.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fast JSON expression binder supporting dot-notation property lookup,
 * iterations ({{#each list}}), conditionals ({{#if flag}}), and filters ({{val | currency}}).
 */
public class JsonDataBinder {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([^}]+?)\\s*}}");
    private static final Pattern EACH_PATTERN = Pattern.compile("\\{\\{#each\\s+([\\w.]+)\\s*}}(.*?)\\{\\{/each}}", Pattern.DOTALL);
    private static final Pattern IF_PATTERN = Pattern.compile("\\{\\{#if\\s+([\\w.]+)\\s*}}(.*?)(?:\\{\\{else}}(.*?))?\\{\\{/if}}", Pattern.DOTALL);

    /**
     * Interpolates JSON dataset into template string.
     */
    public String bind(String template, String json) {
        if (template == null || template.isEmpty()) {
            return "";
        }
        if (json == null || json.isBlank()) {
            return template;
        }

        try {
            JsonNode root = MAPPER.readTree(json);
            return bind(template, root);
        } catch (Exception e) {
            return template;
        }
    }

    public String bind(String template, JsonNode context) {
        if (template == null || context == null) {
            return template != null ? template : "";
        }

        String processed = processConditionals(template, context);
        processed = processLoops(processed, context);
        return processVariables(processed, context);
    }

    private String processConditionals(String template, JsonNode context) {
        Matcher matcher = IF_PATTERN.matcher(template);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String path = matcher.group(1).trim();
            String ifBlock = matcher.group(2);
            String elseBlock = matcher.group(3) != null ? matcher.group(3) : "";

            JsonNode val = resolvePath(context, path);
            boolean isTrue = val != null && !val.isNull() && (
                    (val.isBoolean() && val.asBoolean()) ||
                    (val.isNumber() && val.asDouble() != 0) ||
                    (val.isTextual() && !val.asText().isBlank()) ||
                    (val.isArray() && !val.isEmpty())
            );

            matcher.appendReplacement(sb, Matcher.quoteReplacement(isTrue ? ifBlock : elseBlock));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String processLoops(String template, JsonNode context) {
        Matcher matcher = EACH_PATTERN.matcher(template);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String listPath = matcher.group(1).trim();
            String itemTemplate = matcher.group(2);

            JsonNode arrayNode = resolvePath(context, listPath);
            StringBuilder loopOutput = new StringBuilder();

            if (arrayNode != null && arrayNode.isArray()) {
                String cleanItemTemplate = itemTemplate.strip();
                for (int idx = 0; idx < arrayNode.size(); idx++) {
                    String rowStr = processVariables(cleanItemTemplate, arrayNode.get(idx));
                    loopOutput.append(rowStr).append("\n");
                }
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(loopOutput.toString()));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String processVariables(String template, JsonNode context) {
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String expr = matcher.group(1).trim();
            if (expr.startsWith("#") || expr.startsWith("/")) {
                // Ignore unmatched control tokens
                continue;
            }

            String[] parts = expr.split("\\|", 2);
            String path = parts[0].trim();
            String filter = parts.length > 1 ? parts[1].trim() : null;

            JsonNode node = resolvePath(context, path);
            String value = "";

            if (node != null && !node.isNull()) {
                if (filter != null) {
                    value = applyFilter(node, filter);
                } else {
                    value = node.isValueNode() ? node.asText() : node.toString();
                }
            }

            matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private JsonNode resolvePath(JsonNode root, String path) {
        if (root == null || path == null || path.isEmpty() || path.equals("this") || path.equals(".")) {
            return root;
        }

        String[] segments = path.split("\\.");
        JsonNode curr = root;

        for (String seg : segments) {
            if (curr == null || curr.isNull()) {
                return null;
            }
            if (seg.contains("[") && seg.endsWith("]")) {
                int open = seg.indexOf('[');
                String prop = seg.substring(0, open);
                int idx = Integer.parseInt(seg.substring(open + 1, seg.length() - 1));
                curr = prop.isEmpty() ? curr.get(idx) : curr.path(prop).get(idx);
            } else {
                curr = curr.get(seg);
            }
        }
        return curr;
    }

    private String applyFilter(JsonNode node, String filter) {
        if (filter.equalsIgnoreCase("currency")) {
            double amount = node.isNumber() ? node.asDouble() : Double.parseDouble(node.asText());
            return NumberFormat.getCurrencyInstance(Locale.US).format(amount);
        } else if (filter.equalsIgnoreCase("uppercase")) {
            return node.asText().toUpperCase(Locale.ROOT);
        } else if (filter.equalsIgnoreCase("lowercase")) {
            return node.asText().toLowerCase(Locale.ROOT);
        }
        return node.asText();
    }
}
