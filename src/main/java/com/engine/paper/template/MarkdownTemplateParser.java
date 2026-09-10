package com.engine.paper.template;

import com.engine.paper.domain.element.*;
import com.engine.paper.domain.layout.FlexDirection;
import com.engine.paper.domain.layout.JustifyContent;
import com.engine.paper.domain.model.Color;
import com.engine.paper.domain.model.Document;
import com.engine.paper.domain.model.Margin;
import com.engine.paper.domain.model.PageSize;
import com.engine.paper.domain.style.BorderStyle;
import com.engine.paper.domain.style.ElementStyle;
import com.engine.paper.domain.style.TextStyle;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser converting Markdown text, inline barcode tags, and embedded CSS blocks into Document AST.
 */
public class MarkdownTemplateParser {

    private static final Pattern STYLE_BLOCK_PATTERN = Pattern.compile("<style>(.*?)</style>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern BARCODE_PATTERN = Pattern.compile("\\[barcode:(\\w+)\\s+content=[\"'](.*?)[\"'](?:\\s+size=[\"'](.*?)[\"'])?(?:\\s+width=[\"'](.*?)[\"'])?(?:\\s+height=[\"'](.*?)[\"'])?\\s*]");
    private static final Pattern CSS_RULE_PATTERN = Pattern.compile("([@.#\\w-]+)\\s*\\{([^}]+)}");

    public Document parse(String markdown) {
        Document document = new Document();
        if (markdown == null || markdown.isBlank()) {
            return document;
        }

        Map<String, Map<String, String>> cssRules = new HashMap<>();

        // 1. Extract and parse <style> blocks
        Matcher styleMatcher = STYLE_BLOCK_PATTERN.matcher(markdown);
        StringBuffer cleanMarkdown = new StringBuffer();
        while (styleMatcher.find()) {
            parseCss(styleMatcher.group(1), cssRules, document);
            styleMatcher.appendReplacement(cleanMarkdown, "");
        }
        styleMatcher.appendTail(cleanMarkdown);

        String content = cleanMarkdown.toString();

        // 2. Parse lines into AST
        String[] lines = content.split("\r?\n");
        int i = 0;

        while (i < lines.length) {
            String line = lines[i].trim();

            if (line.isEmpty()) {
                i++;
                continue;
            }

            // Page Break marker
            if (line.equals("<!-- pagebreak -->") || line.equals("[pagebreak]")) {
                document.addElement(new PageBreakElement());
                i++;
                continue;
            }

            // Thematic break / horizontal rule
            if (line.equals("---") || line.equals("***") || line.equals("___")) {
                document.addElement(DividerElement.standard());
                i++;
                continue;
            }

            // Barcode Tag: [barcode:qr content="..." size="..."]
            Matcher barcodeMatcher = BARCODE_PATTERN.matcher(line);
            if (barcodeMatcher.matches()) {
                String typeStr = barcodeMatcher.group(1);
                String payload = barcodeMatcher.group(2);
                String sizeStr = barcodeMatcher.group(3);
                String wStr = barcodeMatcher.group(4);
                String hStr = barcodeMatcher.group(5);

                double w = parseDimension(wStr != null ? wStr : sizeStr, 80.0);
                double h = parseDimension(hStr != null ? hStr : sizeStr, 80.0);

                BarcodeElement.BarcodeType bType = "code128".equalsIgnoreCase(typeStr)
                        ? BarcodeElement.BarcodeType.CODE_128
                        : BarcodeElement.BarcodeType.QR_CODE;

                document.addElement(new BarcodeElement(bType, payload, w, h));
                i++;
                continue;
            }

            // Headings
            if (line.startsWith("#")) {
                int level = 0;
                while (level < line.length() && line.charAt(level) == '#') {
                    level++;
                }
                String headingText = line.substring(level).trim();
                TextBlock heading = switch (level) {
                    case 1 -> TextBlock.h1(headingText);
                    case 2 -> TextBlock.h2(headingText);
                    default -> TextBlock.h3(headingText);
                };
                document.addElement(heading);
                i++;
                continue;
            }

            // Table Block: lines starting with "|"
            if (line.startsWith("|")) {
                List<String> tableLines = new ArrayList<>();
                while (i < lines.length) {
                    String cur = lines[i].trim();
                    if (cur.startsWith("|")) {
                        tableLines.add(cur);
                        i++;
                    } else if (cur.isEmpty() && i + 1 < lines.length && lines[i + 1].trim().startsWith("|")) {
                        // Tolerate empty line generated inside table templates
                        i++;
                    } else {
                        break;
                    }
                }
                TableElement table = parseTable(tableLines);
                if (table != null) {
                    document.addElement(table);
                }
                continue;
            }

            // Normal text paragraph
            TextBlock textBlock = new TextBlock(line);
            document.addElement(textBlock);
            i++;
        }

        return document;
    }

    private void parseCss(String css, Map<String, Map<String, String>> cssRules, Document document) {
        if (css == null) {
            return;
        }

        Matcher matcher = CSS_RULE_PATTERN.matcher(css);
        while (matcher.find()) {
            String selector = matcher.group(1).trim();
            String body = matcher.group(2).trim();

            Map<String, String> properties = new HashMap<>();
            String[] decls = body.split(";");
            for (String decl : decls) {
                String[] kv = decl.split(":", 2);
                if (kv.length == 2) {
                    properties.put(kv[0].trim().toLowerCase(), kv[1].trim());
                }
            }
            cssRules.put(selector, properties);

            if (selector.equalsIgnoreCase("@page")) {
                applyPageProperties(properties, document);
            }
        }
    }

    private void applyPageProperties(Map<String, String> properties, Document document) {
        if (properties.containsKey("size")) {
            String size = properties.get("size").toUpperCase();
            switch (size) {
                case "A3" -> document.setPageSize(PageSize.A3);
                case "A4" -> document.setPageSize(PageSize.A4);
                case "A5" -> document.setPageSize(PageSize.A5);
                case "LETTER" -> document.setPageSize(PageSize.LETTER);
                case "LEGAL" -> document.setPageSize(PageSize.LEGAL);
            }
        }

        if (properties.containsKey("margin")) {
            double m = parseDimension(properties.get("margin"), 36.0);
            document.setMargin(Margin.all(m));
        }
    }

    private TableElement parseTable(List<String> lines) {
        if (lines.isEmpty()) {
            return null;
        }

        TableElement table = new TableElement();
        table.getStyle().setBorderStyle(BorderStyle.solid(1.0, Color.SLATE_200));

        boolean hasHeader = lines.size() > 1 && lines.get(1).contains("---");
        int startIndex = 0;

        if (hasHeader) {
            // First line is header
            TableRow hRow = parseTableRow(lines.get(0), true);
            hRow.getStyle().setBackgroundColor(Color.SLATE_100);
            table.addHeaderRow(hRow);
            startIndex = 2; // Skip separator line
        }

        for (int j = startIndex; j < lines.size(); j++) {
            String rowLine = lines.get(j);
            if (rowLine.contains("---")) {
                continue;
            }
            TableRow bRow = parseTableRow(rowLine, false);
            table.addBodyRow(bRow);
        }

        return table;
    }

    private TableRow parseTableRow(String line, boolean isHeader) {
        TableRow row = new TableRow(isHeader);
        String[] parts = line.split("\\|");

        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                TableCell cell = new TableCell(trimmed);
                if (isHeader) {
                    cell.getContent().getStyle().setTextStyle(TextStyle.DEFAULT.withBold(true));
                }
                row.addCell(cell);
            }
        }
        return row;
    }

    private double parseDimension(String val, double defaultValue) {
        if (val == null || val.isBlank()) {
            return defaultValue;
        }
        String clean = val.toLowerCase().replace("pt", "").replace("px", "").trim();
        try {
            return Double.parseDouble(clean);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
