package com.engine.paper.domain.element;

import com.engine.paper.domain.layout.BoxDimensions;
import com.engine.paper.domain.model.Color;
import com.engine.paper.domain.style.ElementStyle;

/**
 * High-resolution vector barcode or QR code element rendered directly into vector streams.
 */
public class BarcodeElement implements DocumentElement {

    public enum BarcodeType {
        QR_CODE,
        CODE_128,
        EAN_13,
        PDF_417,
        DATA_MATRIX
    }

    private final BoxDimensions dimensions = new BoxDimensions();
    private ElementStyle style = new ElementStyle();
    private BarcodeType barcodeType = BarcodeType.QR_CODE;
    private String payload = "";
    private Color foregroundColor = Color.BLACK;
    private Color backgroundColor = Color.TRANSPARENT;

    public BarcodeElement(BarcodeType barcodeType, String payload, double width, double height) {
        this.barcodeType = barcodeType;
        this.payload = payload != null ? payload : "";
        this.style.setWidth(width);
        this.style.setHeight(height);
        this.dimensions.setContentWidth(width);
        this.dimensions.setContentHeight(height);
    }

    public static BarcodeElement qr(String payload, double size) {
        return new BarcodeElement(BarcodeType.QR_CODE, payload, size, size);
    }

    public static BarcodeElement code128(String payload, double width, double height) {
        return new BarcodeElement(BarcodeType.CODE_128, payload, width, height);
    }

    @Override
    public ElementType getType() {
        return ElementType.BARCODE;
    }

    @Override
    public BoxDimensions getDimensions() {
        return dimensions;
    }

    @Override
    public ElementStyle getStyle() {
        return style;
    }

    @Override
    public void setStyle(ElementStyle style) {
        this.style = style != null ? style : new ElementStyle();
    }

    public BarcodeType getBarcodeType() {
        return barcodeType;
    }

    public void setBarcodeType(BarcodeType barcodeType) {
        this.barcodeType = barcodeType != null ? barcodeType : BarcodeType.QR_CODE;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload != null ? payload : "";
    }

    public Color getForegroundColor() {
        return foregroundColor;
    }

    public void setForegroundColor(Color foregroundColor) {
        this.foregroundColor = foregroundColor != null ? foregroundColor : Color.BLACK;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor != null ? backgroundColor : Color.TRANSPARENT;
    }
}
