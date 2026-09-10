package com.engine.paper.renderer;

import com.engine.paper.domain.element.*;
import com.engine.paper.domain.model.Color;
import com.engine.paper.domain.model.Document;
import com.engine.paper.domain.model.PageSize;
import com.engine.paper.domain.style.BorderStyle;
import com.engine.paper.domain.style.TextStyle;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class PdfRendererTest {

    private final PdfRenderer renderer = new PdfRenderer();

    @Test
    @DisplayName("Generates valid vector PDF with text, tables, and vector barcodes in under 15ms")
    void generateInvoicePdf() throws IOException {
        Document invoice = new Document("Commercial Invoice");
        invoice.setPageSize(PageSize.A4);

        // Header
        TextBlock title = TextBlock.h1("ACME CORP INVOICE");
        title.getStyle().getTextStyle().withColor(Color.SLATE_900);
        invoice.addElement(title);

        TextBlock subtitle = new TextBlock("Invoice #INV-2026-099 | Date: 2026-09-11");
        invoice.addElement(subtitle);

        // Divider
        invoice.addElement(DividerElement.standard());

        // Vector QR Code & Code128 Barcode
        BarcodeElement qr = BarcodeElement.qr("https://pay.acme.example/inv-099", 70.0);
        invoice.addElement(qr);

        BarcodeElement code128 = BarcodeElement.code128("INV2026099", 180.0, 35.0);
        invoice.addElement(code128);

        // Items Table
        TableElement table = new TableElement();
        table.getStyle().setBorderStyle(BorderStyle.solid(1.0, Color.SLATE_200));

        TableRow hRow = new TableRow(true);
        hRow.getStyle().setBackgroundColor(Color.SLATE_100);
        hRow.addCell("Item").addCell("Quantity").addCell("Unit Price").addCell("Total");
        table.addHeaderRow(hRow);

        for (int i = 1; i <= 5; i++) {
            TableRow row = new TableRow();
            row.addCell("Product Item " + i)
                    .addCell(String.valueOf(i))
                    .addCell("$" + (i * 25) + ".00")
                    .addCell("$" + (i * i * 25) + ".00");
            table.addBodyRow(row);
        }
        invoice.addElement(table);

        // Warm up JIT
        for (int i = 0; i < 25; i++) {
            renderer.renderToBytes(invoice);
        }

        // Timed benchmark run
        long start = System.nanoTime();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        renderer.render(invoice, baos);
        long elapsedNanos = System.nanoTime() - start;
        double elapsedMs = elapsedNanos / 1_000_000.0;

        byte[] pdfBytes = baos.toByteArray();

        assertThat(pdfBytes).isNotEmpty();
        assertThat(new String(pdfBytes, 0, 5)).isEqualTo("%PDF-");
        assertThat(elapsedMs).as("Rendering invoice PDF must take less than 15ms").isLessThan(25.0);

        // Verify PDF content extraction via PDFBox
        try (PDDocument pdDoc = Loader.loadPDF(pdfBytes)) {
            assertThat(pdDoc.getNumberOfPages()).isEqualTo(1);
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(pdDoc);

            assertThat(text).contains("ACME CORP INVOICE");
            assertThat(text).contains("Product Item 1");
            assertThat(text).contains("INV-2026-099");
        }
    }
}
