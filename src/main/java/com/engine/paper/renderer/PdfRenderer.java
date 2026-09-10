package com.engine.paper.renderer;

import com.engine.paper.domain.element.*;
import com.engine.paper.domain.layout.BoxDimensions;
import com.engine.paper.domain.model.Color;
import com.engine.paper.domain.model.Document;
import com.engine.paper.domain.model.Page;
import com.engine.paper.domain.model.Watermark;
import com.engine.paper.domain.style.BorderStyle;
import com.engine.paper.domain.style.FontRegistry;
import com.engine.paper.domain.style.TextStyle;
import com.engine.paper.engine.SmartPaginator;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Native vector PDF renderer utilizing Apache PDFBox 3.0.
 * Directly streams vector operators without headless browser dependencies.
 */
public class PdfRenderer implements VectorRenderer {

    private final SmartPaginator paginator;
    private final Map<String, PDFont> loadedFonts = new ConcurrentHashMap<>();

    private static final PDFont FONT_REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDFont FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDFont FONT_ITALIC = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);
    private static final PDFont FONT_BOLD_ITALIC = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD_OBLIQUE);
    private static final PDFont FONT_MONO = new PDType1Font(Standard14Fonts.FontName.COURIER);

    public PdfRenderer() {
        this(new SmartPaginator());
    }

    public PdfRenderer(SmartPaginator paginator) {
        this.paginator = paginator != null ? paginator : new SmartPaginator();
    }

    @Override
    public void render(Document document, OutputStream outputStream) throws IOException {
        List<Page> pages = paginator.paginate(document);
        renderPages(pages, outputStream);
    }

    @Override
    public void renderPages(List<Page> pages, OutputStream outputStream) throws IOException {
        try (PDDocument pdfDoc = new PDDocument()) {
            for (Page page : pages) {
                renderSinglePage(pdfDoc, page);
            }
            pdfDoc.save(outputStream);
        }
    }

    private void renderSinglePage(PDDocument pdfDoc, Page page) throws IOException {
        float pageWidth = (float) page.getPageSize().width();
        float pageHeight = (float) page.getPageSize().height();

        PDPage pdPage = new PDPage(new PDRectangle(pageWidth, pageHeight));
        pdfDoc.addPage(pdPage);

        try (PDPageContentStream stream = new PDPageContentStream(pdfDoc, pdPage)) {
            // Render watermark if present
            if (page.getWatermark() != null && page.getWatermark().isVisible()) {
                renderWatermark(stream, page.getWatermark(), pageWidth, pageHeight);
            }

            // Render header if present
            if (page.getHeader() != null && page.getHeader().getContent() != null) {
                renderElement(pdfDoc, stream, page.getHeader().getContent(), pageHeight);
            }

            // Render page body elements
            for (DocumentElement element : page.getElements()) {
                renderElement(pdfDoc, stream, element, pageHeight);
            }

            // Render footer if present
            if (page.getFooter() != null && page.getFooter().getContent() != null) {
                renderElement(pdfDoc, stream, page.getFooter().getContent(), pageHeight);
            }
        }
    }

    private void renderWatermark(PDPageContentStream stream, Watermark watermark, float pageWidth, float pageHeight) throws IOException {
        stream.saveGraphicsState();
        try {
            PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
            gs.setNonStrokingAlphaConstant((float) watermark.getColor().alpha());
            stream.setGraphicsStateParameters(gs);

            float fontSize = (float) watermark.getFontSize();
            PDFont font = FONT_BOLD;
            String text = sanitizeText(watermark.getText());
            float textWidth = font.getStringWidth(text) / 1000f * fontSize;

            float cx = pageWidth / 2f;
            float cy = pageHeight / 2f;
            double rad = Math.toRadians(watermark.getAngle());

            stream.transform(Matrix.getRotateInstance(rad, cx, cy));

            Color c = watermark.getColor();
            stream.setNonStrokingColor(c.rFloat(), c.gFloat(), c.bFloat());
            stream.beginText();
            stream.setFont(font, fontSize);
            stream.newLineAtOffset(cx - (textWidth / 2f), cy - (fontSize / 3f));
            stream.showText(text);
            stream.endText();
        } finally {
            stream.restoreGraphicsState();
        }
    }

    private void renderElement(PDDocument pdfDoc, PDPageContentStream stream, DocumentElement element, float pageHeight) throws IOException {
        if (element == null) {
            return;
        }

        BoxDimensions dims = element.getDimensions();
        double x = dims.getBorderBoxX();
        double y = dims.getBorderBoxY();
        double w = dims.getBorderBoxWidth();
        double h = dims.getBorderBoxHeight();

        // Convert top-left layout coordinates to bottom-left PDF coordinates
        float pdfX = (float) x;
        float pdfY = (float) (pageHeight - y - h);

        // 1. Draw Background
        Color bg = element.getStyle().getBackgroundColor();
        if (bg != null && bg.alpha() > 0 && w > 0 && h > 0) {
            stream.setNonStrokingColor(bg.rFloat(), bg.gFloat(), bg.bFloat());
            stream.addRect(pdfX, pdfY, (float) w, (float) h);
            stream.fill();
        }

        // 2. Draw Borders
        BorderStyle border = element.getStyle().getBorderStyle();
        if (border != null && border.hasBorder() && w > 0 && h > 0) {
            Color bc = border.color();
            stream.setStrokingColor(bc.rFloat(), bc.gFloat(), bc.bFloat());
            stream.setLineWidth((float) Math.max(border.topWidth(), 1.0));
            stream.addRect(pdfX, pdfY, (float) w, (float) h);
            stream.stroke();
        }

        // 3. Render Element-Specific Content
        if (element instanceof TextBlock textBlock) {
            renderText(pdfDoc, stream, textBlock, pdfX, pdfY, (float) w, (float) h);
        } else if (element instanceof BarcodeElement barcode) {
            try {
                BarcodeVectorWriter.drawPdfVector(stream, barcode, pdfX, pdfY, w, h);
            } catch (Exception e) {
                throw new IOException("Failed to render vector barcode: " + e.getMessage(), e);
            }
        } else if (element instanceof ChartElement chart) {
            renderChart(stream, chart, pdfX, pdfY, (float) w, (float) h);
        } else if (element instanceof ImageElement image) {
            renderImage(pdfDoc, stream, image, pdfX, pdfY, (float) w, (float) h);
        } else if (element instanceof DividerElement divider) {
            renderDivider(stream, divider, pdfX, pdfY, (float) w, (float) h);
        } else if (element instanceof TableElement table) {
            renderTable(pdfDoc, stream, table, pageHeight);
        } else if (element instanceof TableRow row) {
            for (TableCell cell : row.getCells()) {
                renderElement(pdfDoc, stream, cell, pageHeight);
            }
        } else if (element instanceof TableCell cell) {
            if (cell.getContent() != null) {
                renderElement(pdfDoc, stream, cell.getContent(), pageHeight);
            }
        } else if (element instanceof ContainerElement container) {
            for (DocumentElement child : container.getChildren()) {
                renderElement(pdfDoc, stream, child, pageHeight);
            }
        }
    }

    private void renderChart(PDPageContentStream stream, ChartElement chart, float pdfX, float pdfY, float w, float h) throws IOException {
        if (chart == null || w <= 0 || h <= 0) {
            return;
        }

        ChartElement.ChartType type = chart.getChartType();
        List<Double> values = chart.getValues();
        if (values.isEmpty()) {
            return;
        }

        List<Color> palette = chart.getPalette();
        if (palette.isEmpty()) {
            palette = List.of(new Color(2, 132, 199));
        }

        if (type == ChartElement.ChartType.DONUT || type == ChartElement.ChartType.PIE) {
            renderPieOrDonut(stream, chart, pdfX, pdfY, w, h, palette);
            return;
        }

        boolean isSparkline = (type == ChartElement.ChartType.SPARKLINE);
        float padLeft = isSparkline ? 2f : 30f;
        float padBottom = isSparkline ? 2f : 24f;
        float padTop = (chart.getTitle() != null && !chart.getTitle().isBlank()) ? 22f : (isSparkline ? 2f : 8f);
        float padRight = isSparkline ? 2f : 12f;

        // Render Title if present and not sparkline
        if (!isSparkline && chart.getTitle() != null && !chart.getTitle().isBlank()) {
            stream.beginText();
            stream.setFont(FONT_BOLD, 10f);
            stream.setNonStrokingColor(0.1f, 0.15f, 0.25f);
            stream.newLineAtOffset(pdfX + 5f, pdfY + h - 14f);
            stream.showText(sanitizeText(chart.getTitle()));
            stream.endText();
        }

        float plotX = pdfX + padLeft;
        float plotY = pdfY + padBottom;
        float plotW = Math.max(w - padLeft - padRight, 10f);
        float plotH = Math.max(h - padTop - padBottom, 10f);
        double maxVal = chart.getMaxValue();

        if (type == ChartElement.ChartType.BAR) {
            // Draw axis line
            stream.setStrokingColor(0.85f, 0.88f, 0.92f);
            stream.setLineWidth(1.0f);
            stream.moveTo(plotX, plotY);
            stream.lineTo(plotX + plotW, plotY);
            stream.stroke();

            int n = values.size();
            float slotW = plotW / n;
            float barW = slotW * 0.65f;
            float barOffset = slotW * 0.175f;
            List<String> labels = chart.getLabels();

            for (int i = 0; i < n; i++) {
                double val = values.get(i);
                float barH = (float) ((val / maxVal) * plotH);
                float bx = plotX + i * slotW + barOffset;
                float by = plotY;

                Color c = palette.get(i % palette.size());
                stream.setNonStrokingColor(c.rFloat(), c.gFloat(), c.bFloat());
                stream.addRect(bx, by, barW, Math.max(barH, 1.5f));
                stream.fill();

                // Bar label
                if (i < labels.size() && !labels.get(i).isBlank()) {
                    String lbl = sanitizeText(labels.get(i));
                    float lblW = FONT_REGULAR.getStringWidth(lbl) / 1000f * 8f;
                    stream.beginText();
                    stream.setFont(FONT_REGULAR, 8f);
                    stream.setNonStrokingColor(0.4f, 0.45f, 0.55f);
                    stream.newLineAtOffset(bx + (barW - lblW) / 2f, plotY - 12f);
                    stream.showText(lbl);
                    stream.endText();
                }

                // Bar value
                if (chart.isShowValues() && barH > 8f) {
                    String valStr = String.valueOf(val).replaceAll("\\.0$", "");
                    float vW = FONT_BOLD.getStringWidth(valStr) / 1000f * 7.5f;
                    stream.beginText();
                    stream.setFont(FONT_BOLD, 7.5f);
                    stream.setNonStrokingColor(0.2f, 0.25f, 0.35f);
                    stream.newLineAtOffset(bx + (barW - vW) / 2f, by + barH + 3f);
                    stream.showText(valStr);
                    stream.endText();
                }
            }
        } else if (type == ChartElement.ChartType.LINE || type == ChartElement.ChartType.SPARKLINE) {
            int n = values.size();
            Color strokeColor = palette.getFirst();
            stream.setStrokingColor(strokeColor.rFloat(), strokeColor.gFloat(), strokeColor.bFloat());
            stream.setLineWidth(isSparkline ? 1.5f : 2.0f);

            for (int i = 0; i < n; i++) {
                float px = (n == 1) ? plotX + plotW / 2f : plotX + (i * (plotW / (n - 1)));
                float py = (float) (plotY + (values.get(i) / maxVal) * plotH);
                if (i == 0) {
                    stream.moveTo(px, py);
                } else {
                    stream.lineTo(px, py);
                }
            }
            stream.stroke();

            // Point dots on standard line chart
            if (!isSparkline) {
                for (int i = 0; i < n; i++) {
                    float px = (n == 1) ? plotX + plotW / 2f : plotX + (i * (plotW / (n - 1)));
                    float py = (float) (plotY + (values.get(i) / maxVal) * plotH);
                    stream.setNonStrokingColor(strokeColor.rFloat(), strokeColor.gFloat(), strokeColor.bFloat());
                    stream.addRect(px - 2.5f, py - 2.5f, 5f, 5f);
                    stream.fill();
                }
            }
        }
    }

    private void renderPieOrDonut(PDPageContentStream stream, ChartElement chart, float pdfX, float pdfY, float w, float h, List<Color> palette) throws IOException {
        List<Double> values = chart.getValues();
        double sum = values.stream().mapToDouble(Double::doubleValue).sum();
        if (sum <= 0) sum = 1.0;

        float cx = pdfX + w / 2f;
        float cy = pdfY + h / 2f;
        float rOuter = Math.min(w, h) / 2.3f;
        float rInner = (chart.getChartType() == ChartElement.ChartType.DONUT) ? rOuter * 0.55f : 0f;

        double startAngle = 0.0;
        for (int i = 0; i < values.size(); i++) {
            double val = values.get(i);
            double sweep = (val / sum) * 2.0 * Math.PI;
            if (sweep <= 0.001) continue;

            int steps = Math.max(6, (int) (sweep / 0.08));
            Color c = palette.get(i % palette.size());
            stream.setNonStrokingColor(c.rFloat(), c.gFloat(), c.bFloat());

            // Outer arc
            for (int s = 0; s <= steps; s++) {
                double a = startAngle + (s * sweep / steps);
                float ax = (float) (cx + rOuter * Math.cos(a));
                float ay = (float) (cy + rOuter * Math.sin(a));
                if (s == 0) {
                    stream.moveTo(ax, ay);
                } else {
                    stream.lineTo(ax, ay);
                }
            }

            // Inner arc or center
            if (rInner > 0) {
                for (int s = steps; s >= 0; s--) {
                    double a = startAngle + (s * sweep / steps);
                    float ax = (float) (cx + rInner * Math.cos(a));
                    float ay = (float) (cy + rInner * Math.sin(a));
                    stream.lineTo(ax, ay);
                }
            } else {
                stream.lineTo(cx, cy);
            }

            stream.closePath();
            stream.fill();
            startAngle += sweep;
        }
    }

    private void renderText(PDDocument pdfDoc, PDPageContentStream stream, TextBlock textBlock, float pdfX, float pdfY, float w, float h) throws IOException {
        TextStyle textStyle = textBlock.getTextStyle();
        PDFont font = resolveFont(pdfDoc, textStyle);
        float fontSize = (float) textStyle.fontSize();
        float lineHeight = (float) textStyle.calculateLineHeight();
        Color color = textStyle.color() != null ? textStyle.color() : Color.SLATE_900;

        List<String> lines = textBlock.getLines();
        if (lines.isEmpty() && !textBlock.getText().isEmpty()) {
            lines = List.of(textBlock.getText());
        }

        float contentX = (float) textBlock.getDimensions().getContentBoxX();
        float contentW = (float) textBlock.getDimensions().getContentWidth();

        // Baseline calculation from top of text box downwards
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line == null || line.isEmpty()) {
                continue;
            }

            float textWidth = font.getStringWidth(sanitizeText(line)) / 1000f * fontSize;
            float lineX = contentX;

            switch (textStyle.textAlign()) {
                case CENTER -> lineX = contentX + (contentW - textWidth) / 2f;
                case RIGHT -> lineX = contentX + contentW - textWidth;
                default -> {
                    // LEFT
                }
            }

            float lineY = pdfY + h - ((i + 1) * lineHeight) + (fontSize * 0.25f);

            stream.beginText();
            stream.setFont(font, fontSize);
            stream.setNonStrokingColor(color.rFloat(), color.gFloat(), color.bFloat());
            stream.newLineAtOffset(lineX, lineY);
            stream.showText(sanitizeText(line));
            stream.endText();
        }
    }

    private void renderDivider(PDPageContentStream stream, DividerElement divider, float pdfX, float pdfY, float w, float h) throws IOException {
        Color bg = divider.getStyle().getBackgroundColor();
        if (bg != null && bg.alpha() > 0) {
            stream.setNonStrokingColor(bg.rFloat(), bg.gFloat(), bg.bFloat());
            stream.addRect(pdfX, pdfY, w, Math.max(h, 1.0f));
            stream.fill();
        }
    }

    private void renderImage(PDDocument pdfDoc, PDPageContentStream stream, ImageElement image, float pdfX, float pdfY, float w, float h) throws IOException {
        if (image.getRawData() != null && image.getRawData().length > 0) {
            PDImageXObject pdImage = PDImageXObject.createFromByteArray(pdfDoc, image.getRawData(), "img");
            stream.drawImage(pdImage, pdfX, pdfY, w, h);
        }
    }

    private void renderTable(PDDocument pdfDoc, PDPageContentStream stream, TableElement table, float pageHeight) throws IOException {
        for (TableRow hRow : table.getHeaderRows()) {
            renderElement(pdfDoc, stream, hRow, pageHeight);
        }
        for (TableRow bRow : table.getBodyRows()) {
            renderElement(pdfDoc, stream, bRow, pageHeight);
        }
    }

    private PDFont resolveFont(PDDocument pdfDoc, TextStyle style) {
        String family = style.fontFamily() != null ? style.fontFamily().trim() : "helvetica";
        boolean isBold = style.fontWeight() == TextStyle.FontWeight.BOLD;
        boolean isItalic = style.fontStyle() == TextStyle.FontStyle.ITALIC;

        // Check FontRegistry for custom TrueType/OpenType font
        FontRegistry.FontDefinition customFontDef = FontRegistry.getDefault().getFont(family, isBold, isItalic);
        if (customFontDef != null && pdfDoc != null) {
            String cacheKey = System.identityHashCode(pdfDoc) + ":" + family.toLowerCase() + ":" + isBold + ":" + isItalic;
            PDFont cached = loadedFonts.get(cacheKey);
            if (cached != null) {
                return cached;
            }
            try {
                PDFont loaded = null;
                if (customFontDef.fontData() != null && customFontDef.fontData().length > 0) {
                    loaded = PDType0Font.load(pdfDoc, new ByteArrayInputStream(customFontDef.fontData()));
                } else if (customFontDef.sourcePath() != null) {
                    File f = new File(customFontDef.sourcePath());
                    if (f.exists() && f.isFile()) {
                        loaded = PDType0Font.load(pdfDoc, f);
                    }
                }
                if (loaded != null) {
                    loadedFonts.put(cacheKey, loaded);
                    return loaded;
                }
            } catch (Exception ignored) {
                // Fall back gracefully to standard 14 font
            }
        }

        String norm = family.toLowerCase();
        if (norm.contains("mono") || norm.contains("courier")) {
            return FONT_MONO;
        }

        if (isBold && isItalic) {
            return FONT_BOLD_ITALIC;
        } else if (isBold) {
            return FONT_BOLD;
        } else if (isItalic) {
            return FONT_ITALIC;
        }
        return FONT_REGULAR;
    }

    private String sanitizeText(String text) {
        if (text == null) {
            return "";
        }
        // Standard14 Type 1 font encoding supports ISO Latin-1
        return text.replace('\r', ' ').replace('\n', ' ');
    }
}
