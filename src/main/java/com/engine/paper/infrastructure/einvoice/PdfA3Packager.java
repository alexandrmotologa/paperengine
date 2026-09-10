package com.engine.paper.infrastructure.einvoice;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * Enterprise PDF/A-3 (ISO 19005-3) packager for Factur-X and ZUGFeRD electronic invoices.
 * Embeds structured EN16931 XML documents into PDF catalogs with full XMP conformance metadata.
 */
public class PdfA3Packager {

    public enum InvoiceProfile {
        MINIMUM("MINIMUM"),
        BASIC("BASIC"),
        BASIC_WL("BASIC WL"),
        EN_16931("EN 16931"),
        EXTENDED("EXTENDED");

        private final String code;

        InvoiceProfile(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        public static InvoiceProfile fromString(String val) {
            if (val == null || val.isBlank()) {
                return EN_16931;
            }
            String norm = val.trim().toUpperCase().replace("-", "_").replace(" ", "_");
            for (InvoiceProfile p : values()) {
                if (p.name().equals(norm) || p.code.equalsIgnoreCase(val)) {
                    return p;
                }
            }
            return EN_16931;
        }
    }

    /**
     * Converts a standard PDF into a PDF/A-3 compliant Factur-X e-invoice with embedded XML.
     */
    public static byte[] packageFacturX(byte[] pdfBytes, byte[] xmlBytes, InvoiceProfile profile) throws IOException {
        try (PDDocument doc = LoaderHelper.loadDoc(pdfBytes);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            packageDocument(doc, xmlBytes, "factur-x.xml", profile);
            doc.save(out);
            return out.toByteArray();
        }
    }

    /**
     * Packages file on disk.
     */
    public static void packageFacturX(File pdfInput, File xmlInput, File pdfOutput, InvoiceProfile profile) throws IOException {
        byte[] pdfBytes = Files.readAllBytes(pdfInput.toPath());
        byte[] xmlBytes = Files.readAllBytes(xmlInput.toPath());
        byte[] packaged = packageFacturX(pdfBytes, xmlBytes, profile);
        if (pdfOutput.getParentFile() != null) {
            pdfOutput.getParentFile().mkdirs();
        }
        Files.write(pdfOutput.toPath(), packaged);
    }

    /**
     * Attaches any generic auxiliary file (e.g. CSV, XML, JSON) according to PDF/A-3 specification.
     */
    public static byte[] attachFile(byte[] pdfBytes, String filename, byte[] fileBytes, String mimeType, String relationship) throws IOException {
        try (PDDocument doc = LoaderHelper.loadDoc(pdfBytes);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            embedFile(doc, filename, fileBytes, mimeType, relationship != null ? relationship : "Supplement");
            doc.save(out);
            return out.toByteArray();
        }
    }

    private static void packageDocument(PDDocument doc, byte[] xmlBytes, String filename, InvoiceProfile profile) throws IOException {
        PDDocumentCatalog catalog = doc.getDocumentCatalog();

        // 1. Embed structured XML file
        PDComplexFileSpecification fileSpec = embedFile(doc, filename, xmlBytes, "text/xml", "Alternative");

        // 2. Set /AF array in Document Catalog (ISO 19005-3 requirement)
        COSArray afArray = new COSArray();
        afArray.add(fileSpec.getCOSObject());
        catalog.getCOSObject().setItem(COSName.getPDFName("AF"), afArray);

        // 3. Inject PDF/A-3b & Factur-X / ZUGFeRD XMP metadata
        injectFacturXXmp(doc, profile, filename);
    }

    private static PDComplexFileSpecification embedFile(PDDocument doc, String filename, byte[] data, String mimeType, String relationship) throws IOException {
        PDDocumentCatalog catalog = doc.getDocumentCatalog();

        PDEmbeddedFile embeddedFile = new PDEmbeddedFile(doc, new ByteArrayInputStream(data));
        embeddedFile.setSubtype(mimeType);
        embeddedFile.setSize(data.length);
        embeddedFile.setModDate(Calendar.getInstance());

        PDComplexFileSpecification fs = new PDComplexFileSpecification();
        fs.setFile(filename);
        fs.setFileUnicode(filename);
        fs.setEmbeddedFile(embeddedFile);

        // Set AFRelationship attribute (ISO 19005-3)
        COSDictionary fsDict = fs.getCOSObject();
        fsDict.setName(COSName.getPDFName("AFRelationship"), relationship);

        // Add to EmbeddedFiles name tree in catalog
        PDDocumentNameDictionary names = catalog.getNames();
        if (names == null) {
            names = new PDDocumentNameDictionary(catalog);
            catalog.setNames(names);
        }

        PDEmbeddedFilesNameTreeNode efTree = names.getEmbeddedFiles();
        if (efTree == null) {
            efTree = new PDEmbeddedFilesNameTreeNode();
            names.setEmbeddedFiles(efTree);
        }

        Map<String, PDComplexFileSpecification> fileMap = efTree.getNames();
        if (fileMap == null) {
            fileMap = new LinkedHashMap<>();
        } else {
            fileMap = new LinkedHashMap<>(fileMap);
        }
        fileMap.put(filename, fs);
        efTree.setNames(fileMap);

        return fs;
    }

    private static void injectFacturXXmp(PDDocument doc, InvoiceProfile profile, String filename) throws IOException {
        String conformanceLevel = profile != null ? profile.getCode() : "EN 16931";

        String xmp = """
                <?xpacket begin="\uFEFF" id="W5M0MpCehiHzreSzNTczkc9d"?>
                <x:xmpmeta xmlns:x="adobe:ns:meta/">
                  <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                    <rdf:Description rdf:about="" xmlns:pdfaid="http://www.aiim.org/pdfa/ns/id/">
                      <pdfaid:part>3</pdfaid:part>
                      <pdfaid:conformance>B</pdfaid:conformance>
                    </rdf:Description>
                    <rdf:Description rdf:about="" xmlns:zf="urn:zugferd:pdfa:CrossIndustryDocument:invoice:2p0#">
                      <zf:ConformanceLevel>%s</zf:ConformanceLevel>
                      <zf:DocumentType>INVOICE</zf:DocumentType>
                      <zf:DocumentFileName>%s</zf:DocumentFileName>
                      <zf:Version>1.0</zf:Version>
                    </rdf:Description>
                    <rdf:Description rdf:about="" xmlns:pdf="http://ns.adobe.com/pdf/1.3/">
                      <pdf:Producer>PaperEngine 1.0.0 (Java 21 LTS)</pdf:Producer>
                    </rdf:Description>
                    <rdf:Description rdf:about="" xmlns:dc="http://purl.org/dc/elements/1.1/">
                      <dc:title><rdf:Alt><rdf:li xml:lang="x-default">Factur-X / ZUGFeRD E-Invoice</rdf:li></rdf:Alt></dc:title>
                      <dc:creator><rdf:Seq><rdf:li>PaperEngine</rdf:li></rdf:Seq></dc:creator>
                    </rdf:Description>
                  </rdf:RDF>
                </x:xmpmeta>
                <?xpacket end="w"?>
                """.formatted(conformanceLevel, filename);

        PDMetadata metadata = new PDMetadata(doc);
        metadata.importXMPMetadata(xmp.getBytes(StandardCharsets.UTF_8));
        doc.getDocumentCatalog().setMetadata(metadata);
    }

    /**
     * Internal helper to load PDDocument in PDFBox 3.0.
     */
    static class LoaderHelper {
        static PDDocument loadDoc(byte[] bytes) throws IOException {
            return org.apache.pdfbox.Loader.loadPDF(bytes);
        }
    }
}
