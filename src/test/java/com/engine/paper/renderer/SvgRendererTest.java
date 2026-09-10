package com.engine.paper.renderer;

import com.engine.paper.domain.element.BarcodeElement;
import com.engine.paper.domain.element.DividerElement;
import com.engine.paper.domain.element.TextBlock;
import com.engine.paper.domain.model.Document;
import com.engine.paper.domain.model.PageSize;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class SvgRendererTest {

    private final SvgRenderer svgRenderer = new SvgRenderer();

    @Test
    @DisplayName("Renders valid SVG document with text and vector barcodes")
    void rendersValidSvg() throws IOException {
        Document doc = new Document("SVG Vector Test");
        doc.setPageSize(PageSize.A4);

        doc.addElement(TextBlock.h1("SVG Vector Report"));
        doc.addElement(new TextBlock("Generated cleanly with zero raster artifacts."));
        doc.addElement(DividerElement.standard());
        doc.addElement(BarcodeElement.qr("https://paperengine.example/vector", 60.0));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        svgRenderer.render(doc, baos);

        String svgContent = baos.toString(StandardCharsets.UTF_8);

        assertThat(svgContent).startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        assertThat(svgContent).contains("<svg");
        assertThat(svgContent).contains("SVG Vector Report");
        assertThat(svgContent).contains("<path d="); // Barcode vector path
        assertThat(svgContent).contains("</svg>");
    }
}
