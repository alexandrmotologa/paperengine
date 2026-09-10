package com.engine.paper.renderer;

import com.engine.paper.domain.element.ChartElement;
import com.engine.paper.domain.model.Color;
import com.engine.paper.domain.model.Document;
import com.engine.paper.template.MarkdownTemplateParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ChartRendererTest {

    @Test
    @DisplayName("Should parse and render bar, sparkline, and donut charts to PDF without error")
    void testChartPdfRendering() throws IOException {
        String template = """
                # Executive Analytics

                [chart:bar title="Revenue Growth" labels="Q1,Q2,Q3,Q4" values="100,150,220,310" width="300pt" height="120pt"]

                [chart:sparkline values="10,25,18,32,45" width="120pt" height="24pt"]

                [chart:donut title="Market Share" labels="Europe,US,Asia" values="45,35,20" size="140pt"]
                """;

        MarkdownTemplateParser parser = new MarkdownTemplateParser();
        Document doc = parser.parse(template);

        assertThat(doc.getBody().getChildren()).hasSize(4);
        assertThat(doc.getBody().getChildren().get(1)).isInstanceOf(ChartElement.class);

        PdfRenderer renderer = new PdfRenderer();
        byte[] pdfBytes = renderer.renderToBytes(doc);

        assertThat(pdfBytes).isNotEmpty();
        assertThat(new String(pdfBytes, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("Should render vector charts to SVG with rect, polyline, and path elements")
    void testChartSvgRendering() throws IOException {
        Document doc = new Document("Charts");

        ChartElement bar = ChartElement.bar(300, 100);
        bar.setTitle("Sales");
        bar.setData(List.of("Jan", "Feb", "Mar"), List.of(50.0, 80.0, 120.0));
        doc.addElement(bar);

        ChartElement spark = ChartElement.sparkline(100, 20);
        spark.setData(List.of(), List.of(5.0, 15.0, 8.0, 20.0));
        doc.addElement(spark);

        ChartElement donut = ChartElement.donut(120);
        donut.setData(List.of("A", "B"), List.of(60.0, 40.0));
        doc.addElement(donut);

        SvgRenderer svgRenderer = new SvgRenderer();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        svgRenderer.render(doc, baos);
        String svg = baos.toString(StandardCharsets.UTF_8);

        assertThat(svg).contains("<svg");
        assertThat(svg).contains("<rect");
        assertThat(svg).contains("<polyline");
        assertThat(svg).contains("<path");
    }
}
