package com.engine.paper.renderer;

import com.engine.paper.domain.element.*;
import com.engine.paper.domain.layout.BoxDimensions;
import com.engine.paper.domain.model.Color;
import com.engine.paper.domain.model.Document;
import com.engine.paper.domain.model.Page;
import com.engine.paper.domain.model.Watermark;
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

        // Watermark
        if (page.getWatermark() != null && page.getWatermark().isVisible()) {
            Watermark wm = page.getWatermark();
            double cx = page.getPageSize().width() / 2.0;
            double cy = page.getPageSize().height() / 2.0;
            double angle = wm.getAngle();
            sb.append(String.format("    <text x=\"%.2f\" y=\"%.2f\" font-family=\"Helvetica, sans-serif\" font-size=\"%.2f\" font-weight=\"bold\" fill=\"%s\" opacity=\"%.2f\" text-anchor=\"middle\" dominant-baseline=\"middle\" transform=\"rotate(-%.2f, %.2f, %.2f)\">%s</text>\n",
                    cx, cy, wm.getFontSize(), wm.getColor().toSvgHex(), wm.getColor().alpha(), angle, cx, cy, escapeXml(wm.getText())));
        }

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
        } else if (element instanceof ChartElement chart) {
            renderChart(chart, sb);
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

    private void renderChart(ChartElement chart, StringBuilder sb) {
        BoxDimensions dims = chart.getDimensions();
        double x = dims.getBorderBoxX();
        double y = dims.getBorderBoxY();
        double w = dims.getBorderBoxWidth();
        double h = dims.getBorderBoxHeight();

        List<Double> values = chart.getValues();
        if (values.isEmpty()) {
            return;
        }

        ChartElement.ChartType type = chart.getChartType();
        List<Color> palette = chart.getPalette();
        if (palette.isEmpty()) {
            palette = List.of(new Color(2, 132, 199));
        }

        if (type == ChartElement.ChartType.DONUT || type == ChartElement.ChartType.PIE) {
            renderSvgPieOrDonut(chart, sb, x, y, w, h, palette);
            return;
        }

        boolean isSparkline = (type == ChartElement.ChartType.SPARKLINE);
        double padLeft = isSparkline ? 2.0 : 30.0;
        double padBottom = isSparkline ? 2.0 : 24.0;
        double padTop = (chart.getTitle() != null && !chart.getTitle().isBlank()) ? 22.0 : (isSparkline ? 2.0 : 8.0);
        double padRight = isSparkline ? 2.0 : 12.0;

        if (!isSparkline && chart.getTitle() != null && !chart.getTitle().isBlank()) {
            sb.append(String.format("    <text x=\"%.2f\" y=\"%.2f\" font-family=\"Helvetica, sans-serif\" font-size=\"10\" font-weight=\"bold\" fill=\"#1e293b\">%s</text>\n",
                    x + 5.0, y + 14.0, escapeXml(chart.getTitle())));
        }

        double plotX = x + padLeft;
        double plotY = y + padTop;
        double plotW = Math.max(w - padLeft - padRight, 10.0);
        double plotH = Math.max(h - padTop - padBottom, 10.0);
        double maxVal = chart.getMaxValue();

        if (type == ChartElement.ChartType.BAR) {
            // Axis line
            double axisY = plotY + plotH;
            sb.append(String.format("    <line x1=\"%.2f\" y1=\"%.2f\" x2=\"%.2f\" y2=\"%.2f\" stroke=\"#e2e8f0\" stroke-width=\"1\" />\n",
                    plotX, axisY, plotX + plotW, axisY));

            int n = values.size();
            double slotW = plotW / n;
            double barW = slotW * 0.65;
            double barOffset = slotW * 0.175;
            List<String> labels = chart.getLabels();

            for (int i = 0; i < n; i++) {
                double val = values.get(i);
                double barH = (val / maxVal) * plotH;
                double bx = plotX + i * slotW + barOffset;
                double by = plotY + (plotH - barH);

                Color c = palette.get(i % palette.size());
                sb.append(String.format("    <rect x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\" rx=\"2\" fill=\"%s\" />\n",
                        bx, by, barW, Math.max(barH, 1.5), c.toSvgHex()));

                // Label
                if (i < labels.size() && !labels.get(i).isBlank()) {
                    sb.append(String.format("    <text x=\"%.2f\" y=\"%.2f\" font-family=\"Helvetica, sans-serif\" font-size=\"8\" fill=\"#64748b\" text-anchor=\"middle\">%s</text>\n",
                            bx + barW / 2.0, axisY + 12.0, escapeXml(labels.get(i))));
                }

                // Value
                if (chart.isShowValues() && barH > 8.0) {
                    String valStr = String.valueOf(val).replaceAll("\\.0$", "");
                    sb.append(String.format("    <text x=\"%.2f\" y=\"%.2f\" font-family=\"Helvetica, sans-serif\" font-size=\"7.5\" font-weight=\"bold\" fill=\"#334155\" text-anchor=\"middle\">%s</text>\n",
                            bx + barW / 2.0, by - 3.0, escapeXml(valStr)));
                }
            }
        } else if (type == ChartElement.ChartType.LINE || type == ChartElement.ChartType.SPARKLINE) {
            int n = values.size();
            Color strokeColor = palette.getFirst();
            StringBuilder pts = new StringBuilder();

            for (int i = 0; i < n; i++) {
                double px = (n == 1) ? plotX + plotW / 2.0 : plotX + (i * (plotW / (n - 1)));
                double py = plotY + (plotH - (values.get(i) / maxVal) * plotH);
                if (i > 0) pts.append(" ");
                pts.append(String.format(java.util.Locale.US, "%.2f,%.2f", px, py));
            }

            sb.append(String.format("    <polyline points=\"%s\" fill=\"none\" stroke=\"%s\" stroke-width=\"%.1f\" stroke-linecap=\"round\" stroke-linejoin=\"round\" />\n",
                    pts, strokeColor.toSvgHex(), isSparkline ? 1.5 : 2.0));

            if (!isSparkline) {
                for (int i = 0; i < n; i++) {
                    double px = (n == 1) ? plotX + plotW / 2.0 : plotX + (i * (plotW / (n - 1)));
                    double py = plotY + (plotH - (values.get(i) / maxVal) * plotH);
                    sb.append(String.format("    <circle cx=\"%.2f\" cy=\"%.2f\" r=\"2.5\" fill=\"%s\" />\n",
                            px, py, strokeColor.toSvgHex()));
                }
            }
        }
    }

    private void renderSvgPieOrDonut(ChartElement chart, StringBuilder sb, double x, double y, double w, double h, List<Color> palette) {
        List<Double> values = chart.getValues();
        double sum = values.stream().mapToDouble(Double::doubleValue).sum();
        if (sum <= 0) sum = 1.0;

        double cx = x + w / 2.0;
        double cy = y + h / 2.0;
        double rOuter = Math.min(w, h) / 2.3;
        double rInner = (chart.getChartType() == ChartElement.ChartType.DONUT) ? rOuter * 0.55 : 0.0;

        double startAngle = -Math.PI / 2.0; // Start from top
        for (int i = 0; i < values.size(); i++) {
            double val = values.get(i);
            double sweep = (val / sum) * 2.0 * Math.PI;
            if (sweep <= 0.001) continue;

            double endAngle = startAngle + sweep;
            int largeArc = (sweep > Math.PI) ? 1 : 0;

            double x1 = cx + rOuter * Math.cos(startAngle);
            double y1 = cy + rOuter * Math.sin(startAngle);
            double x2 = cx + rOuter * Math.cos(endAngle);
            double y2 = cy + rOuter * Math.sin(endAngle);

            Color c = palette.get(i % palette.size());
            String path;
            if (rInner > 0) {
                double x3 = cx + rInner * Math.cos(endAngle);
                double y3 = cy + rInner * Math.sin(endAngle);
                double x4 = cx + rInner * Math.cos(startAngle);
                double y4 = cy + rInner * Math.sin(startAngle);
                path = String.format(java.util.Locale.US,
                        "M %.2f %.2f A %.2f %.2f 0 %d 1 %.2f %.2f L %.2f %.2f A %.2f %.2f 0 %d 0 %.2f %.2f Z",
                        x1, y1, rOuter, rOuter, largeArc, x2, y2, x3, y3, rInner, rInner, largeArc, x4, y4);
            } else {
                path = String.format(java.util.Locale.US,
                        "M %.2f %.2f A %.2f %.2f 0 %d 1 %.2f %.2f L %.2f %.2f Z",
                        x1, y1, rOuter, rOuter, largeArc, x2, y2, cx, cy);
            }

            sb.append(String.format("    <path d=\"%s\" fill=\"%s\" />\n", path, c.toSvgHex()));
            startAngle = endAngle;
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
