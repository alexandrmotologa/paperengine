package com.engine.paper.infrastructure;

import com.engine.paper.domain.model.Document;
import com.engine.paper.infrastructure.einvoice.PdfA3Packager;
import com.engine.paper.renderer.PdfRenderer;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class PdfA3PackagerTest {

    @Test
    @DisplayName("Should embed Factur-X XML and inject PDF/A-3b conformance metadata")
    void testFacturXEmbedding() throws IOException {
        Document doc = new Document("Standard Invoice");
        PdfRenderer renderer = new PdfRenderer();
        byte[] standardPdf = renderer.renderToBytes(doc);

        String sampleXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rsm:CrossIndustryInvoice xmlns:rsm="urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:100">
                    <rsm:ExchangedDocument>
                        <ram:ID>INV-2026-001</ram:ID>
                    </rsm:ExchangedDocument>
                </rsm:CrossIndustryInvoice>
                """;
        byte[] xmlBytes = sampleXml.getBytes(StandardCharsets.UTF_8);

        byte[] facturXPdf = PdfA3Packager.packageFacturX(standardPdf, xmlBytes, PdfA3Packager.InvoiceProfile.EN_16931);

        assertThat(facturXPdf).isNotEmpty();
        assertThat(facturXPdf.length).isGreaterThan(standardPdf.length);

        try (PDDocument verifiedDoc = Loader.loadPDF(facturXPdf)) {
            // Verify EmbeddedFiles name tree exists
            assertThat(verifiedDoc.getDocumentCatalog().getNames()).isNotNull();
            assertThat(verifiedDoc.getDocumentCatalog().getNames().getEmbeddedFiles()).isNotNull();
            assertThat(verifiedDoc.getDocumentCatalog().getNames().getEmbeddedFiles().getNames())
                    .containsKey("factur-x.xml");

            // Verify Associated Files /AF array exists (ISO 19005-3)
            COSArray af = (COSArray) verifiedDoc.getDocumentCatalog().getCOSObject().getItem(COSName.getPDFName("AF"));
            assertThat(af).isNotNull();
            assertThat(af.size()).isEqualTo(1);

            // Verify XMP metadata exists
            PDMetadata metadata = verifiedDoc.getDocumentCatalog().getMetadata();
            assertThat(metadata).isNotNull();
            String xmpString = new String(metadata.toByteArray(), StandardCharsets.UTF_8);
            assertThat(xmpString).contains("pdfaid:part>3");
            assertThat(xmpString).contains("pdfaid:conformance>B");
            assertThat(xmpString).contains("zf:ConformanceLevel>EN 16931");
            assertThat(xmpString).contains("factur-x.xml");
        }
    }
}
