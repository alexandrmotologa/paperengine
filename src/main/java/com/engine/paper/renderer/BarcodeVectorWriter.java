package com.engine.paper.renderer;

import com.engine.paper.domain.element.BarcodeElement;
import com.engine.paper.domain.model.Color;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Pure vector barcode generator translating ZXing bit matrices into resolution-independent
 * vector paths directly within PDFBox and SVG streams. Zero rasterization overhead.
 */
public class BarcodeVectorWriter {

    private static final MultiFormatWriter WRITER = new MultiFormatWriter();

    /**
     * Encodes payload into a minimal BitMatrix according to BarcodeType.
     */
    public static BitMatrix encode(BarcodeElement barcode) throws WriterException {
        BarcodeFormat format = mapFormat(barcode.getBarcodeType());
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 0);

        if (format == BarcodeFormat.QR_CODE) {
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            return WRITER.encode(barcode.getPayload(), format, 0, 0, hints);
        } else {
            // 1D barcodes (Code 128, EAN 13, etc.) - request 1-row height
            return WRITER.encode(barcode.getPayload(), format, 0, 1, hints);
        }
    }

    /**
     * Draws the bit matrix directly into the PDFBox page content stream using vector rectangle paths.
     */
    public static void drawPdfVector(PDPageContentStream stream, BarcodeElement barcode,
                                     double pdfX, double pdfY, double width, double height) throws IOException, WriterException {
        BitMatrix matrix = encode(barcode);

        int cols = matrix.getWidth();
        int rows = matrix.getHeight();

        double moduleWidth = width / cols;
        double moduleHeight = height / rows;

        Color bg = barcode.getBackgroundColor();
        if (bg != null && bg.alpha() > 0) {
            stream.setNonStrokingColor(bg.rFloat(), bg.gFloat(), bg.bFloat());
            stream.addRect((float) pdfX, (float) pdfY, (float) width, (float) height);
            stream.fill();
        }

        Color fg = barcode.getForegroundColor() != null ? barcode.getForegroundColor() : Color.BLACK;
        stream.setNonStrokingColor(fg.rFloat(), fg.gFloat(), fg.bFloat());

        // Stream all active barcode modules as vector rectangles in a single path
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (matrix.get(c, r)) {
                    // PDF Y starts from bottom-left
                    double modX = pdfX + (c * moduleWidth);
                    double modY = pdfY + ((rows - 1 - r) * moduleHeight);
                    stream.addRect((float) modX, (float) modY, (float) moduleWidth, (float) moduleHeight);
                }
            }
        }
        stream.fill();
    }

    /**
     * Emits vector SVG paths representing the barcode modules.
     */
    public static String toSvgPath(BarcodeElement barcode, double x, double y, double width, double height) throws WriterException {
        BitMatrix matrix = encode(barcode);

        int cols = matrix.getWidth();
        int rows = matrix.getHeight();

        double moduleWidth = width / cols;
        double moduleHeight = height / rows;

        StringBuilder sb = new StringBuilder();
        Color fg = barcode.getForegroundColor() != null ? barcode.getForegroundColor() : Color.BLACK;

        sb.append("<path d=\"");
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (matrix.get(c, r)) {
                    double modX = x + (c * moduleWidth);
                    double modY = y + (r * moduleHeight);
                    sb.append(String.format("M%.2f,%.2fh%.2fv%.2fh-%.2fz ",
                            modX, modY, moduleWidth, moduleHeight, moduleWidth));
                }
            }
        }
        sb.append(String.format("\" fill=\"%s\" />", fg.toSvgHex()));
        return sb.toString();
    }

    private static BarcodeFormat mapFormat(BarcodeElement.BarcodeType type) {
        return switch (type) {
            case QR_CODE -> BarcodeFormat.QR_CODE;
            case CODE_128 -> BarcodeFormat.CODE_128;
            case EAN_13 -> BarcodeFormat.EAN_13;
            case PDF_417 -> BarcodeFormat.PDF_417;
            case DATA_MATRIX -> BarcodeFormat.DATA_MATRIX;
        };
    }
}
