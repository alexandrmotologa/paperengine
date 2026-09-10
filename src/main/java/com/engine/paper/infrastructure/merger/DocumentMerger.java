package com.engine.paper.infrastructure.merger;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * High-performance PDF document assembly and merger.
 * Combines multiple generated or external PDF files into a single unified document stream.
 */
public class DocumentMerger {

    /**
     * Merges a list of PDF byte arrays into a single output PDF byte array.
     */
    public static byte[] merge(List<byte[]> pdfByteList) throws IOException {
        if (pdfByteList == null || pdfByteList.isEmpty()) {
            return new byte[0];
        }

        List<PDDocument> openDocs = new ArrayList<>();
        try (PDDocument targetDoc = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            for (byte[] pdfBytes : pdfByteList) {
                if (pdfBytes == null || pdfBytes.length == 0) {
                    continue;
                }
                PDDocument srcDoc = Loader.loadPDF(pdfBytes);
                openDocs.add(srcDoc);
                for (PDPage page : srcDoc.getPages()) {
                    targetDoc.importPage(page);
                }
            }

            targetDoc.save(baos);
            return baos.toByteArray();
        } finally {
            for (PDDocument doc : openDocs) {
                try {
                    doc.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    /**
     * Merges multiple PDF files on disk into a single target destination file.
     */
    public static void mergeFiles(List<File> inputFiles, File outputFile) throws IOException {
        if (inputFiles == null || inputFiles.isEmpty()) {
            throw new IllegalArgumentException("No input files provided for merging");
        }

        List<byte[]> byteList = new ArrayList<>();
        for (File f : inputFiles) {
            if (!f.exists() || !f.isFile()) {
                throw new FileNotFoundException("PDF file not found: " + f.getAbsolutePath());
            }
            byteList.add(Files.readAllBytes(f.toPath()));
        }

        byte[] merged = merge(byteList);

        if (outputFile.getParentFile() != null) {
            outputFile.getParentFile().mkdirs();
        }
        Files.write(outputFile.toPath(), merged);
    }
}
