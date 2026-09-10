package com.engine.paper.infrastructure.signature;

import com.engine.paper.domain.element.TextBlock;
import com.engine.paper.domain.model.Document;
import com.engine.paper.domain.model.PageSize;
import com.engine.paper.renderer.PdfRenderer;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.cert.Certificate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class PdfSignerTest {

    private final PdfSigner signer = new PdfSigner();
    private final PdfRenderer renderer = new PdfRenderer();

    @Test
    @DisplayName("Applies valid PKCS#7 / PAdES detached digital signature to generated PDF")
    void signPdfDocument() throws Exception {
        Document doc = new Document("Signed Contract");
        doc.setPageSize(PageSize.LETTER);
        doc.addElement(TextBlock.h1("Confidential Agreement"));
        doc.addElement(new TextBlock("This document is verified and signed electronically."));

        byte[] rawPdf = renderer.renderToBytes(doc);

        // Generate self-signed test certificate
        PdfSigner.KeyPairAndCertificate keyPairAndCert = PdfSigner.generateSelfSignedKeyPair("CN=John Doe, O=PaperEngine Corp, C=US");

        PdfSigner.SignatureMetadata metadata = new PdfSigner.SignatureMetadata("John Doe", "San Francisco, CA", "Contract Execution");
        byte[] signedPdf = signer.signPdf(
                rawPdf,
                keyPairAndCert.keyPair().getPrivate(),
                new Certificate[]{keyPairAndCert.certificate()},
                metadata
        );

        assertThat(signedPdf).isNotEmpty();
        assertThat(signedPdf.length).isGreaterThan(rawPdf.length);

        // Verify digital signature dictionary in output PDF
        try (PDDocument signedDoc = Loader.loadPDF(signedPdf)) {
            List<PDSignature> signatures = signedDoc.getSignatureDictionaries();
            assertThat(signatures).hasSize(1);

            PDSignature sig = signatures.getFirst();
            assertThat(sig.getName()).isEqualTo("John Doe");
            assertThat(sig.getLocation()).isEqualTo("San Francisco, CA");
            assertThat(sig.getReason()).isEqualTo("Contract Execution");
            assertThat(sig.getFilter()).isEqualTo("Adobe.PPKLite");
            assertThat(sig.getSubFilter()).isEqualTo("adbe.pkcs7.detached");
        }
    }

    @Test
    @DisplayName("Encrypts PDF with AES password protection")
    void encryptPdfDocument() throws Exception {
        Document doc = new Document("Encrypted Statement");
        doc.addElement(new TextBlock("Secret Financial Information"));

        byte[] rawPdf = renderer.renderToBytes(doc);
        byte[] encryptedPdf = signer.encryptPdf(rawPdf, "user123", "admin123");

        assertThat(encryptedPdf).isNotEmpty();

        // Loading without password should report document is encrypted
        try (PDDocument encDoc = Loader.loadPDF(encryptedPdf, "user123")) {
            assertThat(encDoc.isEncrypted()).isTrue();
        }
    }
}
