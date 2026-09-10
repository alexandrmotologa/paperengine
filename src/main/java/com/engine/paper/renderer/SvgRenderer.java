package com.engine.paper.renderer;

import com.engine.paper.domain.element.*;
import com.engine.paper.domain.layout.BoxDimensions;
import com.engine.paper.domain.model.Color;
import com.engine.paper.domain.model.Document;
import com.engine.paper.domain.model.Page;
import com.engine.paper.domain.style.BorderStyle;
import com.engine.paper.domain.style.TextStyle;
import com.engine.paper.engine.SmartPaginator;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * High-fidelity SVG vector exporter serializing layout trees into standalone SVG documents.
 */
public class SvgRenderer implements VectorRenderer {

    private final SmartPaginator paginator;

    public SvgRenderer() {
        this(new SmartPaginator());
    }

    public SvgRenderer(SmartPaginator paginator) {
        this.paginator = paginator != null ? paginator : new SmartPaginator();
    }

    @Override
    public void render(Document document, OutputStream outputStream) throws IOException {
        List<Page> pages = paginator.paginate(document);
        renderPages(pages, outputStream);
    }

    @Override
    public void renderPages(List<Page> pages, OutputStream outputStream) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");

        if (pages.size() == 1) {
            renderSinglePageSvg(pages.getFirst(), sb);
        } else {
            // Multi-page SVG stacked vertically
            double totalHeight = 0;
            double maxWidth = 0;
            for (Page p : pages) {
                totalHeight += p.getPageSize().height() + 20; // 20pt gap between pages
                maxWidth = Math.max(maxWidth, p.getPageSize().width());
            }

            sb.append(String.format("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%.2f\" height=\"%.2f\" viewBox=\"0 0 %.2f %.2f\">\n",
                    maxWidth, totalHeight, maxWidth, totalHeight));

            double currentY = 0;
            for (Page page : pages) {
                sb.append(String.format("  <g transform=\"translate(0, %.2f)\">\n", currentY));
                renderPageContent(page, sb);
                sb.append("  </g>\n");
                currentY += page.getPageSize().height() + 20;
            }
            sb.append("</svg>\n");
        }

