package com.engine.paper.renderer;

import com.engine.paper.domain.model.Document;
import com.engine.paper.domain.model.Page;
import com.engine.paper.domain.model.Watermark;
import com.engine.paper.engine.SmartPaginator;
import com.engine.paper.template.MarkdownTemplateParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class WatermarkTest {

    @Test
    @DisplayName("Should parse @page watermark attributes and propagate to paginated pages")
    void testWatermarkParsingAndPagination() {
        String template = """
                <style>
                @page {
                  watermark: "CONFIDENTIAL";
                  watermark-color: rgba(239, 68, 68, 0.20);
                  watermark-angle: 45deg;
                  watermark-font-size: 72pt;
                }
                </style>

                # First Page
                Content on first page.

                <!-- pagebreak -->

                # Second Page
                Content on second page.
                """;

        MarkdownTemplateParser parser = new MarkdownTemplateParser();
        Document doc = parser.parse(template);

        assertThat(doc.getWatermark()).isNotNull();
        assertThat(doc.getWatermark().getText()).isEqualTo("CONFIDENTIAL");
        assertThat(doc.getWatermark().getAngle()).isEqualTo(45.0);
        assertThat(doc.getWatermark().getFontSize()).isEqualTo(72.0);

        SmartPaginator paginator = new SmartPaginator();
        List<Page> pages = paginator.paginate(doc);

        assertThat(pages).hasSize(2);
        assertThat(pages.get(0).getWatermark()).isNotNull();
        assertThat(pages.get(0).getWatermark().getText()).isEqualTo("CONFIDENTIAL");
        assertThat(pages.get(1).getWatermark()).isNotNull();
        assertThat(pages.get(1).getWatermark().getText()).isEqualTo("CONFIDENTIAL");
    }

    @Test
    @DisplayName("Should render watermark text to both PDF and SVG")
    void testWatermarkRendering() throws IOException {
        Document doc = new Document("Watermark Doc");
        doc.setWatermark(new Watermark("SAMPLE STAMP"));

        PdfRenderer pdfRenderer = new PdfRenderer();
        byte[] pdfBytes = pdfRenderer.renderToBytes(doc);
        assertThat(pdfBytes).isNotEmpty();

        SvgRenderer svgRenderer = new SvgRenderer();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        svgRenderer.render(doc, baos);
        String svg = baos.toString(StandardCharsets.UTF_8);

        assertThat(svg).contains("SAMPLE STAMP");
        assertThat(svg).contains("rotate(-45");
    }
}
