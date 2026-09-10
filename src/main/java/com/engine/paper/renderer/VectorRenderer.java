package com.engine.paper.renderer;

import com.engine.paper.domain.model.Document;
import com.engine.paper.domain.model.Page;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * Common rendering interface for exporting layout documents into target formats.
 */
public interface VectorRenderer {

    void render(Document document, OutputStream outputStream) throws IOException;

    void renderPages(List<Page> pages, OutputStream outputStream) throws IOException;

    default byte[] renderToBytes(Document document) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        render(document, baos);
        return baos.toByteArray();
    }

    default void renderToFile(Document document, java.io.File outputFile) throws IOException {
        if (outputFile.getParentFile() != null) {
            outputFile.getParentFile().mkdirs();
        }
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(outputFile)) {
            render(document, fos);
        }
    }
}