        outputStream.write(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void renderSinglePageSvg(Page page, StringBuilder sb) {
        double w = page.getPageSize().width();
        double h = page.getPageSize().height();

        sb.append(String.format("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%.2f\" height=\"%.2f\" viewBox=\"0 0 %.2f %.2f\">\n",
                w, h, w, h));
        renderPageContent(page, sb);
        sb.append("</svg>\n");
    }

    private void renderPageContent(Page page, StringBuilder sb) {
        // Page background rect
        sb.append(String.format("    <rect width=\"%.2f\" height=\"%.2f\" fill=\"#ffffff\" />\n",
                page.getPageSize().width(), page.getPageSize().height()));

        // Header
        if (page.getHeader() != null && page.getHeader().getContent() != null) {
            renderElement(page.getHeader().getContent(), sb);
        }

        // Body elements
        for (DocumentElement element : page.getElements()) {
            renderElement(element, sb);
        }

        // Footer
        if (page.getFooter() != null && page.getFooter().getContent() != null) {
            renderElement(page.getFooter().getContent(), sb);
        }
    }

    private void renderElement(DocumentElement element, StringBuilder sb) {
        if (element == null) {
            return;
        }

        BoxDimensions dims = element.getDimensions();
        double x = dims.getBorderBoxX();
        double y = dims.getBorderBoxY();
        double w = dims.getBorderBoxWidth();
        double h = dims.getBorderBoxHeight();

        // 1. Background
        Color bg = element.getStyle().getBackgroundColor();
        if (bg != null && bg.alpha() > 0 && w > 0 && h > 0) {
            sb.append(String.format("    <rect x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\" fill=\"%s\" opacity=\"%.2f\" />\n",
                    x, y, w, h, bg.toSvgHex(), bg.alpha()));
        }

        // 2. Borders
        BorderStyle border = element.getStyle().getBorderStyle();
        if (border != null && border.hasBorder() && w > 0 && h > 0) {
            sb.append(String.format("    <rect x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\" fill=\"none\" stroke=\"%s\" stroke-width=\"%.2f\" />\n",
                    x, y, w, h, border.color().toSvgHex(), Math.max(border.topWidth(), 1.0)));
        }

        // 3. Content
        if (element instanceof TextBlock textBlock) {
            renderText(textBlock, sb);
        } else if (element instanceof BarcodeElement barcode) {
            try {
                String path = BarcodeVectorWriter.toSvgPath(barcode, x, y, w, h);
                sb.append("    ").append(path).append("\n");
            } catch (Exception e) {
                sb.append(String.format("    <!-- Barcode render error: %s -->\n", escapeXml(e.getMessage())));
            }
        } else if (element instanceof ImageElement image) {
            if (image.getRawData() != null && image.getRawData().length > 0) {
                String b64 = Base64.getEncoder().encodeToString(image.getRawData());
                sb.append(String.format("    <image x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\" href=\"data:%s;base64,%s\" />\n",
                        x, y, w, h, image.getMimeType(), b64));
            }
        } else if (element instanceof DividerElement divider) {
            Color divBg = divider.getStyle().getBackgroundColor();
            sb.append(String.format("    <line x1=\"%.2f\" y1=\"%.2f\" x2=\"%.2f\" y2=\"%.2f\" stroke=\"%s\" stroke-width=\"%.2f\" />\n",
                    x, y + (h / 2.0), x + w, y + (h / 2.0), divBg != null ? divBg.toSvgHex() : "#cbd5e1", Math.max(h, 1.0)));
        } else if (element instanceof TableElement table) {
            for (TableRow hRow : table.getHeaderRows()) {
                renderElement(hRow, sb);
            }
            for (TableRow bRow : table.getBodyRows()) {
                renderElement(bRow, sb);
            }
        } else if (element instanceof TableRow row) {
            for (TableCell cell : row.getCells()) {
                renderElement(cell, sb);
            }
        } else if (element instanceof TableCell cell) {
            if (cell.getContent() != null) {
                renderElement(cell.getContent(), sb);
            }
        } else if (element instanceof ContainerElement container) {
            for (DocumentElement child : container.getChildren()) {
                renderElement(child, sb);
            }
        }
    }

    private void renderText(TextBlock textBlock, StringBuilder sb) {
        TextStyle textStyle = textBlock.getTextStyle();
        List<String> lines = textBlock.getLines();
        if (lines.isEmpty() && !textBlock.getText().isEmpty()) {
            lines = List.of(textBlock.getText());
        }

        double contentX = textBlock.getDimensions().getContentBoxX();
        double contentY = textBlock.getDimensions().getContentBoxY();
        double contentW = textBlock.getDimensions().getContentWidth();
        double lineHeight = textStyle.calculateLineHeight();
        double fontSize = textStyle.fontSize();

        String anchor = switch (textStyle.textAlign()) {
            case CENTER -> "middle";
            case RIGHT -> "end";
            default -> "start";
        };

        double anchorX = switch (textStyle.textAlign()) {
            case CENTER -> contentX + (contentW / 2.0);
            case RIGHT -> contentX + contentW;
            default -> contentX;
        };

        String fontWeight = textStyle.fontWeight() == TextStyle.FontWeight.BOLD ? "bold" : "normal";
        String fontStyle = textStyle.fontStyle() == TextStyle.FontStyle.ITALIC ? "italic" : "normal";
        String fill = textStyle.color() != null ? textStyle.color().toSvgHex() : "#0f172a";

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line == null || line.isEmpty()) {
                continue;
            }
            double lineY = contentY + ((i + 1) * lineHeight) - (fontSize * 0.2);

            sb.append(String.format("    <text x=\"%.2f\" y=\"%.2f\" font-family=\"%s\" font-size=\"%.2f\" font-weight=\"%s\" font-style=\"%s\" fill=\"%s\" text-anchor=\"%s\">%s</text>\n",
                    anchorX, lineY, escapeXml(textStyle.fontFamily()), fontSize, fontWeight, fontStyle, fill, anchor, escapeXml(line)));
        }
    }

    private String escapeXml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
