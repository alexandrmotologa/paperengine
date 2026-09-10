package com.engine.paper.renderer;

import com.engine.paper.domain.element.*;
import com.engine.paper.domain.layout.BoxDimensions;
import com.engine.paper.domain.model.Color;
import com.engine.paper.domain.model.Document;
import com.engine.paper.domain.model.Page;
import com.engine.paper.domain.style.BorderStyle;
import com.engine.paper.domain.style.TextStyle;
import com.engine.paper.engine.SmartPaginator;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * Native vector PDF renderer utilizing Apache PDFBox 3.0.
 * Directly streams vector operators without headless browser dependencies.
 */
public class PdfRenderer implements VectorRenderer {

    private final SmartPaginator paginator;

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
            renderText(stream, textBlock, pdfX, pdfY, (float) w, (float) h);
        } else if (element instanceof BarcodeElement barcode) {
            try {
                BarcodeVectorWriter.drawPdfVector(stream, barcode, pdfX, pdfY, w, h);
            } catch (Exception e) {
                throw new IOException("Failed to render vector barcode: " + e.getMessage(), e);
            }
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

    private void renderText(PDPageContentStream stream, TextBlock textBlock, float pdfX, float pdfY, float w, float h) throws IOException {
        TextStyle textStyle = textBlock.getTextStyle();
        PDFont font = resolveFont(textStyle);
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

    private PDFont resolveFont(TextStyle style) {
        String family = style.fontFamily() != null ? style.fontFamily().toLowerCase() : "helvetica";
        boolean isBold = style.fontWeight() == TextStyle.FontWeight.BOLD;
        boolean isItalic = style.fontStyle() == TextStyle.FontStyle.ITALIC;

        if (family.contains("mono") || family.contains("courier")) {
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
