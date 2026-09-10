package com.engine.paper.infrastructure;

import com.engine.paper.domain.element.TextBlock;
import com.engine.paper.domain.model.Document;
import com.engine.paper.infrastructure.merger.DocumentMerger;
import com.engine.paper.renderer.PdfRenderer;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class DocumentMergerTest {

    @Test
    @DisplayName("Should assemble multiple PDF documents into a combined multi-page document")
    void testMergeBytes() throws IOException {
        PdfRenderer renderer = new PdfRenderer();

        Document doc1 = new Document("Doc 1");
        doc1.addElement(new TextBlock("First Document Content"));
        byte[] pdf1 = renderer.renderToBytes(doc1);

        Document doc2 = new Document("Doc 2");
        doc2.addElement(new TextBlock("Second Document Content"));
        byte[] pdf2 = renderer.renderToBytes(doc2);

        byte[] merged = DocumentMerger.merge(List.of(pdf1, pdf2));
        assertThat(merged).isNotEmpty();

        try (PDDocument resultDoc = Loader.loadPDF(merged)) {
            assertThat(resultDoc.getNumberOfPages()).isEqualTo(2);
        }
    }

    @Test
    @DisplayName("Should merge multiple PDF files from disk into target destination")
    void testMergeFiles(@TempDir Path tempDir) throws IOException {
        PdfRenderer renderer = new PdfRenderer();

        Document doc1 = new Document("File 1");
        doc1.addElement(new TextBlock("Page 1"));
        File f1 = tempDir.resolve("doc1.pdf").toFile();
        renderer.renderToFile(doc1, f1);

        Document doc2 = new Document("File 2");
        doc2.addElement(new TextBlock("Page 2"));
        File f2 = tempDir.resolve("doc2.pdf").toFile();
        renderer.renderToFile(doc2, f2);

        File mergedFile = tempDir.resolve("combined.pdf").toFile();
        DocumentMerger.mergeFiles(List.of(f1, f2), mergedFile);

        assertThat(mergedFile).exists();
        assertThat(mergedFile.length()).isGreaterThan(0);

        try (PDDocument resultDoc = Loader.loadPDF(mergedFile)) {
            assertThat(resultDoc.getNumberOfPages()).isEqualTo(2);
        }
    }
}
